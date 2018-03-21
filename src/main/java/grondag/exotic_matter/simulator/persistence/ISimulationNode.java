package grondag.exotic_matter.simulator.persistence;

/**
 * Should be implemented by all top-level simulation components.
 * Must be registered with simulator. Simulator will register with PersistenceManager.  
 * 
 */
public interface ISimulationNode extends IPersistenceNode
{
    /**
     * Called by simulator at shutdown
     */
    public void unload();
    
    /**
     * Called by simulator after all top-level nodes are deserialized (if anything found) 
     * but before first simulation tick.  This is the time to re-create references to other nodes if needed.
     */
    public void afterDeserialization();
    
}
