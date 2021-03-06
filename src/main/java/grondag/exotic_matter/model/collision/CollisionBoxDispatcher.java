package grondag.exotic_matter.model.collision;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import grondag.exotic_matter.cache.ObjectSimpleCacheLoader;
import grondag.exotic_matter.cache.ObjectSimpleLoadingCache;
import grondag.exotic_matter.model.state.ISuperModelState;
import net.minecraft.util.math.AxisAlignedBB;

public class CollisionBoxDispatcher
{
    static final BlockingQueue<Runnable> QUEUE = new LinkedBlockingQueue<Runnable>();
    private static final ExecutorService EXEC = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            QUEUE,
            new ThreadFactory()
            {
                private AtomicInteger count = new AtomicInteger(1);
                @Override
                public Thread newThread(@Nullable Runnable r)
                {
                    Thread thread = new Thread(r, "Exotic Matter Collision Box Optimizer - " + count.getAndIncrement());
                    thread.setDaemon(true);
                    thread.setPriority(Thread.NORM_PRIORITY - 2);
                    return thread;
                }
            })
            {
                @Override
                protected void finalize()
                {
                    super.finalize();
                    shutdown();
                }
            };
    
    private static final ObjectSimpleLoadingCache<ISuperModelState, OptimizingBoxList> modelBounds = new ObjectSimpleLoadingCache<ISuperModelState, OptimizingBoxList>(new CollisionBoxLoader(),  0xFFF);

    private static ThreadLocal<FastBoxGenerator> fastBoxGen = new ThreadLocal<FastBoxGenerator>()
    {
        @Override
        protected FastBoxGenerator initialValue()
        {
            return new FastBoxGenerator();
        }
    };
    
    public static ImmutableList<AxisAlignedBB> getCollisionBoxes(ISuperModelState modelState)
    {
        return modelBounds.get(modelState.geometricState()).getList();
    }
    
    /**
     * Clears the cache.
     */
    public static void clear()
    {
        modelBounds.clear();
        QUEUE.clear();
    }
    
    private static class CollisionBoxLoader implements ObjectSimpleCacheLoader<ISuperModelState, OptimizingBoxList>
    {
//        static AtomicInteger runCounter = new AtomicInteger();
//        static AtomicLong totalNanos = new AtomicLong();
        
        @Override
        public OptimizingBoxList load(ISuperModelState key)
        {
//            final long start = System.nanoTime();
            
            final FastBoxGenerator generator = fastBoxGen.get();
            key.getShape().meshFactory().produceShapeQuads(key, generator);
            
            // note that build clears for next use
            OptimizingBoxList result = new OptimizingBoxList(generator.build(), key);
            EXEC.execute(result);
            
//            long total = totalNanos.addAndGet(System.nanoTime() - start);
//            if(runCounter.incrementAndGet() == 100)
//            {
//                ExoticMatter.INSTANCE.info("Avg fast collision box nanos, past 100 samples = %d", total / 100);
//                runCounter.addAndGet(-100);
//                totalNanos.addAndGet(-total);
//            }
            
            return result;
        }
    }
}
