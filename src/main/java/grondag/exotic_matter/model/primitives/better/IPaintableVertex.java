package grondag.exotic_matter.model.primitives.better;

public interface IPaintableVertex extends IMutableGeometricVertex, IPaintedVertex
{
    /** input accepts int but is handled as a short */
    public IPaintableVertex setGlow(int layerIndex, short glow);
    
    public default IPaintableVertex setGlow(int layerIndex, int glow)
    {
        return setGlow(layerIndex, (short)glow);
    }

    public IPaintableVertex setColor(int layerIndex, int color);

    public IPaintableVertex setUV(int layerIndex, float u, float v);

    public IPaintableVertex setU(int layerIndex, float u);
    
    public IPaintableVertex setV(int layerIndex, float v);
}
