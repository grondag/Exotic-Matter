package grondag.exotic_matter.concurrency;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;

import grondag.exotic_matter.ExoticMatter;
import sun.misc.Unsafe;


@SuppressWarnings("restriction")
public class ScatterGatherThreadPoolImpl implements ScatterGatherThreadPool
{
    /**
     * Will be number of cores less one because calling thread also does work.
     */
    public static final int POOL_SIZE = Runtime.getRuntime().availableProcessors() - 1;
    
    /**
     * By default, each thread will have four "batches" of work it can pick up, assuming
     * each thread does equal work.  More likely, some threads will do more and some will do fewer.
     */
    public static final int DEFAULT_BATCHES_PER_THREAD = 4;

    /**
     * Total batches for all threads, including calling thread, unless a custom batch size is given.
     */
    public static final int DEFAULT_BATCH_COUNT = (POOL_SIZE + 1) * DEFAULT_BATCHES_PER_THREAD;
    
    /**
     * Arbitrary.  For quick tasks with large number of elements, larger numbers would be better.
     */
    public static final int DEFAULT_MINIMUM_TASKS_PER_BATCH = 64;
    
    /**
     * If using the default batch size, this the is number of task elements needed to make
     * scatter-gather worthwhile. Tasks with fewer elements will simply be run on the calling thread.
     */
    public static final int DEFAULT_CONCURRENCY_THRESHOLD = DEFAULT_BATCH_COUNT * DEFAULT_MINIMUM_TASKS_PER_BATCH;
    
    /**
     * Compute the number of elements per batch if no specific batch size is provided.
     * Will ensure there are exactly {@link #DEFAULT_BATCH_COUNT} batches, unless
     * the number of elements is less than that, in which case the batch size will be 1.
     */
    public static final int defaultBatchSize(final int elementCount)
    {
        return (elementCount + DEFAULT_BATCH_COUNT - 1) / DEFAULT_BATCH_COUNT;
    }
            
    /**
     * Signals no task and guards against NPE from errant threads by doing nothing
     * and indicating no work if somehow called.
     */
    private static final SharableTask DUMMY_TASK = new SharableTask()
    {
        @Override
        public boolean doSomeWork(int batchIndex) { return false; }

        @Override
        public void onThreadComplete() { }
    };
    
    /**
     * Used here to avoid a pointer chase for the atomic batch counter at the core of the implementation.
     */
    private static final Unsafe UNSAFE = Danger.UNSAFE;
    
    /**
     * Unsafe address for atomic access to {@link #nextBatchIndex}
     */
    private static final long nextBatchIndexOffset;

    static
    {
        try 
        {
            nextBatchIndexOffset = UNSAFE.objectFieldOffset
                    (ScatterGatherThreadPoolImpl.class.getDeclaredField("nextBatchIndex"));
        } catch (Exception ex) { throw new Error(ex); }
    }
    
    /**
     * Keep references to worker threads for debugging.
     */
    @SuppressWarnings("unused")
    private final ImmutableList<Thread> threads;
    
    /**
     * Essentially a single-element work queue. Set to {@link #DUMMY_TASK} when empty.
     */
    private SharableTask thingNeedingDone = DUMMY_TASK;
    
    /**
     * Signal for shutdown.
     */
    private boolean running = true;
    
    /**
     * Used to wake up worker threads when there is new work.
     */
    private final Object startLock = new Object();

    /**
     * Worker threads hold a read lock on this for as long as they are working. Calling
     * thread will block until it can get a write lock, meaning all worker threads
     * have completed.
     */
    private final ReadWriteLock completionLock = new ReentrantReadWriteLock();
    
    /**
     * Efficient access to write lock for calling thread.
     */
    private final Lock completionWriteLock = ScatterGatherThreadPoolImpl.this.completionLock.writeLock();
    
    /**
     * The core mechanism for dynamic work assignment.  Set to 1 at the start of each task
     * (because batch 0 is reserved for calling thread) and atomically incremented as workers
     * claim batches until the task is complete.
     */
    @SuppressWarnings("unused")
    private volatile int nextBatchIndex;
    
