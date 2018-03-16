package grondag.exotic_matter;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;

public class CommonProxy 
{
	public void preInit(FMLPreInitializationEvent event) 
	{
		Log.setLog(event.getModLog());
	}

	public void init(FMLInitializationEvent event) 
	{
	    
	}

	public void postInit(FMLPostInitializationEvent event) 
	{
	}

    public void serverStarting(FMLServerStartingEvent event)
    {
    }

    public void serverStopping(FMLServerStoppingEvent event)
    {
    }
    
    public void serverAboutToStart(FMLServerAboutToStartEvent event)
    {
      
    }
}