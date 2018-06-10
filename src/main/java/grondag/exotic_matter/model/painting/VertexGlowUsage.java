package grondag.exotic_matter.model.painting;

/**
 * Governs how vertex glow values (0-255) 
 * are used by quad painters to transform per-vertex
 * lightmaps and alpha.<p>
 * 
 * Painter must scale lightmap values from 0-255 to 0-15.<p>
 * 
 * Note that vertex effects may be governed by the surface,
 * and in those cases this value may be ignored. For example,
 * If the surface being painted is a lamp gradient, glow is 
 * used to blend lamp color/brightness with normal color/brightness. 
 */
public enum VertexGlowUsage
{
    IGNORE,
    
    /**
     * Glow is used directly as the output, scaled for lightmaps.
     */
    REPLACE,
    
    /**
     * 255 - glow is used directly as the output, scaled for lightmaps.
     */
    REPLACE_INVERSE,
    
    /**
     * Glow is converted to a 0-1 float value and result multiplied.
     */
    MULTIPLY,
    
    /**
     * Glow is converted to a 1-0 (inverted) float value and result multiplied.
     */
    MULTIPLY_INVERSE,
    
    /**
     * Glow (scaled for lightmap) is used as the min clamp value.
     */
    MIN,
    
    /**
     * Glow (scaled for lightmap) is used as the max clamp value.
     */
    MAX
}
