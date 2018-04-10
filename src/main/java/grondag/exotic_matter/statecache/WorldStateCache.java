package grondag.exotic_matter.statecache;

import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import grondag.exotic_matter.model.ISuperBlock;
import grondag.exotic_matter.model.ISuperModelState;
import grondag.exotic_matter.varia.PackedBlockPos;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class WorldStateCache extends AbstractWorldStateCache
{
    @Nullable protected World world;
    
    public void setWorld(@Nullable World world)
    {
        if(this.world != null) this.world.removeEventListener(this);
        this.world = world;
        if(world != null) world.addEventListener(this);
        this.clear();
    }
    
    private final ConcurrentHashMap<Long, WorldStateNibble> nibbles = new ConcurrentHashMap<>();

    @SuppressWarnings("null")
    private WorldStateNibble getNibble(BlockPos pos)
    {
        Long key = computeKey(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
        return this.nibbles.computeIfAbsent(key, k -> new WorldStateNibble());
    }
    
    @Override
    public ISuperModelState getModelState(ISuperBlock block, IBlockAccess world, IBlockState blockState, BlockPos pos)
    {
        return this.getNibble(pos).getModelState(block, world, blockState, pos);
    }

    @Override
    public int getFlowHeight(IBlockAccess world, MutableBlockPos pos)
    {
        return this.getNibble(pos).getFlowHeight(world, pos);
    }
    
    @Override
    protected void clear()
    {
        this.nibbles.clear();
    }

    @Override
    protected void invalidateNibble(int chunkX, int nibbleY, int chunkZ)
    {
        this.nibbles.remove(computeKey(chunkX, nibbleY, chunkZ));
    }

    private long computeKey(int chunkX, int nibbleY, int chunkZ)
    {
        return PackedBlockPos.pack(chunkX, nibbleY, chunkZ);
    }
}
