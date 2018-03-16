package grondag.exotic_matter;

import org.eclipse.jdt.annotation.Nullable;

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

	@SidedProxy(clientSide = "grondag.exotic_matter.ClientProxy", serverSide = "grondag.exotic_matter.ServerProxy")
	@Nullable
	public static CommonProxy proxy;

    static
    {
        FluidRegistry.enableUniversalBucket();
    }
    
    @EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		if(proxy != null) proxy.preInit(event);
	}

	@EventHandler
	public void init(FMLInitializationEvent event)
	{
	    if(proxy != null) proxy.init(event);
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{
	    if(proxy != null) proxy.postInit(event);
	}
	
	
   @EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) 
   {
       if(proxy != null) proxy.serverAboutToStart(event);
    }
   
	@EventHandler
    public void serverStarting(FMLServerStartingEvent event) 
	{
	    if(proxy != null) proxy.serverStarting(event);
    }

   @EventHandler
   public void serverStopping(FMLServerStoppingEvent event)
   {
       if(proxy != null) proxy.serverStopping(event);
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