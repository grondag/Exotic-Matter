package grondag.exotic_matter;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.junit.Test;

import grondag.exotic_matter.concurrency.SimpleConcurrentList;
import grondag.exotic_matter.concurrency.SimpleThreadPoolExecutor;
import grondag.exotic_matter.concurrency.SimpleThreadPoolExecutor.ArrayTask;

public class ThreadPoolTest
{
    SimpleConcurrentList<TestSubject> bigThings =  SimpleConcurrentList.create(TestSubject.class, false, "blort", null);
    
    SimpleConcurrentList<TestSubject> smallThings =  SimpleConcurrentList.create(TestSubject.class, false, "blort", null);
    
    {
        for(int i = 0; i < 1000000; i++)
        {
            bigThings.add(new TestSubject());
        }
        
        for(int i = 0; i < 100; i++)
        {
            smallThings.add(new TestSubject());
        }
    }
    
    private class TestSubject 
    {
        private int data;
        
        public void doSomething()
        {
            data++;
        }
    }
    
    
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
    
    final SimpleThreadPoolExecutor SIMPLE_POOL = new SimpleThreadPoolExecutor();
    
    
    
    @Test
    public void test() throws InterruptedException, ExecutionException
    {
        
        System.out.println("Warm ups");
        for(int i = 0; i < 10; i++)
        {
            SIMPLE_POOL.completeTask(new ArrayTask<>(smallThings.getOperands(), t -> t.doSomething(), 0, smallThings.size()));
            this.SIMULATION_POOL.submit(() -> smallThings.stream(true).forEach(t -> t.doSomething())).get();
            SIMPLE_POOL.completeTask(new ArrayTask<>(bigThings.getOperands(), t -> t.doSomething(), 0, bigThings.size()));
            this.SIMULATION_POOL.submit(() -> bigThings.stream(true).forEach(t -> t.doSomething())).get();
        }
        System.out.println("");
        System.out.println("");
        
        
        long iSmall = 0;
        long iBig = 0;
        long nanosSmallSimple = 0;
        long nanosSmallStream = 0;
        long nanosBigSimple = 0;
        long nanosBigStream = 0;
        
        while(true)
        {
            long start = System.nanoTime();
            SIMPLE_POOL.completeTask(new ArrayTask<>(smallThings.getOperands(), t -> t.doSomething(), 0, smallThings.size()));
            long end = System.nanoTime();
            nanosSmallSimple += (end - start);
            
            start = System.nanoTime();
            this.SIMULATION_POOL.submit(() -> smallThings.stream(true).forEach(t -> t.doSomething())).get();
            end = System.nanoTime();
            nanosSmallStream += (end - start);
            
            start = System.nanoTime();
            SIMPLE_POOL.completeTask(new ArrayTask<>(bigThings.getOperands(), t -> t.doSomething(), 0, bigThings.size()));
            end = System.nanoTime();
            nanosBigSimple += (end - start);
            
            start = System.nanoTime();
            this.SIMULATION_POOL.submit(() -> bigThings.stream(true).forEach(t -> t.doSomething())).get();
            end = System.nanoTime();
            nanosBigStream += (end - start);
            
            iSmall += this.smallThings.size();
            iBig += this.bigThings.size();
            System.out.println("Avg Small Stream = "  + nanosSmallStream / (double)iSmall);
            System.out.println("Avg Small Simple = "  + nanosSmallSimple / (double)iSmall);
            System.out.println("Avg Big Stream = "  + nanosBigStream / (double)iBig);
            System.out.println("Avg Big Simple = "  + nanosBigSimple / (double)iBig);
            System.out.println("");
        }
    }
}