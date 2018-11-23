package grondag.exotic_matter.model.primitives.better;

import grondag.exotic_matter.model.painting.Surface;
import net.minecraft.util.EnumFacing;

public interface IMutablePoly extends IPoly
{
    public IMutablePoly setNominalFace(EnumFacing face);
    
    IMutablePoly setSurfaceInstance(Surface surface);

    IMutablePoly scaleFromBlockCenter(float scaleFactor);

    IMutablePoly invertFaceNormal();

    /**
     * Sets all attributes that are available in the source vertex.
     * DOES NOT retain a reference to the input vertex.
     */
    IMutablePoly copyVertexFrom(int vertexIndex, IGeometricVertex source);

    IMutablePoly clearFaceNormal();
    
}
