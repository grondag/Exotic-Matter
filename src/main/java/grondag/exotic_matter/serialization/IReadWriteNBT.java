package grondag.exotic_matter.serialization;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * Slightly more flexible version of INBTSerializable that allows for
 * writing to an existing tag instead of always creating a new one.
 */
public interface IReadWriteNBT extends INBTSerializable<NBTTagCompound>
{
    @Override
    public abstract void deserializeNBT(@Nullable NBTTagCompound tag);
    
    public abstract void serializeNBT(NBTTagCompound tag);
    
    @Override
    default public NBTTagCompound serializeNBT()
    {
        NBTTagCompound result = new NBTTagCompound();
        this.serializeNBT(result);
        return result;
    }
}
