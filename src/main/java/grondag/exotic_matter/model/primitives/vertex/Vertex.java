package grondag.exotic_matter.model.primitives.vertex;


import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector4f;

import grondag.exotic_matter.varia.ColorHelper;
import net.minecraft.util.EnumFacing;

public class Vertex implements IPaintableVertex, IMutableVertex
{
    public static final IVertexFactory DEFAULT_FACTORY = new IVertexFactory()
    {
        @Override
        public Vertex newVertex(float x, float y, float z, float u, float v, int color, @Nullable Vec3f normal, int glow)
        {
            return new Vertex(x, y, z, u, v, color, normal, glow);
        }

        @Override
        public Vertex interpolate(float newX, float newY, float newZ, @Nullable Vec3f newNormal, IVertex from, IVertex to, float toWeight)
        {
            final int newGlow = (int) (from.glow() + (to.glow() - from.glow()) * toWeight);
            
            int newColor = ColorHelper.interpolate(from.color(), to.color(), toWeight);
            final float newU = from.u() + (to.u() - from.u()) * toWeight;
            final float newV = from.v() + (to.v() - from.v()) * toWeight;
            return newVertex(newX, newY, newZ, newU, newV, newColor, newNormal, newGlow);
        }

        @Override
        public Vertex withColorGlow(Vertex vertex, int colorIn, int glowIn)
        {
            return newVertex(vertex.pos.x(), vertex.pos.y(), vertex.pos.z(), vertex.u, vertex.v, colorIn, vertex.normal, glowIn);
        }

        @Override
        public Vertex withUV(Vertex vertex, float uNew, float vNew)
        {
            return newVertex(vertex.pos.x(), vertex.pos.y(), vertex.pos.z(), uNew, vNew, vertex.color, vertex.normal, vertex.glow);
        }

        @Override
        public Vertex withGeometry(Vertex vertex, float x, float y, float z, @Nullable Vec3f normal)
        {
            return newVertex(x, y, z, vertex.u, vertex.v, vertex.color, normal, vertex.glow);
        }
    };
    
    protected final Vec3f pos;
    protected final float u;
    protected final float v;
    protected final int color;
    protected final @Nullable Vec3f normal;
    protected final short glow;
    
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
    @Override
    public final Vertex flipped()
    {
        //TODO: should normal already be mutable?
        return this.normal == null
                ? this
                : factory().withGeometry(this, this.pos.x(), this.pos.y(), this.pos.z(), this.normal.mutableCopy().invert());
    }
    
    /** returns copy of this vertex with given normal */
    @Override
    public final Vertex withNormal(Vec3f normal)
    {
        return factory().withGeometry(this, this.pos.x(), this.pos.y(), this.pos.z(), normal);
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
    @Override
    public final Vertex withXYZ(float xNew, float yNew, float zNew)
    {
        return factory().withGeometry(this, xNew, yNew, zNew, this.normal);
    }
    
    @Override
    public final Vertex transform(Matrix4f matrix, boolean rescaleToUnitCube)
    {

        Vector4f tmp = new Vector4f(this.pos.x(), this.pos.y(), this.pos.z(), 1f);

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
            Vector4f tmpNormal = new Vector4f(this.normal.x(), this.normal.y(), this.normal.z(), 1f);
            matrix.transform(tmpNormal);
            float normScale= (float) (1/Math.sqrt(tmpNormal.x*tmpNormal.x + tmpNormal.y*tmpNormal.y + tmpNormal.z*tmpNormal.z));
            return factory().withGeometry(this, tmp.x, tmp.y, tmp.z, Vec3f.create(tmpNormal.x * normScale, tmpNormal.y * normScale, tmpNormal.z * normScale));
        }

    }

    /**
     * Returns a new, linearly interpolated vertex based on this vertex
     * and the other vertex provided.  Neither vertex is changed.
     * Factor 0 returns this vertex. Factor 1 return other vertex, 
     * with values in between returning a weighted average.
     */
    @Override
    public final Vertex interpolate(IVertex otherVertex, final float otherWeight)
    {
        // tx = 2
        // ox = 1
        // w = 0
        // 2 +(1 - 2) * 0 = 2
        // 2 +(1 - 2) * 1 = 1
        
        final float newX = this.pos.x() + (otherVertex.x() - this.pos.x()) * otherWeight;
        final float newY = this.pos.y() + (otherVertex.y() - this.pos.y()) * otherWeight;
        final float newZ = this.pos.z() + (otherVertex.z() - this.pos.z()) * otherWeight;
        
        final Vec3f thisNormal = this.normal;
        final Vec3f otherNormal = otherVertex.normal();
        
        if(thisNormal == null || otherNormal == null)
        {
            return factory().interpolate(newX, newY, newZ, null, this, otherVertex, otherWeight);
        }
        else
        {
            final float normX = thisNormal.x() + (otherNormal.x() - thisNormal.x()) * otherWeight;
            final float normY = thisNormal.y() + (otherNormal.y() - thisNormal.y()) * otherWeight;
            final float normZ = thisNormal.z() + (otherNormal.z() - thisNormal.z()) * otherWeight;
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
                return this.withUV((1 - this.pos.z()), (1 - this.pos.y()));
                
            case WEST:
                return this.withUV(this.pos.z(), (1 - this.pos.y()));
                
            case NORTH:
                return this.withUV((1 - this.pos.x()), (1 - this.pos.y()));
                
            case SOUTH:
                return this.withUV(this.pos.x(), (1 - this.pos.y()));
                
            case DOWN:
                return this.withUV(this.pos.x(), (1 - this.pos.z()));
                
            case UP:
            default:
                // our default semantic for UP is different than MC
                // "top" is north instead of south
                return this.withUV(this.pos.x(), this.pos.z());
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
    public float x()
    {
        return this.pos.x();
    }
    
    @Override
    public float y()
    {
        return this.pos.y();
    }
    
    @Override
    public float z()
    {
        return this.pos.z();
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

    @Override
    public Vec3f pos()
    {
        return this.pos;
    }

    @Override
    public int layerIndex()
    {
        return 0;
    }
}