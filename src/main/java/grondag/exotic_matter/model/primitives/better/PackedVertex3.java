package grondag.exotic_matter.model.primitives.better;

import grondag.exotic_matter.model.primitives.better.PolygonAccessor.VertexLayer;

public class PackedVertex3 extends AbstractPackedVertex<PackedVertex3>
{
    @SuppressWarnings("unchecked")
    private static final VertexLayer<PackedVertex3>[] LAYERS = new VertexLayer[3];
    
    static
    {
        LAYERS[0] = new VertexLayer<PackedVertex3>()
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
        
        LAYERS[1] = new VertexLayer<PackedVertex3>()
        {
            {
                this.colorGetter = v -> v.color1;
                this.colorSetter = (v, color) -> v.color1 = color;
                
                this.glowGetter = v -> v.glow1;
                this.glowSetter = (v, glow) -> v.glow1 = (short)glow;
                
                this.uGetter = v -> v.u1;
                this.uSetter = (v, u) -> v.u1 = u;
                
                this.vGetter = v -> v.v1;
                this.vSetter = (v, vVal) -> v.v1 = vVal;
                
                this.uvSetter = (v, u, vVal) -> 
                {
                    v.u1 = u;
                    v.v1 = vVal;
                };
            }
        };
        
        LAYERS[2] = new VertexLayer<PackedVertex3>()
        {
            {
                this.colorGetter = v -> v.color2;
                this.colorSetter = (v, color) -> v.color2 = color;
                
                this.glowGetter = v -> v.glow2;
                this.glowSetter = (v, glow) -> v.glow2 = (short)glow;
                
                this.uGetter = v -> v.u2;
                this.uSetter = (v, u) -> v.u2 = u;
                
                this.vGetter = v -> v.v2;
                this.vSetter = (v, vVal) -> v.v2 = vVal;
                
                this.uvSetter = (v, u, vVal) -> 
                {
                    v.u2 = u;
                    v.v2 = vVal;
                };
            }
        };
    }

    private float u0;
    private float v0;
    private short glow0;
    private int color0;
    
    private float u1;
    private float v1;
    private short glow1;
    private int color1;
    
    private float u2;
    private float v2;
    private short glow2;
    private int color2;
    
    @Override
    protected final VertexLayer<PackedVertex3>[] layerVertexArray()
    {
        return (VertexLayer<PackedVertex3>[]) LAYERS;
    }
}
