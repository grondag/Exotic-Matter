package grondag.exotic_matter.model.primitives.polygon;

import grondag.exotic_matter.model.primitives.polygon.PolygonAccessor.Layer;
import grondag.exotic_matter.model.primitives.polygon.PolygonAccessor.VertexLayer;

public class Polygon2x4 extends AbstractPolygonNx4<Polygon2x4>
{
    @SuppressWarnings("unchecked")
    private static final Layer<Polygon2x4>[] LAYERS = new Layer[2];
    
    static
    {
        LAYERS[0] = new Layer<Polygon2x4>()
        {
            {
                this.textureGetter = p -> p.texture0;
                this.textureSetter = (p, v) -> p.texture0 = v;
                
                this.uMaxGetter = p -> p.uMax0;
                this.uMaxSetter = (p, v) -> p.uMax0 = v;
                
                this.uMinGetter = p -> p.uMin0;
                this.uMinSetter = (p, v) -> p.uMin0 = v;
                
                this.vMaxGetter = p -> p.vMax0;
                this.vMaxSetter = (p, v) -> p.vMax0 = v;
                
                this.vMaxSetter = (p, v) -> p.vMax0 = v;
                this.vMinGetter = p -> p.vMin0;
                this.vMinSetter = (p, v) -> p.vMin0 = v;
            }
        };
        
        LAYERS[1] = new Layer<Polygon2x4>()
        {
            {
                this.textureGetter = p -> p.texture1;
                this.textureSetter = (p, v) -> p.texture1 = v;
                
                this.uMaxGetter = p -> p.uMax1;
                this.uMaxSetter = (p, v) -> p.uMax1 = v;
                
                this.uMinGetter = p -> p.uMin1;
                this.uMinSetter = (p, v) -> p.uMin1 = v;
                
                this.vMaxGetter = p -> p.vMax1;
                this.vMaxSetter = (p, v) -> p.vMax1 = v;
                
                this.vMaxSetter = (p, v) -> p.vMax1 = v;
                this.vMinGetter = p -> p.vMin1;
                this.vMinSetter = (p, v) -> p.vMin1 = v;
            }
        };
    }
    
    @SuppressWarnings("unchecked")
    private static final VertexLayer<Polygon2x4>[][] VERTEX_LAYERS = new VertexLayer[2][4];
    
