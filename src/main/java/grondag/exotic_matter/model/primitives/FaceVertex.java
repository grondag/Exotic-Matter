package grondag.exotic_matter.model.primitives;


/**
 * Used to express quads on a face (2D).  By default u,v map directly to x, y on the given face
 */
public class FaceVertex
{
    public final float x;
    public final float y;
    public final float depth;

    public FaceVertex(float x, float y, float depth)
    {
        this.x = x;
        this.y = y;
        this.depth = depth;
    }

    public int glow()
    {
        return 0;
    }

    @Override
    public FaceVertex clone()
    {
        return new FaceVertex(x, y, depth);
    }

    public FaceVertex withXY(float x, float y)
    {
        return new FaceVertex(x, y, this.depth);
    }
    
    public FaceVertex withDepth(float depth)
    {
        return new FaceVertex(this.x, this.y, depth);
    }
    
    public FaceVertex withColor(int color, int glow)
    {
        return new FaceVertex.Colored(this.x, this.y, depth, color, glow);
    }
    
    public FaceVertex withUV(float u, float v)
    {
        return new FaceVertex.UV(this.x, this.y, this.depth, u, v);
    }

    public int color(int defaultColor)
    {
        return defaultColor;
    }
    
    /**
     * This value is logical 0-1 within the texture for this face. NOT 0-16.  And NOT interpolated for the sprite. <br><br>
     * 
     * Note that the V orientation is flipped from the Y axis used for vertices.
     * Origin is at the top left for textures, vs. bottom left for vertex coordinates. 
     * This means the default values for u, v will be x, 1-y.  <br><br>
     * 
     * The bottom face is handled differently and RawQuad will flip it automatically.. 
     */
    public float u()
    {
        return x;
    }
    
    /**
     * See {@link #u()}
     */
    public float v()
    {
        return 1-y;
    }

    public static class Colored extends FaceVertex
    {
        private final int color;
        private final short glow;

        public Colored(float x, float y, float depth, int color, int glow)
        {
            super(x, y, depth);
            this.color = color;
            this.glow = (short) (glow & 0xFF);
        }

        public Colored(float x, float y, float depth, float u, float v, int color, int glow)
        {
            super(x, y, depth);
            this.color = color;
            this.glow = (short) (glow & 0xFF);
        }

        @Override
        public FaceVertex clone()
        {
            return new FaceVertex.Colored(x, y, depth, color, glow);
        }

        @Override
        public int color(int defaultColor)
        {
            return color;
        }
        
        @Override
        public int glow()
        {
            return this.glow;
        }
        
        @Override
        public FaceVertex withXY(float x, float y)
        {
            return new FaceVertex.Colored(x, y, this.depth, this.color, this.glow);
        }
        
        @Override
        public FaceVertex withDepth(float depth)
        {
            return new FaceVertex.Colored(this.x, this.y, depth, this.color, this.glow);
        }
        
        @Override
        public FaceVertex withColor(int color, int glow)
        {
            return new FaceVertex.Colored(this.x, this.y, depth, color, glow);
        }
        
        @Override
        public FaceVertex withUV(float u, float v)
        {
            return new FaceVertex.UVColored(this.x, this.y, this.depth, u, v, this.color, this.glow);
        }
    }
    
    public static class UV extends FaceVertex
    {
        private final float u;
        private final float v;
        
        public UV(float x, float y, float depth, float u, float v)
        {
            super(x, y, depth);
            this.u = u;
            this.v = v;
        }

        @Override
        public float u()
        {
            return this.u;
        }
        
        @Override
        public float v()
        {
            return this.v;
        }
        
        @Override
        public FaceVertex clone()
        {
            return new FaceVertex.UV(x, y, depth, u, v);
        }
        
        @Override
        public FaceVertex withXY(float x, float y)
        {
            return new FaceVertex.UV(x, y, this.depth, this.u, this.v);
        }
        
        @Override
        public FaceVertex withDepth(float depth)
        {
            return new FaceVertex.UV(this.x, this.y, depth, this.u, this.v);
        }
        
        @Override
        public FaceVertex withColor(int color, int glow)
        {
            return new FaceVertex.UVColored(this.x, this.y, depth, this.u, this.v, color, glow);
        }
        
        @Override
        public FaceVertex withUV(float u, float v)
        {
            return new FaceVertex.UV(this.x, this.y, this.depth, u, v);
        }
    }
    
    public static class UVColored extends FaceVertex
    {
        private final float u;
        private final float v;
        private final int color;
        private final short glow;
        
        public UVColored(float x, float y, float depth, float u, float v, int color, int glow)
        {
            super(x, y, depth);
            this.u = u;
            this.v = v;
            this.color = color;
            this.glow = (short) (glow & 0xFF);
        }
        
        @Override
        public int color(int defaultColor)
        {
            return color;
        }
        
        @Override
        public int glow()
        {
            return this.glow();
        }

        @Override
        public float u()
        {
            return this.u;
        }
        
        @Override
        public float v()
        {
            return this.v;
        }
        
        @Override
        public FaceVertex clone()
        {
            return new FaceVertex.UVColored(x, y, depth, u, v, color, glow);
        }
        
        @Override
        public FaceVertex withXY(float x, float y)
        {
            return new FaceVertex.UVColored(x, y, this.depth, this.u, this.v, this.color, this.glow);
        }
        
        @Override
        public FaceVertex withDepth(float depth)
        {
            return new FaceVertex.UVColored(this.x, this.y, depth, this.u, this.v, this.color, this.glow);
        }
        
        @Override
        public FaceVertex withColor(int color, int glow)
        {
            return new FaceVertex.UVColored(this.x, this.y, this.depth, this.u, this.v, color, glow);
        }
        
        @Override
        public FaceVertex withUV(float u, float v)
        {
            return new FaceVertex.UVColored(this.x, this.y, this.depth, u, v, this.color, this.glow);
        }
    }
}