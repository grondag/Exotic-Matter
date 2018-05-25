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
        public void doSomeWork();
        
        public boolean hasWork();
    }

    private static final ISharableTask DUMMY_TASK = new ISharableTask()
    {
        @Override
        public void doSomeWork() {  }

        @Override
        public boolean hasWork() { return false; }
    };
    
    @SuppressWarnings("unused")
    private final ImmutableList<Thread> threads;
    
    private ISharableTask thingNeedingDone = DUMMY_TASK;
    
    private boolean running = true;
            
    private final Object startLock = new Object();

    private final ReadWriteLock completionLock = new ReentrantReadWriteLock();
    
    private final Lock completionWriteLock = SimpleThreadPoolExecutor.this.completionLock.writeLock();
    
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
                
                completionLock.lock();
                do
                {
                    try
                    { 
                        t.doSomeWork();
                    }
                    catch (Exception e) 
                    { 
                        ExoticMatter.INSTANCE.error("Unhandled error during concurrent processing. Impact unknown.", e);
                    }
                } while(t.hasWork());
                
                completionLock.unlock();
                
                synchronized(lock)
                {
                    try
                    {
                        while (running && !thingNeedingDone.hasWork())
                        {
                            lock.wait();
                        }
                    }
                    catch (InterruptedException e)  { }
                }
            }
        }

    }
    
    public void completeTask(ISharableTask task)
    {
        this.thingNeedingDone = task;
        
        synchronized(startLock)
        {
            startLock.notifyAll();
        }
        
        do
        {
            try
            { 
                task.doSomeWork();
            }
            catch (Exception e) 
            { 
                e.printStackTrace();
            }
        } while(task.hasWork());
       
        completionWriteLock.lock();
        
        // don't hold reference
        this.thingNeedingDone = DUMMY_TASK;
        
        completionWriteLock.unlock();
    }
    
    private static final long startIndexOffset;

    static
    {
        try 
        {
            startIndexOffset = UNSAFE.objectFieldOffset
                (ArrayTask.class.getDeclaredField("startIndex"));
        } catch (Exception ex) { throw new Error(ex); }
    }
    
    public static class ArrayTask<T> implements ISharableTask
    {
        protected final T[] theArray;
        protected volatile int startIndex;
        protected final int endIndex;
        protected final int batchSize;
        protected final Consumer<T> operation;
        
        public ArrayTask(T[] inputs, Consumer<T> operation, int startIndex, int endIndex, int batchSize)
        {
            this.theArray = inputs;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.batchSize = batchSize;
            this.operation = operation;
        }
       
        public ArrayTask(T[] inputs, Consumer<T> operation, int startIndex, int endIndex)
        {
            this(inputs, operation, startIndex, endIndex, Math.max(1, (endIndex - startIndex) / 64));
        }
        
        public ArrayTask(T[] inputs, Consumer<T> operation)
        {
            this(inputs, operation, 0, inputs.length);
        }
        
        @Override
        public final void doSomeWork()
        {
            int start = UNSAFE.getAndAddInt(this, startIndexOffset, batchSize);
            final int end = Math.min(this.endIndex, start + batchSize);
            for(; start < end; start++)
            {
                operation.accept(theArray[start]);
            }
//            System.out.println("Confirming stupid run of " + start + " to " + end + " on " + Thread.currentThread().getName());
        }

        @Override
        public final boolean hasWork()
        {
            return startIndex < endIndex;
        }
    }
}