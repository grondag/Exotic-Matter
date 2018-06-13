package grondag.exotic_matter.model.painting;

import net.minecraft.util.math.MathHelper;

public class Surface
{
    private static int clampUVScale(int rawValue)
    {
        if(rawValue <= 1) return 1;
        
        if((rawValue & rawValue - 1) == 0) return rawValue;
        
        final int high = MathHelper.smallestEncompassingPowerOfTwo(rawValue);
        final int low = high / 2;
        
        return (high - rawValue) < (rawValue - low) ? high : low;
    }
    
    private static int clampUVScale(double rawValue)
    {
        if(rawValue <= 1) return 1;
        
        return clampUVScale((int) Math.round(rawValue));
    }
    
    public final SurfaceType surfaceType;
    public final SurfaceTopology topology;
    
    /**
     * Instance with unit scale uvScale = 1, uses depth for bigtex
     */
    public final SurfaceInstance unitInstance;
    
    public Surface(SurfaceType paintType, SurfaceTopology topology)
    {
        this.surfaceType = paintType;
        this.topology = topology;
        this.unitInstance = new SurfaceInstance(1, false, true, 0, false);
    }
    
    public SurfaceInstance newInstance(int uvScale)
    {
        return new SurfaceInstance(clampUVScale(uvScale), false, true, 0, false);
    }
    
    public SurfaceInstance newInstance(double uvScale)
    {
        return new SurfaceInstance(clampUVScale(uvScale), false, true, 0, false);
    }
    
    public SurfaceInstance newInstance(int uvScale, boolean ignoreDepthForRandomization)
    {
        return new SurfaceInstance(clampUVScale(uvScale), ignoreDepthForRandomization, true, 0, false);
    }
    
    public SurfaceInstance newInstance(double uvScale, boolean ignoreDepthForRandomization)
    {
        return new SurfaceInstance(clampUVScale(uvScale), ignoreDepthForRandomization, true, 0, false);
    }
    
    public SurfaceInstance newInstance(int uvScale, boolean ignoreDepthForRandomization, boolean allowBorders)
    {
        return new SurfaceInstance(clampUVScale(uvScale), ignoreDepthForRandomization, allowBorders, 0, false);
    }
    
    public SurfaceInstance newInstance(double uvScale, boolean ignoreDepthForRandomization, boolean allowBorders)
    {
        return new SurfaceInstance(clampUVScale(uvScale), ignoreDepthForRandomization, allowBorders, 0, false);
    }
    
    public SurfaceInstance newInstance(int uvScale, boolean ignoreDepthForRandomization, boolean allowBorders, int textureSalt)
    {
        return new SurfaceInstance(clampUVScale(uvScale), ignoreDepthForRandomization, allowBorders, textureSalt, false);
    }
    
    public SurfaceInstance newInstance(double uvScale, boolean ignoreDepthForRandomization, boolean allowBorders, int textureSalt)
    {
        return new SurfaceInstance(clampUVScale(uvScale), ignoreDepthForRandomization, allowBorders, textureSalt, false);
    }
    
    public SurfaceInstance newInstance(boolean ignoreDepthForRandomization)
    {
        return new SurfaceInstance(1, ignoreDepthForRandomization, true, 0, false);
    }
    
    public SurfaceInstance newInstance(boolean ignoreDepthForRandomization, boolean allowBorders)
    {
        return new SurfaceInstance(1, ignoreDepthForRandomization, allowBorders, 0, false);
    }
    
    public SurfaceInstance newInstance(boolean ignoreDepthForRandomization, boolean allowBorders, int textureSalt)
    {
        return new SurfaceInstance(1, ignoreDepthForRandomization, allowBorders, textureSalt, false);
    }
    
    public class SurfaceInstance
    {
        /** 
         * The maximum wrapping uv distance for either dimension on this surface.<p>
         * 
         * Must be zero or positive. Setting to zero disable uvWrapping - painter will use a 1:1 scale.<p>
         * 
         * If the surface is painted with a texture larger than this distance, the texture will be
         * scaled down to fit in order to prevent visible seams. A scale of 4, for example,
         * would force a 32x32 texture to be rendered at 1/8 scale.<p>
         * 
         * if the surface is painted with a texture smaller than this distance, then the texture
         * will be zoomed tiled to fill the surface.<p>
         * 
         * Default is 0 and generally only comes into play for non-cubic surface painters.<p>
         * 
         * See also {@link SurfaceTopology#TILED}
         */
        public final float uvWrapDistance;
        
        /**
         * If true, texture painting should not vary by axis
         * orthogonal to the surface.  Ignored if {@link #textureSalt} is non-zero.
         */
        public final boolean ignoreDepthForRandomization;
        
        /**
         * If false, border and masonry painters will not render on this surface.
         * Set false for topologies that don't play well with borders.
         */
        public final boolean allowBorders;
        
