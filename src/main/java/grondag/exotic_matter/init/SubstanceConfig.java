package grondag.exotic_matter.init;

import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.RangeDouble;
import net.minecraftforge.common.config.Config.RangeInt;
import net.minecraftforge.common.config.Config.RequiresMcRestart;

public class SubstanceConfig
{
    @RequiresMcRestart
    @Comment("Material hardness. 2 is typical for things like rock, wood. Anything above 10 is extreme. -1 is unbreakable")
    @RangeInt(min = -1, max = 2000)
    public int hardness;

    @RequiresMcRestart
    @Comment("Tool used to break block. Normal values are pickaxe, shovel and axe")
    public String harvestTool;

    @RequiresMcRestart
    @Comment("Level of tool needed to break block. Range 1-3 is normal for vanilla.")
    @RangeInt(min = 0, max = 10)
    public int harvestLevel;

    @RequiresMcRestart
    @Comment("Material explosion resistance")
    @RangeInt(min = 1, max = 2000)
    public int resistance;

    @RequiresMcRestart
    @Comment("Material speed modifier for entities walking on its surface.")
    @RangeDouble(min = 0.25, max = 2.0)
    public double walkSpeedFactor;

    public SubstanceConfig(int hardness, String harvestTool, int harvestLevel, int resistance, double walkSpeedFactor)
    {
        this.hardness = hardness;
        this.harvestTool = harvestTool;
        this.harvestLevel = harvestLevel;
        this.resistance = resistance;
        this.walkSpeedFactor = walkSpeedFactor;
    }
}