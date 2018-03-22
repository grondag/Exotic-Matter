package grondag.exotic_matter.simulator.persistence;

import grondag.exotic_matter.serialization.IReadWriteNBT;

/**
 * Interface for anything persistence manager should persist, including
 * simulator itself and all top-level simulation nodes.
 */
public interface IPersistenceNode extends IDirtKeeper, IReadWriteNBT
{
    public abstract String tagName();

}
