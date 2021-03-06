package grondag.exotic_matter.model.primitives.vertex;

import grondag.exotic_matter.model.primitives.QuadHelper;
import grondag.exotic_matter.varia.Useful;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;

public interface IVec3f
{
    public default float dotProduct(float xIn, float yIn, float zIn)
    {
        return Vec3Function.dotProduct(this.x(), this.y(), this.z(), xIn, yIn, zIn);
    }
    
    public default float dotProduct(IVec3f vec)
    {
        return dotProduct(vec.x(), vec.y(), vec.z());
    }

    /**
     * Returns a new vector with the result of this vector x the specified vector.
     */
    public default IVec3f crossProduct(IVec3f vec)
    {
        return Vec3f.create(this.y() * vec.z() - this.z() * vec.y(), this.z() * vec.x() - this.x() * vec.z(), this.x() * vec.y() - this.y() * vec.x());
    }
    
    public default float length()
    {
        return MathHelper.sqrt(lengthSquared());
    }

    public default float lengthSquared()
    {
        final float x = this.x();
        final float y = this.y();
        final float z = this.z();
        return x * x + y * y + z * z;
    }

    public float x();

    public float y();

    public float z();
    
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
    public default boolean isCsgEqual(IVec3f vertexIn)
    {
        return Math.abs(vertexIn.x() - this.x()) < QuadHelper.EPSILON
                && Math.abs(vertexIn.y() - this.y()) < QuadHelper.EPSILON
                && Math.abs(vertexIn.z() - this.z()) < QuadHelper.EPSILON;
    }

    /**
     * True if point i,j,k is on line formed by x0,y0,z0 and x1, y1, z1.<p>
     * 
     * Will return false for points that are "very close" to each other
     * because there essentially isn't enough resolution to make a firm
     * determination of what the line is.
     */
    public static boolean isPointOnLine(float i, float j, float k, float x0, float y0, float z0, float x1, float y1, float z1)
    {
        // points have to be far enough apart to form a line
        float ab = Useful.distance(x0, y0, z0, x1, y1, z1);
        if(ab < QuadHelper.EPSILON * 5) return false;
        
        float bThis = Useful.distance(i, j, k, x1, y1, z1);
        float aThis = Useful.distance(x0, y0, z0, i, j, k);
        return(Math.abs(ab - bThis - aThis) < QuadHelper.EPSILON);
    }
    
    /**
     * True if this point is on the line formed by the two given points.<p>
     * 
     * Will return false for points that are "very close" to each other
     * because there essentially isn't enough resolution to make a firm
     * determination of what the line is.
     */
    public default boolean isOnLine(float x0, float y0, float z0, float x1, float y1, float z1)
    {
        return isPointOnLine(this.x(), this.y(), this.z(), x0, y0, z0, x1, y1, z1);
    }

    public default boolean isOnLine(IVec3f v0, IVec3f v1)
    {
        return this.isOnLine(v0.x(), v0.y(), v0.z(), v1.x(), v1.y(), v1.z());
    }
    
    /**
     * Loads our x, y, z values into the provided array.
     */
    public default void toArray(float[] data)
    {
        data[0] = x();
        data[1] = y();
        data[2] = z();
    }
}
