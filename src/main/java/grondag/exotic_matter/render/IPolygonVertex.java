package grondag.exotic_matter.render;

import grondag.exotic_matter.varia.Useful;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;

public interface IPolygonVertex
{

    //TODO: should simply return face normal if not fancy
    @Deprecated
    boolean hasNormal();

    /**
     * Returns a new, linearly interpolated vertex based on this vertex
     * and the other vertex provided.  Neither vertex is changed.
     * Factor 0 returns this vertex. Factor 1 return other vertex, 
     * with values in between returning a weighted average.
     */
    IPolygonVertex interpolate(IPolygonVertex otherVertex, float otherWeight);

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

    public default float[] xyzToFloatArray()
    {
        float[] retVal = new float[3];
        retVal[0] = this.x();
        retVal[1] = this.y();
        retVal[2] = this.z();
        return retVal;
    }

    public default float[] normalToFloatArray()
    {
        float[] retVal = null;
        if(this.hasNormal())
        {
            retVal = new float[3];
            retVal[0] = this.normalX();
            retVal[1] = this.normalY();
            retVal[2] = this.normalZ();
        }
        return retVal;
    }

    /** 
     * True if both vertices are at the same point. 
     */
    public default boolean isCsgEqual(IPolygonVertex vertexIn)
    {
        return Math.abs(vertexIn.x() - this.x()) < QuadHelper.EPSILON
                && Math.abs(vertexIn.y() - this.y()) < QuadHelper.EPSILON
                && Math.abs(vertexIn.z() - this.z()) < QuadHelper.EPSILON;
    }

    /**
     * True if this point is on the line formed by the two given points.
     */
    public default boolean isOnLine(float x0, float y0, float z0, float x1, float y1, float z1)
    {
        float ab = Useful.distance(x0, y0, z0, x1, y1, z1);
        float bThis = Useful.distance(this.x(), this.y(), this.z(), x1, y1, z1);
        float aThis = Useful.distance(x0, y0, z0, this.x(), this.y(), this.z());
        return(Math.abs(ab - bThis - aThis) < QuadHelper.EPSILON);
    }

    public default boolean isOnLine(IPolygonVertex v0, IPolygonVertex v1)
    {
        return this.isOnLine(v0.x(), v0.y(), v0.z(), v1.x(), v1.y(), v1.z());
    }

    @Deprecated
    public default Vec3d toVec3d()
    {
        return new Vec3d(this.x(), this.y(), this.z());
    }

    float x();

    float y();

    float z();

    float u();

    float v();

    int color();

    float normalX();

    float normalY();

    float normalZ();

}