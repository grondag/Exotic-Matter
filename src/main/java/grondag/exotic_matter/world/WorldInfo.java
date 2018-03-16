package grondag.exotic_matter.world;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber
public class WorldInfo
{
    
    private static Int2FloatOpenHashMap brightnessFactors = new Int2FloatOpenHashMap(0);

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
    
    private static void updateCurrentTime() { currentTimeMillis = System.currentTimeMillis(); }
    
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) 
    {
        if(event.phase == Phase.START && FMLCommonHandler.instance().getSide() == Side.SERVER) 
        {
            updateCurrentTime();
            refreshBrightness();
        }
    }
    
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) 
    {
        if(event.phase == Phase.START) 
        {
            updateCurrentTime();
            refreshBrightness();
        }
    }
    
    private static void refreshBrightness()
    {
        World[] worlds = net.minecraftforge.common.DimensionManager.getWorlds();
        
        Int2FloatOpenHashMap newFactors = new Int2FloatOpenHashMap(worlds.length);

        for(World w : worlds)
        {
            newFactors.put(w.provider.getDimension(), w.getSunBrightnessFactor(0));
        }
        
        brightnessFactors = newFactors;
    }
    
    /**
     * Can be safely called off-tick and from multiple threads.<br>
     * Will return 0 if world not loaded or not found.<br>
     * Valid for both client and server.<br>
     */
    public static float sunBrightnessFactor(int dimensionID)
    {
        return brightnessFactors.get(dimensionID);
    }

}
