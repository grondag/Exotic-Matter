package grondag.exotic_matter.statecache;

import grondag.exotic_matter.model.ISuperBlock;
import grondag.exotic_matter.model.ISuperModelState;
import grondag.exotic_matter.model.TerrainState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.IBlockAccess;

public class FakeWorldStateCache extends AbstractWorldStateCache
{
    @Override
    public ISuperModelState getModelState(ISuperBlock block, IBlockAccess world, IBlockState blockState, BlockPos pos, boolean refreshFromWorld)
    {
        return ISuperBlock.computeModelState(block, world, blockState, pos, refreshFromWorld);
    }

    @Override
    public int getFlowHeight(IBlockAccess world, MutableBlockPos pos)
    {
        return TerrainState.getFlowHeight(world, pos);
    }
    
    @Override
    protected void clear()
    {
        // NOOP
    }

    @Override
    protected void invalidateNibble(int chunkX, int nibbleY, int chunkZ)
    {
        // NOOP
    }

}
