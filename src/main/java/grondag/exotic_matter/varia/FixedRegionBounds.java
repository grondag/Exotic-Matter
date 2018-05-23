package grondag.exotic_matter.varia;

import grondag.exotic_matter.serialization.NBTDictionary;
import grondag.exotic_matter.world.PackedBlockPos;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

/**
 * Data carrier for fixed region definition to reduce number of
 * methods and method calls for fixed regions.  Fixed regions 
 * can be arbitrarily large and don't have to be cubic - shape
 * depends on interpretation by the placement builder.
 */
public class FixedRegionBounds
{
    private static final String TAG_START = NBTDictionary.claim("fixedRegionStart");
    private static final String TAG_END = NBTDictionary.claim("fixedRegionEnd");

    public final BlockPos fromPos;
    public final boolean fromIsCentered;
    public final BlockPos toPos;
    public final boolean toIsCentered;
    
    public FixedRegionBounds(BlockPos fromPos, boolean fromIsCentered, BlockPos toPos, boolean toIsCentered)
    {
        this.fromPos = fromPos;
        this.fromIsCentered = fromIsCentered;
        this.toPos = toPos;
        this.toIsCentered = toIsCentered;
    }

    public FixedRegionBounds(NBTTagCompound tag)
    {
        final long from = tag.getLong(TAG_START);
        this.fromPos = PackedBlockPos.unpack(from);
        this.fromIsCentered = PackedBlockPos.getExtra(from) == 1;
        final long to = tag.getLong(TAG_END);
        this.toPos = PackedBlockPos.unpack(to);
        this.toIsCentered = PackedBlockPos.getExtra(to) == 1;
    }

    public void saveToNBT(NBTTagCompound tag)
    {
        tag.setLong(TAG_START, PackedBlockPos.pack(this.fromPos, this.fromIsCentered ? 1 : 0));
        tag.setLong(TAG_END, PackedBlockPos.pack(this.toPos, this.toIsCentered ? 1 : 0));
    }
    
    public static boolean isPresentInTag(NBTTagCompound tag)
    {
        return tag.hasKey(TAG_START) 
                && tag.hasKey(TAG_END);
    }
}