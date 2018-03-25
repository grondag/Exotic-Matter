package grondag.exotic_matter;

import java.io.IOException;
import java.util.Map;

import grondag.exotic_matter.block.SuperBlockTESR;
import grondag.exotic_matter.block.SuperDispatcher;
import grondag.exotic_matter.block.SuperDispatcher.DispatchDelegate;
import grondag.exotic_matter.block.SuperModelTileEntityTESR;
import grondag.exotic_matter.block.SuperStateMapper;
import grondag.exotic_matter.block.SuperTileEntity;
import grondag.exotic_matter.block.SuperTileEntityTESR;
import grondag.exotic_matter.font.FontHolder;
import grondag.exotic_matter.model.ISuperBlock;
import grondag.exotic_matter.model.ITexturePalette;
import grondag.exotic_matter.model.TextureLayout;
import grondag.exotic_matter.model.TexturePaletteRegistry;
import grondag.exotic_matter.model.varia.BlockHighlighter;
import grondag.exotic_matter.network.PacketHandler;
import grondag.exotic_matter.network.PacketUpdateModifierKeys;
import grondag.exotic_matter.player.ModifierKeys;
import grondag.exotic_matter.render.CompressedAnimatedSprite;
import grondag.exotic_matter.render.EnhancedSprite;
import grondag.exotic_matter.render.QuadCache;
import grondag.exotic_matter.render.TextureHelper;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientEventHandler
{
    /** used to detect key down/up for modifier keys */
    private static int modifierKeyFlags = 0;
    
    private static int clientStatCounter = ConfigXM.RENDER.clientStatReportingInterval * 20;

    
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) 
    {
        if(event.phase == Phase.START) 
        {
            Minecraft mc = Minecraft.getMinecraft();
            EntityPlayerSP player = mc.player;
            
            if(player != null && player.world != null)
            {
                int keyFlags = (GuiScreen.isCtrlKeyDown() ? ModifierKeys.ModifierKey.CTRL_KEY.flag : 0) 
                        | (GuiScreen.isAltKeyDown() ? ModifierKeys.ModifierKey.ALT_KEY.flag : 0);
                
                if(keyFlags != modifierKeyFlags)
                {
                    modifierKeyFlags = keyFlags;
                    ModifierKeys.setModifierFlags(Minecraft.getMinecraft().player, keyFlags);
                    PacketHandler.CHANNEL.sendToServer(new PacketUpdateModifierKeys(keyFlags));
                }
            }
        }
        else
        {
            if ((ConfigXM.RENDER.enableQuadCacheStatistics || ConfigXM.RENDER.enableAnimationStatistics)
                    && --clientStatCounter == 0) 
            {
                clientStatCounter = ConfigXM.RENDER.clientStatReportingInterval * 20;
                
                if(ConfigXM.RENDER.enableQuadCacheStatistics)
                {
                    ExoticMatter.INSTANCE.info("QuadCache stats = " + QuadCache.INSTANCE.cache.stats().toString());
                }
    
                if(ConfigXM.RENDER.enableAnimatedTextures && ConfigXM.RENDER.enableAnimationStatistics)
                {
                    CompressedAnimatedSprite.perfCollectorUpdate.outputStats();
                    CompressedAnimatedSprite.perfCollectorUpdate.clearStats();
                }
            }

        }
    }
    
    @SubscribeEvent()
    public static void onRenderTick(TickEvent.RenderTickEvent event)
    {
        if(event.phase == Phase.START) ClientProxy.updateCamera();
    }
    
    /**
     * Register all textures that will be needed for associated models. 
     * Happens before model bake.
     */
    @SubscribeEvent
    public static void stitcherEventPre(TextureStitchEvent.Pre event)
    {
        TextureMap map = event.getMap();
        
        for(ITexturePalette p : TexturePaletteRegistry.all())
        {
            for(String s : p.getTexturesForPrestich())
            {
                ResourceLocation loc = new ResourceLocation(s);
                
                if(p.textureLayout() == TextureLayout.BIGTEX_ANIMATED)
                {
                    if(map.getTextureExtry(loc.toString()) == null)
                    {
                        map.setTextureEntry(new CompressedAnimatedSprite(loc, p.ticksPerFrame()));
                    }
                }
                else
                {
                    if(map.getTextureExtry(loc.toString()) == null)
                    {
                        map.setTextureEntry(new EnhancedSprite(loc.toString()));
                    }
                    map.registerSprite(loc);
                }
            }
        }
        
        FontHolder.preStitch(event);
    }
    
    @SubscribeEvent
    public static void stitcherEventPost(TextureStitchEvent.Post event)
    {
        TextureHelper.postStitch();
        FontHolder.postStitch(event);
        
        if(ConfigXM.RENDER.enableAnimationStatistics && CompressedAnimatedSprite.perfLoadRead.runCount() > 0)
        {
            CompressedAnimatedSprite.perfCollectorLoad.outputStats();
            CompressedAnimatedSprite.perfCollectorLoad.clearStats();
            CompressedAnimatedSprite.reportMemoryUsage();
        }
        
        CompressedAnimatedSprite.tearDown();
    }
    
    /**
     * Check for blocks that need a custom block highlight and draw if checked.
     * Adapted from the vanilla highlight code.
     */
    @SubscribeEvent
    public static void onDrawBlockHighlightEvent(DrawBlockHighlightEvent event) 
    {
        BlockHighlighter.handleDrawBlockHighlightEvent(event);
    }
    
    @SubscribeEvent
    public static void onActionPerformed(ActionPerformedEvent.Pre event)
    {
        if(event.getGui() != null && event.getGui() instanceof GuiOptions )
        {
            SuperTileEntity.updateRenderDistance();
        }
    }
    
    @SubscribeEvent
    public static void modelRegistryEvent(ModelRegistryEvent event)
    {
        IForgeRegistry<Block> blockReg = GameRegistry.findRegistry(Block.class);
    
        for(Map.Entry<ResourceLocation, Block> entry: blockReg.getEntries())
        {
            Block block = entry.getValue();
            if(block instanceof ISuperBlock)
            {
                ModelLoader.setCustomStateMapper(block, SuperStateMapper.INSTANCE);
            }
        }
        
        // Bind TESR to tile entity
        ClientRegistry.bindTileEntitySpecialRenderer(SuperTileEntityTESR.class, SuperBlockTESR.INSTANCE);
        ClientRegistry.bindTileEntitySpecialRenderer(SuperModelTileEntityTESR.class, SuperBlockTESR.INSTANCE);
    }
    
    @SubscribeEvent()
    public static void onModelBakeEvent(ModelBakeEvent event) throws IOException
    {
        SuperDispatcher.INSTANCE.clear();
     
        for(DispatchDelegate delegate : SuperDispatcher.INSTANCE.delegates)
        {
            event.getModelRegistry().putObject(new ModelResourceLocation(delegate.getModelResourceString()), delegate);
        }
    }
}