        /**
         * If non-zero, signals painter to randomize texture on this surface
         * to be different from and not join with adjacent textures.
         * Use to make cuts into the surface visually distance from adjacent surfaces. 
         */
        public final int textureSalt;
        
        /** 
         * If true, generator will assign colors to vertexes to indicate proximity to lamp surface.
         * Vertices next to lamp have color WHITE and those away have color BLACK.
         * If the lighting mode for the surface is shaded, then quad bake should color
         * vertices to form a gradient. <p>
         * 
         * If the surface is full-brightness, need to re-color all vertices to white.
         */
        public final boolean isLampGradient;
        
        /**
         * Bits here indicate a layer is <em>disabled</em>
         */
        private final int layerDisabledFlags;
        
        public final boolean isLayerDisabled(PaintLayer layer)
        {
            return PaintLayer.BENUMSET.isFlagSetForValue(layer, layerDisabledFlags);
        }
        
        
        private SurfaceInstance(
                float uvWrapDistance, 
                boolean ignoreDepthForRandomization, 
                boolean allowBorders, 
                int textureSalt, 
                boolean isLampGradient,
                PaintLayer... disabledLayers)
        {
            this(uvWrapDistance, ignoreDepthForRandomization, allowBorders, textureSalt, isLampGradient,
                    PaintLayer.BENUMSET.getFlagsForIncludedValues(disabledLayers));
        }
        
        private SurfaceInstance(
                float uvWrapDistance, 
                boolean ignoreDepthForRandomization, 
                boolean allowBorders, 
                int textureSalt, 
                boolean isLampGradient,
                int layerDisabledFlags)
        {
            this.uvWrapDistance = uvWrapDistance;
            this.ignoreDepthForRandomization = ignoreDepthForRandomization;
            this.allowBorders = allowBorders;
            this.textureSalt = textureSalt;
            this.isLampGradient = isLampGradient;
            this.layerDisabledFlags = layerDisabledFlags;
        }
        
        public SurfaceInstance(PaintLayer... disabledLayers)
        {
            this(0, false, false, 0, false, disabledLayers);
        }
        
        /**
         * See {@link #uvWrapDistance}
         */
        public SurfaceInstance withWrap(float uvWrapDistance)
        {
            return new SurfaceInstance(uvWrapDistance, this.ignoreDepthForRandomization, this.allowBorders, this.textureSalt, this.isLampGradient,
                    this.layerDisabledFlags);
        }
        
        public SurfaceInstance withIgnoreDepthForRandomization(boolean ignoreDepthForRandomization)
        {
            return new SurfaceInstance(this.uvWrapDistance, ignoreDepthForRandomization, this.allowBorders, this.textureSalt, this.isLampGradient,
                    this.layerDisabledFlags);
        }
        
        public SurfaceInstance withAllowBorders(boolean allowBorders)
        {
            return new SurfaceInstance(this.uvWrapDistance, this.ignoreDepthForRandomization, allowBorders, this.textureSalt, this.isLampGradient,
                    this.layerDisabledFlags);
        }
        
        public SurfaceInstance withTextureSalt(int textureSalt)
        {
            return new SurfaceInstance(this.uvWrapDistance, this.ignoreDepthForRandomization, this.allowBorders, textureSalt, this.isLampGradient,
                    this.layerDisabledFlags);
        }
        
        public SurfaceInstance withLampGradient(boolean isLampGradient)
        {
            return new SurfaceInstance(this.uvWrapDistance, this.ignoreDepthForRandomization, this.allowBorders, this.textureSalt, isLampGradient,
                    this.layerDisabledFlags);
        }
        
        /**
         * Note the new list <em>replaces</em> the existing list of disabled layers.
         */
        public SurfaceInstance withDisabledLayers(PaintLayer... disabledLayers)
        {
            return new SurfaceInstance(this.uvWrapDistance, this.ignoreDepthForRandomization, this.allowBorders, this.textureSalt, this.isLampGradient,
                    PaintLayer.BENUMSET.getFlagsForIncludedValues(disabledLayers));
        }
        
        /**
         * Convenient when only one or two layers are enabled.
         * Note the new list <em>replaces</em> the existing list of disabled layers.
         */
        public SurfaceInstance withEnabledLayers(PaintLayer... enabledLayers)
        {
            return new SurfaceInstance(this.uvWrapDistance, this.ignoreDepthForRandomization, this.allowBorders, this.textureSalt, this.isLampGradient,
                    ~PaintLayer.BENUMSET.getFlagsForIncludedValues(enabledLayers));
        }
        
        public Surface surface()
        {
            return Surface.this;
        }
        
        public SurfaceType surfaceType() { return surfaceType; }
        public SurfaceTopology topology() { return topology; }
        public boolean isLampGradient() { return isLampGradient; }
    }
}