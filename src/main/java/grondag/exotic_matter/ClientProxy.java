package grondag.exotic_matter;


import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import grondag.acuity.api.IAcuityListener;
import grondag.acuity.api.IAcuityRuntime;
import grondag.acuity.api.IPipelineManager;
import grondag.acuity.api.IRenderPipeline;
import grondag.acuity.api.TextureFormat;
import grondag.exotic_matter.block.DummyColorHandler;
import grondag.exotic_matter.block.ISuperBlock;
import grondag.exotic_matter.block.SuperModelLoader;
import grondag.exotic_matter.block.SuperTileEntity;
import grondag.exotic_matter.model.collision.CollisionBoxDispatcher;
import grondag.exotic_matter.model.color.BlockColorMapProvider;
import grondag.exotic_matter.model.varia.SuperDispatcher;
import grondag.exotic_matter.statecache.IWorldStateCache;
import grondag.exotic_matter.statecache.WorldStateCache;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.animation.Animation;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy implements IAcuityListener
{
    /**
     * Nulled out at start of each render and then initialized if needed.
     * Allows reuse whereever needed
     */
    @Nullable
    private static ICamera camera;
    private static double cameraX;
    private static double cameraY;
    private static double cameraZ;
    
    private static float worldTime;
    
    private static @Nullable IAcuityRuntime acuity;
    private static @Nullable IPipelineManager pipelineManager;
    
    private static void refreshCamera()
    {
        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
        if(entity == null) return;
        float partialTicks = Animation.getPartialTickTime();
        
        ICamera newCam = new Frustum();
        double d0 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double d1 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double d2 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        newCam.setPosition(d0, d1, d2);
        cameraX = d0;
        cameraY = d1;
        cameraZ = d2;
        camera = newCam;
    }

    public static void updateCamera()
    {
        camera = null;
        
        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
        if(entity == null) return;

        float partialTicks = Animation.getPartialTickTime();
        if(entity.world != null)
            worldTime = Animation.getWorldTime(entity.world, partialTicks);
    }

    @Nullable
    public static ICamera camera()
    {
        if(camera == null) refreshCamera();
        return camera;
    }

    public static double cameraX()
    {
        if(camera == null) refreshCamera();
        return cameraX;
    }
    
    public static double cameraY()
    {
        if(camera == null) refreshCamera();
        return cameraY;
    }
    
    public static double cameraZ()
    {
        if(camera == null) refreshCamera();
        return cameraZ;
    }

    public static @Nullable IAcuityRuntime acuity()
    {
        return acuity;
    }
    
    @Override
    public boolean isAcuityEnabled()
    {
        return acuity != null && acuity.isAcuityEnabled();
    }
    
    @Override
    public void preInit(FMLPreInitializationEvent event)
    {
        super.preInit(event);
        ModelLoaderRegistry.registerLoader(SuperModelLoader.INSTANCE);
        if(ConfigXM.RENDER.debugOutputColorAtlas)
        {
            BlockColorMapProvider.writeColorAtlas(event.getModConfigurationDirectory());
        }
        
        FMLInterModComms.sendFunctionMessage("acuity", "getAcuityRuntime", "grondag.exotic_matter.ClientProxy$AcuityRuntimeConsumer");

    }
    
    public static class AcuityRuntimeConsumer implements Function<IAcuityRuntime, Void>
    {
        @Override
        public @Nullable Void apply(@Nullable IAcuityRuntime runtime)
        {
            acuity = runtime;
            pipelineManager = runtime.getPipelineManager();
            runtime.registerListener((ClientProxy)ExoticMatter.proxy);
            return null;
        }
    }
    
    @Override
    public void init(FMLInitializationEvent event) 
    {
        IForgeRegistry<Block> blockReg = GameRegistry.findRegistry(Block.class);
        
        for(Map.Entry<ResourceLocation, Block> entry: blockReg.getEntries())
        {
            Block block = entry.getValue();
            if(block instanceof ISuperBlock)
            {
                // won't work in pre-init because BlockColors/ItemColors aren't instantiated yet
                // Minecraft.getMinecraft().getBlockColors().registerBlockColorHandler(block.blockModelHelper.dispatcher, block);
                Minecraft.getMinecraft().getItemColors().registerItemColorHandler(DummyColorHandler.INSTANCE, block);
            }
        }
    }
    
    @Override
    public void postInit(FMLPostInitializationEvent event) 
    {
        super.postInit(event);
        SuperTileEntity.updateRenderDistance();
    }
    
    static final IWorldStateCache worldStateCache = new WorldStateCache();

    @Override
    public IWorldStateCache clientWorldStateCache()
    {
        return worldStateCache;
    }

    public static float getWorldTime()
    {
        return worldTime;
    }

    @Override
    public void onAcuityStatusChange(boolean newEnabledStatus)
    {
        // force rebuild of containers because render layouts may change
        SuperDispatcher.INSTANCE.clear();
        
        // some blocks may produce different model states depending on API status
        this.clientWorldStateCache().clear();
    }

    @Override
    public void onRenderReload()
    {
        // enable force reload of cached collision boxes for debugging
        CollisionBoxDispatcher.clear();
    }

    /**
     * Handles null checking and shortens pointer chase
     */
    public static @Nullable IRenderPipeline acuityPipeline(int value)
    {
        final IPipelineManager p = pipelineManager;
        return p == null ? null : p.getPipelineByIndex(value);
    }
    
    /**
     * Handles null checking and shortens pointer chase
     */
    public static @Nullable IRenderPipeline acuityDefaultPipeline(TextureFormat format)
    {
        final IPipelineManager p = pipelineManager;
        return p == null ? null : p.getDefaultPipeline(format);
    }
}
