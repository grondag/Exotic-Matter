package grondag.exotic_matter;


import java.util.Map;

import javax.annotation.Nullable;

import grondag.exotic_matter.block.DummyColorHandler;
import grondag.exotic_matter.block.ISuperBlock;
import grondag.exotic_matter.block.SuperModelLoader;
import grondag.exotic_matter.block.SuperTileEntity;
import grondag.exotic_matter.model.color.BlockColorMapProvider;
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
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy
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

    @Override
    public void preInit(FMLPreInitializationEvent event)
    {
        super.preInit(event);
        ModelLoaderRegistry.registerLoader(SuperModelLoader.INSTANCE);
        if(ConfigXM.RENDER.debugOutputColorAtlas)
        {
            BlockColorMapProvider.writeColorAtlas(event.getModConfigurationDirectory());
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

}
