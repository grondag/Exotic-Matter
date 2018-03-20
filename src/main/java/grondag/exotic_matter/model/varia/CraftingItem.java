package grondag.exotic_matter.model.varia;

import grondag.exotic_matter.model.BlockColorMapProvider;
import grondag.exotic_matter.model.ISuperModelState;
import grondag.exotic_matter.model.PaintLayer;
import net.minecraft.item.Item;

/**
 * Generic item class with a SuperModel render and state.
 * Be sure to set creative tab for mod that uses it.
 */
public class CraftingItem extends Item
{
    public final ISuperModelState modelState;
    
    public CraftingItem(String name, ISuperModelState modelState)
    {
        super();
        this.modelState = modelState;
        int colorIndex = this.hashCode() % BlockColorMapProvider.INSTANCE.getColorMapCount();
        this.modelState.setColorMap(PaintLayer.BASE, 
                BlockColorMapProvider.INSTANCE.getColorMap(colorIndex));
        this.setRegistryName(name);
        this.setUnlocalizedName(name);
    }
}
