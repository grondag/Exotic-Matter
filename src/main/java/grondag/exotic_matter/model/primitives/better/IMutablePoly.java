package grondag.exotic_matter.model.primitives.better;

import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.primitives.vertex.IVec3f;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import net.minecraft.util.EnumFacing;

public interface IMutablePoly extends IPoly
{
    IMutablePoly setNominalFace(EnumFacing face);
    
    IMutablePoly setSurfaceInstance(Surface surface);

    IMutablePoly scaleFromBlockCenter(float scaleFactor);

    IMutablePoly invertFaceNormal();

    /**
     * Sets all attributes that are available in the source vertex.
     * DOES NOT retain a reference to the input vertex.
     */
    IMutablePoly copyVertexFrom(int vertexIndex, IGeometricVertex source);

    IMutablePoly clearFaceNormal();
    
    IMutablePoly setVertexNormal(int vertexIndex, Vec3f normal);
    
    default IMutablePoly setVertexNormal(int vertexIndex, IVec3f normal)
    {
        return setVertexNormal(vertexIndex, normal.x(), normal.y(), normal.z());
    }
    
    IMutablePoly setVertexNormal(int vertexIndex, float x, float y, float z);
    
}
