package grondag.exotic_matter.model.collision;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ForwardingList;

import grondag.exotic_matter.ExoticMatter;
import grondag.exotic_matter.model.state.ISuperModelState;
import net.minecraft.util.math.AxisAlignedBB;

public class OptimizingBoxList extends ForwardingList<AxisAlignedBB> implements Runnable
{
    // singleton is fine because called from a single thread
    private static final OptimalBoxGenerator boxGen = new OptimalBoxGenerator();
    
    private List<AxisAlignedBB> wrapped;
    private @Nullable ISuperModelState modelState;
    
    OptimizingBoxList(List<AxisAlignedBB> initialList, ISuperModelState modelState)
    {
        this.wrapped = initialList;
        this.modelState = modelState;
    }
    
    @Override
    protected List<AxisAlignedBB> delegate()
    {
        return wrapped;
    }

    @SuppressWarnings("null")
    @Override
    public void run()
    {
        final OptimalBoxGenerator generator = boxGen;
//        final int startCount = this.wrapped.size();
        
        generator.prepare();
        modelState.getShape().meshFactory().produceShapeQuads(modelState, generator);
        wrapped = generator.build();
        modelState = null;
        
//        ExoticMatter.INSTANCE.info("Collision Box Optimization Result: old count = %d, new count = %d", startCount, wrapped.size());
    }
}