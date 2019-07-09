package grondag.exotic_matter.concurrency;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import grondag.exotic_matter.varia.structures.AbstractUnorderedArrayList;

/**
 * Similar to a Collector in a Java stream - accumulates results from the mapping function in each thread
 * and then dumps them into a collector after all batches are completed.<p>
 * 
 * The right half of the BiConsumer (another consumer) provides access to the in-thread sink for map outputs.
 * It's not represented as a map function in order to support functions that might not be 1:1 maps.
 */
public class ArrayMappingConsumer<T,V>
{
    private final BiConsumer<T, Consumer<V>> operation;
    private final Consumer<AbstractUnorderedArrayList<V>> collector;
    
    protected final ThreadLocal<WorkerState> workerStates = new ThreadLocal<WorkerState>()
    {
        @Override
        protected ArrayMappingConsumer<T, V>.WorkerState initialValue()
        {
            return new WorkerState();
        }
    };
    
    /**
     * For custom collectors - the collector provided must accept a SimpleUnorderedArrayList and will be
     * called in each thread where work as done after all batches are complete. <p>
     * 
     * The collector MUST be thread safe.
     */
    public ArrayMappingConsumer(BiConsumer<T, Consumer<V>> operation, Consumer<AbstractUnorderedArrayList<V>> collector)
    {
        this.operation = operation;
        this.collector = collector;
    }
    
    /**
     * The easy way - provide a simple concurrent list a the collector.  Note that
     * this implementation does not clear the list between runs. If a consumer is reused, this
     * will need to be handled externally if necessary.
     */
    public ArrayMappingConsumer(BiConsumer<T, Consumer<V>> operation, SimpleConcurrentList<V> target)
    {
        this.operation = operation;
        this.collector = (r) -> {if(!r.isEmpty()) target.addAll(r);};
    }
    
    /**
     * Holds the per-thread results and provides access to the mapping function.
     */
    private class WorkerState extends AbstractUnorderedArrayList<V> implements Consumer<T>
    {
        @Override
        public final void accept(@SuppressWarnings("null") T t)
        {
            operation.accept(t, v -> this.add(v));
        }
        
        /**
         * Called in each thread after all batches (for that thread) are complete.
         */
        protected final void completeThread()
        {
            collector.accept(this);
            this.clear();
        }
    }
    
    /**
     * Gets the mapping function for this thread. Using the function will collect output
     * in the calling thread for later consolidation via {@link #completeThread()}
     */
    protected final Consumer<T> getWorkerConsumer()
    {
        return workerStates.get();
    }
    
    /**
     * Signals worker state to perform result consolidation for this thread.
     */
    protected final void completeThread()
    {
        workerStates.get().completeThread();
    }
}