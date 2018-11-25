package grondag.exotic_matter.model.primitives.better;

import grondag.exotic_matter.model.primitives.better.PolygonAccessor.VertexLayer;

public class PackedVertex1 extends AbstractPackedVertex<PackedVertex1>
{
    @SuppressWarnings("unchecked")
    private static final VertexLayer<PackedVertex1>[] LAYERS = new VertexLayer[1];
    
    static
    {
        LAYERS[0] = new VertexLayer<PackedVertex1>()
        {
            {
                this.colorGetter = v -> v.color0;
                this.colorSetter = (v, color) -> v.color0 = color;
                
                this.glowGetter = v -> v.glow0;
                this.glowSetter = (v, glow) -> v.glow0 = (short)glow;
                
                this.uGetter = v -> v.u0;
                this.uSetter = (v, u) -> v.u0 = u;
                
                this.vGetter = v -> v.v0;
                this.vSetter = (v, vVal) -> v.v0 = vVal;
                
                this.uvSetter = (v, u, vVal) -> 
                {
                    v.u0 = u;
                    v.v0 = vVal;
                };
            }
        };
    }

    private float u0;
    private float v0;
    private short glow0;
    private int color0;
    
    @Override
    protected final VertexLayer<PackedVertex1>[] layerVertexArray()
    {
        return (VertexLayer<PackedVertex1>[]) LAYERS;
    }

    @Override
    public int getLayerCount()
    {
        return 1;
    }
}
