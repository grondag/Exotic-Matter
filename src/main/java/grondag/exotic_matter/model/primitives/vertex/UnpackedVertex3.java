package grondag.exotic_matter.model.primitives.vertex;

import javax.annotation.Nullable;

import grondag.exotic_matter.model.primitives.polygon.PolygonAccessor.VertexLayer;

public class UnpackedVertex3 extends AbstractVertex<UnpackedVertex3> implements IMutableVertex
{
    @SuppressWarnings("unchecked")
    private static final VertexLayer<UnpackedVertex3>[] LAYERS = new VertexLayer[3];
    
    static
    {
        LAYERS[0] = new VertexLayer<UnpackedVertex3>()
        {
            {
                this.colorGetter = v -> v.color0;
                this.colorSetter = (v, color) -> v.color0 = color;
                
                this.glowGetter = v -> v.glowAndLayerCount & 0xFF;
                this.glowSetter = (v, glow) -> v.glowAndLayerCount = (v.glowAndLayerCount & 0xFFFFFF00) | (glow & 0xFF);
                
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
        
        LAYERS[1] = new VertexLayer<UnpackedVertex3>()
        {
            {
                this.colorGetter = v -> v.color1;
                this.colorSetter = (v, color) -> v.color1 = color;
                
                this.glowGetter = v -> (v.glowAndLayerCount >> 8) & 0xFF;
                this.glowSetter = (v, glow) -> v.glowAndLayerCount = (v.glowAndLayerCount & 0xFFFF00FF) | ((glow & 0xFF) << 8);
                
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
        
        LAYERS[2] = new VertexLayer<UnpackedVertex3>()
        {
            {
                this.colorGetter = v -> v.color2;
                this.colorSetter = (v, color) -> v.color2 = color;
                
                this.glowGetter = v -> (v.glowAndLayerCount >> 16) & 0xFF;
                this.glowSetter = (v, glow) -> v.glowAndLayerCount = (v.glowAndLayerCount & 0xFF00FFFF) | ((glow & 0xFF) << 16);
                
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

    private float x;
    private float y;
    private float z;
    
    private float normX;
    private float normY;
    private float normZ;
    
    // low 3 bytes are per-layer glow values
    // high byte is layer count
    private int glowAndLayerCount = 0x1000000F;
    
    private float u0;
    private float v0;
    private int color0;
    
    private float u1;
    private float v1;
    private int color1;
    
    private float u2;
    private float v2;
    private int color2;

    
    @Override
    protected final VertexLayer<UnpackedVertex3>[] layerVertexArray()
    {
        return (VertexLayer<UnpackedVertex3>[]) LAYERS;
    }

    @Override
    public Vec3f pos()
    {
        return Vec3f.create(x, y, z);
    }

    @Override
    public Vec3f normal()
    {
        return hasNormal() ? Vec3f.create(normX, normY, normZ) : null;
    }

    @Override
    public float normalX()
    {
        return normX;
    }

    @Override
    public float normalY()
    {
        return normY;
    }

    @Override
    public float normalZ()
    {
        return normZ;
    }

    @Override
    public boolean hasNormal()
    {
        return !(normX == 0 && normY == 0 && normZ == 0);
    }

    @Override
    public IMutableVertex flip()
    {
        normX = -normX;
        normY = -normY;
        normZ = -normZ;
        return this;
    }

    @Override
    public void setNormal(@Nullable Vec3f normal)
    {
        if(normal == null)
        {
            normX = 0;
            normY = 0;
            normZ = 0;
        }
        else
        {
            normX = normal.x();
            normY = normal.y();
            normZ = normal.z();
        }
    }

    @Override
    public void setNormal(float x, float y, float z)
    {
        normX = x;
        normY = y;
        normZ = z;        
    }

    @Override
    public void setPos(Vec3f pos)
    {
        x = pos.x();
        y = pos.y();
        z = pos.z();
    }

    @Override
    public void setPos(float x, float y, float z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public float x()
    {
        return x;
    }

    @Override
    public float y()
    {
        return y;
    }

    @Override
    public float z()
    {
        return z;
    }

    @Override
    public int getLayerCount()
    {
        return (glowAndLayerCount >>> 24) & 0xFF;
    }
    
    @Override
    public void setLayerCount(int layerCount)
    {
        glowAndLayerCount = (glowAndLayerCount & 0xFFFFFF) | (layerCount << 24);
    }
    
    @Override
    public final void copyFrom(IMutableVertex source)
    {
        if(source instanceof UnpackedVertex3)
            copyFromFast((UnpackedVertex3) source);
        else
            IMutableVertex.super.copyFrom(source);
    }
    
    private final void copyFromFast(UnpackedVertex3 source)
    {
        this.x = source.x;
        this.y = source.y;
        this.z = source.z;
        
        this.normX = source.normX;
        this.normY = source.normY;
        this.normZ = source.normZ;
        
        this.glowAndLayerCount = source.glowAndLayerCount;
        
        this.u0 = source.u0;
        this.v0 = source.v0;
        this.color0 = source.color0;
        
        this.u1 = source.u1;
        this.v1 = source.v1;
        this.color1 = source.color1;
        
        this.u2 = source.u2;
        this.v2 = source.v2;
        this.color2 = source.color2;
    }
}
