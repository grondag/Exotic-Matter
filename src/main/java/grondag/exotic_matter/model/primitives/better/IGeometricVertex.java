package grondag.exotic_matter.model.primitives.better;

import javax.annotation.Nullable;

import grondag.exotic_matter.model.primitives.vertex.IVec3f;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;

public interface IGeometricVertex extends IVec3f
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
}
