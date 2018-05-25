package grondag.exotic_matter.concurrency;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;

import grondag.exotic_matter.ExoticMatter;
import sun.misc.Unsafe;

@SuppressWarnings("restriction")
public class SimpleThreadPoolExecutor
{
    @SuppressWarnings("null")
    private static final Unsafe UNSAFE = Danger.UNSAFE;
            
    public static interface ISharableTask
    {
        /**
         * Returns true if more work remains.
         */
        public boolean doSomeWork(int batchIndex);
        
        /**
         * If true, won't use parallel execution.
         */
        public boolean isSingleBatch();
    }

    private static final ISharableTask DUMMY_TASK = new ISharableTask()
    {
        @Override
        public boolean doSomeWork(int batchIndex) { return false; }

        @Override
        public boolean isSingleBatch() { return true; }

    };
    
    private static final long nextBatchIndexOffset;

    static
    {
        try 
        {
            nextBatchIndexOffset = UNSAFE.objectFieldOffset
                    (SimpleThreadPoolExecutor.class.getDeclaredField("nextBatchIndex"));
        } catch (Exception ex) { throw new Error(ex); }
    }
    
    
    @SuppressWarnings("unused")
    private final ImmutableList<Thread> threads;
    
    private ISharableTask thingNeedingDone = DUMMY_TASK;
    
    private boolean running = true;
            
    private final Object startLock = new Object();

    private final ReadWriteLock completionLock = new ReentrantReadWriteLock();
    
    private final Lock completionWriteLock = SimpleThreadPoolExecutor.this.completionLock.writeLock();
    
    @SuppressWarnings("unused")
    private volatile int nextBatchIndex;
    
    public SimpleThreadPoolExecutor()
    {
        final int poolSize = Runtime.getRuntime().availableProcessors() - 1;
        
        ImmutableList.Builder<Thread> builder = ImmutableList.builder();
        
        for(int i = 0; i < poolSize; i++)
        {
            Thread thread = new Thread(
                    new DoerOfThings(), 
                    "Exotic Matter Simulation Thread - " + i);
            thread.setDaemon(true);
            builder.add(thread);
            thread.start();
        }
        this.threads = builder.build();
    }
    
    private final int getNextBatchIndex()
    {
        return UNSAFE.getAndAddInt(this, nextBatchIndexOffset, 1);
    }
    
    public void stop()
    {
        this.running = false;
        synchronized(startLock)
        {
            startLock.notifyAll();
        }
    }
    
    private class DoerOfThings implements Runnable
    {
        @Override
        public void run()
        {
            final Object lock = SimpleThreadPoolExecutor.this.startLock;
            final Lock completionLock = SimpleThreadPoolExecutor.this.completionLock.readLock();
            
            while(running)
            {
                final ISharableTask t = thingNeedingDone;
                
                if(t != DUMMY_TASK)
                {
                    completionLock.lock();
                    try
                    { 
                        while(t.doSomeWork(getNextBatchIndex())) {};
                    }
                    catch (Exception e) 
                    { 
                        ExoticMatter.INSTANCE.error("Unhandled error during concurrent processing. Impact unknown.", e);
                    }
                    completionLock.unlock();
                }
                
                synchronized(lock)
                {
                    try
                    {
                        do
                        {
                            lock.wait();
                        } while (running && thingNeedingDone == DUMMY_TASK);
                    }
                    catch (InterruptedException e)  { }
                }
            }
        }

    }
    
    public final <V> void completeTask (V[] inputs, Consumer<V> operation, int startIndex, int endIndex, int concurrencyThreshold)
    {
        if(endIndex - startIndex <= concurrencyThreshold)
        {
            for(int i = startIndex; i < endIndex; i++)
            {
                operation.accept(inputs[i]);
            }
        }
        else
        {
            this.completeTaskParallel(new ArrayTask<>(inputs, operation, startIndex, endIndex));
        }
    }
   
    public final <V> void completeTask(V[] inputs, Consumer<V> operation, int startIndex, int endIndex)
    {
        completeTask(inputs, operation, startIndex, endIndex, 200);
    }
    
    public final <V> void completeTask(V[] inputs, Consumer<V> operation)
    {
        completeTask(inputs, operation, 0, inputs.length);
    }
    
    public final <V> void completeTask(V[] inputs, Consumer<V> operation, int concurrencyThreshold)
    {
        completeTask(inputs, operation, 0, inputs.length, concurrencyThreshold);
    }
    
    public final void completeTask(ISharableTask task)
    {
        if(task.isSingleBatch())
        {
            try
            { 
                task.doSomeWork(0);
            }
            catch (Exception e) 
            { 
                ExoticMatter.INSTANCE.error("Unhandled error during concurrent processing. Impact unknown.", e);
            }
        }
        else completeTaskParallel(task);
    }
    
    private void completeTaskParallel(ISharableTask task)
    {
        this.thingNeedingDone = task;
        
        // first batch always belongs to control thread
        this.nextBatchIndex = 1;
        
        // wake up worker threads
        synchronized(startLock)
        {
            startLock.notifyAll();
        }
        
        try
        { 
            if(task.doSomeWork(0))
            {
                while(task.doSomeWork(getNextBatchIndex())) {};
            }
        }
        catch (Exception e) 
        { 
            ExoticMatter.INSTANCE.error("Unhandled error during concurrent processing. Impact unknown.", e);
        }
       
        // don't hold reference & prevent restart of worker threads
        this.thingNeedingDone = DUMMY_TASK;

        // await completion of worker threads
        completionWriteLock.lock();
        completionWriteLock.unlock();
    }
    
    protected static class ArrayTask<T> implements ISharableTask
    {
        protected final T[] theArray;
        protected final int endIndex;
        protected final int batchSize;
        protected final Consumer<T> operation;
        
        protected ArrayTask(final T[] inputs, final Consumer<T> operation, final int startIndex, final int endIndex)
        {
            this.theArray = inputs;
            this.endIndex = endIndex;
            this.batchSize = Math.max(1, (endIndex - startIndex) / 64);
            this.operation = operation;
        }
        
        @Override
        public final boolean doSomeWork(final int batchIndex)
        {
            int start = batchIndex * batchSize;
            final int end = Math.min(endIndex, start + batchSize);
            for(; start < end; start++)
            {
                operation.accept(theArray[start]);
            }
//            System.out.println("Confirming stimple run of " + start + " to " + end + " on " + Thread.currentThread().getName());
            return end < endIndex;
        }

        @Override
        public boolean isSingleBatch()
        {
            return endIndex <= batchSize;
        }
    }
}