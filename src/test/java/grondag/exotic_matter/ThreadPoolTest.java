package grondag.exotic_matter;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;

import org.junit.Test;

import grondag.exotic_matter.concurrency.CountedJob;
import grondag.exotic_matter.concurrency.JobTask;
import grondag.exotic_matter.concurrency.SimpleConcurrentList;
import grondag.exotic_matter.concurrency.StupidThreadPoolExecutor;
import grondag.exotic_matter.concurrency.StupidThreadPoolExecutor.IIndexedRunnable;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class ThreadPoolTest
{
    final int SIZE = 1000000;
    SimpleConcurrentList<TestSubject> things =  SimpleConcurrentList.create(TestSubject.class, false, "blort", null);
    
    TestSubject[] thingArray;
    
    final Random r = new Random(5);
    StupidThreadPoolExecutor stupid = new StupidThreadPoolExecutor();
    
    private final JobTask<TestSubject> jobTask = new JobTask<TestSubject>() 
    {
        @Override
        public void doJobTask(TestSubject operand)
        {
            operand.doSomething();
        }
    };
    
    private CountedJob<TestSubject> job = new CountedJob<TestSubject>(this.things, this.jobTask, 200, 
            false, "Dothings", null);
    
    private final AtomicInteger[] externals = new AtomicInteger[16];
    
    private class TestSubject extends Vec3d
    {
        private final AtomicInteger externalRef;
        
        public TestSubject(double xIn, double yIn, double zIn)
        {
            super(xIn, yIn, zIn);
            this.externalRef = externals[((int)xIn) & 0xF];
        }
        
        public void doSomething()
        {
            // just a bunch of garbage
            final int i = this.externalRef.getAndIncrement();
            Vec3d other = new Vec3d(i, -i, i * 2).normalize();
            this.externalRef.addAndGet((int) Math.abs(this.dotProduct(other)));
            
        }
    }
    
    private void clearExternals()
    {
        for(int i = 0; i < this.externals.length; i++)
        {
            this.externals[i].set(1);
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
    
    public ThreadPoolTest()
    {
        for(int i = 0; i < this.externals.length; i++)
        {
            this.externals[i] = new AtomicInteger();
        }
    }
    
    private void expandList(int howMany)
    {
        for(int i = 0; i < howMany; i++)
        {
            things.add(new TestSubject(r.nextDouble(), r.nextDouble(), r.nextDouble()));
        }
    }
    
    @Test
    public void test() throws InterruptedException, ExecutionException
    {
        this.expandList(1000);
        
        System.out.println("Warm ups");
        for(int i = 0; i < 10; i++)
        {
            this.runSingle();
            this.runParallel();
            this.runJob();
        }
        System.out.println("");
        System.out.println("");
        
        for(int i = 0; i < 1000; i++)
        {
            this.runSingle();
            this.runParallel();
            this.runJob();
            System.out.println("");
            
            this.expandList(1000);
        }
        
        
//        for(int i = 0; i < 7; i++)
//        {
//            runStreaming();
//        }
//        
//        for(int i = 0; i < 7; i++)
//        {
//            runStupidly();
//        }
//        
//        for(int i = 0; i < 7; i++)
//        {
//            runJob();
//        }
//        
//        for(int i = 0; i < 7; i++)
//        {
//            runStreaming();
//        }
    }
    
    private void runSingle()
    {
        System.out.println("Running single thread, count = " + things.size());
        this.clearExternals();
        long start = System.nanoTime();
        things.stream(false).forEach(t -> t.doSomething());
        long end = System.nanoTime();
        System.out.println("Avg ns per run: " + (end - start) / things.size());
    }

    private void runParallel() throws InterruptedException, ExecutionException
    {
        System.out.println("Running parallel, count = " + things.size());
        this.clearExternals();
        long start = System.nanoTime();
        this.SIMULATION_POOL.submit(() -> things.stream(true).forEach(t -> t.doSomething())).get();
        long end = System.nanoTime();
        System.out.println("Avg ns per run: " + (end - start) / things.size());
    }
    
    private void runStupidly()
    {
        StupidTask task = new StupidTask();
        System.out.println("Running stupid thread pool");
        this.thingArray = things.getOperands();
        long start = System.nanoTime();
        stupid.doThings(task, 0, SIZE);
        long end = System.nanoTime();
        System.out.println("Avg ns per run: " + (end - start) / SIZE);
    }
    
    private void runStreaming() throws InterruptedException, ExecutionException
    {
        System.out.println("Running parallel stream");
        long start = System.nanoTime();
        if(things.size() > 1600)
            this.SIMULATION_POOL.submit(() -> things.stream(true).forEach(t -> t.doSomething())).get();
        else
            things.stream(false).forEach(t -> t.doSomething());
            
        long end = System.nanoTime();
        System.out.println("Avg ns per run: " + (end - start) / SIZE);
    }
    
    private void runJob()
    {
        System.out.println("Running counted job, count = " + things.size());
        long start = System.nanoTime();
        this.job.runOn(SIMULATION_POOL);
        long end = System.nanoTime();
        System.out.println("Avg ns per run: " + (end - start) / things.size());
    }
    
    public class StupidTask implements IIndexedRunnable
    {
        @Override
        public void run(int index)
        {
            thingArray[index].doSomething();
        }
    }
}