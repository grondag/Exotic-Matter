package grondag.exotic_matter.model.primitives.better;

import grondag.exotic_matter.model.painting.Surface;
import net.minecraft.util.EnumFacing;

public interface IMutablePoly extends IPoly
{
    public IMutablePoly setNominalFace(EnumFacing face);
    
    IMutablePoly setSurfaceInstance(Surface surface);

    IMutablePoly claimCopy(int vertexCount, int layerCount);

    IMutablePoly claimCopy(int vertexCount);

    IMutablePoly claimCopy();

    IMutablePoly scaleFromBlockCenter(float scaleFactor);

    IMutablePoly invertFaceNormal();

    /**
     * Sets all attributes that are available in the source vertex.
     * DOES NOT retain a reference to the input vertex.
     */
    IMutablePoly copyVertex(int vertexIndex, IGeometricVertex source);
    
    /**
     * Transfers mutable copies of this poly's vertices to the provided array.
     * Array size must be >= {@link #vertexCount()}.
     * Retains no reference to the copies, which should be released when no longer used.
     * For use in CSG operations.
     */
    public void claimVertexCopiesToArray(IMutableGeometricVertex[] vertex);
    
}
