package grondag.exotic_matter.simulator.persistence;

import grondag.exotic_matter.serialization.IReadWriteNBT;

/**
 * Should be implemented by all top-level simulation components.
 * Must be registered with simulator. Simulator will register with PersistenceManager.  
 * 
 */
public interface ISimulationNode extends IDirtKeeper, IReadWriteNBT
{
    public abstract String tagName();
    
    public 
}
