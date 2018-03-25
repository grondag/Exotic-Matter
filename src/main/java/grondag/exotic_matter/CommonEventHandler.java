package grondag.exotic_matter;

import java.util.Map;

import grondag.exotic_matter.block.SuperModelBlock;
import grondag.exotic_matter.init.IBlockItemRegistrator;
import grondag.exotic_matter.player.ModifierKeys;
import mcjty.theoneprobe.TheOneProbe;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Config.Type;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

@Mod.EventBusSubscriber
public class CommonEventHandler 
{
    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) 
    {
        if (event.getModID().equals(ExoticMatter.MODID))
        {
            ConfigManager.sync(ExoticMatter.MODID, Type.INSTANCE);
            ConfigXM.recalcDerived();
        }
    }
    
    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) 
    {
        SuperModelBlock.registerSuperModelBlocks(event);
    }
    
    @SubscribeEvent
    public static void attachCaps(AttachCapabilitiesEvent<Entity> event)
    {
        if(event.getObject() instanceof EntityPlayer)
        {
            if (!event.getObject().hasCapability(ModifierKeys.CAP_INSTANCE, null)) {
                event.addCapability(new ResourceLocation(TheOneProbe.MODID, "PlayerCaps"), new ModifierKeys());
            }
        }
    }
    
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) 
    {
        IForgeRegistry<Item> itemReg = event.getRegistry();
        IForgeRegistry<Block> blockReg = GameRegistry.findRegistry(Block.class);
        
        for(Map.Entry<ResourceLocation, Block> entry: blockReg.getEntries())
        {
            if(entry.getValue() instanceof IBlockItemRegistrator)
            {
                ((IBlockItemRegistrator)entry.getValue()).registerItems(itemReg);
            }
        }
    }
}
