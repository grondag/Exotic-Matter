package grondag.exotic_matter.concurrency;

import java.util.function.Consumer;

/**
 * Thread pool optimized for scatter-gather processing patterns with an array, list or
 * other linearly-addressable data structure. Performance is equivalent to a Java fork-join
 * pool for large work loads and seems to have somewhat lower overhead and lower latency for small batches.<p>
 * 
 * The main motivation is simplicity: it is much easier (for the author at least) to understand and debug
 * than a custom counted-completer fork join task. (Based on actual experience creating same.)
 * It's also easier to use and requires less code for its intended use cases. <p>
 * 
 * The pool does not have a queue, and all calls to the various flavors of completeTask() are blocking.
 * This design is consistent with the scatter-gather patterns for which this pool is used - the intention
 * is to complete the entire task <em>now</em>, as quickly as possible, and then move on with another 
 * task that may depend on those results.<p>
 * 
 * Calls into the pool for execution are not thread-safe! (Again, no queue - it can only do one thing at a time.)
 * While usage could be externally synchronized, the intended usage pattern is to call into the pool
 * from a consumer thread that generates tasks dynamically and/or drain a queue of tasks into the pool.<p>
 * 
 * Size of the pool is always the system parallelism level, less one, because the calling thread is
 * recruited to do some of the work.<p>
 * 
 * Without a queue, there is no work stealing, however tasks are apportioned incrementally, with worker threads
 * claiming work only as they get scheduled.  Generally it will not be worthwhile to use the pool
 * unless the submitted tasks have enough tasks to keep all threads occupied. Some execution methods include
 * concurrency thresholds that, if not met, will simply execute the entire task on the calling thread so that
 * special case logic isn't needed in the calling code.<p>
 * 
 * A perfectly efficient pool would always have all threads finishing at the same time.
 * Even with dynamic work assignment, some thread will always finish some finite amount of time after
 * the other threads finish.  This waste can be minimized by slicing the work into smaller batches but
 * this comes with the price of increased overhead because shared state must be updated with each new batch.
 * Some execution parameters can be used to tune the batch size for a particular work load.<p>
 * 
 * Note this pool is <em>NOT</em> suitable as a generic thread pool for tasks that cannot be shared across
 * multiple cores and/or that are meant to be completed asynchronously. For that, the common ForkJoin pool, 
 * a fixed thread pool, dedicated threads, will all be better.
 */
public interface ScatterGatherThreadPool {
    /**
     * Applies the given operation to every in-range element of the array.  If the number of elements to be
     * processed is less than the given concurrency threshold, the operations will happen on the calling thread.
     * In either case, will block until all elements are processed.<p>
     * 
     * Use larger batch sizes (and larger thresholds) for fast operations on many elements.  Use smaller values 
     * for long-running elements. 
     */
    <V> void completeTask(V[] inputs, int startIndex, int count, int concurrencyThreshold, Consumer<V> operation, int batchSize);

    <V> void completeTask(V[] inputs, int concurrencyThreshold, Consumer<V> operation, int batchSize);

    <V> void completeTask(V[] inputs, int concurrencyThreshold, Consumer<V> operation);

    <V> void completeTask(V[] inputs, Consumer<V> operation, int batchSize);

    <V> void completeTask(V[] inputs, Consumer<V> operation);

    <V> void completeTask(V[] inputs, int startIndex, int count, Consumer<V> operation, int batchSize);

    <V> void completeTask(V[] inputs, int startIndex, int count, Consumer<V> operation);

    <V> void completeTask(V[] inputs, int startIndex, int count, int concurrencyThreshold, Consumer<V> operation);

    /**
     * Like {@link #completeTask(Object[], int, int, int, Consumer, int)} but with a mapping consumer.
     */
    <T, V> void completeTask(final T[] inputs, final int startIndex, final int count, final int concurrencyThreshold, final ArrayMappingConsumer<T,V> operation, int batchSize);

    <T, V> void completeTask(T[] inputs, int concurrencyThreshold, final ArrayMappingConsumer<T,V> operation, int batchSize);

    <T, V> void completeTask(T[] inputs, int concurrencyThreshold, final ArrayMappingConsumer<T,V> operation);

    <T, V> void completeTask(T[] inputs, final ArrayMappingConsumer<T,V> operation, int batchSize);

    <T, V> void completeTask(T[] inputs, final ArrayMappingConsumer<T,V> operation);

    <T, V> void completeTask(T[] inputs, int startIndex, int count, final ArrayMappingConsumer<T,V>operation, int batchSize);

    <T, V> void completeTask(T[] inputs, int startIndex, int count, final ArrayMappingConsumer<T,V>operation);

    <T, V> void completeTask(T[] inputs, int startIndex, int count, int concurrencyThreshold, ArrayMappingConsumer<T,V>operation);

    /**
     * Process a specialized task.  Will always attempt to use the pool because no information is
     * provided that would allow evaluation of fitness for concurrency.  Blocks until all done.
     */
    void completeTask(SharableTask task);

    /**
     * Convenient when your work happens to be in a SimpleConcurrentList
     */
    <V> void completeTask(SimpleConcurrentList<V> list, Consumer<V> operation);

    <V> void completeTask(SimpleConcurrentList<V> list, int concurrencyThreshold, Consumer<V> operation);
}