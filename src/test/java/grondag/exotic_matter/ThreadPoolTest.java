package grondag.exotic_matter;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.junit.Test;

import grondag.exotic_matter.concurrency.StupidThreadPoolExecutor;
import grondag.exotic_matter.concurrency.StupidThreadPoolExecutor.IIndexedRunnable;
import net.minecraft.util.math.Vec3i;

public class ThreadPoolTest
{
    final int SIZE = 50000000;
    Vec3i things[] = new Vec3i[SIZE];
    final Random r = new Random(5);
    StupidThreadPoolExecutor stupid = new StupidThreadPoolExecutor();
    
    final ForkJoinPool SIMULATION_POOL = new ForkJoinPool(
            Runtime.getRuntime().availableProcessors(),
            new ForkJoinWorkerThreadFactory()
            {
                private AtomicInteger count = new AtomicInteger(1);

                @Override
                public ForkJoinWorkerThread newThread(@Nullable ForkJoinPool pool)
                {
                    ForkJoinWorkerThread result = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                    result.setName("Exotic Matter Simulation Thread -" + count.getAndIncrement());
                    return result;
                }
            },
            new UncaughtExceptionHandler()
            {
                @Override
                public void uncaughtException(@Nullable Thread t, @Nullable Throwable e)
                {
                    ExoticMatter.INSTANCE.getLog().error("Simulator thread terminated due to uncaught exception.  Badness may ensue.", e);
                }}, 
            true);
    
    public ThreadPoolTest()
    {
        for(int i = 0; i < SIZE; i++)
        {
            things[i] = new Vec3i(r.nextInt(), r.nextInt(), r.nextInt());
        }
    }
    
    
    @Test
    public void test() throws InterruptedException, ExecutionException
    {
        for(int i = 0; i < 7; i++)
        {
            runStreaming();
        }
        
        for(int i = 0; i < 7; i++)
        {
            runStupidly();
        }
    }

    private void runStupidly()
    {
        StupidTask task = new StupidTask();
        System.out.println("Running stupid thread pool");
        long start = System.nanoTime();
        stupid.doThings(task, 0, SIZE);
        long end = System.nanoTime();
        System.out.println("Avg ns per run: " + (end - start) / SIZE);
    }
    
    private void runStreaming() throws InterruptedException, ExecutionException
    {
        System.out.println("Running parallel stream");
        long start = System.nanoTime();
        this.SIMULATION_POOL.submit(() -> Arrays.stream(this.things, 0, SIZE).parallel().forEach(t -> t.getDistance(5, 4, 3))).get();
        long end = System.nanoTime();
        System.out.println("Avg ns per run: " + (end - start) / SIZE);
    }
    
    public class StupidTask implements IIndexedRunnable
    {
        @Override
        public void run(int index)
        {
            things[index].getDistance(5, 4, 3);
        }
    }
}