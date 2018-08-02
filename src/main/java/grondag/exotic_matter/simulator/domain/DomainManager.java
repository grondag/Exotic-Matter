package grondag.exotic_matter.simulator.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import grondag.exotic_matter.ExoticMatter;
import grondag.exotic_matter.serialization.NBTDictionary;
import grondag.exotic_matter.simulator.Simulator;
import grondag.exotic_matter.simulator.persistence.AssignedNumber;
import grondag.exotic_matter.simulator.persistence.AssignedNumbersAuthority.IdentifiedIndex;
import grondag.exotic_matter.simulator.persistence.IIdentified;
import grondag.exotic_matter.simulator.persistence.ISimulationTopNode;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.common.MinecraftForge;

public class DomainManager implements ISimulationTopNode
{  
    private static final String NBT_DOMAIN_MANAGER = NBTDictionary.claim("domMgr");
    private static final String NBT_DOMAIN_MANAGER_DOMAINS = NBTDictionary.claim("domMgrAll");
    private static final String NBT_DOMAIN_PLAYER_DOMAINS = NBTDictionary.claim("domMgrPlayer");
    private static final String NBT_DOMAIN_ACTIVE_DOMAINS = NBTDictionary.claim("domMgrActive");
    
    /**
     * Set to null when Simulator creates singleton and when it shuts down to force retrieval of current instance.
     */
    private static DomainManager instance;
    
    public static DomainManager instance()
    {
        return instance;
    }
    
    private boolean isDeserializationInProgress = false;
    
    boolean isDirty = false;
    
    private boolean isLoaded = false;

    private IDomain defaultDomain;

    /** 
     * Each player has a domain that is automatically created for them
     * and which they always own.  This will be their initially active domain.
     */
    private HashMap<String, IDomain> playerIntrinsicDomains = new HashMap<>();
    
    /** 
     * Each player has a currently active domain. This will initially be their intrinsic domain.
     */
    private HashMap<String, IDomain> playerActiveDomains = new HashMap<>();
    
    /**
     * If isNew=true then won't wait for a deserialize to become loaded.
     */
    public DomainManager() 
    {
        // force refresh of singleton reference
        instance = null;

    }
   
    /**
     * Called at shutdown
     */
    @Override
    public void unload()
    {
        this.playerActiveDomains.clear();
        this.playerIntrinsicDomains.clear();
        this.defaultDomain = null;
        this.isLoaded = false;
    }
    
    @Override
    public void afterCreated(Simulator sim)
    {
        instance = this;
    }

    @Override
    public void loadNew()
    {
        this.unload();
        this.isLoaded = true;
    }
    
    /**
     * Domain for unmanaged objects.  
     */
    public IDomain defaultDomain()
    {
        this.checkLoaded();
        if(this.defaultDomain == null)
        {
            defaultDomain = domainFromId(1);
            if(defaultDomain == null)
            {
                this.defaultDomain = new Domain(this);
                this.defaultDomain.setSecurityEnabled(false);
                this.defaultDomain.setId(IIdentified.DEFAULT_ID);
                this.defaultDomain.setName("Public");;
                Simulator.instance().assignedNumbersAuthority().register(defaultDomain);
            }
        }
        return this.defaultDomain;
    }
    
    public List<IDomain> getAllDomains()
    {
        this.checkLoaded();
        ImmutableList.Builder<IDomain> builder = ImmutableList.builder();
        for (IIdentified domain : Simulator.instance().assignedNumbersAuthority().getIndex(AssignedNumber.DOMAIN).valueCollection())
        {
            builder.add((Domain)domain);
        }
        return builder.build();
    }

    public IDomain getDomain(int id)
    {
        this.checkLoaded();
        return domainFromId(id);
    }
    
    public synchronized IDomain createDomain()
    {
        this.checkLoaded();
        Domain result = new Domain(this);
        Simulator.instance().assignedNumbersAuthority().register(result);
        result.name = "Domain " + result.id;
        this.isDirty = true;
        return result;
    }
    
    /**
     * Does NOT destroy any of the contained objects in the domain!
     */
    public synchronized void removeDomain(IDomain domain)
    {
        this.checkLoaded();
        Simulator.instance().assignedNumbersAuthority().unregister(domain);
        this.isDirty = true;
    }
    
    @Override
    public boolean isSaveDirty()
    {
        return this.isDirty;
    }

    @Override
    public void setSaveDirty(boolean isDirty)
    {
        this.isDirty = isDirty;
    }
    
