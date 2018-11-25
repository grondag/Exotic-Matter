package grondag.exotic_matter.model.primitives.better;

import grondag.exotic_matter.model.primitives.vertex.IVec3f;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;

public abstract class AbstractLargePolygon<T extends AbstractLargePolygon<T>> extends AbstractPolygon<T>
{
    protected abstract IMutableVertex[] vertices();
    
    protected abstract int computeArrayIndex(int vertexIndex);
    
    @Override
    public final IVec3f getVertexNormal(int vertexIndex)
    {
        return vertices()[computeArrayIndex(vertexIndex)].normal();
    }
    
    @Override
    public final float getVertexNormalX(int vertexIndex)
    {
        IMutableVertex v =  vertices()[computeArrayIndex(vertexIndex)];
        return v.hasNormal() ? v.normalX() : this.getFaceNormal().x();
    }
    
    @Override
    public final float getVertexNormalY(int vertexIndex)
    {
        IMutableVertex v =  vertices()[computeArrayIndex(vertexIndex)];
        return v.hasNormal() ? v.normalY() : this.getFaceNormal().y();
    }
    
    @Override
    public final float getVertexNormalZ(int vertexIndex)
    {
        IMutableVertex v =  vertices()[computeArrayIndex(vertexIndex)];
        return v.hasNormal() ? v.normalZ() : this.getFaceNormal().z();
    }
    
    @Override
    public final IVec3f getPos(int vertexIndex)
    {
        return vertices()[computeArrayIndex(vertexIndex)].pos();
    }
    
    @Override
    public final float getVertexX(int vertexIndex)
    {
        return vertices()[computeArrayIndex(vertexIndex)].x();
    }

    @Override
    public final float getVertexY(int vertexIndex)
    {
        return vertices()[computeArrayIndex(vertexIndex)].y();
    }

    @Override
    public final float getVertexZ(int vertexIndex)
    {
        return vertices()[computeArrayIndex(vertexIndex)].z();
    }
    
    @Override
    public final int getVertexColor(int layerIndex, int vertexIndex)
    {
        return vertices()[computeArrayIndex(vertexIndex)].getColor(layerIndex);
    }

    @Override
    public final int getVertexGlow(int layerIndex, int vertexIndex)
    {
        return vertices()[computeArrayIndex(vertexIndex)].getGlow(layerIndex);
    }

    @Override
    public final float getVertexU(int layerIndex, int vertexIndex)
    {
        return vertices()[computeArrayIndex(vertexIndex)].getU(layerIndex);
    }

    @Override
    public final float getVertexV(int layerIndex, int vertexIndex)
    {
        return vertices()[computeArrayIndex(vertexIndex)].getV(layerIndex);
    }

    /** supports mutable interface */
    @Override
    protected final void setVertexLayerImpl(int layerIndex, int vertexIndex, float u, float v, int color, int glow)
    {
        final IMutableVertex vertex = vertices()[computeArrayIndex(vertexIndex)];
        vertex.setUV(0, u, v);
        vertex.setColorGlow(0, color, glow);
    }

    /** supports mutable interface */
    @Override
    protected final void setVertexPosImpl(int vertexIndex, float x, float y, float z)
    {
        vertices()[computeArrayIndex(vertexIndex)].setPos(x, y, z);
    }

    /** supports mutable interface */
    @Override
    protected final void setVertexPosImpl(int vertexIndex, Vec3f pos)
    {
        vertices()[computeArrayIndex(vertexIndex)].setPos(pos);
    }
    
    /** supports mutable interface */
    @Override
    protected final void setVertexNormalImpl(int vertexIndex, Vec3f normal)
    {
        vertices()[computeArrayIndex(vertexIndex)].setNormal(normal);
    }

    /** supports mutable interface */
    @Override
    protected final void setVertexNormalImpl(int vertexIndex, float x, float y, float z)
    {
        vertices()[computeArrayIndex(vertexIndex)].setNormal(x, y, z);
    }
    
    /** supports mutable interface */
    @Override
    protected final void setVertexColorGlowImpl(int layerIndex, int vertexIndex, int color, int glow)
    {
        vertices()[computeArrayIndex(vertexIndex)].setColorGlow(layerIndex, color, glow);
    }

    /** supports mutable interface */
    @Override
    protected final void setVertexColorImpl(int layerIndex, int vertexIndex, int color)
    {
        vertices()[computeArrayIndex(vertexIndex)].setColor(layerIndex, color);
    }

    /** supports mutable interface */
    @Override
    protected final void setVertexUVImpl(int layerIndex, int vertexIndex, float u, float v)
    {
        vertices()[computeArrayIndex(vertexIndex)].setUV(layerIndex, u, v);
    }

    /** supports mutable interface */
    @Override
    protected final void setVertexUImpl(int layerIndex, int vertexIndex, float u)
    {
        vertices()[computeArrayIndex(vertexIndex)].setU(layerIndex, u);
    }
    
    /** supports mutable interface */
    @Override
    protected final void setVertexVImpl(int layerIndex, int vertexIndex, float v)
    {
        vertices()[computeArrayIndex(vertexIndex)].setV(layerIndex, v);
    }
    
    /** supports mutable interface */
    @Override
    protected final void setVertexGlowImpl(int layerIndex, int vertexIndex, int glow)
    {
        vertices()[computeArrayIndex(vertexIndex)].setGlow(layerIndex, glow);
    }
}
