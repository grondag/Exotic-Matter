package grondag.exotic_matter.render;

/**
 * All surfaces in a model are assigned a surface type.
 * Each surface type in a model is assigned one or more painters.
 * Painter(s) assigned to that paint type will paint all surfaces in the model with the given type.
 */
public enum SurfaceType
{
    /** Surface that comprises most of the model. Can glow or be translucent. May or may not align with a block face. */
    MAIN,

    /**
     * Cuts into the block or the cut sides of blocks that result from CSG cuts to world block boundaries.
     */
    CUT,
    
    /** 
     * Surface that should be distinct from other surfaces. 
     * Typically used for inset lamp surfaces. 
     */
    LAMP
}
