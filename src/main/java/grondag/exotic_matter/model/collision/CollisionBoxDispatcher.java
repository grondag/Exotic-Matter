package grondag.exotic_matter.model.collision;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import grondag.acuity.RunTimer;
import grondag.exotic_matter.ExoticMatter;
import grondag.exotic_matter.cache.ObjectSimpleCacheLoader;
import grondag.exotic_matter.cache.ObjectSimpleLoadingCache;
import grondag.exotic_matter.model.state.ISuperModelState;
import net.minecraft.util.math.AxisAlignedBB;

public class CollisionBoxDispatcher
{
    public static final CollisionBoxDispatcher INSTANCE = new CollisionBoxDispatcher();
    
    //TODO: give thread descriptive name, make low priority
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    
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
        //TODO: disable
        static AtomicInteger runCounter = new AtomicInteger();
        static AtomicLong totalNanos = new AtomicLong();
        
        @Override
        public List<AxisAlignedBB> load(ISuperModelState key)
        {
            final long start = System.nanoTime();
            
            final FastBoxGenerator generator = fastBoxGen.get();
            generator.prepare();
            key.getShape().meshFactory().produceShapeQuads(key, generator);
            OptimizingBoxList result = new OptimizingBoxList(generator.build(), key);
            EXEC.execute(result);
            
            long total = totalNanos.addAndGet(System.nanoTime() - start);
            if(runCounter.incrementAndGet() == 100)
            {
                ExoticMatter.INSTANCE.info("Avg fast collision box nanos, past 100 samples = %d", total / 100);
                runCounter.addAndGet(-100);
                totalNanos.addAndGet(-total);
            }
            
            return result;
        }
    }
}
