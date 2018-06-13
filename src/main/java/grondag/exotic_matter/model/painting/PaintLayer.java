package grondag.exotic_matter.model.painting;

import grondag.exotic_matter.serialization.NBTDictionary;
import grondag.exotic_matter.varia.BinaryEnumSet;
import net.minecraft.util.text.translation.I18n;

/**
 * Primitive models can have up to five paint layers.
 * These are vaguely akin to shaders or materials in that
 * each layer will have a texture and other appearance-defining
 * attributes.<p>
 * 
 * The layers have names that describe typical use but
 * they can be used for anything, provided the mesh generator
 * and the model state agree on which surfaces should get which paint.<p>
 * 
 */
@SuppressWarnings("deprecation")
public enum PaintLayer
{
    /** 
     * Typically used as the undecorated appearance the primary surface, often the only paint layer.
     * Lowest (unmodified) z position.  
     */
    BASE,
    
    /**
     * Typically used to decorate the primary surface.
     * Middle z-position when other layers are present.
     */
    MIDDLE,
    
    /**
     * Typically used to decorate the primary surface.
     * Outer z-position when other layers are present.
     */
    OUTER,
    
    /**
     * Typically used to render a secondary surface within a model.
     * Middle z-position.
     */
    LAMP,
    
    /**
     * Typically used to render sides or bottoms, or the cut surfaces
     * of CSG outputs.
     */
    CUT;
    

    /** Convenience for values().length */
    public static final int SIZE;
    
    /** Convenience for values() */
    public static final PaintLayer VALUES[];
    
    static
    {
        SIZE = values().length;
        VALUES = values();
    }
    
    public static BinaryEnumSet<PaintLayer> BENUMSET = new BinaryEnumSet<>(PaintLayer.class);
    
    /**
     * NBT Tag name used to save textures for this paint layer.
     */
    public final String tagName;
    
    private PaintLayer()
    {
        this.tagName = NBTDictionary.claim("tex_" + this.name().toLowerCase());
    }
    
    public String localizedName()
    {
        return I18n.translateToLocal("paintlayer." + this.name().toLowerCase());
    }
    
}
