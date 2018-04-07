package grondag.exotic_matter.simulator.domain;

import java.util.HashSet;
import java.util.IdentityHashMap;

import javax.annotation.Nullable;

import grondag.exotic_matter.ExoticMatter;
import grondag.exotic_matter.serialization.IReadWriteNBT;
import grondag.exotic_matter.serialization.NBTDictionary;
import net.minecraft.nbt.NBTTagCompound;

public class DomainUser implements IReadWriteNBT, IDomainMember
{
    private static final HashSet<Class<? extends IUserCapability>> capabilityTypes = new HashSet<>();
    
    public static void registerCapability(Class<? extends IUserCapability> capabilityType)
    {
        capabilityTypes.add(capabilityType);
    }
    
    private static final String DOMAIN_USER_NAME = NBTDictionary.claim("domUserName");
    private static final String DOMAIN_USER_FLAGS = NBTDictionary.claim("domUserFlags");
    
    private final IDomain domain;

    public String userName;
    
    private int privilegeFlags;
    
    private final IdentityHashMap<Class<? extends IUserCapability>, IUserCapability> capabilities = new IdentityHashMap<>();

    public DomainUser(IDomain domain, String playerName)
    {
        this.domain = domain;
        this.userName = playerName;
        this.createCapabilities();
    }
    
    public DomainUser(IDomain domain, NBTTagCompound tag)
    {
        this.domain = domain;
        this.createCapabilities();
        this.deserializeNBT(tag);
    }
    
    private void createCapabilities()
    {
        this.capabilities.clear();
        if(!capabilityTypes.isEmpty())
        {
            for(Class<? extends IUserCapability> capType : capabilityTypes)
            {
                try
                {
                    IUserCapability cap;
                    cap = capType.newInstance();
                    cap.setDomainUser(this);
                    this.capabilities.put(capType, cap);
                }
                catch (Exception e)
                {
                    ExoticMatter.INSTANCE.error("Unable to create domain user capability", e);
                }
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public <V extends IUserCapability> V getCapability(Class<V> capability)
    {
        return (V) this.capabilities.get(capability);
    }

    /**
     * Will return true for admin users, regardless of other Privilege grants.
     * Will also return true if security is disabled for the domain.
     */
    public boolean hasPrivilege(Privilege p)
    {
        return  !this.domain.isSecurityEnabled()
                || Privilege.PRIVILEGE_FLAG_SET.isFlagSetForValue(Privilege.ADMIN, privilegeFlags)
                || Privilege.PRIVILEGE_FLAG_SET.isFlagSetForValue(p, privilegeFlags);
    }
    
    public void grantPrivilege(Privilege p, boolean hasPrivilege)
    {
        this.privilegeFlags = Privilege.PRIVILEGE_FLAG_SET.setFlagForValue(p, privilegeFlags, hasPrivilege);
        this.domain.setDirty();;
    }
    
    public void setPrivileges(Privilege... granted)
    {
        this.privilegeFlags = Privilege.PRIVILEGE_FLAG_SET.getFlagsForIncludedValues(granted);
        this.domain.setDirty();;
    }

    @Override
    public void serializeNBT(NBTTagCompound nbt)
    {
        nbt.setString(DOMAIN_USER_NAME, this.userName);
        nbt.setInteger(DOMAIN_USER_FLAGS, this.privilegeFlags);
        
        if(!this.capabilities.isEmpty())
        {
            for(IUserCapability cap : this.capabilities.values())
            {
                if(!cap.isSerializationDisabled()) nbt.setTag(cap.tagName(), cap.serializeNBT());
            }
        }
    }

    @Override
    public void deserializeNBT(@Nullable NBTTagCompound nbt)
    {
        this.userName = nbt.getString(DOMAIN_USER_NAME);
        this.privilegeFlags = nbt.getInteger(DOMAIN_USER_FLAGS);
        this.capabilities.clear();
        
        if(!this.capabilities.isEmpty())
        {
            for(IUserCapability cap : this.capabilities.values())
            {
                if(nbt.hasKey(cap.tagName()))
                {
                    cap.deserializeNBT(nbt.getCompoundTag(cap.tagName()));
                }
            }
        }
    }

    @Override
    public @Nullable IDomain getDomain()
    {
        return this.domain;
    }
    
}