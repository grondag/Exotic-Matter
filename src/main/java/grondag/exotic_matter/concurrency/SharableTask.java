package grondag.exotic_matter.concurrency;

public interface SharableTask
{
    /**
     * Return true if more work remains. Must be safe
     * to call if there is no work or all work is done.<p>
     * 
     * The provided batch index is an atomically increasing zero-based
     * positive integer.  batchIndex 0 is always sent to the initiating thread.<p>
     * 
     * The task is responsible for knowing how many batches of work it has 
     * and must ignore (and return false) for batches beyond that range.<p>
     * 
     * The task is also responsible for knowing what state is affected 
     * by the batch identified by the given index and for managing the 
     * synchronization of shared state affected by processing the batch.
     */
    public boolean doSomeWork(int batchIndex);
    
    /**
     * Called on each thread after it has completed all work it
     * will do for the current task being executed. Meant as a hook
     * for aggregating results, clean up etc. Will not be
     * called if the thread did not participate, but cannot
     * guarantee the thread actually completed any work. 
     */
    public void onThreadComplete();
}