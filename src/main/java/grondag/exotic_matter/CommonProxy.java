package grondag.exotic_matter;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;

public class CommonProxy 
{
    /**
     * Updated by client and server tick events to avoid having calls 
     * to System.currentTimeMillis() littered around the code
     * and calling it more frequently than needed.
     * 
     * MC Server does same thing, but is not exposed on client side, plus
     * we need current time in some client-side methods.
     */
    private static long currentTimeMillis;
    
    /**
     * Current system time, as of the most recent client or server tick.
     */
    public static long currentTimeMillis() { return currentTimeMillis; }
    
    public static void updateCurrentTime() { currentTimeMillis = System.currentTimeMillis(); }
    
    private static Int2ObjectOpenHashMap<LoadedWorldInfo> worldInfos 
        = new Int2ObjectOpenHashMap<LoadedWorldInfo>();
    
    public static void refreshWorldInfos()
    {
        // clearing each pass because don't want to retain unloaded worlds.
        // Solar panels would be stuck at a fixed time of day.
        worldInfos.clear();
        for(World w : net.minecraftforge.common.DimensionManager.getWorlds())
        {
            worldInfos.put(w.provider.getDimension(), new LoadedWorldInfo(w));
        }
    }
    
    @SuppressWarnings("null")
    @Nullable
    public static LoadedWorldInfo loadedWorldInfo(int dimensionID)
    {
        return worldInfos.get(dimensionID);
    }
    
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