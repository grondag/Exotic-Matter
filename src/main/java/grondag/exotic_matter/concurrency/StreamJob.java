package grondag.exotic_matter.concurrency;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import grondag.exotic_matter.simulator.Simulator;


public class StreamJob<V> extends Job
{
    private final Collection<V> backer;
    private final JobTask<V> task;

    public StreamJob(Collection<V> backer, JobTask<V> task, boolean enablePerfCounting, String jobTitle, PerformanceCollector perfCollector)
    {
        super(enablePerfCounting, jobTitle, perfCollector);
        this.backer = backer;
        this.task = task;
    }
    
    public StreamJob(Collection<V> backer, JobTask<V> task, PerformanceCounter perfCounter)
    {
        super(perfCounter);
        this.backer = backer;
        this.task = task;
    }
    
    /**
     * Check occurs during the run.
     */
    @Override
    public boolean canRun()
    {
        return true;
    }

    @Override
    public boolean worthRunningParallel()
    {
        return this.backer.size() > 200;
    }

    @SuppressWarnings("null")
    @Override
    public int run()
    {
        int size = backer.size();
        backer.forEach(i -> this.task.doJobTask(i));
        return size;
    }

    @SuppressWarnings("unchecked")
    @Override
    public int executeOn(Executor executor, List<Future<Void>> futures)
    {
        int size = backer.size();
        futures.add((Future<Void>) Simulator.SIMULATION_POOL.submit(() -> backer.parallelStream().forEach(i -> task.doJobTask(i))));
        return size;
    }
}

