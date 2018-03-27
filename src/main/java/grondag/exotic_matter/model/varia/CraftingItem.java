package grondag.exotic_matter.model.varia;

import grondag.exotic_matter.block.SuperDispatcher;
import grondag.exotic_matter.init.IItemModelRegistrant;
import grondag.exotic_matter.model.BlockColorMapProvider;
import grondag.exotic_matter.model.ISuperModelState;
import grondag.exotic_matter.model.PaintLayer;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelBakeEvent;

/**
 * Generic item class with a SuperModel render and state.
 * Be sure to set creative tab for mod that uses it.
 */
public class CraftingItem extends Item implements IItemModelRegistrant
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

    @Override
    public void handleBake(ModelBakeEvent event)
    {
        event.getModelRegistry().putObject(new ModelResourceLocation(this.getRegistryName(), "inventory"),
                SuperDispatcher.INSTANCE.getItemDelegate());        
    }
}
