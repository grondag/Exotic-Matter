package grondag.exotic_matter.model.collision;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import grondag.exotic_matter.ConfigXM;
import grondag.exotic_matter.model.state.ISuperModelState;
import grondag.exotic_matter.varia.Useful;
import net.minecraft.util.math.AxisAlignedBB;

public class OptimizingBoxList implements Runnable
{
    // singleton is fine because called from a single thread
    private static final OptimalBoxGenerator boxGen = new OptimalBoxGenerator();
    
    private ImmutableList<AxisAlignedBB> wrapped;
    private @Nullable ISuperModelState modelState;
    
    OptimizingBoxList(ImmutableList<AxisAlignedBB> initialList, ISuperModelState modelState)
    {
        this.wrapped = initialList;
        this.modelState = modelState;
    }
    
    protected ImmutableList<AxisAlignedBB> getList()
    {
        return wrapped;
    }

    @SuppressWarnings("null")
    @Override
    public void run()
    {
        final OptimalBoxGenerator generator = boxGen;
        modelState.getShape().meshFactory().produceShapeQuads(modelState, generator);

        final int oldSize = wrapped.size();
        double oldVolume = Useful.volumeAABB(wrapped);
        double trueVolume = generator.prepare();
        if(trueVolume == 0)
            assert oldSize == 0 : "Fast collision box non-empty but detailed is empty";
        else
        {
            if(oldSize > ConfigXM.BLOCKS.collisionBoxBudget || Math.abs(trueVolume - oldVolume) > 1.0 / OptimalBoxGenerator.VOXEL_VOLUME)
                wrapped = generator.build();
        }
        modelState = null;
    }
}
