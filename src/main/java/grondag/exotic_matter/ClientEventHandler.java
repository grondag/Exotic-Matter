package grondag.exotic_matter;

import grondag.exotic_matter.model.ITexturePalette;
import grondag.exotic_matter.model.TextureLayout;
import grondag.exotic_matter.model.TexturePaletteRegistry;
import grondag.exotic_matter.render.CompressedAnimatedSprite;
import grondag.exotic_matter.render.EnhancedSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientEventHandler
{

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
    }
}
