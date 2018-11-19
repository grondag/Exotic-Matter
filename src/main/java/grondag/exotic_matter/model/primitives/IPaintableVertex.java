package grondag.exotic_matter.model.primitives;

public interface IPaintableVertex extends IMutableVertex
{
    public int layerIndex();
    
    public IPaintableVertex withUV(float uThis, float v);

    public IPaintableVertex withColorGlow(int colorIn, int glowIn);

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
}
