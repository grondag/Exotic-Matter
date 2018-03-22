package grondag.exotic_matter.simulator.domain;

import grondag.exotic_matter.simulator.persistence.ISimulationNode;

public interface IDomainCapability extends IDomainMember, ISimulationNode
{
    public String tagName();
    
    /**
     * Called by domain after construction
     */
    public void setDomain(IDomain domain);
    
}