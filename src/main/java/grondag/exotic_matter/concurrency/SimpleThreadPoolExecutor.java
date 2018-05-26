package grondag.exotic_matter.concurrency;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;

import grondag.exotic_matter.ExoticMatter;
import grondag.exotic_matter.varia.SimpleUnorderedArrayList;
import sun.misc.Unsafe;

@SuppressWarnings("restriction")
public class SimpleThreadPoolExecutor
{
    @SuppressWarnings("null")
    private static final Unsafe UNSAFE = Danger.UNSAFE;
    
    public static final int POOL_SIZE = Runtime.getRuntime().availableProcessors() - 1;
    
    public static interface ISharableTask
    {
        /**
         * Returns true if more work remains.
         */
        public boolean doSomeWork(int batchIndex);
        
        public void onThreadComplete();
    }

    private static final ISharableTask DUMMY_TASK = new ISharableTask()
    {
        @Override
        public boolean doSomeWork(int batchIndex) { return false; }

        @Override
        public void onThreadComplete() { }
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
        ImmutableList.Builder<Thread> builder = ImmutableList.builder();
        
        for(int i = 0; i < POOL_SIZE; i++)
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
                        t.onThreadComplete();
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
            this.completeTask(new ArrayTask<>(inputs, operation, startIndex, endIndex));
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
    
    public final <T, V> void completeTask (final T[] inputs, final ArrayMappingConsumer<T,V> operation, final int startIndex, final int endIndex, final int concurrencyThreshold)
    {
        if(endIndex - startIndex <= concurrencyThreshold)
        {
            final Consumer<T> consumer = operation.getWorker();
            for(int i = startIndex; i < endIndex; i++)
            {
                consumer.accept(inputs[i]);
            }
            operation.completeThread();
        }
        else
        {
            this.completeTask(new ArrayMappingTask<>(inputs, operation, startIndex, endIndex));
        }
    }
    
    public final <T, V> void completeTask(T[] inputs, final ArrayMappingConsumer<T,V>operation, int startIndex, int endIndex)
    {
        completeTask(inputs, operation, startIndex, endIndex, 200);
    }
    
    public final <T, V> void completeTask(T[] inputs, final ArrayMappingConsumer<T,V> operation)
    {
        completeTask(inputs, operation, 0, inputs.length);
    }
    
    public final <T, V> void completeTask(T[] inputs, final ArrayMappingConsumer<T,V> operation, int concurrencyThreshold)
    {
        completeTask(inputs, operation, 0, inputs.length, concurrencyThreshold);
    }
    
    
    public final void completeTask(ISharableTask task)
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
            task.onThreadComplete();
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
    
    public static abstract  class AbstractArrayTask<T> implements ISharableTask
    {
        protected static final int SPLIT = (POOL_SIZE + 1) * 4;
        protected final T[] theArray;
        protected final int endIndex;
        protected final int batchSize;
        protected final int batchCount;
        
        protected abstract Consumer<T> getConsumer();
        
        protected AbstractArrayTask(final T[] inputs, final int startIndex, final int endIndex)
        {
            this.theArray = inputs;
            this.endIndex = endIndex;
            final int size = endIndex - startIndex;
            this.batchSize = Math.max(1, size / SPLIT);
            this.batchCount  = (size + batchSize - 1) / batchSize;
        }
        
        @Override
        public final boolean doSomeWork(final int batchIndex)
        {
            if(batchIndex < batchCount)
            {
                final Consumer<T> operation = getConsumer();
                int start = batchIndex * batchSize;
                final int end = Math.min(endIndex, start + batchSize);
                for(; start < end; start++)
                {
                    operation.accept(theArray[start]);
                }
                return end < endIndex;
            } 
            else return false;
        }
    }
    
    private static class ArrayTask<T> extends AbstractArrayTask<T>
    {
        protected final Consumer<T> operation;
        
        protected ArrayTask(T[] inputs, Consumer<T> operation, int startIndex, int endIndex)
        {
            super(inputs, startIndex, endIndex);
            this.operation = operation;
        }

        @Override
        public final void onThreadComplete() { }
        
        @Override
        public final Consumer<T> getConsumer()
        {
            return this.operation;
        }
    }
    
    public static class ArrayMappingConsumer<T,V>
    {
        private final BiConsumer<T, SimpleUnorderedArrayList<V>> operation;
        private final Consumer<SimpleUnorderedArrayList<V>> collector;
        
        protected final ThreadLocal<Worker> workers = new ThreadLocal<Worker>()
        {
            @Override
            protected ArrayMappingConsumer<T, V>.Worker initialValue()
            {
                return new Worker();
            }
        };
        
        public ArrayMappingConsumer(BiConsumer<T, SimpleUnorderedArrayList<V>> operation, Consumer<SimpleUnorderedArrayList<V>> collector)
        {
            this.operation = operation;
            this.collector = collector;
        }
        
        public ArrayMappingConsumer(BiConsumer<T, SimpleUnorderedArrayList<V>> operation, SimpleConcurrentList<V> target)
        {
            this.operation = operation;
            this.collector = (r) -> {if(!r.isEmpty()) target.addAll(r);};
        }
        
        private class Worker implements Consumer<T>
        {
            protected final SimpleUnorderedArrayList<V> results = new SimpleUnorderedArrayList<V>();
            
            @Override
            public final void accept(@SuppressWarnings("null") T t)
            {
                operation.accept(t, results);
            }
            
            protected final void completeThread()
            {
                collector.accept(results);
                results.clear();
            }
        }
        
        protected final Consumer<T> getWorker()
        {
            return workers.get();
        }
        
        protected final void completeThread()
        {
            workers.get().completeThread();
        }
    }
    
    private static class ArrayMappingTask<T, V> extends AbstractArrayTask<T>
    {
        protected final ArrayMappingConsumer<T,V> operation;
        
        protected ArrayMappingTask(T[] inputs, ArrayMappingConsumer<T,V> operation, int startIndex, int endIndex)
        {
            super(inputs, startIndex, endIndex);
            this.operation = operation;
        }

        @Override
        protected Consumer<T> getConsumer()
        {
            return operation.getWorker();
        }

        @Override
        public void onThreadComplete()
        {
            operation.completeThread();
        }
    }
    
   
}