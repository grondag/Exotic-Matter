package grondag.exotic_matter.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

import grondag.exotic_matter.Log;
import grondag.exotic_matter.init.SubstanceConfig;
import grondag.exotic_matter.serialization.NBTDictionary;
import grondag.exotic_matter.varia.ILocalized;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.translation.I18n;

/**
 * Similar to Minecraft Material. Didn't want to tie to that implementation.
 * Determines Minecraft material and other physical properties.
 */
public class BlockSubstance implements ILocalized
{
    private static final String NBT_SUBSTANCE = NBTDictionary.claim("substance");
    
    /**
     * Finite number of substances defined to facilitate bit-wise serialization to client GUIs
     */
    public static final int MAX_SUBSTANCES = 4096;
    
    private static final HashMap<String, BlockSubstance> allByName = new HashMap<>();
    private static final ArrayList<BlockSubstance> allByOrdinal = new ArrayList<>();
    private static final List<BlockSubstance> allReadOnly = Collections.unmodifiableList(allByOrdinal);
    
    private static int nextOrdinal = 0;
    
	public static BlockSubstance deserializeNBT(NBTTagCompound tag)
    {
        return allByName.get(tag.getString(NBT_SUBSTANCE));
    }


    public static BlockSubstance fromBytes(PacketBuffer pBuff)
    {
        int ordinal = pBuff.readByte();
        return ordinal >= 0 && ordinal < allByOrdinal.size() ? allByOrdinal.get(ordinal) : null;
    }

    @Nullable
    public static BlockSubstance get(String systemName)
    {
        return allByName.get(systemName);
    }
    
    @Nullable
    public static BlockSubstance get(int ordinal)
    {
        return ordinal < 0 || ordinal >= allByOrdinal.size() ? null : allByOrdinal.get(ordinal);
    }
	    
    public static BlockSubstance create(String systemName, SubstanceConfig config, Material material, SoundType sound, int defaultColorMapID, boolean isHyperMaterial)
    {
        BlockSubstance existing = get(systemName);
        if(existing != null)
        {
            assert false : "Duplicate substance name";
            Log.warn("Block substance with duplicate name %s not created.  Existing substance with that name be used instead.", systemName);
            return existing;
        }
        
        return new BlockSubstance(systemName, config, material, sound, defaultColorMapID, isHyperMaterial);
    }
    
    public static BlockSubstance create(String systemName, SubstanceConfig config, Material material, SoundType sound, int defaultColorMapID)
    {
        return create(systemName, config, material, sound, defaultColorMapID, false);
    }
    
    public static BlockSubstance createHypermatter(String systemName, SubstanceConfig config, Material material, SoundType sound, int defaultColorMapID)
    {
        return create(systemName, config, material, sound, defaultColorMapID, true);
    }
    
	public final Material material;
	public final SoundType soundType;

	public final String systemName;
	public final int ordinal;
	public final int hardness;
	public final int resistance;
	public final BlockHarvestTool harvestTool;
	public final int harvestLevel;
	public final int defaultColorMapID;
	public final boolean isHyperMaterial;
	public final boolean isTranslucent;
	public final double walkSpeedFactor;
	
	private BlockSubstance(String systemName, SubstanceConfig substance, Material material, SoundType sound, int defaultColorMapID, boolean isHyperMaterial) 
	{
	    this.systemName = systemName;
	    this.ordinal = nextOrdinal++;
		this.material = material;
		this.isHyperMaterial = isHyperMaterial;
		soundType = sound;
		this.defaultColorMapID = defaultColorMapID;
		this.isTranslucent = this.material == Material.GLASS;

		this.hardness = substance.hardness;
		this.resistance = substance.resistance;
		this.harvestTool = substance.harvestTool;
		this.harvestLevel = substance.harvestLevel;
		this.walkSpeedFactor = substance.walkSpeedFactor;
		
		if(this.ordinal < MAX_SUBSTANCES)
		{  
		    allByName.put(systemName, this);
            allByOrdinal.add(this);
		}
        else
        {
            Log.warn("Block substance limit of %d exceeded.  Substance %s will not be usable.", MAX_SUBSTANCES, systemName);
        }
		
	}
	

    @Override
    @SuppressWarnings("deprecation")
    public String localizedName()
    {
        return I18n.translateToLocal("material." + this.systemName.toLowerCase());
    }
    
    public void serializeNBT(NBTTagCompound tag)
    {
        tag.setString(NBT_SUBSTANCE, this.systemName);
    }
    
    public void toBytes(PacketBuffer pBuff)
    {
        pBuff.writeByte(this.ordinal);
    }
    
    public static List<BlockSubstance> all()
    {
        return allReadOnly;
    }
}