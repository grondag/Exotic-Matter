package grondag.exotic_matter.model;

import javax.annotation.Nonnull;

import net.minecraft.util.IStringSerializable;

public enum BlockHarvestTool implements IStringSerializable
{
    ANY(null),
    PICK("pickaxe"),
    AXE("axe"),
    SHOVEL("shovel");
    
    /**
     * String MC uses to compare test for this tool type.
     * Null means any tool can harvest.
     */
    public final String toolString;
    
    private BlockHarvestTool(String toolString)
    {
        this.toolString = toolString;
    }

    @Override
    public @Nonnull String getName()
    {
        return this.name().toLowerCase();
    }
}