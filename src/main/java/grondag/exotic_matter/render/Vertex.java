package grondag.exotic_matter.render;


import javax.annotation.Nonnull;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector4f;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;

public class Vertex extends Vec3d
{
    public final double u;
    public final double v;
    public final int color;
    protected final Vec3d normal;
    
    public Vertex(Vec3d point, double u, double v, int color, Vec3d normal)
    {
        this(point.x, point.y, point.z, u, v, color, normal);
    }
    
    public Vertex(double x, double y, double z, double u, double v, int color)
    {
        this(x, y, z, u, v, color, null);
    }

    public Vertex(double x, double y, double z, double u, double v, int color, Vec3d normal)
    {
        super(x, y, z);
        this.u = u;
        this.v = v;
        this.color = color;
        this.normal = normal;
    }

    /** returns copy of this vertex with given normal */
    public Vertex withNormal(Vec3d normalIn)
    {
        return new Vertex(this.x, this.y, this.z, this.u, this.v, this.color, normalIn);
    }

    /** returns copy of this vertex with given color */
    public Vertex withColor(int colorIn)
    {
        return new Vertex(this.x, this.y, this.z, this.u, this.v, colorIn, this.normal);
    }

    /** returns copy of this vertex with given UV */
    public Vertex withUV(double uNew, double vNew)
    {
        return new Vertex(this.x, this.y, this.z, uNew, vNew, this.color, this.normal);
    }
    
    /** returns copy of this vertex with given XYZ coords */
    public Vertex withXYZ(double xNew, double yNew, double zNew)
    {
        return new Vertex(xNew, yNew, zNew, this.u, this.v, this.color, this.normal);
    }
    
    public boolean hasNormal()
    {
        return this.normal != null;
    }

    public Vec3d getNormal()
    {
        return this.normal;
    }
    /**
     * Returns a new, linearly interpolated vertex based on this vertex
     * and the other vertex provided.  Neither vertex is changed.
     * Factor 0 returns this vertex. Factor 1 return other vertex, 
     * with values in between returning a weighted average.
     */
    public Vertex interpolate(Vertex otherVertex, double otherWeight)
    {
        Vec3d newPos = this.add(otherVertex.subtract(this).scale(otherWeight));
        Vec3d newNorm = null;

        if(this.normal != null && otherVertex.normal != null)
        {
            newNorm = this.normal.add(otherVertex.normal.subtract(this.normal).scale(otherWeight));
        }
        double newU = this.u + (otherVertex.u - this.u) * otherWeight;
        double newV = this.v + (otherVertex.v - this.v) * otherWeight;

        int newColor = (int) ((this.color & 0xFF) + ((otherVertex.color & 0xFF) - (this.color & 0xFF)) * otherWeight);
        newColor |= (int) ((this.color & 0xFF00) + ((otherVertex.color & 0xFF00) - (this.color & 0xFF00)) * otherWeight);
        newColor |= (int) ((this.color & 0xFF0000) + ((otherVertex.color & 0xFF0000) - (this.color & 0xFF0000)) * otherWeight);
        newColor |= (int) ((this.color & 0xFF000000) + ((otherVertex.color & 0xFF000000) - (this.color & 0xFF000000)) * otherWeight);

        return new Vertex(newPos.x, newPos.y, newPos.z, newU, newV, newColor, newNorm);
    }

    /**
     * Returns a signed distance to the plane of the given face.
     * Positive numbers mean in front of face, negative numbers in back.
     */
//    public double distanceToFacePlane(EnumFacing face)
//    {
//        int offset = face.getAxisDirection() == AxisDirection.POSITIVE ? 1 : 0;
//        return new Vec3d(face.getDirectionVec()).dotProduct(this) - offset;
//    }

    /**
     * Returns a signed distance to the plane of the given face.
     * Positive numbers mean in front of face, negative numbers in back.
     */
    public double distanceToFacePlane(EnumFacing face)
    {
        // could use dot product, but exploiting privileged case for less math
        switch(face)
        {
        case UP:
            return this.y - 1;

        case DOWN:
            return - this.y;
            
        case EAST:
            return this.x - 1;

        case WEST:
            return -this.x;

        case NORTH:
            return -this.z;
            
        case SOUTH:
            return this.z - 1;

        default:
            // make compiler shut up about unhandled case
            return 0;
        }
    }
    
    public boolean isOnFacePlane(EnumFacing face, double tolerance)
    {
        return Math.abs(this.distanceToFacePlane(face)) < tolerance;
    }
    
    @Override
    public Vertex clone()
    {
        return new Vertex(this.x, this.y, this.z, this.u, this.v, this.color, this.normal);
    }

    public Vertex transform(Matrix4f matrix, boolean rescaleToUnitCube)
    {

        Vector4f tmp = new Vector4f((float) x, (float) y, (float) z, 1f);

        matrix.transform(tmp);
        if (rescaleToUnitCube && Math.abs(tmp.w - 1f) > 1e-5)
        {
            tmp.scale(1f / tmp.w);
        }

        if(this.hasNormal())
        {
            Vector4f tmpNormal = new Vector4f((float)this.normal.x, (float)this.normal.y, (float)this.normal.z, 1f);
            matrix.transform(tmp);
            Vec3d newNormal = new Vec3d(tmpNormal.x, tmpNormal.y, tmpNormal.z);
            newNormal.normalize();
            return new Vertex(tmp.x, tmp.y, tmp.z, u, v, color, newNormal);
        }
        else
        {
            return new Vertex(tmp.x, tmp.y, tmp.z, u, v, color);
        }

    }
    
    @Override
    public @Nonnull Vertex add(@Nonnull Vec3d vec)
    {
        return this.addVector(vec.x, vec.y, vec.z);
    }

    /**
     * Adds the specified x,y,z vector components to this vertex and returns the resulting vector. Does not change this
     * vertex. UV values remain same as original. 
     */
    @Override
    public @Nonnull Vertex addVector(double x, double y, double z)
    {
        return new Vertex(this.x + x, this.y + y, this.z + z, u, v, color);
    }

    public float[] xyzToFloatArray()
    {
        float[] retVal = new float[3];
        retVal[0] = (float)this.x;
        retVal[1] = (float)this.y;
        retVal[2] = (float)this.z;
        return retVal;
    }

    public float[] normalToFloatArray()
    {
        float[] retVal = null;
        if(this.hasNormal())
        {
            retVal = new float[3];
            retVal[0] = (float) this.normal.x;
            retVal[1] = (float) this.normal.y;
            retVal[2] = (float) this.normal.z;
        }
        return retVal;
    }

    /** 
     * True if both vertices are at the same point. 
     */
    public boolean isCsgEqual(Vertex vertexIn)
    {
        return Math.abs(vertexIn.x - this.x) < QuadHelper.EPSILON
            && Math.abs(vertexIn.y - this.y) < QuadHelper.EPSILON
            && Math.abs(vertexIn.z - this.z) < QuadHelper.EPSILON;

    }

    /**
     * True if this point is on the line formed by the two given points.
     */
    public boolean isOnLine(Vec3d pointA, Vec3d pointB)
    {
        return(Math.abs(pointA.distanceTo(pointB) - pointB.distanceTo(this) - pointA.distanceTo(this)) < QuadHelper.EPSILON);
    }
}