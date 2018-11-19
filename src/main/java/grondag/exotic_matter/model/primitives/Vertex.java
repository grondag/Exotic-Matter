package grondag.exotic_matter.model.primitives;


import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector4f;

import grondag.exotic_matter.varia.ColorHelper;
import grondag.exotic_matter.varia.Useful;
import net.minecraft.util.EnumFacing;

public class Vertex implements IPaintableVertex
{
    public static final IVertexFactory DEFAULT_FACTORY = new IVertexFactory()
    {
        @Override
        public Vertex newVertex(float x, float y, float z, float u, float v, int color, @Nullable Vec3f normal, int glow)
        {
            return new Vertex(x, y, z, u, v, color, normal, glow);
        }

        @Override
        public Vertex interpolate(float newX, float newY, float newZ, @Nullable Vec3f newNormal, Vertex from, Vertex to, float toWeight)
        {
            final int newGlow = (int) (from.glow + (to.glow - from.glow) * toWeight);
            
            int newColor = ColorHelper.interpolate(from.color, to.color, toWeight);
            final float newU = from.u + (to.u - from.u) * toWeight;
            final float newV = from.v + (to.v - from.v) * toWeight;
            return newVertex(newX, newY, newZ, newU, newV, newColor, newNormal, newGlow);
        }

        @Override
        public Vertex withColorGlow(Vertex vertex, int colorIn, int glowIn)
        {
            return newVertex(vertex.pos.x, vertex.pos.y, vertex.pos.z, vertex.u, vertex.v, colorIn, vertex.normal, glowIn);
        }

        @Override
        public Vertex withUV(Vertex vertex, float uNew, float vNew)
        {
            return newVertex(vertex.pos.x, vertex.pos.y, vertex.pos.z, uNew, vNew, vertex.color, vertex.normal, vertex.glow);
        }

        @Override
        public Vertex withGeometry(Vertex vertex, float x, float y, float z, @Nullable Vec3f normal)
        {
            return newVertex(x, y, z, vertex.u, vertex.v, vertex.color, normal, vertex.glow);
        }
    };
    
    public final Vec3f pos;
    public final float u;
    public final float v;
    public final int color;
    public final @Nullable Vec3f normal;
    public final short glow;
    
    protected Vertex(float x, float y, float z, float u, float v, int color, @Nullable Vec3f normal, int glow)
    {
        this.pos = Vec3f.create(x, y, z);
        this.u = u;
        this.v = v;
        this.color = color;
        this.normal = normal;
        this.glow = (short) (glow & 0xFF);
    }
    
