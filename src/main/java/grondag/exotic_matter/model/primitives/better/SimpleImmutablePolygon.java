package grondag.exotic_matter.model.primitives.better;

import javax.annotation.Nullable;

import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.primitives.vertex.IVec3f;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import grondag.exotic_matter.varia.structures.ITuple;

public class SimpleImmutablePolygon extends AbstractPolygon
{
    protected @Nullable Vec3f faceNormal;

    protected Surface surface = Surface.NO_SURFACE;

    private final ITuple<IMutableVertex> vertices;
    
    final @Nullable private ITuple<IVec3f> normals;
    
    private final ITuple<IPolygonLayerProperties> layerProps;
    
    SimpleImmutablePolygon(ITuple<IMutableVertex> vertices, @Nullable ITuple<IVec3f> normals, ITuple<IPolygonLayerProperties> layerProps)
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
    public final Surface getSurface()
    {
        return surface;
    }
}
