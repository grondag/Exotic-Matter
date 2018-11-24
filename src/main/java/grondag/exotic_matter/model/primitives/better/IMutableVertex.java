package grondag.exotic_matter.model.primitives.better;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;

import grondag.exotic_matter.model.primitives.vertex.IVec3f;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;

public interface IMutableVertex extends IVec3f
{
    /**
     * WARNING: Will always return an immutable reference to ensure safety.
     * Do not use on mutable instances to avoid memory allocation overhead.
     */
    public Vec3f pos();
    
    @Override
    public default float x()
    {
        return pos().x();
    }

    @Override
    public default float y()
    {
        return pos().y();
    }
    
    @Override
    public default float z()
    {
        return pos().z();
    }

    /**
     * WARNING: Will always return an immutable reference to ensure safety.
     * Do not use on mutable instances to avoid memory allocation overhead.
     */
    public @Nullable Vec3f normal();
    
    public default float normalX()
    {
        final Vec3f n = normal();
        return n == null ? 0 : n.x();
    }
    
    public default float normalY()
    {
        final Vec3f n = normal();
        return n == null ? 0 : n.y();
    }
    
    public default float normalZ()
    {
        final Vec3f n = normal();
        return n == null ? 0 : n.z();
    }

    public default boolean hasNormal()
    {
        return this.normal() != null;
    }
    
    public IMutableVertex flip();
    
    /**
     * Will not retain a reference to normal if it is mutable.
     */
    public void setNormal(Vec3f normal);
    
    public void setNormal(float x, float y, float z);
    
    /**
     * Will not retain a reference to pos if it is mutable.
     */
    public void setPos(Vec3f pos);
    
    public void setPos(float x, float y, float z);
    
    public void transform(Matrix4f matrix, boolean rescaleToUnitCube);

    /**
     * The two input vertices must be of the same concrete type and both will be unmodified.<br>
     * Returned instance will be a new instance.<br>
     * Does not retain a reference to the output or either input.<br>
     */
    public IMutableVertex interpolate(IMutableVertex jVertex, float t);

    public int getColor(int layerIndex);

    public int getGlow(int layerIndex);

    public float getU(int layerIndex);
    
    public float getV(int layerIndex);

    public void setColorGlow(int layerIndex, int color, int glow);

    public void setColor(int layerIndex, int color);

    public void setGlow(int layerIndex, int glow);

    public void setUV(int layerIndex, float u, float v);
    
}
