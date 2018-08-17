package grondag.exotic_matter.terrain;

import javax.annotation.Nullable;

import grondag.exotic_matter.world.PackedBlockPos;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;

/**
 * Caches expensive world state lookups until next prepare() call.
 */
public class TerrainWorldAdapter implements IBlockAccess
{
    protected World world;
    
    protected Long2ObjectOpenHashMap<IBlockState> blockStates = new Long2ObjectOpenHashMap<>();
    protected Long2ObjectOpenHashMap<TerrainState> terrainStates = new Long2ObjectOpenHashMap<>();
    
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
    public IBlockState getBlockState(BlockPos pos)
    {
        long packedBlockPos = PackedBlockPos.pack(pos);
        IBlockState result = blockStates.get(packedBlockPos);
        if(result == null)
        {
            result = world.getBlockState(pos);
            blockStates.put(packedBlockPos, result);
        }
        return result;
    }
    
    private final MutableBlockPos getBlockPos = new MutableBlockPos();
    public IBlockState getBlockState(long packedBlockPos)
    {
        IBlockState result = blockStates.get(packedBlockPos);
        if(result == null)
        {
            PackedBlockPos.unpackTo(packedBlockPos, getBlockPos);
            result = world.getBlockState(getBlockPos);
            blockStates.put(packedBlockPos, result);
        }
        return result;
    }

    private final MutableBlockPos getTerrainPos = new MutableBlockPos();
    
    public TerrainState terrainState(IBlockState state, long packedBlockPos)
    {
        TerrainState result = terrainStates.get(packedBlockPos);
        if(result == null)
        {
            PackedBlockPos.unpackTo(packedBlockPos, getTerrainPos);
            result = TerrainBlockHelper.getTerrainState(state, this, getTerrainPos);
            terrainStates.put(packedBlockPos, result);
        }
        return result;
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
    
    @Override
    @Nullable
    public TileEntity getTileEntity(BlockPos pos)
    {
        return world.getTileEntity(pos);
    }

    @Override
    public int getCombinedLight(BlockPos pos, int lightValue)
    {
        return world.getCombinedLight(pos, lightValue);
    }

    @Override
    public boolean isAirBlock(BlockPos pos)
    {
        IBlockState state = this.getBlockState(pos);
        return state.getBlock().isAir(state, this, pos);
    }

    @Override
    public Biome getBiome(BlockPos pos)
    {
        return world.getBiome(pos);
    }

    @Override
    public int getStrongPower(BlockPos pos, EnumFacing direction)
    {
        return world.getStrongPower(pos, direction);
    }

    @Override
    public WorldType getWorldType()
    {
        return world.getWorldType();
    }

    @Override
    public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default)
    {
        return world.isSideSolid(pos, side, _default); 
    }
}
