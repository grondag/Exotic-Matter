package grondag.exotic_matter.simulator.domain;

import grondag.exotic_matter.serialization.IReadWriteNBT;

public interface IUserCapability extends IReadWriteNBT
{
    public String tagName();
    
    /**
     * Called by domain user after construction
     */
    public void setDomainUser(DomainUser user);
    
    /**
     * If true, won't be serialized and on reload
     * will have a new (empty) instance.
     */
    public boolean isSerializationDisabled();
    
}