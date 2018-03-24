package grondag.exotic_matter;


import javax.annotation.Nonnull;

import grondag.exotic_matter.block.SuperModelBlock;
import grondag.exotic_matter.model.BlockSubstance;
import grondag.exotic_matter.model.ModelState;
import grondag.exotic_matter.network.PacketHandler;
import grondag.exotic_matter.network.PacketUpdateModifierKeys;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod(   modid = ExoticMatter.MODID, 
        name = ExoticMatter.MODNAME,
        version = ExoticMatter.VERSION,
        acceptedMinecraftVersions = "[1.12]",
        dependencies = "after:theoneprobe")

public class ExoticMatter 
{
	public static final String MODID = "exotic_matter";
	public static final String MODNAME = "Exotic Matter";
	public static final String VERSION = "0.0.1";
	
	@Instance
	public static ExoticMatter INSTANCE = new ExoticMatter();

	public static CreativeTabs tabMod = new CreativeTabs("Hard Science") 
    {
        @Override
        @SideOnly(Side.CLIENT)
        public @Nonnull ItemStack getTabIconItem() 
        {
            return SuperModelBlock.findAppropriateSuperModelBlock(BlockSubstance.DEFAULT, new ModelState()).getSubItems().get(0);
        }
    };
	    
	@SidedProxy(clientSide = "grondag.exotic_matter.ClientProxy", serverSide = "grondag.exotic_matter.ServerProxy")
	@SuppressWarnings("null")
	public static CommonProxy proxy;

    static
    {
        FluidRegistry.enableUniversalBucket();
        
        // Packets handled on Server side, sent from Client
        PacketHandler.registerMessage(PacketUpdateModifierKeys.class, PacketUpdateModifierKeys.class, Side.SERVER);
        
        // Packets handled on Client side, sent from Server        
    }
    
    @EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		proxy.preInit(event);
	}

	@EventHandler
	public void init(FMLInitializationEvent event)
	{
	    proxy.init(event);
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{
	    proxy.postInit(event);
	}
	
	
   @EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) 
   {
       proxy.serverAboutToStart(event);
    }
   
	@EventHandler
    public void serverStarting(FMLServerStartingEvent event) 
	{
	    proxy.serverStarting(event);
    }

   @EventHandler
   public void serverStopping(FMLServerStoppingEvent event)
   {
       proxy.serverStopping(event);
   }
   
   /**
    * Puts mod ID and . in front of whatever is passed in
    */
   public static String prefixName(String name)
   {
       return String.format("%s.%s", MODID, name.toLowerCase());
   }
   
   public static String prefixResource(String name)
   {
       return String.format("%s:%s", MODID, name.toLowerCase());
   }
   
   public static ResourceLocation resource(String name)
   {
       return new ResourceLocation(prefixResource(name));
   }
}