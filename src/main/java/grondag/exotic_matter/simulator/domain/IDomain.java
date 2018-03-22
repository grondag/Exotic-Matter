package grondag.exotic_matter.simulator.domain;

import java.util.List;

import com.google.common.eventbus.EventBus;

import grondag.exotic_matter.simulator.persistence.IDirtListenerProvider;
import grondag.exotic_matter.simulator.persistence.IIdentified;
import grondag.exotic_matter.simulator.persistence.ISimulationNode;
import net.minecraft.entity.player.EntityPlayer;

public interface IDomain extends ISimulationNode, IDirtListenerProvider, IIdentified
{

    EventBus eventBus();
    
    List<DomainUser> getAllUsers();

    DomainUser findPlayer(EntityPlayer player);

    DomainUser findUser(String userName);

    boolean hasPrivilege(EntityPlayer player, Privilege privilege);

    /** 
     * Will return existing user if already exists.
     */
    DomainUser addPlayer(EntityPlayer player);

    /** 
     * Will return existing user if already exists.
     */
    DomainUser addUser(String userName);

    String getName();

    void setName(String name);

    boolean isSecurityEnabled();

    void setSecurityEnabled(boolean isSecurityEnabled);

    <V extends IDomainCapability> V getCapability(Class<V> capability);


}