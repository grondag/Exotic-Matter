package grondag.exotic_matter.model.painting;

import grondag.exotic_matter.serialization.NBTDictionary;
import net.minecraft.util.text.translation.I18n;

public enum PaintLayer
{
    /** 
     * Textures the MAIN surface.  
     * Must always be present.
     */
    BASE(SurfaceType.MAIN),
    
    /**
     * Textures the MAIN and CUT surfaces.  
     * Provides dirt or other character to a base layer.
     * Optional.
     * Middle z-position when other layers are present.
     * Will generally be partial quads, translucent or clipped.
     * Individual quads can be solid or translucent, shaded or lit.
     * Has separate color.
     */
    MIDDLE(SurfaceType.MAIN),
    
    /**
      * Textures the MAIN surface only.  
     * Provides dirt or other character to a base layer.
     * Optional.
     * Outer z-position when other layers are present.
     * Will generally be partial quads, translucent or clipped.
     * Individual quads can be solid or translucent, shaded or lit.
     * Has separate color.
     */
    OUTER(SurfaceType.MAIN),
    
    /**
     * Textures the LAMP surface.  
     * Optional.
     * Middle z-position.
     * Has separate color.
     */
    LAMP(SurfaceType.LAMP),
    
    /**
     * Textures the CUT surface. 
     * Same color and lighting mode as base layer but may have a separate texture.
     * Use static index (ordinal) for texture and dyamic index for color, lighting.
     * Must always be present.
     */
    CUT(SurfaceType.CUT);
    

    /** slightly more convenient than values().length, also more clear - includes CUT in addition to dynamic values*/
    public static final int SIZE;
    
    /** Does include the CUT layer. Sane as values(), but more clear. */
    public static final PaintLayer VALUES[];
    static
    {
        SIZE = values().length;
        VALUES = values();
    }
    
    public final SurfaceType surfaceType;
    
    /**
     * NBT Tag name used to save textures for this paint layer.
     */
    public final String tagName;
    
    private PaintLayer(SurfaceType surfaceType)
    {
        this.surfaceType = surfaceType;
        this.tagName = NBTDictionary.claim("tex_" + this.name().toLowerCase());
    }
    
    @SuppressWarnings("deprecation")
    public String localizedName()
    {
        return I18n.translateToLocal("paintlayer." + this.name().toLowerCase());
    }
    
}
