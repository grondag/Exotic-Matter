package grondag.exotic_matter.concurrency;

public interface CountedJobTask<V>
{
    public abstract void doJobTask(V operand);
}