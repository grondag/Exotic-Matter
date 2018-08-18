package grondag.exotic_matter.terrain;

import static it.unimi.dsi.fastutil.HashCommon.arraySize;

import java.util.function.Supplier;

import grondag.exotic_matter.block.ISuperBlockAccess;
import grondag.exotic_matter.world.PackedBlockPos;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Caches expensive world state lookups until next prepare() call.
 * For server-side use on server thread only. Doesn't try to track 
 * state changes while not in active use, and all state changes
 * must be single threaded and occur through this instance.
 * 
 * TODO: add caching for flow height
 */
public class TerrainWorldAdapter implements ISuperBlockAccess
{
    protected World world;
    
    @SuppressWarnings("serial")
    
    public static class FastMap<V> extends Long2ObjectOpenHashMap<V>
    {
        /** 
         * DOES NOT SUPPORT ZERO-VALUED KEYS<p>
         * 
         * Only computes key 1x and scans arrays 1x for a modest savings.
         * Here because block updates are the on-tick performance bottleneck for lava sim.
         */
        private V computeFast(final long k, final Supplier<V> v)
        {
            final long[] key = this.key;
            int pos = (int) it.unimi.dsi.fastutil.HashCommon.mix(k) & mask;
            long curr = key[pos];
            
            // The starting point.
            if (curr != 0)
            {
                if (curr == k) return value[pos];
                while (!((curr = key[pos = (pos + 1) & mask]) == (0)))
                    if (curr == k) return value[pos];
            }
            
            final V result = v.get();
            key[pos] = k;
            value[pos] = result;
            if (size++ >= maxFill) rehash(arraySize(size + 1, f));
            return result;
        }
    }
    protected FastMap<IBlockState> blockStates = new FastMap<>();
    protected FastMap<TerrainState> terrainStates = new FastMap<>();
    
    @SuppressWarnings("null")
    public TerrainWorldAdapter()
    {
        
    }
    
    @SuppressWarnings("null")
    public TerrainWorldAdapter(World world)
    {
        this.prepare(world);
    }
    
    public void prepare(World world)
    {
        this.world = world;
        this.blockStates.clear();
        this.terrainStates.clear();
    }
    
    @Override
    public IBlockAccess wrapped()
    {
        return this.world;
    }
    
    @Override
    public IBlockState getBlockState(final BlockPos pos)
    {
        long packedBlockPos = PackedBlockPos.pack(pos);
        return blockStates.computeFast(packedBlockPos, () -> world.getBlockState(pos));
    }
    
    private final MutableBlockPos getBlockPos = new MutableBlockPos();
    @Override
    public IBlockState getBlockState(long packedBlockPos)
    {
        return blockStates.computeFast(packedBlockPos, () -> 
        { 
            PackedBlockPos.unpackTo(packedBlockPos, getBlockPos);
            return world.getBlockState(getBlockPos);
        });
    }

    private final MutableBlockPos getTerrainPos = new MutableBlockPos();
    @Override
    public TerrainState terrainState(IBlockState state, long packedBlockPos)
    {
        return terrainStates.computeFast(packedBlockPos, () -> 
        { 
            PackedBlockPos.unpackTo(packedBlockPos, getTerrainPos);
            return TerrainState.terrainState(this, state, getTerrainPos);
        });
    }

    @Override
    public TerrainState terrainState(IBlockState state, BlockPos pos)
    {
        return terrainStates.computeFast(PackedBlockPos.pack(pos), () -> 
        { 
            return TerrainState.terrainState(this, state, pos);
        });
    }
    
    /**
     * WARNING - do not use to set flow height blocks.
     * Assumes terrain states in cache will not change as a result of any block state changes.
     */
    public void setBlockState(long packedBlockPos, IBlockState newState)
    {
        world.setBlockState(PackedBlockPos.unpack(packedBlockPos), newState);
        blockStates.put(packedBlockPos, newState);
        for(int x = -1; x <= 1; x++)
        {
            for(int z = -1; z <= 1; z++)
            {
                for(int y = -2; y <= 2; y++)
                {
                    terrainStates.remove(PackedBlockPos.add(packedBlockPos, x, y, z));
                }
            }
        }
    }
    
    public void setBlockState(BlockPos blockPos, IBlockState newState)
    {
        this.setBlockState(PackedBlockPos.pack(blockPos), newState);
    }
}
