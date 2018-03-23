package grondag.exotic_matter.simulator.domain;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Posted when player active domain changes.
 */
public class DomainChangeEvent extends PlayerEvent
{

    public final IDomain oldDomain;
    public final IDomain newDomain;
    
    public DomainChangeEvent(EntityPlayer player, IDomain oldDomain, IDomain newDomain)
    {
        super(player);
        this.oldDomain = oldDomain;
        this.newDomain = newDomain;
    }


}
