package grondag.exotic_matter.concurrency;

import javax.annotation.Nonnull;

/**
 * Similar to consumer functional interface, but does not accept nulls.
 */
@FunctionalInterface
public interface JobTask<V>
{
    public abstract void doJobTask(@Nonnull V operand);
}