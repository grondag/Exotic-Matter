package grondag.exotic_matter.render;

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
         * The approximate in-world scale of a 0-16 UV texture span.<br>
         * Mist be at least 1 or a power of two.<br>
         * If set to something that is not a power of two, will be rounded
         * to the nearest power of 2 (and at least 1).<br>
         * Scale of 1 means 0-16 is the size of one MC block.<br>
         * Scale of 4, for example, means a UV span of 0-4 (16/4) would cover one MC block in world.<br>
         * Default is 1 and generally only comes into play for non-cubic surface painters.<p>
         * 
         * See also {@link SurfaceTopology#TILED}
         */
        public final int uvScale;
        
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
         * If true, base layer painting will be disabled.
         */
        public final boolean disableBase;
        
        /**
         * If true, middle layer painting (if applicable) will be disabled.
         */
        public final boolean disableMiddle;
        
        /**
         * If true, outer layer painting (if applicable) will be disabled.
         */
        public final boolean disableOuter;
        
        private SurfaceInstance(
                int uvScale, 
                boolean ignoreDepthForRandomization, 
                boolean allowBorders, 
                int textureSalt, 
                boolean isLampGradient)
        {
            this(uvScale, ignoreDepthForRandomization, allowBorders, textureSalt, isLampGradient, false, false, false);
        }
        
        private SurfaceInstance(
                int uvScale, 
                boolean ignoreDepthForRandomization, 
                boolean allowBorders, 
                int textureSalt, 
                boolean isLampGradient,
                boolean disableBase,
                boolean disableMiddle,
                boolean disableOuter)
        {
            this.uvScale = uvScale;
            this.ignoreDepthForRandomization = ignoreDepthForRandomization;
            this.allowBorders = allowBorders;
            this.textureSalt = textureSalt;
            this.isLampGradient = isLampGradient;
            this.disableBase = disableBase;
            this.disableMiddle = disableMiddle;
            this.disableOuter = disableOuter;
        }
        
        public SurfaceInstance()
        {
            this.uvScale = 1;
            this.ignoreDepthForRandomization = false;
            this.allowBorders = false;
            this.textureSalt = 0;
            this.isLampGradient = false;
            this.disableBase = false;
            this.disableMiddle = false;
            this.disableOuter = false;
        }
        
        public SurfaceInstance withScale(int uvScale)
        {
            return new SurfaceInstance(clampUVScale(uvScale), this.ignoreDepthForRandomization, this.allowBorders, this.textureSalt, this.isLampGradient,
                    this.disableBase, this.disableMiddle, this.disableOuter);
        }
        
        public SurfaceInstance withScale(double uvScale)
        {
            return new SurfaceInstance(clampUVScale(uvScale), this.ignoreDepthForRandomization, this.allowBorders, this.textureSalt, this.isLampGradient,
                    this.disableBase, this.disableMiddle, this.disableOuter);
        }
        
        public SurfaceInstance withIgnoreDepthForRandomization(boolean ignoreDepthForRandomization)
        {
            return new SurfaceInstance(this.uvScale, ignoreDepthForRandomization, this.allowBorders, this.textureSalt, this.isLampGradient,
                    this.disableBase, this.disableMiddle, this.disableOuter);
        }
        
        public SurfaceInstance withAllowBorders(boolean allowBorders)
        {
            return new SurfaceInstance(this.uvScale, this.ignoreDepthForRandomization, allowBorders, this.textureSalt, this.isLampGradient,
                    this.disableBase, this.disableMiddle, this.disableOuter);
        }
        
        public SurfaceInstance withTextureSalt(int textureSalt)
        {
            return new SurfaceInstance(this.uvScale, this.ignoreDepthForRandomization, this.allowBorders, textureSalt, this.isLampGradient,
                    this.disableBase, this.disableMiddle, this.disableOuter);
        }
        
        public SurfaceInstance withLampGradient(boolean isLampGradient)
        {
            return new SurfaceInstance(this.uvScale, this.ignoreDepthForRandomization, this.allowBorders, this.textureSalt, isLampGradient,
                    this.disableBase, this.disableMiddle, this.disableOuter);
        }
        
        public SurfaceInstance withDisableBase(boolean disableBase)
        {
            return new SurfaceInstance(this.uvScale, this.ignoreDepthForRandomization, this.allowBorders, this.textureSalt, this.isLampGradient,
                    disableBase, this.disableMiddle, this.disableOuter);
        }

        public SurfaceInstance withDisableMiddle(boolean disableMiddle)
        {
            return new SurfaceInstance(this.uvScale, this.ignoreDepthForRandomization, this.allowBorders, this.textureSalt, this.isLampGradient,
                    this.disableBase, disableMiddle, this.disableOuter);
        }
        
        public SurfaceInstance withDisableOuter(boolean disableOuter)
        {
            return new SurfaceInstance(this.uvScale, this.ignoreDepthForRandomization, this.allowBorders, this.textureSalt, this.isLampGradient,
                    this.disableBase, this.disableMiddle, disableOuter);
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