package grondag.exotic_matter.render;

import grondag.exotic_matter.varia.Useful;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;

public interface IPolygonVertex
{


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
    
    @Deprecated
    public default Vec3f toVec3f()
    {
        return new Vec3f(this.x(), this.y(), this.z());
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
    
    //TODO: should simply return face normal if not fancy
    @Deprecated
    public default boolean hasNormal()
    {
        return !(this.normalX() == 0 && this.normalY() == 0 && this.normalZ() == 0);
    }

    /**
     * Returns a new, linearly interpolated vertex based on this vertex
     * and the other vertex provided.  Neither vertex is changed.
     * Factor 0 returns this vertex. Factor 1 return other vertex, 
     * with values in between returning a weighted average.
     */
    public default IPolygonVertex interpolate(IPolygonVertex otherVertex, float otherWeight)
    {
        // tx = 2
        // ox = 1
        // w = 0
        // 2 +(1 - 2) * 0 = 2
        // 2 +(1 - 2) * 1 = 1
        
        float newX = this.x() + (otherVertex.x() - this.x()) * otherWeight;
        float newY = this.y() + (otherVertex.y() - this.y()) * otherWeight;
        float newZ = this.z() + (otherVertex.z() - this.z()) * otherWeight;
        
        float normX = 0;
        float normY = 0;
        float normZ = 0;
        
        if(this.hasNormal() && otherVertex.hasNormal())
        {
            normX = this.normalX() + (otherVertex.normalX() - this.normalX()) * otherWeight;
            normY = this.normalY() + (otherVertex.normalY() - this.normalY()) * otherWeight;
            normZ = this.normalZ() + (otherVertex.normalZ() - this.normalZ()) * otherWeight;
            
            float normScale= (float) (1/Math.sqrt(normX*normX + normY*normY + normZ*normZ));
            normX *= normScale;
            normY *= normScale;
            normZ *= normScale;
        }
        
        float newU = this.u() + (otherVertex.u() - this.u()) * otherWeight;
        float newV = this.v() + (otherVertex.v() - this.v()) * otherWeight;

        int newColor = (int) ((this.color() & 0xFF) + ((otherVertex.color() & 0xFF) - (this.color() & 0xFF)) * otherWeight);
        newColor |= (int) ((this.color() & 0xFF00) + ((otherVertex.color() & 0xFF00) - (this.color() & 0xFF00)) * otherWeight);
        newColor |= (int) ((this.color() & 0xFF0000) + ((otherVertex.color() & 0xFF0000) - (this.color() & 0xFF0000)) * otherWeight);
        newColor |= (int) ((this.color() & 0xFF000000) + ((otherVertex.color() & 0xFF000000) - (this.color() & 0xFF000000)) * otherWeight);

        return new Vertex(newX, newY, newZ, newU, newV, newColor, normX, normY, normZ);
    }
}