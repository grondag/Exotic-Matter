package grondag.exotic_matter.model.primitives.better;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector4f;

import grondag.exotic_matter.model.primitives.better.PolygonAccessor.VertexLayer;

public abstract class AbstractVertex<T extends AbstractVertex<T>> implements IMutableVertex
{
    protected abstract VertexLayer<T>[] layerVertexArray();
    
    @Override
    public IMutableVertex interpolate(IMutableVertex jVertex, float t)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final int getColor(int layerIndex)
    {
        return layerVertexArray()[layerIndex].colorGetter.get((T) this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final int getGlow(int layerIndex)
    {
        return layerVertexArray()[layerIndex].glowGetter.get((T) this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final float getU(int layerIndex)
    {
        return layerVertexArray()[layerIndex].uGetter.get((T) this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final float getV(int layerIndex)
    {
        return layerVertexArray()[layerIndex].vGetter.get((T) this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void setColorGlow(int layerIndex, int color, int glow)
    {
        final VertexLayer<T> access = layerVertexArray()[layerIndex];
        access.colorSetter.set((T) this, color);
        access.glowSetter.set((T) this, glow);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void setColor(int layerIndex, int color)
    {
        layerVertexArray()[layerIndex].colorSetter.set((T) this, color);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void setGlow(int layerIndex, int glow)
    {
        layerVertexArray()[layerIndex].glowSetter.set((T) this, glow);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void setUV(int layerIndex, float u, float v)
    {
        layerVertexArray()[layerIndex].uvSetter.set((T) this, u, v);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void setU(int layerIndex, float u)
    {
        layerVertexArray()[layerIndex].uSetter.set((T) this, u);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final void setV(int layerIndex, float v)
    {
        layerVertexArray()[layerIndex].vSetter.set((T) this, v);
    }

    @Override
    public final void transform(Matrix4f matrix, boolean rescaleToUnitCube)
    {
          Vector4f tmp = new Vector4f(this.x(), this.y(), this.z(), 1f);
    
          matrix.transform(tmp);
          if (rescaleToUnitCube && Math.abs(tmp.w - 1f) > 1e-5)
          {
              tmp.scale(1f / tmp.w);
          }
    
          this.setPos(tmp.x, tmp.y, tmp.z);
          
          if(this.hasNormal())
          {
              Vector4f tmpNormal = new Vector4f(this.normalX(), this.normalY(), this.normalZ(), 1f);
              matrix.transform(tmpNormal);
              float normScale= (float) (1/Math.sqrt(tmpNormal.x*tmpNormal.x + tmpNormal.y*tmpNormal.y + tmpNormal.z*tmpNormal.z));
              this.setNormal(tmpNormal.x * normScale, tmpNormal.y * normScale, tmpNormal.z * normScale);
          }        
    }
}
