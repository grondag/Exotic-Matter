package grondag.exotic_matter.model.primitives.better;

import javax.annotation.Nullable;

import grondag.exotic_matter.model.primitives.better.PolygonAccessor.Vertex;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;

@SuppressWarnings("null")
public abstract class AbstractPolygonNx3<T extends AbstractPolygonNx3<T>> extends AbstractSmallPolygon<T>
{
    Vec3f pos0;
    Vec3f pos1;
    Vec3f pos2;
    
    static class Normals
    {
        @Nullable Vec3f norm0;
        @Nullable Vec3f norm1;
        @Nullable Vec3f norm2;
    }
    
    @Nullable Normals normals;
    
    @SuppressWarnings("unchecked")
    private static final Vertex<AbstractPolygonNx3<? extends AbstractPolygonNx3<?>>>[] VERTEX_ACCESS = new Vertex[4];
    
    static
    {
        VERTEX_ACCESS[0] = new Vertex<AbstractPolygonNx3<? extends AbstractPolygonNx3<?>>>()
        {
            {
                this.posGetter = p -> p.pos0;
                this.xGetter = p -> p.pos0.x();
                this.yGetter = p -> p.pos0.y();
                this.zGetter = p -> p.pos0.z();
                
                this.posSetter = (p, v) -> p.pos0 = v;
                this.xyzSetter = (p, x, y, z) -> p.pos0 = Vec3f.create(x, y, z);
                
                this.normalGetter = p -> p.normals == null ? null : p.normals.norm0;
                this.normXGetter = p -> p.normals == null ? 0 : p.normals.norm0.x();
                this.normYGetter = p -> p.normals == null ? 0 : p.normals.norm0.y();
                this.normZGetter = p -> p.normals == null ? 0 : p.normals.norm0.z();
                
                this.normalSetter = (p, v) -> p.normals().norm0 = v;
                this.normXYZSetter = (p, x, y, z) -> p.normals().norm0 = Vec3f.create(x, y, z);
            }
        };
        
        VERTEX_ACCESS[1] = new Vertex<AbstractPolygonNx3<? extends AbstractPolygonNx3<?>>>()
        {
            {
                this.posGetter = p -> p.pos1;
                this.xGetter = p -> p.pos1.x();
                this.yGetter = p -> p.pos1.y();
                this.zGetter = p -> p.pos1.z();
                
                this.posSetter = (p, v) -> p.pos1 = v;
                this.xyzSetter = (p, x, y, z) -> p.pos1 = Vec3f.create(x, y, z);
                
                this.normalGetter = p -> p.normals == null ? null : p.normals.norm1;
                this.normXGetter = p -> p.normals == null ? 0 : p.normals.norm1.x();
                this.normYGetter = p -> p.normals == null ? 0 : p.normals.norm1.y();
                this.normZGetter = p -> p.normals == null ? 0 : p.normals.norm1.z();
                
                this.normalSetter = (p, v) -> p.normals().norm1 = v;
                this.normXYZSetter = (p, x, y, z) -> p.normals().norm1 = Vec3f.create(x, y, z);
            }
        };
        
        VERTEX_ACCESS[2] = new Vertex<AbstractPolygonNx3<? extends AbstractPolygonNx3<?>>>()
        {
            {
                this.posGetter = p -> p.pos2;
                this.xGetter = p -> p.pos2.x();
                this.yGetter = p -> p.pos2.y();
                this.zGetter = p -> p.pos2.z();
                
                this.posSetter = (p, v) -> p.pos2 = v;
                this.xyzSetter = (p, x, y, z) -> p.pos2 = Vec3f.create(x, y, z);
                
                this.normalGetter = p -> p.normals == null ? null : p.normals.norm2;
                this.normXGetter = p -> p.normals == null ? 0 : p.normals.norm2.x();
                this.normYGetter = p -> p.normals == null ? 0 : p.normals.norm2.y();
                this.normZGetter = p -> p.normals == null ? 0 : p.normals.norm2.z();
                
                this.normalSetter = (p, v) -> p.normals().norm2 = v;
                this.normXYZSetter = (p, x, y, z) -> p.normals().norm2 = Vec3f.create(x, y, z);
            }
        };
        
        VERTEX_ACCESS[3] = VERTEX_ACCESS[0];
    }
    
    @Override
    public final int vertexCount()
    {
        return 3;
    }
    
    private Normals normals()
    {
        Normals result = normals;
        if(result == null)
        {
            result = new Normals();
            normals = result;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Vertex<T>[] vertexArray()
    {
        return (Vertex<T>[]) VERTEX_ACCESS;
    }
}
