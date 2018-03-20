package grondag.exotic_matter.simulator.persistence;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.storage.WorldSavedData;

public class PersistenceDelegate extends WorldSavedData
{
    /** saves initial nbt load until we know our node reference */
    private NBTTagCompound nbtCache;
    
    private ISimulationNode pnode = null;
    
    public PersistenceDelegate(ISimulationNode pnode) 
    {
        super(pnode.tagName());
        this.pnode = pnode;
    }
    
    /**
     * Needed by WorldSavedData load constructor.
     * PersistenceManager will call {@link #setNode(ISimulationNode)} after.
     */
    public PersistenceDelegate(String tagName) 
    {
        super(tagName);
    }
    
    /**
     * Used on load to set node after instantiation.
     * Necessary because WorldSavedData thingymabobber creates the instance
     * and doesn't know / isn't able to do this.
     */
    public void setNode(ISimulationNode pnode)
    {
        this.pnode = pnode;
    }
    
    @Override
    public void readFromNBT(@Nonnull NBTTagCompound nbt)
    {
        if(pnode == null)
        {
            nbtCache = nbt;
        }
        else 
        {
            pnode.deserializeNBT(nbt); 
        }
    }
    
    public void readCachedNBT()
    {
        if(pnode != null && nbtCache != null)
        {
            pnode.deserializeNBT(this.nbtCache);
            nbtCache = null;
        }
    }
    
    @Override
    public @Nonnull NBTTagCompound writeToNBT(@Nonnull NBTTagCompound nbt) 
    { 
        if(pnode != null) pnode.serializeNBT(nbt);
        return nbt;
    }

    @Override
    public boolean isDirty() { return pnode == null ? false : pnode.isSaveDirty(); }

    @Override
    public void setDirty(boolean isDirty) 
    { 
        if(pnode != null) pnode.setSaveDirty(isDirty); 
    }
}