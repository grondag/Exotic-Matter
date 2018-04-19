package grondag.exotic_matter.statecache;

import grondag.exotic_matter.model.ISuperBlock;
import grondag.exotic_matter.model.ISuperModelState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Non-static model states depend on world state and in some cases deriving the world-dependent
 * components can be computationally expensive, typically because of many block states lookups. <p>
 * 
 * Superblocks that have a tile entity can minimize this cost by caching the model state in the tile entity.
 * But we do not want to add tile entities for simple blocks that don't need to persist state because
 * tile entities come with their own problems and overhead.<p>
 * 
 * Terrain blocks are a special case that have the same problem of model state overhead compounded
 * by the needed to compute terrain height for each neighboring column, which in turn requires
 * the lookup of many block states for each neighbor.<p>
 * 
 */
public interface IWorldStateCache
{
    ISuperModelState getModelState(ISuperBlock block, IBlockAccess world, IBlockState blockState, BlockPos pos, boolean refreshFromWorld);
    
    int getFlowHeight(IBlockAccess world, MutableBlockPos pos);
}
