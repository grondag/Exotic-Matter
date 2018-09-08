package grondag.exotic_matter.model.collision;

import java.util.List;

import grondag.exotic_matter.cache.ObjectSimpleCacheLoader;
import grondag.exotic_matter.cache.ObjectSimpleLoadingCache;
import grondag.exotic_matter.model.state.ISuperModelState;
import net.minecraft.util.math.AxisAlignedBB;

public class CollisionBoxDispatcher
{
    public static final CollisionBoxDispatcher INSTANCE = new CollisionBoxDispatcher();
    
    private final ObjectSimpleLoadingCache<ISuperModelState, List<AxisAlignedBB>> modelBounds = new ObjectSimpleLoadingCache<ISuperModelState, List<AxisAlignedBB>>(new CollisionBoxLoader(),  0xFFF);

    private static ThreadLocal<FastBoxGenerator> fastBoxGen = new ThreadLocal<FastBoxGenerator>()
    {
        @Override
        protected FastBoxGenerator initialValue()
        {
            return new FastBoxGenerator();
        }
    };
    
    public List<AxisAlignedBB> getCollisionBoxes(ISuperModelState modelState)
    {
        return this.modelBounds.get(modelState.geometricState());
    }
    
    private static class CollisionBoxLoader implements ObjectSimpleCacheLoader<ISuperModelState, List<AxisAlignedBB>>
    {
        @Override
        public List<AxisAlignedBB> load(ISuperModelState key)
        {
            final FastBoxGenerator generator = fastBoxGen.get();
            generator.prepare();
            key.getShape().meshFactory().produceShapeQuads(key, generator);
            return generator.build();
        }
    }
}
