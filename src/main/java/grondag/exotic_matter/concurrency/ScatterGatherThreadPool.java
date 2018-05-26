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
public class ScatterGatherThreadPool
{
    @SuppressWarnings("null")
    private static final Unsafe UNSAFE = Danger.UNSAFE;
    
    public static final int DEFAULT_BATCH_SIZE = 200;
    
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
                    (ScatterGatherThreadPool.class.getDeclaredField("nextBatchIndex"));
        } catch (Exception ex) { throw new Error(ex); }
    }
    
    
    @SuppressWarnings("unused")
    private final ImmutableList<Thread> threads;
    
    private ISharableTask thingNeedingDone = DUMMY_TASK;
    
    private boolean running = true;
            
    private final Object startLock = new Object();

    private final ReadWriteLock completionLock = new ReentrantReadWriteLock();
    
    private final Lock completionWriteLock = ScatterGatherThreadPool.this.completionLock.writeLock();
    
    @SuppressWarnings("unused")
    private volatile int nextBatchIndex;
    
    public ScatterGatherThreadPool()
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
            final Object lock = ScatterGatherThreadPool.this.startLock;
            final Lock completionLock = ScatterGatherThreadPool.this.completionLock.readLock();
            
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
    
    public final <V> void completeTask (V[] inputs, int startIndex, int endIndex, int concurrencyThreshold, Consumer<V> operation)
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
            this.completeTask(new ArrayTask<>(inputs, startIndex, endIndex, operation));
        }
    }
    
    public final <V> void completeTask (V[] inputs, int startIndex, int endIndex, int concurrencyThreshold, Consumer<V> operation, int batchSize)
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
            this.completeTask(new ArrayTask<>(inputs, startIndex, endIndex, operation, batchSize));
        }
    }
   
    public final <V> void completeTask(V[] inputs, int startIndex, int endIndex, Consumer<V> operation)
    {
        completeTask(inputs, startIndex, endIndex, DEFAULT_BATCH_SIZE, operation);
    }
    
    public final <V> void completeTask(V[] inputs, int startIndex, int endIndex, Consumer<V> operation, int batchSize)
    {
        completeTask(inputs, startIndex, endIndex, (POOL_SIZE + 1) * batchSize, operation, batchSize);
    }
    
    public final <V> void completeTask(V[] inputs, Consumer<V> operation)
    {
        completeTask(inputs, 0, inputs.length, operation);
    }
    
    public final <V> void completeTask(V[] inputs, Consumer<V> operation, int batchSize)
    {
        completeTask(inputs, 0, inputs.length, operation, batchSize);
    }
    
    public final <V> void completeTask(V[] inputs, int concurrencyThreshold, Consumer<V> operation)
    {
        completeTask(inputs,0, inputs.length, concurrencyThreshold,  operation);
    }
    
    public final <V> void completeTask(V[] inputs, int concurrencyThreshold, Consumer<V> operation, int batchSize)
    {
        completeTask(inputs,0, inputs.length, concurrencyThreshold,  operation, batchSize);
    }
    
    public final <V> void completeTask(SimpleConcurrentList<V> list, int concurrencyThreshold, Consumer<V> operation)
    {
        completeTask(list.getOperands(), 0, list.size(), concurrencyThreshold, operation);
    }
    
    public final <T, V> void completeTask (final T[] inputs, final int startIndex, final int endIndex, final int concurrencyThreshold, final ArrayMappingConsumer<T,V> operation)
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
            this.completeTask(new ArrayMappingTask<>(inputs, startIndex, endIndex, operation));
        }
    }
    
    public final <T, V> void completeTask (final T[] inputs, final int startIndex, final int endIndex, final int concurrencyThreshold, final ArrayMappingConsumer<T,V> operation, int batchSize)
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
            this.completeTask(new ArrayMappingTask<>(inputs, startIndex, endIndex, operation, batchSize));
        }
    }
    
    public final <T, V> void completeTask(T[] inputs, int startIndex, int endIndex, final ArrayMappingConsumer<T,V>operation)
    {
        completeTask(inputs, startIndex, endIndex, DEFAULT_BATCH_SIZE, operation);
    }
    
    public final <T, V> void completeTask(T[] inputs, int startIndex, int endIndex, final ArrayMappingConsumer<T,V>operation, int batchSize)
    {
        completeTask(inputs, startIndex, endIndex, (POOL_SIZE + 1) * batchSize, operation, batchSize);
    }
    
    public final <T, V> void completeTask(T[] inputs, final ArrayMappingConsumer<T,V> operation)
    {
        completeTask(inputs, 0, inputs.length, operation);
    }
    
    public final <T, V> void completeTask(T[] inputs, final ArrayMappingConsumer<T,V> operation, int batchSize)
    {
        completeTask(inputs, 0, inputs.length, operation, batchSize);
    }
    
    public final <T, V> void completeTask(T[] inputs, int concurrencyThreshold, final ArrayMappingConsumer<T,V> operation)
    {
        completeTask(inputs, 0, inputs.length, concurrencyThreshold, operation);
    }
    
    public final <T, V> void completeTask(T[] inputs, int concurrencyThreshold, final ArrayMappingConsumer<T,V> operation, int batchSize)
    {
        completeTask(inputs, 0, inputs.length, concurrencyThreshold, operation, batchSize);
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
        
        protected AbstractArrayTask(final T[] inputs, final int startIndex, final int endIndex, final int batchSize)
        {
            this.theArray = inputs;
            this.endIndex = endIndex;
            this.batchSize = batchSize;
            this.batchCount  = (endIndex - startIndex + batchSize - 1) / batchSize;
        }
        
        protected AbstractArrayTask(final T[] inputs, final int startIndex, final int endIndex)
        {
            this(inputs, startIndex, endIndex, Math.max(1, (endIndex - startIndex) / SPLIT));
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
        
        protected ArrayTask(T[] inputs, int startIndex, int endIndex, Consumer<T> operation)
        {
            super(inputs, startIndex, endIndex);
            this.operation = operation;
        }

        protected ArrayTask(T[] inputs, int startIndex, int endIndex, Consumer<T> operation, int batchSize)
        {
            super(inputs, startIndex, endIndex, batchSize);
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
        private final BiConsumer<T, Consumer<V>> operation;
        private final Consumer<SimpleUnorderedArrayList<V>> collector;
        
        protected final ThreadLocal<Worker> workers = new ThreadLocal<Worker>()
        {
            @Override
            protected ArrayMappingConsumer<T, V>.Worker initialValue()
            {
                return new Worker();
            }
        };
        
        public ArrayMappingConsumer(BiConsumer<T, Consumer<V>> operation, Consumer<SimpleUnorderedArrayList<V>> collector)
        {
            this.operation = operation;
            this.collector = collector;
        }
        
        public ArrayMappingConsumer(BiConsumer<T, Consumer<V>> operation, SimpleConcurrentList<V> target)
        {
            this.operation = operation;
            this.collector = (r) -> {if(!r.isEmpty()) target.addAll(r);};
        }
        
        private class Worker extends SimpleUnorderedArrayList<V> implements Consumer<T>
        {
            @Override
            public final void accept(@SuppressWarnings("null") T t)
            {
                operation.accept(t, v -> this.add(v));
            }
            
            protected final void completeThread()
            {
                collector.accept(this);
                this.clear();
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
        
        protected ArrayMappingTask(T[] inputs, int startIndex, int endIndex, ArrayMappingConsumer<T,V> operation)
        {
            super(inputs, startIndex, endIndex);
            this.operation = operation;
        }
        
        protected ArrayMappingTask(T[] inputs, int startIndex, int endIndex, ArrayMappingConsumer<T,V> operation, int batchSize)
        {
            super(inputs, startIndex, endIndex, batchSize);
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