package grondag.exotic_matter.statecache;

import javax.annotation.Nullable;

import grondag.exotic_matter.block.ISuperBlock;
import grondag.exotic_matter.model.state.ISuperModelState;
import grondag.exotic_matter.terrain.TerrainState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.IBlockAccess;
import scala.actors.threadpool.Arrays;


/**
 * Cache for a single 16x16x16 region.
 */
public class WorldStateNibble implements IWorldStateCache
{
    private @Nullable ISuperModelState[] modelStates;
    private @Nullable byte[] flowHeights;
    
//    private static AtomicInteger terrainLookups = new AtomicInteger(0);
//    private static AtomicInteger terrainHits = new AtomicInteger(0);
//    private static AtomicInteger modelLookups = new AtomicInteger(0);
//    private static AtomicInteger modelHits = new AtomicInteger(0);
    
    @Override
    public ISuperModelState getModelState(ISuperBlock block, IBlockAccess world, IBlockState blockState, BlockPos pos, boolean refreshFromWorld)
    {
        ISuperModelState result;
        final int index =  computeIndex(pos);
        
        if(modelStates == null)
        {
            modelStates = new ISuperModelState[4096];
            result = ISuperBlock.computeModelState(block, world, blockState, pos, refreshFromWorld);
            
            // don't save in cache if not being refreshed - don't want to cache stale states
            if(refreshFromWorld) modelStates[index] = result;
        }
        else
        {
            result = modelStates[index];
            if(result == null)
            {
                result = ISuperBlock.computeModelState(block, world, blockState, pos, refreshFromWorld);
                // don't save in cache if not being refreshed - don't want to cache stale states
                if(refreshFromWorld) modelStates[index] = result;
            }
            else
            {
                if(refreshFromWorld) result.refreshFromWorld(blockState, world, pos);
//                modelHits.incrementAndGet();
            }
        }
        
//        if((modelLookups.incrementAndGet() & 0xFFF) == 0xFFF) System.out.println("World state cache model hit rate = " + modelHits.get() / (float) modelLookups.get()); 
        
        return result;
    }
    
    @Override
    public int getFlowHeight(IBlockAccess world, MutableBlockPos pos)
    {
        int result;
        final int index =  computeIndex(pos);
        
        if(flowHeights == null)
        {
            flowHeights = new byte[4096];
            Arrays.fill(flowHeights, Byte.MIN_VALUE);
            result = TerrainState.getFlowHeight(world, pos);
            flowHeights[index] = (byte) result;
        }
        else
        {
            result = flowHeights[index];
            if(result == Byte.MIN_VALUE)
            {
                result = TerrainState.getFlowHeight(world, pos);
                flowHeights[index] = (byte) result;
            }
//            else terrainHits.incrementAndGet();
        }
        
//        if((terrainLookups.incrementAndGet() & 0xFFFF) == 0xFFFF) System.out.println("World state cache terrain flow height hit rate = " + terrainHits.get() / (float) terrainLookups.get()); 

        return result;
    }
    
    private int computeIndex(BlockPos pos)
    {
        return (pos.getX() & 0xF) | ((pos.getY() & 0xF) << 4) | ((pos.getZ() & 0xF) << 8);
    }


}