    protected IVertexFactory factory()
    {
        return DEFAULT_FACTORY;
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
     * Returns copy of self if normal is present. If normal is present return copy with inverted normal.
     */
    @SuppressWarnings("null")
    public final Vertex flipped()
    {
        return this.normal == null
                ? this
                : factory().withGeometry(this, this.pos.x, this.pos.y, this.pos.z, this.normal.inverse());
    }
    
    /** returns copy of this vertex with given normal */
    public final Vertex withNormal(float normalXIn, float normalYIn, float normalZIn)
    {
        return factory().withGeometry(this, this.pos.x, this.pos.y, this.pos.z, Vec3f.create(normalXIn, normalYIn, normalZIn));
    }
    
    /** returns copy of this vertex with given normal */
    public final Vertex withNormal(Vec3f normal)
    {
        return factory().withGeometry(this, this.pos.x, this.pos.y, this.pos.z, normal);
    }
    
    @Override
    public final Vertex withColorGlow(int colorIn, int glowIn)
    {
        return factory().withColorGlow(this, colorIn, glowIn);
    }
    
    /** returns copy of this vertex with given UV */
    @Override
    public final Vertex withUV(float uNew, float vNew)
    {
        return factory().withUV(this, uNew, vNew);
    }
    
    /** returns copy of this vertex with given XYZ coords */
    public final Vertex withXYZ(float xNew, float yNew, float zNew)
    {
        return factory().withGeometry(this, xNew, yNew, zNew, this.normal);
    }
    
    public final Vertex transform(Matrix4f matrix, boolean rescaleToUnitCube)
    {

        Vector4f tmp = new Vector4f(this.pos.x, this.pos.y, this.pos.z, 1f);

        matrix.transform(tmp);
        if (rescaleToUnitCube && Math.abs(tmp.w - 1f) > 1e-5)
        {
            tmp.scale(1f / tmp.w);
        }

        if(this.normal == null)
        {
            return factory().withGeometry(this, tmp.x, tmp.y, tmp.z, null);
        }
        else
        {
            @SuppressWarnings("null")
            Vector4f tmpNormal = new Vector4f(this.normal.x, this.normal.y, this.normal.z, 1f);
            matrix.transform(tmpNormal);
            float normScale= (float) (1/Math.sqrt(tmpNormal.x*tmpNormal.x + tmpNormal.y*tmpNormal.y + tmpNormal.z*tmpNormal.z));
            return factory().withGeometry(this, tmp.x, tmp.y, tmp.z, Vec3f.create(tmpNormal.x * normScale, tmpNormal.y * normScale, tmpNormal.z * normScale));
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
            return this.pos.y - 1;

        case DOWN:
            return - this.pos.y;
            
        case EAST:
            return this.pos.x - 1;

        case WEST:
            return -this.pos.x;

        case NORTH:
            return -this.pos.z;
            
        case SOUTH:
            return this.pos.z - 1;

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
        return Math.abs(vertexIn.pos.x - this.pos.x) < QuadHelper.EPSILON
                && Math.abs(vertexIn.pos.y - this.pos.y) < QuadHelper.EPSILON
                && Math.abs(vertexIn.pos.z - this.pos.z) < QuadHelper.EPSILON;
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
        float bThis = Useful.distance(this.pos.x, this.pos.y, this.pos.z, x1, y1, z1);
        float aThis = Useful.distance(x0, y0, z0, this.pos.x, this.pos.y, this.pos.z);
        return(Math.abs(ab - bThis - aThis) < QuadHelper.EPSILON);
    }

    public final boolean isOnLine(Vertex v0, Vertex v1)
    {
        return this.isOnLine(v0.pos.x, v0.pos.y, v0.pos.z, v1.pos.x, v1.pos.y, v1.pos.z);
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
        
        final float newX = this.pos.x + (otherVertex.pos.x - this.pos.x) * otherWeight;
        final float newY = this.pos.y + (otherVertex.pos.y - this.pos.y) * otherWeight;
        final float newZ = this.pos.z + (otherVertex.pos.z - this.pos.z) * otherWeight;
        
        final Vec3f thisNormal = this.normal;
        final Vec3f otherNormal = otherVertex.normal;
        
        if(thisNormal == null || otherNormal == null)
        {
            return factory().interpolate(newX, newY, newZ, null, this, otherVertex, otherWeight);
        }
        else
        {
            final float normX = thisNormal.x + (otherNormal.x - thisNormal.x) * otherWeight;
            final float normY = thisNormal.y + (otherNormal.y - thisNormal.y) * otherWeight;
            final float normZ = thisNormal.z + (otherNormal.z - thisNormal.z) * otherWeight;
            final float normScale= (float) (1/Math.sqrt(normX*normX + normY*normY + normZ*normZ));
            
            return factory().interpolate(newX, newY, newZ, Vec3f.create(normX * normScale, normY * normScale, normZ * normScale), this, otherVertex, otherWeight);
        }
    }
    
    /**
     * Returns copy of this vertex with UV coordinates locked to the given face.
     */
    public final Vertex uvLocked(EnumFacing face)
    {
        switch(face)
        {
            case EAST:
                return this.withUV((1 - this.pos.z), (1 - this.pos.y));
                
            case WEST:
                return this.withUV(this.pos.z, (1 - this.pos.y));
                
            case NORTH:
                return this.withUV((1 - this.pos.x), (1 - this.pos.y));
                
            case SOUTH:
                return this.withUV(this.pos.x, (1 - this.pos.y));
                
            case DOWN:
                return this.withUV(this.pos.x, (1 - this.pos.z));
                
            case UP:
            default:
                // our default semantic for UP is different than MC
                // "top" is north instead of south
                return this.withUV(this.pos.x, this.pos.z);
        }
    }

    @Override
    public short glow()
    {
        return this.glow;
    }

    @Override
    public int color()
    {
        return this.color;
    }

    @Override
    public float u()
    {
        return this.u;
    }

    @Override
    public float v()
    {
        return this.v;
    }

    @Override
    public IPaintableVertex interpolate(IPaintableVertex nextVertex, float dist)
    {
        return this.interpolate((Vertex)nextVertex, dist);
    }

    @Override
    public float x()
    {
        return this.pos.x;
    }
    
    @Override
    public float y()
    {
        return this.pos.y;
    }
    
    @Override
    public float z()
    {
        return this.pos.z;
    }
    
    @Override
    public @Nullable Vec3f normal()
    {
        return this.normal;
    }
    
    @Override
    public IPaintableVertex forTextureLayer(int layer)
    {
        switch(layer)
        {
        case 0:
            return this;
        default:
            throw new IndexOutOfBoundsException();
        }
    }
}