    @Override
    public void deserializeNBT(@Nullable NBTTagCompound tag)
    {
        this.isDeserializationInProgress = true;
        
        this.unload();

        // need to do this before loading domains, otherwise they will cause complaints
        this.isLoaded = true;
        
        if(tag == null) return;
        
        NBTTagList nbtDomains = tag.getTagList(NBT_DOMAIN_MANAGER_DOMAINS, 10);
        if( nbtDomains != null && !nbtDomains.hasNoTags())
        {
            for (int i = 0; i < nbtDomains.tagCount(); ++i)
            {
                Domain domain = new Domain(this, nbtDomains.getCompoundTagAt(i));
                Simulator.instance().assignedNumbersAuthority().register(domain);
            }   
        }
        
        NBTTagCompound nbtPlayerDomains = tag.getCompoundTag(NBT_DOMAIN_PLAYER_DOMAINS);
        if(nbtPlayerDomains != null && !nbtPlayerDomains.hasNoTags())
        {
            for(String playerName : nbtPlayerDomains.getKeySet())
            {
                IDomain d = domainFromId(nbtPlayerDomains.getInteger(playerName));
                if(d != null) this.playerIntrinsicDomains.put(playerName, d);
            }
        }
        
        NBTTagCompound nbtActiveDomains = tag.getCompoundTag(NBT_DOMAIN_ACTIVE_DOMAINS);
        if(nbtActiveDomains != null && !nbtActiveDomains.hasNoTags())
        {
            for(String playerName : nbtActiveDomains.getKeySet())
            {
                IDomain d = domainFromId(nbtActiveDomains.getInteger(playerName));
                if(d != null) this.playerActiveDomains.put(playerName, d);
            }
        }
        
        this.isDeserializationInProgress = false;
    }

    @Override
    public void serializeNBT(NBTTagCompound tag)
    {
        NBTTagList nbtDomains = new NBTTagList();
        
        IdentifiedIndex domains = Simulator.instance().assignedNumbersAuthority().getIndex(AssignedNumber.DOMAIN);
        
        if(!domains.isEmpty())
        {
            for (IIdentified domain : domains.valueCollection())
            {
                nbtDomains.appendTag(((Domain)domain).serializeNBT());
            }
        }
        tag.setTag(NBT_DOMAIN_MANAGER_DOMAINS, nbtDomains);
        
        if(!this.playerIntrinsicDomains.isEmpty())
        {
            NBTTagCompound nbtPlayerDomains = new NBTTagCompound();
            for(Entry<String, IDomain> entry : this.playerIntrinsicDomains.entrySet())
            {
                nbtPlayerDomains.setInteger(entry.getKey(), entry.getValue().getId());
            }
            tag.setTag(NBT_DOMAIN_PLAYER_DOMAINS, nbtPlayerDomains);
        }
        
        if(!this.playerActiveDomains.isEmpty())
        {
            NBTTagCompound nbtActiveDomains = new NBTTagCompound();
            for(Entry<String, IDomain> entry : this.playerActiveDomains.entrySet())
            {
                nbtActiveDomains.setInteger(entry.getKey(), entry.getValue().getId());
            }
            tag.setTag(NBT_DOMAIN_ACTIVE_DOMAINS, nbtActiveDomains);
        }
    }

    @Override
    public String tagName()
    {
        return NBT_DOMAIN_MANAGER;
    }

    private boolean checkLoaded()
    {
        if(!this.isLoaded)
        {
            ExoticMatter.INSTANCE.warn("Domain manager accessed before it was loaded.  This is a bug and probably means simulation state has been lost.");
        }
        return this.isLoaded;
    }


    /**
     * The player's currently active domain. If player
     * has never specified, will be the player's intrinsic domain.
     */
    public IDomain getActiveDomain(EntityPlayerMP player)
    {
        IDomain result = this.playerActiveDomains.get(player.getName());
        if(result == null)
        {
            synchronized(this.playerActiveDomains)
            {
                result = this.playerActiveDomains.get(player.getName());
                if(result == null)
                {
                    result = this.getIntrinsicDomain(player);
                    this.playerActiveDomains.put(player.getName(), result);
                }
            }
        }
        return result;
    }
    
    /**
     * Set the player's currently active domain.<br>
     * Posts an event so that anything dependent on active domain can react.
     */
    public void setActiveDomain(EntityPlayerMP player, IDomain domain)
    {
        synchronized(this.playerActiveDomains)
        {
            IDomain result = this.playerActiveDomains.put(player.getName(), domain);
            if(result == null || result != domain )
            {
                MinecraftForge.EVENT_BUS.post(new DomainChangeEvent(player, result, domain));
            }
        }
    }
    
    /**
     * The player's private, default domain. Created if does not already exist.
     */
    public IDomain getIntrinsicDomain(EntityPlayerMP player)
    {
        IDomain result = this.playerIntrinsicDomains.get(player.getName());
        if(result == null)
        {
            synchronized(this.playerIntrinsicDomains)
            {
                result = this.playerIntrinsicDomains.get(player.getName());
                if(result == null)
                {
                    result = this.createDomain();
                    result.setSecurityEnabled(true);
                    result.setName(I18n.translateToLocalFormatted("misc.default_domain_template", player.getName()));
                    DomainUser user = result.addPlayer(player);
                    user.setPrivileges(Privilege.ADMIN);
                    this.playerIntrinsicDomains.put(player.getName(), result);
                }
            }
        }
        return result;
    }
    
    public boolean isDeserializationInProgress()
    {
        return this.isDeserializationInProgress;
    }
    
    // convenience object lookup methods
    public static IDomain domainFromId(int id)
    {
        return (Domain)  Simulator.instance().assignedNumbersAuthority().get(id, AssignedNumber.DOMAIN);
    }
    
    @Override
    public void afterDeserialization()
    {
        IdentifiedIndex domains =  Simulator.instance().assignedNumbersAuthority().getIndex(AssignedNumber.DOMAIN);
        
        if(!domains.isEmpty())
        {
            for (IIdentified domain : domains.valueCollection())
            {
                ((Domain)domain).afterDeserialization();
            }
        }
    }

}
