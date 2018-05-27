package grondag.exotic_matter.render;


import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector4f;

import grondag.exotic_matter.varia.Useful;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;

public final class Vertex extends Vec3f
{
    /**
     * Note that vertex texture coordinates are scaled 0-1 instead of 
     * the 0-16 used in the quad min/max texture coordinates.
     */
    public final float u;
    /**
     * Note that vertex texture coordinates are scaled 0-1 instead of 
     * the 0-16 used in the quad min/max texture coordinates.
     */
    public final float v;
    public final int color;
    public final @Nullable Vec3f normal;
    
    public Vertex(Vec3f point, float u, float v, int color, Vec3f normal)
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
        this(x, y, z, u, v, color, null);
    }

    public Vertex(float x, float y, float z, float u, float v, int color, @Nullable Vec3f normal)
    {
        super(x, y, z);
        this.u = u;
        this.v = v;
        this.color = color;
        this.normal = normal;
    }
    
    public Vertex(float x, float y, float z, float u, float v, int color, float normalX, float normalY, float normalZ)
    {
        this(x, y, z, u, v, color, new Vec3f(normalX, normalY, normalZ));
    }

    /**
     * Not supported because is immutable
     */
    @Override
    public final Vertex clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }
    
    /**
     * Returns copy of self. If normal is present it is inverted in the copy.
     * @return
     */
    public final Vertex flipped()
    {
        return this.normal == null
                ? new Vertex(this.x, this.y, this.z, this.u, this.v, this.color)
                : new Vertex(this.x, this.y, this.z, this.u, this.v, this.color, this.normal.inverse());
    }
    
    /** returns copy of this vertex with given normal */
    public final Vertex withNormal(float normalXIn, float normalYIn, float normalZIn)
    {
        return new Vertex(this.x, this.y, this.z, this.u, this.v, this.color, normalXIn, normalYIn, normalZIn);
    }
    
    /** returns copy of this vertex with given normal */
    public final Vertex withNormal(Vec3f normal)
    {
        return new Vertex(this.x, this.y, this.z, this.u, this.v, this.color, normal);
    }

    /** returns copy of this vertex with given color */
    public final Vertex withColor(int colorIn)
    {
        return new Vertex(this.x, this.y, this.z, this.u, this.v, colorIn, this.normal);
    }

    /** returns copy of this vertex with given UV */
    public final Vertex withUV(float uNew, float vNew)
    {
        return new Vertex(this.x, this.y, this.z, uNew, vNew, this.color, this.normal);
    }
    
    /** returns copy of this vertex with given XYZ coords */
    public final Vertex withXYZ(float xNew, float yNew, float zNew)
    {
        return new Vertex(xNew, yNew, zNew, this.u, this.v, this.color, this.normal);
    }
    
    public final Vertex transform(Matrix4f matrix, boolean rescaleToUnitCube)
    {

        Vector4f tmp = new Vector4f(this.x, this.y, this.z, 1f);

        matrix.transform(tmp);
        if (rescaleToUnitCube && Math.abs(tmp.w - 1f) > 1e-5)
        {
            tmp.scale(1f / tmp.w);
        }

        if(this.normal != null)
        {
            Vector4f tmpNormal = new Vector4f(this.normal.x, this.normal.y, this.normal.z, 1f);
            matrix.transform(tmpNormal);
            float normScale= (float) (1/Math.sqrt(tmpNormal.x*tmpNormal.x + tmpNormal.y*tmpNormal.y + tmpNormal.z*tmpNormal.z));
            return new Vertex(tmp.x, tmp.y, tmp.z, this.u, this.v, this.color, tmpNormal.x * normScale, tmpNormal.y * normScale, tmpNormal.z * normScale);
        }
        else
        {
            return new Vertex(tmp.x, tmp.y, tmp.z, this.u, this.v, this.color);
        }

    }
    
    /**
     * Returns a signed distance to the plane of the given face.
     * Positive numbers mean in front of face, negative numbers in back.
     */
    public final float distanceToFacePlane(EnumFacing face)
    {
        // could use dot product, but exploiting special case for less math
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

    public final boolean isOnFacePlane(EnumFacing face, float tolerance)
    {
        return Math.abs(this.distanceToFacePlane(face)) < tolerance;
    }

    /** 
     * True if both vertices are at the same point. 
     */
    public final boolean isCsgEqual(Vertex vertexIn)
    {
        return Math.abs(vertexIn.x - this.x) < QuadHelper.EPSILON
                && Math.abs(vertexIn.y - this.y) < QuadHelper.EPSILON
                && Math.abs(vertexIn.z - this.z) < QuadHelper.EPSILON;
    }

    /**
     * True if this point is on the line formed by the two given points.
     * Will return false for points that are "very close" to each other
     * because there essentially isn't enough resolution to make a firm
     * determination of what the line is.
     */
    public final boolean isOnLine(float x0, float y0, float z0, float x1, float y1, float z1)
    {
        float ab = Useful.distance(x0, y0, z0, x1, y1, z1);
        if(ab < QuadHelper.EPSILON * 5) return false;
        float bThis = Useful.distance(this.x, this.y, this.z, x1, y1, z1);
        float aThis = Useful.distance(x0, y0, z0, this.x, this.y, this.z);
        return(Math.abs(ab - bThis - aThis) < QuadHelper.EPSILON);
    }

    public final boolean isOnLine(Vertex v0, Vertex v1)
    {
        return this.isOnLine(v0.x, v0.y, v0.z, v1.x, v1.y, v1.z);
    }

    public final boolean hasNormal()
    {
        return this.normal != null;
    }

    /**
     * Returns a new, linearly interpolated vertex based on this vertex
     * and the other vertex provided.  Neither vertex is changed.
     * Factor 0 returns this vertex. Factor 1 return other vertex, 
     * with values in between returning a weighted average.
     */
    public final Vertex interpolate(Vertex otherVertex, final float otherWeight)
    {
        // tx = 2
        // ox = 1
        // w = 0
        // 2 +(1 - 2) * 0 = 2
        // 2 +(1 - 2) * 1 = 1
        
        final float newX = this.x + (otherVertex.x - this.x) * otherWeight;
        final float newY = this.y + (otherVertex.y - this.y) * otherWeight;
        final float newZ = this.z + (otherVertex.z - this.z) * otherWeight;
        
        final float newU = this.u + (otherVertex.u - this.u) * otherWeight;
        final float newV = this.v + (otherVertex.v - this.v) * otherWeight;
        
        final int thisRed = this.color & 0xFF;
        final int thisGreen = this.color & 0xFF00;
        final int thisBlue = this.color & 0xFF0000;
        final int thisAlpha = this.color & 0xFF000000;

        int newColor = (int) (thisRed + ((otherVertex.color & 0xFF) - thisRed) * otherWeight);
        newColor |= (int) (thisGreen + ((otherVertex.color & 0xFF00) - thisGreen) * otherWeight);
        newColor |= (int) (thisBlue + ((otherVertex.color & 0xFF0000) - thisBlue) * otherWeight);
        newColor |= (int) (thisAlpha + ((otherVertex.color & 0xFF000000) - thisAlpha) * otherWeight);
        
        final Vec3f thisNormal = this.normal;
        final Vec3f otherNormal = otherVertex.normal;
        
        if(thisNormal == null || otherNormal == null)
        {
            return new Vertex(newX, newY, newZ, newU, newV, newColor);
        }
        else
        {
            final float normX = thisNormal.x + (otherNormal.x - thisNormal.x) * otherWeight;
            final float normY = thisNormal.y + (otherNormal.y - thisNormal.y) * otherWeight;
            final float normZ = thisNormal.z + (otherNormal.z - thisNormal.z) * otherWeight;
            final float normScale= (float) (1/Math.sqrt(normX*normX + normY*normY + normZ*normZ));
            
            return new Vertex(newX, newY, newZ, newU, newV, newColor, normX * normScale, normY * normScale, normZ * normScale);
        }
    }
  
}