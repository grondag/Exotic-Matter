package grondag.exotic_matter.model.primitives.better;

import javax.annotation.Nullable;

import grondag.exotic_matter.model.primitives.QuadHelper;
import grondag.exotic_matter.model.primitives.vertex.IVertex;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import grondag.exotic_matter.varia.Useful;
import net.minecraft.util.EnumFacing;

public interface IGeometricVertex
{
    /**
     * WARNING: Will always return an immutable reference to ensure safety.
     * Do not use on mutable instances to avoid memory allocation overhead.
     */
    public Vec3f pos();
    
    public default float x()
    {
        return pos().x();
    }

    public default float y()
    {
        return pos().y();
    }
    
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
    
    /**
     * Returns a signed distance to the plane of the given face.
     * Positive numbers mean in front of face, negative numbers in back.
     */
    public default float distanceToFacePlane(EnumFacing face)
    {
        // could use dot product, but exploiting special case for less math
        switch(face)
        {
        case UP:
            return this.y() - 1;

        case DOWN:
            return - this.y();
            
        case EAST:
            return this.x() - 1;

        case WEST:
            return -this.x();

        case NORTH:
            return -this.z();
            
        case SOUTH:
            return this.z() - 1;

        default:
            // make compiler shut up about unhandled case
            return 0;
        }
    }

    public default boolean isOnFacePlane(EnumFacing face, float tolerance)
    {
        return Math.abs(this.distanceToFacePlane(face)) < tolerance;
    }

    /** 
     * True if both vertices are at the same point. 
     */
    public default boolean isCsgEqual(IVertex vertexIn)
    {
        return Math.abs(vertexIn.x() - this.x()) < QuadHelper.EPSILON
                && Math.abs(vertexIn.y() - this.y()) < QuadHelper.EPSILON
                && Math.abs(vertexIn.z() - this.z()) < QuadHelper.EPSILON;
    }

    /**
     * True if this point is on the line formed by the two given points.
     * Will return false for points that are "very close" to each other
     * because there essentially isn't enough resolution to make a firm
     * determination of what the line is.
     */
    public default boolean isOnLine(float x0, float y0, float z0, float x1, float y1, float z1)
    {
        float ab = Useful.distance(x0, y0, z0, x1, y1, z1);
        if(ab < QuadHelper.EPSILON * 5) return false;
        float bThis = Useful.distance(this.x(), this.y(), this.z(), x1, y1, z1);
        float aThis = Useful.distance(x0, y0, z0, this.x(), this.y(), this.z());
        return(Math.abs(ab - bThis - aThis) < QuadHelper.EPSILON);
    }

    public default boolean isOnLine(IVertex v0, IVertex v1)
    {
        return this.isOnLine(v0.x(), v0.y(), v0.z(), v1.x(), v1.y(), v1.z());
    }

    public default boolean hasNormal()
    {
        return this.normal() != null;
    }
}
