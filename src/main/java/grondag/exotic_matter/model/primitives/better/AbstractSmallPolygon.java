package grondag.exotic_matter.model.primitives.better;

import grondag.exotic_matter.model.primitives.better.PolygonAccessor.Vertex;
import grondag.exotic_matter.model.primitives.better.PolygonAccessor.VertexLayer;
import grondag.exotic_matter.model.primitives.vertex.IVec3f;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;

public abstract class AbstractSmallPolygon<T extends AbstractSmallPolygon<T>> extends AbstractPolygon<T>
{
    protected abstract Vertex<T>[] vertexArray();
    protected abstract VertexLayer<T>[][] layerVertexArray();

    @SuppressWarnings("unchecked")
    @Override
    public final IVec3f getVertexNormal(int vertexIndex)
    {
        return vertexArray()[vertexIndex].normalGetter.get((T) this);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public float getVertexNormalX(int vertexIndex)
    {
        return vertexArray()[vertexIndex].normXGetter.get((T) this);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public float getVertexNormalY(int vertexIndex)
    {
        return vertexArray()[vertexIndex].normYGetter.get((T) this);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public float getVertexNormalZ(int vertexIndex)
    {
        return vertexArray()[vertexIndex].normZGetter.get((T) this);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public final IVec3f getPos(int vertexIndex)
    {
        return vertexArray()[vertexIndex].posGetter.get((T) this);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public float getVertexX(int vertexIndex)
    {
        return vertexArray()[vertexIndex].xGetter.get((T) this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public float getVertexY(int vertexIndex)
    {
        return vertexArray()[vertexIndex].yGetter.get((T) this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public float getVertexZ(int vertexIndex)
    {
        return vertexArray()[vertexIndex].zGetter.get((T) this);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public final int getVertexColor(int layerIndex, int vertexIndex)
    {
        return layerVertexArray()[layerIndex][vertexIndex].colorGetter.get((T) this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final int getVertexGlow(int layerIndex, int vertexIndex)
    {
        return layerVertexArray()[layerIndex][vertexIndex].glowGetter.get((T) this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final float getVertexU(int layerIndex, int vertexIndex)
    {
        return layerVertexArray()[layerIndex][vertexIndex].uGetter.get((T) this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final float getVertexV(int layerIndex, int vertexIndex)
    {
        return layerVertexArray()[layerIndex][vertexIndex].vGetter.get((T) this);
    }
    
    /** supports mutable interface */
    @SuppressWarnings("unchecked")
    @Override
    protected final void setVertexLayerImpl(int layerIndex, int vertexIndex, float u, float v, int color, int glow)
    {
        final VertexLayer<T> vl = layerVertexArray()[0][vertexIndex];
        vl.uvSetter.set((T) this, u, v);
        vl.colorSetter.set((T) this, color);
    }

    /** supports mutable interface */
    @SuppressWarnings("unchecked")
    @Override
    protected final void setVertexPosImpl(int vertexIndex, float x, float y, float z)
    {
        vertexArray()[vertexIndex].xyzSetter.set((T) this, x, y, z);
    }

    /** supports mutable interface */
    @SuppressWarnings("unchecked")
    @Override
    protected final void setVertexPosImpl(int vertexIndex, Vec3f pos)
    {
        vertexArray()[vertexIndex].posSetter.set((T) this, pos);
    }
    
    /** supports mutable interface */
    @SuppressWarnings("unchecked")
    @Override
    protected final void setVertexNormalImpl(int vertexIndex, Vec3f normal)
    {
        vertexArray()[vertexIndex].normalSetter.set((T) this, normal);
    }

    /** supports mutable interface */
    @SuppressWarnings("unchecked")
    @Override
    protected final void setVertexNormalImpl(int vertexIndex, float x, float y, float z)
    {
        vertexArray()[vertexIndex].normXYZSetter.set((T) this, x, y, z);
    }
    
    /** supports mutable interface */
    @SuppressWarnings("unchecked")
    @Override
    protected final void setVertexColorGlowImpl(int layerIndex, int vertexIndex, int color, int glow)
    {
        final VertexLayer<T> vl = layerVertexArray()[layerIndex][vertexIndex];
        vl.colorSetter.set((T) this, color);
        vl.glowSetter.set((T) this, glow);
    }

    /** supports mutable interface */
    @SuppressWarnings("unchecked")
    @Override
    protected final void setVertexColorImpl(int layerIndex, int vertexIndex, int color)
    {
        layerVertexArray()[layerIndex][vertexIndex].colorSetter.set((T) this, color);
    }

    /** supports mutable interface */
    @SuppressWarnings("unchecked")
    @Override
    protected final void setVertexUVImpl(int layerIndex, int vertexIndex, float u, float v)
    {
        layerVertexArray()[layerIndex][vertexIndex].uvSetter.set((T) this, u, v);
    }

    /** supports mutable interface */
    @SuppressWarnings("unchecked")
    @Override
    protected final void setVertexUImpl(int layerIndex, int vertexIndex, float u)
    {
        layerVertexArray()[layerIndex][vertexIndex].uSetter.set((T) this, u);
    }
    
    /** supports mutable interface */
    @SuppressWarnings("unchecked")
    @Override
    protected final void setVertexVImpl(int layerIndex, int vertexIndex, float v)
    {
        layerVertexArray()[layerIndex][vertexIndex].vSetter.set((T) this, v);
    }
    
    /** supports mutable interface */
    @SuppressWarnings("unchecked")
    @Override
    protected final void setVertexGlowImpl(int layerIndex, int vertexIndex, int glow)
    {
        layerVertexArray()[layerIndex][vertexIndex].glowSetter.set((T) this, glow);
    }
}
