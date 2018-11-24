package grondag.exotic_matter.model.primitives.better;

import javax.annotation.Nullable;

import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.primitives.vertex.IVec3f;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import grondag.exotic_matter.varia.structures.ITuple;

public class SimpleMutablePolygon extends AbstractMutablePolygon
{
    protected @Nullable Vec3f faceNormal;

    protected Surface surface = Surface.NO_SURFACE;

    private final ITuple<IMutableVertex> vertices;
    
    final @Nullable private ITuple<IVec3f> normals;
    
    private final ITuple<IPolygonLayerProperties> layerProps;
    
    SimpleMutablePolygon(ITuple<IMutableVertex> vertices, @Nullable ITuple<IVec3f> normals, ITuple<IPolygonLayerProperties> layerProps)
    {
        this.vertices = vertices;
        this.normals = normals;
        this.layerProps = layerProps;
    }
    
    @Override
    protected final ITuple<IMutableVertex> getVertices()
    {
        return vertices;
    }

    @Override
    protected final ITuple<IVec3f> getNormals()
    {
        return normals;
    }

    @Override
    protected final ITuple<IPolygonLayerProperties> getLayerProps()
    {
        return layerProps;
    }

    @Override
    public final Vec3f getFaceNormal()
    {
        Vec3f result = this.faceNormal;
        if(result == null)
        {
            result = computeFaceNormal();
            this.faceNormal = result;
        }
        return result;
    }

    @Override
    public IMutablePolygon clearFaceNormal()
    {
        this.faceNormal = null;
        return this;
    }
    
    @Override
    public final Surface getSurface()
    {
        return surface;
    }

    @Override
    public final IMutablePolygon invertFaceNormal()
    {
        final Vec3f norm = this.faceNormal;
        if(norm != null)
        this.faceNormal = Vec3f.create(-norm.x(), -norm.y(), -norm.z());
        return this;
    }

    @Override
    public final IMutablePolygon setSurface(Surface surface)
    {
        this.surface = surface;
        return this;
    }

    @Override
    public final void release()
    {
        // NOOP
    }
}
