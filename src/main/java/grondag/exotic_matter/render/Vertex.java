package grondag.exotic_matter.render;


import javax.annotation.Nonnull;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import net.minecraft.util.math.Vec3d;

public class Vertex extends Vec3f implements IFancyVertex
{
    private final float u;
    private final float v;
    private final int color;
    private final float normalX;
    private final float normalY;
    private final float normalZ;
    
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
        super(x, y, z);
        this.u = u;
        this.v = v;
        this.color = color;
        this.normalX = normalX;
        this.normalY = normalY;
        this.normalZ = normalZ;
    }

    /**
     * Not supported because is immutable
     */
    @Override
    public Vertex clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }
    
    /** returns copy of this vertex with given normal */
    public Vertex withNormal(float normalXIn, float normalYIn, float normalZIn)
    {
        return new Vertex(this.x(), this.y(), this.z(), this.u(), this.v(), this.color(), normalXIn, normalYIn, normalZIn);
    }

    /** returns copy of this vertex with given color */
    public Vertex withColor(int colorIn)
    {
        return new Vertex(this.x(), this.y(), this.z(), this.u(), this.v(), colorIn, this.normalX(), this.normalY(), this.normalZ());
    }

    /** returns copy of this vertex with given UV */
    public Vertex withUV(float uNew, float vNew)
    {
        return new Vertex(this.x(), this.y(), this.z(), uNew, vNew, this.color(), this.normalX(), this.normalY(), this.normalZ());
    }
    
    /** returns copy of this vertex with given XYZ coords */
    public Vertex withXYZ(float xNew, float yNew, float zNew)
    {
        return new Vertex(xNew, yNew, zNew, this.u(), this.v(), this.color(), this.normalX(), this.normalY(), this.normalZ());
    }
    
 
    
    public IPolygonVertex transform(Matrix4f matrix, boolean rescaleToUnitCube)
    {

        Vector4f tmp = new Vector4f(this.x(), this.y(), this.z(), 1f);

        matrix.transform(tmp);
        if (rescaleToUnitCube && Math.abs(tmp.w - 1f) > 1e-5)
        {
            tmp.scale(1f / tmp.w);
        }

        if(this.hasNormal())
        {
            Vector4f tmpNormal = new Vector4f(this.normalX(), this.normalY(), this.normalZ(), 1f);
            matrix.transform(tmpNormal);
            float normScale= (float) (1/Math.sqrt(tmpNormal.x*tmpNormal.x + tmpNormal.y*tmpNormal.y + tmpNormal.z*tmpNormal.z));
            return new Vertex(tmp.x, tmp.y, tmp.z, u(), v(), color(), tmpNormal.x * normScale, tmpNormal.y * normScale, tmpNormal.z * normScale);
        }
        else
        {
            return new Vertex(tmp.x, tmp.y, tmp.z, u(), v(), color());
        }

    }
    
    public @Nonnull IPolygonVertex add(@Nonnull Vertex vec)
    {
        return this.addVector(vec.x(), vec.y(), vec.z());
    }
    
    
    @Deprecated
    @Override
    public Vec3f toVec3f()
    {
        return this;
    }

    /**
     * Adds the specified x,y,z vector components to this vertex and returns the resulting vector. Does not change this
     * vertex. UV values remain same as original. 
     */
    @Override
    public @Nonnull Vertex addVector(float x, float y, float z)
    {
        return new Vertex(this.x() + x, this.y() + y, this.z() + z, u(), v(), color());
    }

    @Override
    public float x()
    {
        return x;
    }

    @Override
    public float y()
    {
        return y;
    }

    @Override
    public float z()
    {
        return z;
    }

    @Override
    public float u()
    {
        return u;
    }

    @Override
    public float v()
    {
        return v;
    }

    @Override
    public int color()
    {
        return color;
    }

    @Override
    public float normalX()
    {
        return normalX;
    }

    @Override
    public float normalY()
    {
        return normalY;
    }

    @Override
    public float normalZ()
    {
        return normalZ;
    }
}