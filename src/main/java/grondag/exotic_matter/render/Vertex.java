package grondag.exotic_matter.render;


import javax.annotation.Nonnull;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import grondag.exotic_matter.varia.Useful;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;

public class Vertex
{
    public final float x;
    public final float y;
    public final float z;
    public final float u;
    public final float v;
    public final int color;
    public final float normalX;
    public final float normalY;
    public final float normalZ;
    
    public Vertex(Vector3f point, float u, float v, int color, Vector3f normal)
    {
        this(point.x, point.y, point.z, u, v, color, normal);
    }
    
    @Deprecated
    public Vertex(Vec3d point, double u, double v, int color, Vec3d normal)
    {
        this((float)point.x, (float)point.y, (float)point.z, (float)u, (float)v, color, (float)normal.x, (float)normal.y, (float)normal.z);
    }
    
    public Vertex(float x, float y, float z, float u, float v, int color)
    {
        this(x, y, z, u, v, color, 0, 0, 0);
    }

    public Vertex(float x, float y, float z, float u, float v, int color, Vector3f normal)
    {
        this(x, y, z, u, v, color, normal.x, normal.y, normal.z);
    }
    
    public Vertex(float x, float y, float z, float u, float v, int color, float normalX, float normalY, float normalZ)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.u = u;
        this.v = v;
        this.color = color;
        this.normalX = normalX;
        this.normalY = normalY;
        this.normalZ = normalZ;
    }

    /** returns copy of this vertex with given normal */
    public Vertex withNormal(float normalXIn, float normalYIn, float normalZIn)
    {
        return new Vertex(this.x, this.y, this.z, this.u, this.v, this.color, normalXIn, normalYIn, normalZIn);
    }

    /** returns copy of this vertex with given color */
    public Vertex withColor(int colorIn)
    {
        return new Vertex(this.x, this.y, this.z, this.u, this.v, colorIn, this.normalX, this.normalY, this.normalZ);
    }

    /** returns copy of this vertex with given UV */
    public Vertex withUV(float uNew, float vNew)
    {
        return new Vertex(this.x, this.y, this.z, uNew, vNew, this.color, this.normalX, this.normalY, this.normalZ);
    }
    
    /** returns copy of this vertex with given XYZ coords */
    public Vertex withXYZ(float xNew, float yNew, float zNew)
    {
        return new Vertex(xNew, yNew, zNew, this.u, this.v, this.color, this.normalX, this.normalY, this.normalZ);
    }
    
    public boolean hasNormal()
    {
        return !(this.normalX == 0 && this.normalY == 0 && this.normalZ == 0);
    }

 
    /**
     * Returns a new, linearly interpolated vertex based on this vertex
     * and the other vertex provided.  Neither vertex is changed.
     * Factor 0 returns this vertex. Factor 1 return other vertex, 
     * with values in between returning a weighted average.
     */
    public Vertex interpolate(Vertex otherVertex, float otherWeight)
    {
        // tx = 2
        // ox = 1
        // w = 0
        // 2 +(1 - 2) * 0 = 2
        // 2 +(1 - 2) * 1 = 1
        
        float newX = this.x + (otherVertex.x - this.x) * otherWeight;
        float newY = this.y + (otherVertex.y - this.y) * otherWeight;
        float newZ = this.z + (otherVertex.z - this.z) * otherWeight;
        
        float normX = 0;
        float normY = 0;
        float normZ = 0;
        
        if(this.hasNormal() && otherVertex.hasNormal())
        {
            normX = this.normalX + (otherVertex.normalX - this.normalX) * otherWeight;
            normY = this.normalY + (otherVertex.normalY - this.normalY) * otherWeight;
            normZ = this.normalZ + (otherVertex.normalZ - this.normalZ) * otherWeight;
            
            float normScale= (float) (1/Math.sqrt(normX*normX + normY*normY + normZ*normZ));
            normX *= normScale;
            normY *= normScale;
            normZ *= normScale;
        }
        
        float newU = this.u + (otherVertex.u - this.u) * otherWeight;
        float newV = this.v + (otherVertex.v - this.v) * otherWeight;

        int newColor = (int) ((this.color & 0xFF) + ((otherVertex.color & 0xFF) - (this.color & 0xFF)) * otherWeight);
        newColor |= (int) ((this.color & 0xFF00) + ((otherVertex.color & 0xFF00) - (this.color & 0xFF00)) * otherWeight);
        newColor |= (int) ((this.color & 0xFF0000) + ((otherVertex.color & 0xFF0000) - (this.color & 0xFF0000)) * otherWeight);
        newColor |= (int) ((this.color & 0xFF000000) + ((otherVertex.color & 0xFF000000) - (this.color & 0xFF000000)) * otherWeight);

        return new Vertex(newX, newY, newZ, newU, newV, newColor, normX, normY, normZ);
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
    public float distanceToFacePlane(EnumFacing face)
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
    
    public boolean isOnFacePlane(EnumFacing face, float tolerance)
    {
        return Math.abs(this.distanceToFacePlane(face)) < tolerance;
    }
    
    @Override
    public Vertex clone()
    {
        return new Vertex(this.x, this.y, this.z, this.u, this.v, this.color, this.normalX, this.normalY, this.normalZ);
    }

    public Vertex transform(Matrix4f matrix, boolean rescaleToUnitCube)
    {

        Vector4f tmp = new Vector4f(this.x, this.y, this.z, 1f);

        matrix.transform(tmp);
        if (rescaleToUnitCube && Math.abs(tmp.w - 1f) > 1e-5)
        {
            tmp.scale(1f / tmp.w);
        }

        if(this.hasNormal())
        {
            Vector4f tmpNormal = new Vector4f(this.normalX, this.normalY, this.normalZ, 1f);
            matrix.transform(tmpNormal);
            float normScale= (float) (1/Math.sqrt(tmpNormal.x*tmpNormal.x + tmpNormal.y*tmpNormal.y + tmpNormal.z*tmpNormal.z));
            return new Vertex(tmp.x, tmp.y, tmp.z, u, v, color, tmpNormal.x * normScale, tmpNormal.y * normScale, tmpNormal.z * normScale);
        }
        else
        {
            return new Vertex(tmp.x, tmp.y, tmp.z, u, v, color);
        }

    }
    
    public @Nonnull Vertex add(@Nonnull Vertex vec)
    {
        return this.addVector(vec.x, vec.y, vec.z);
    }

    /**
     * Adds the specified x,y,z vector components to this vertex and returns the resulting vector. Does not change this
     * vertex. UV values remain same as original. 
     */
    public @Nonnull Vertex addVector(float x, float y, float z)
    {
        return new Vertex(this.x + x, this.y + y, this.z + z, u, v, color);
    }

    public float[] xyzToFloatArray()
    {
        float[] retVal = new float[3];
        retVal[0] = this.x;
        retVal[1] = this.y;
        retVal[2] = this.z;
        return retVal;
    }

    public float[] normalToFloatArray()
    {
        float[] retVal = null;
        if(this.hasNormal())
        {
            retVal = new float[3];
            retVal[0] = this.normalX;
            retVal[1] = this.normalY;
            retVal[2] = this.normalZ;
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
    public boolean isOnLine(float x0, float y0, float  z0, float x1, float y1,  float z1)
    {
        float ab = Useful.distance(x0, y0, z0, x1, y1, z1);
        float bThis = Useful.distance(this.x, this.y, this.z, x1, y1, z1);
        float aThis = Useful.distance(x0, y0, z0, this.x, this.y, this.z);
        return(Math.abs(ab - bThis - aThis) < QuadHelper.EPSILON);
    }
    
    public boolean isOnLine(Vertex v0, Vertex v1)
    {
        return this.isOnLine(v0.x, v0.y, v0.z, v1.x, v1.y, v1.z);
    }
    
    @Deprecated
    public Vec3d toVec3d()
    {
        return new Vec3d(this.x, this.y, this.z);
    }
}