    public ScatterGatherThreadPoolImpl()
    {
        ImmutableList.Builder<Thread> builder = ImmutableList.builder();
        
        for(int i = 0; i < POOL_SIZE; i++)
        {
            Thread thread = new Thread(
                    new Worker(), 
                    "Exotic Matter Simulation Thread - " + i);
            thread.setDaemon(true);
            builder.add(thread);
            thread.start();
        }
        this.threads = builder.build();
    }
    
    /**
     * See {@link #nextBatchIndex}
     */
    private final int getNextBatchIndex()
    {
        return UNSAFE.getAndAddInt(this, nextBatchIndexOffset, 1);
    }
    
    /**
     * Signals worker threads to stop and immediately returns. 
     * Pool provides no means to be restarted once stopped.
     */
    public void stop()
    {
        this.running = false;
        synchronized(startLock)
        {
            startLock.notifyAll();
        }
    }
    
    private class Worker implements Runnable
    {
        @Override
        public void run()
        {
            final Object lock = ScatterGatherThreadPoolImpl.this.startLock;
            final Lock completionLock = ScatterGatherThreadPoolImpl.this.completionLock.readLock();
            
            while(running)
            {
                final SharableTask t = thingNeedingDone;
                
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
    
    @Override
    public final <V> void completeTask (V[] inputs, int startIndex, int count, int concurrencyThreshold, Consumer<V> operation, int batchSize)
    {
        if(count <= concurrencyThreshold)
        {
            final int endIndex = startIndex + count;
            for(int i = startIndex; i < endIndex; i++)
            {
                operation.accept(inputs[i]);
            }
        }
        else
        {
            this.completeTask(new ArrayTask<>(inputs, startIndex, count, operation, batchSize));
        }
    }
   
    @Override
    public final <V> void completeTask (V[] inputs, int startIndex, int count, int concurrencyThreshold, Consumer<V> operation)
    {
        completeTask(inputs, startIndex, count, concurrencyThreshold,  operation, defaultBatchSize(count));
    }
    
    @Override
    public final <V> void completeTask(V[] inputs, int startIndex, int count, Consumer<V> operation)
    {
        completeTask(inputs, startIndex, count, DEFAULT_CONCURRENCY_THRESHOLD, operation, defaultBatchSize(count));
    }
    
    @Override
    public final <V> void completeTask(V[] inputs, int startIndex, int count, Consumer<V> operation, int batchSize)
    {
        completeTask(inputs, startIndex, count, (POOL_SIZE + 1) * batchSize, operation, batchSize);
    }
    
    @Override
    public final <V> void completeTask(V[] inputs, Consumer<V> operation)
    {
        completeTask(inputs, 0, inputs.length, operation);
    }
    
    @Override
    public final <V> void completeTask(V[] inputs, Consumer<V> operation, int batchSize)
    {
        completeTask(inputs, 0, inputs.length, operation, batchSize);
    }
    
    @Override
    public final <V> void completeTask(V[] inputs, int concurrencyThreshold, Consumer<V> operation)
    {
        completeTask(inputs, 0, inputs.length, concurrencyThreshold,  operation, defaultBatchSize(inputs.length));
    }
    
    @Override
    public final <V> void completeTask(V[] inputs, int concurrencyThreshold, Consumer<V> operation, int batchSize)
    {
        completeTask(inputs, 0, inputs.length, concurrencyThreshold,  operation, batchSize);
    }
    
    @Override
    public final <V> void completeTask(SimpleConcurrentList<V> list, int concurrencyThreshold, Consumer<V> operation)
    {
        completeTask(list.getOperands(), 0, list.size(), concurrencyThreshold, operation, defaultBatchSize(list.size()));
    }
    
    @Override
    public final <V> void completeTask(SimpleConcurrentList<V> list, Consumer<V> operation)
    {
        completeTask(list.getOperands(), 0, list.size(), DEFAULT_CONCURRENCY_THRESHOLD, operation, defaultBatchSize(list.size()));
    }
    
    @Override
    public final <T, V> void completeTask (final T[] inputs, final int startIndex, final int count, final int concurrencyThreshold, final ArrayMappingConsumer<T,V> operation, int batchSize)
    {
        if(count <= concurrencyThreshold)
        {
            final int endIndex = startIndex + count;
            final Consumer<T> consumer = operation.getWorkerConsumer();
            for(int i = startIndex; i < endIndex; i++)
            {
                consumer.accept(inputs[i]);
            }
            operation.completeThread();
        }
        else
        {
            this.completeTask(new ArrayMappingTask<>(inputs, startIndex, count, operation, batchSize));
        }
    }
    
    @Override
    public final <T, V> void completeTask (T[] inputs, int startIndex, int count, int concurrencyThreshold, ArrayMappingConsumer<T,V>operation)
    {
        completeTask(inputs, startIndex, count, concurrencyThreshold,  operation, defaultBatchSize(count));
    }
    
    @Override
    public final <T, V> void completeTask(T[] inputs, int startIndex, int count, final ArrayMappingConsumer<T,V>operation)
    {
        completeTask(inputs, startIndex, count, DEFAULT_CONCURRENCY_THRESHOLD, operation, defaultBatchSize(count));
    }
    
    @Override
    public final <T, V> void completeTask(T[] inputs, int startIndex, int count, final ArrayMappingConsumer<T,V>operation, int batchSize)
    {
        completeTask(inputs, startIndex, count, (POOL_SIZE + 1) * batchSize, operation, batchSize);
    }
    
    @Override
    public final <T, V> void completeTask(T[] inputs, final ArrayMappingConsumer<T,V> operation)
    {
        completeTask(inputs, 0, inputs.length, operation);
    }
    
    @Override
    public final <T, V> void completeTask(T[] inputs, final ArrayMappingConsumer<T,V> operation, int batchSize)
    {
        completeTask(inputs, 0, inputs.length, operation, batchSize);
    }
    
    @Override
    public final <T, V> void completeTask(T[] inputs, int concurrencyThreshold, final ArrayMappingConsumer<T,V> operation)
    {
        completeTask(inputs, 0, inputs.length, concurrencyThreshold, operation, defaultBatchSize(inputs.length));
    }
    
    @Override
    public final <T, V> void completeTask(T[] inputs, int concurrencyThreshold, final ArrayMappingConsumer<T,V> operation, int batchSize)
    {
        completeTask(inputs, 0, inputs.length, concurrencyThreshold, operation, batchSize);
    }
    
    @Override
    public final void completeTask(SharableTask task)
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
    
    public static abstract  class AbstractArrayTask<T> implements SharableTask
    {
        protected final T[] theArray;
        protected final int startIndex;
        protected final int endIndex;
        protected final int batchSize;
        protected final int batchCount;
        
        protected abstract Consumer<T> getConsumer();
        
        protected AbstractArrayTask(final T[] inputs, final int startIndex, final int count, final int batchSize)
        {
            this.theArray = inputs;
            this.startIndex = startIndex;
            this.endIndex = startIndex + count;
            this.batchSize = batchSize;
            this.batchCount  = (count + batchSize - 1) / batchSize;
        }
        
        @Override
        public final boolean doSomeWork(final int batchIndex)
        {
            if(batchIndex < batchCount)
            {
                final Consumer<T> operation = getConsumer();
                int start = startIndex + batchIndex * batchSize;
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
        
        protected ArrayTask(T[] inputs, int startIndex, int count, Consumer<T> operation, int batchSize)
        {
            super(inputs, startIndex, count, batchSize);
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
    
    /**
     * All of the important work is done in the consumer implementation.  
     * This just gives it the shape of a task.
     */
    private static class ArrayMappingTask<T, V> extends AbstractArrayTask<T>
    {
        protected final ArrayMappingConsumer<T,V> operation;
        
        protected ArrayMappingTask(T[] inputs, int startIndex, int count, ArrayMappingConsumer<T,V> operation, int batchSize)
        {
            super(inputs, startIndex, count, batchSize);
            this.operation = operation;
        }

        @Override
        protected Consumer<T> getConsumer()
        {
            return operation.getWorkerConsumer();
        }

        @Override
        public void onThreadComplete()
        {
            operation.completeThread();
        }
    }
}