package grondag.exotic_matter.varia;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ItemHelper
{
    // for unit testing
    public static class TestItemStack 
    {

        public Item item;
        public int size;
        public NBTTagCompound tag;
        public int meta;
        
        public TestItemStack(Item item, int size, int meta, NBTTagCompound tag)
        {
            this.item = item;
            this.size = size;
            this.meta = meta;
            this.tag = tag;
        }
       

        public Item getItem()
        {
            return this.item;
        }

        public boolean hasTagCompound()
        {
            return this.tag != null;
        }
        
        @Nullable
        public NBTTagCompound getTagCompound()
        {
            return this.tag;
        }

        public int getMetadata()
        {
            return this.meta;
        }

        public boolean areCapsCompatible(TestItemStack stack1)
        {
            return true;
        }

    }
    
    public static NBTTagCompound getOrCreateStackTag(ItemStack stack)
    {
        NBTTagCompound result = stack.getTagCompound();
        if(result == null)
        {
            result = new NBTTagCompound();
            stack.setTagCompound(result);
        }
        return result;
    }
    
    /**
     * True if item stacks can stack with each other - does not check for stack limit
     */
    @SuppressWarnings("null")
    public static boolean canStacksCombine(ItemStack stack1, ItemStack stack2)
    //public static boolean canStacksCombine(ItemStack stack1, ItemStack stack2)
    {
        if (stack1.isEmpty())
        {
            return false;
        }
        else if (stack1.getItem() != stack2.getItem())
        {
            return false;
        }
        else if (stack1.hasTagCompound() ^ stack2.hasTagCompound())
        {
            return false;
        }
        else if (stack1.hasTagCompound() && !stack1.getTagCompound().equals(stack2.getTagCompound()))
        {
            return false;
        }
        
        else if (stack1.getMetadata() != stack2.getMetadata())
        {
            return false;
        }
        else if (!stack2.areCapsCompatible(stack1))
        {
            return false;
        }
        
        return true;
    }
    
    /**
     * Returns hash codes that should be equal if {@link #canStacksCombine(ItemStack, ItemStack)} returns true;
     * Does not consider capabilities in hash code.
     */
//    public static int stackHashCode(ItemStack stack)
    @SuppressWarnings("null")
    public static int stackHashCode(TestItemStack stack)
    {
        Item item = stack.getItem();
        
        if(item == null) return 0;
        
        int hash = item.hashCode();
        
        if(stack.hasTagCompound())
        {
            hash = hash * 7919 + stack.getTagCompound().hashCode();
        }
        
        if(stack.getMetadata() != 0) 
        {
            hash = hash * 7919 + stack.getMetadata();
        }
  
        return hash;
    }
    
    /**
     * Retrieves item cap NBT - for comparison and storage purposes.
     */
    public static NBTTagCompound itemCapsNBT(ItemStack stack)
    {
        NBTTagCompound caps;
        if(stack.areCapsCompatible(ItemStack.EMPTY))
        {
            caps = null;
        }
        {
            // See what you make us do, Lex?
            NBTTagCompound lookForCaps = stack.serializeNBT();
            caps = lookForCaps.hasKey("ForgeCaps") ? lookForCaps.getCompoundTag("ForgeCaps") : null;
        }
        return caps;
    }
}
