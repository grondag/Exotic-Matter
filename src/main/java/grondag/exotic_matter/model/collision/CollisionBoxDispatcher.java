package grondag.exotic_matter.model.collision;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;

import grondag.exotic_matter.ExoticMatter;
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
                    thread.setPriority(Thread.MIN_PRIORITY);
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
    
    private static final ObjectSimpleLoadingCache<ISuperModelState, List<AxisAlignedBB>> modelBounds = new ObjectSimpleLoadingCache<ISuperModelState, List<AxisAlignedBB>>(new CollisionBoxLoader(),  0xFFF);

    private static ThreadLocal<FastBoxGenerator> fastBoxGen = new ThreadLocal<FastBoxGenerator>()
    {
        @Override
        protected FastBoxGenerator initialValue()
        {
            return new FastBoxGenerator();
        }
    };
    
    public static List<AxisAlignedBB> getCollisionBoxes(ISuperModelState modelState)
    {
        return modelBounds.get(modelState.geometricState());
    }
    
    /**
     * Clears the cache.
     */
    public static void clear()
    {
        modelBounds.clear();
        QUEUE.clear();
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