    static
    {
        VERTEX_LAYERS[0][0] = new VertexLayer<Polygon2x4>()
        {
            {
                this.colorGetter = v -> v.color0_0;
                this.colorSetter = (v, color) -> v.color0_0 = color;
                
                this.uGetter = v -> v.u0_0;
                this.uSetter = (v, u) -> v.u0_0 = u;
                
                this.vGetter = v -> v.v0_0;
                this.vSetter = (v, vVal) -> v.v0_0 = vVal;
                
                this.uvSetter = (v, u, vVal) -> 
                {
                    v.u0_0 = u;
                    v.v0_0 = vVal;
                };
            }
        };
        
        VERTEX_LAYERS[0][1] = new VertexLayer<Polygon2x4>()
        {
            {
                this.colorGetter = v -> v.color0_2;
                this.colorSetter = (v, color) -> v.color0_2 = color;
                
                this.uGetter = v -> v.u0_2;
                this.uSetter = (v, u) -> v.u0_2 = u;
                
                this.vGetter = v -> v.v0_2;
                this.vSetter = (v, vVal) -> v.v0_2 = vVal;
                
                this.uvSetter = (v, u, vVal) -> 
                {
                    v.u0_2 = u;
                    v.v0_2 = vVal;
                };
            }
        };
        
        VERTEX_LAYERS[0][2] = new VertexLayer<Polygon2x4>()
        {
            {
                this.colorGetter = v -> v.color0_1;
                this.colorSetter = (v, color) -> v.color0_1 = color;
                
                this.uGetter = v -> v.u0_1;
                this.uSetter = (v, u) -> v.u0_1 = u;
                
                this.vGetter = v -> v.v0_1;
                this.vSetter = (v, vVal) -> v.v0_1 = vVal;
                
                this.uvSetter = (v, u, vVal) -> 
                {
                    v.u0_1 = u;
                    v.v0_1 = vVal;
                };
            }
        };
        
        VERTEX_LAYERS[0][3] = new VertexLayer<Polygon2x4>()
        {
            {
                this.colorGetter = v -> v.color0_3;
                this.colorSetter = (v, color) -> v.color0_3 = color;
                
                this.uGetter = v -> v.u0_3;
                this.uSetter = (v, u) -> v.u0_3 = u;
                
                this.vGetter = v -> v.v0_3;
                this.vSetter = (v, vVal) -> v.v0_3 = vVal;
                
                this.uvSetter = (v, u, vVal) -> 
                {
                    v.u0_3 = u;
                    v.v0_3 = vVal;
                };
            }
        };
        
        ////////
        
        VERTEX_LAYERS[1][0] = new VertexLayer<Polygon2x4>()
        {
            {
                this.colorGetter = v -> v.color1_0;
                this.colorSetter = (v, color) -> v.color1_0 = color;
                
                this.uGetter = v -> v.u1_0;
                this.uSetter = (v, u) -> v.u1_0 = u;
                
                this.vGetter = v -> v.v1_0;
                this.vSetter = (v, vVal) -> v.v1_0 = vVal;
                
                this.uvSetter = (v, u, vVal) -> 
                {
                    v.u1_0 = u;
                    v.v1_0 = vVal;
                };
            }
        };
        
        VERTEX_LAYERS[1][1] = new VertexLayer<Polygon2x4>()
        {
            {
                this.colorGetter = v -> v.color1_2;
                this.colorSetter = (v, color) -> v.color1_2 = color;
                
                this.uGetter = v -> v.u1_2;
                this.uSetter = (v, u) -> v.u1_2 = u;
                
                this.vGetter = v -> v.v1_2;
                this.vSetter = (v, vVal) -> v.v1_2 = vVal;
                
                this.uvSetter = (v, u, vVal) -> 
                {
                    v.u1_2 = u;
                    v.v1_2 = vVal;
                };
            }
        };
        
        VERTEX_LAYERS[1][2] = new VertexLayer<Polygon2x4>()
        {
            {
                this.colorGetter = v -> v.color1_1;
                this.colorSetter = (v, color) -> v.color1_1 = color;
                
                this.uGetter = v -> v.u1_1;
                this.uSetter = (v, u) -> v.u1_1 = u;
                
                this.vGetter = v -> v.v1_1;
                this.vSetter = (v, vVal) -> v.v1_1 = vVal;
                
                this.uvSetter = (v, u, vVal) -> 
                {
                    v.u1_1 = u;
                    v.v1_1 = vVal;
                };
            }
        };
        
        VERTEX_LAYERS[1][3] = new VertexLayer<Polygon2x4>()
        {
            {
                this.colorGetter = v -> v.color1_3;
                this.colorSetter = (v, color) -> v.color1_3 = color;
                
                this.uGetter = v -> v.u1_3;
                this.uSetter = (v, u) -> v.u1_3 = u;
                
                this.vGetter = v -> v.v1_3;
                this.vSetter = (v, vVal) -> v.v1_3 = vVal;
                
                this.uvSetter = (v, u, vVal) -> 
                {
                    v.u1_3 = u;
                    v.v1_3 = vVal;
                };
            }
        };
    }
    
    private String texture0;
    private float uMin0;
    private float uMax0;
    private float vMin0;
    private float vMax0;
    
    private String texture1;
    private float uMin1;
    private float uMax1;
    private float vMin1;
    private float vMax1;
    
    private float u0_0;
    private float v0_0;
    private int color0_0 = 0xFFFFFFFF;
    
    private float u0_1;
    private float v0_1;
    private int color0_1 = 0xFFFFFFFF;
    
    private float u0_2;
    private float v0_2;
    private int color0_2 = 0xFFFFFFFF;
    
    private float u0_3;
    private float v0_3;
    private int color0_3 = 0xFFFFFFFF;
    
    private float u1_0;
    private float v1_0;
    private int color1_0 = 0xFFFFFFFF;
    
    private float u1_1;
    private float v1_1;
    private int color1_1 = 0xFFFFFFFF;
    
    private float u1_2;
    private float v1_2;
    private int color1_2 = 0xFFFFFFFF;
    
    private float u1_3;
    private float v1_3;
    private int color1_3 = 0xFFFFFFFF;
    
    @Override
    public final int layerCount()
    {
        return 2;
    }

    @Override
    protected final VertexLayer<Polygon2x4>[][] layerVertexArray()
    {
        return VERTEX_LAYERS;
    }

    @Override
    protected final Layer<Polygon2x4>[] layerAccess()
    {
        return LAYERS;
    }

}
