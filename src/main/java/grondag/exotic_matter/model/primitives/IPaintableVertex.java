package grondag.exotic_matter.model.primitives;

public interface IPaintableVertex
{
    default IPaintableVertex forTextureLayer(int layer)
    {
        assert layer == 0;
        return this;
    }
    
    short glow();

    int color();

    float u();

    float v();

    IPaintableVertex withUV(float uThis, float v);

    IPaintableVertex interpolate(IPaintableVertex nextVertex, float dist);

    IPaintableVertex withColorGlow(int colorIn, int glowIn);

    /** returns copy of this vertex with given color */
    public default IPaintableVertex withColor(int colorIn)
    {
        return withColorGlow(colorIn, this.glow());
    }

    /** returns copy of this vertex with given glow (0-255) */
    public default IPaintableVertex withGlow(int glowIn)
    {
        return withColorGlow(this.color(), glowIn);
    }

    float x();

    float z();

    float y();

}
