package grondag.exotic_matter.model.primitives.better;

import javax.annotation.Nullable;

import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.primitives.FaceVertex;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;

public interface IPaintablePoly  extends IMutablePoly, IPaintedPoly
{
    /**
     * Same vertex count, layer count. Includes vertex data.
     */
    @Override
    default IPaintablePoly claimCopy()
    {
        return claimCopy(this.vertexCount(), this.layerCount());
    }
    
    /**
     * Same layer count. Includes vertex data.
     */
    @Override
    default  IPaintablePoly claimCopy(int vertexCount)
    {
        return claimCopy(vertexCount, this.layerCount());
    }

    @Override
    IPaintablePoly claimCopy(int vertexCount, int layerCount);
    
    void release();

    /**
     * Sets all attributes that are available in the source vertex.
     * DOES NOT retain a reference to the input vertex.
     */
    @Override
    IPaintablePoly copyVertex(int vertexIndex, IGeometricVertex source);
    
    IPaintablePoly setVertex(int vertexIndex, float x, float y, float z, float u, float v, int color);

    IPaintablePoly setVertex(int vertexIndex, float x, float y, float z, float u, float v, int color, float normX, float normY, float normZ);

    default IPaintablePoly setVertex(int vertexIndex, Vec3d pos, double u, double v, int color, @Nullable Vec3d normal)
    {
        if(normal == null)
            return setVertex(vertexIndex, (float)pos.x, (float)pos.y, (float)pos.z, (float)u, (float)v, color);
        else
            return setVertex(vertexIndex, (float)pos.x, (float)pos.y, (float)pos.z, (float)u, (float)v, color, 
                    (float)normal.x, (float)normal.y, (float)normal.z);
    }

    IPaintablePoly setMaxU(int layerIndex, float maxU);
    
    IPaintablePoly setMaxV(int layerIndex, float maxV);
    
    IPaintablePoly setMinU(int layerIndex, float minU);

    IPaintablePoly setMinV(int layerIndex, float minV);
    
    IPaintablePoly setColor(int layerIndex, int color);

    IPaintablePoly setTextureSalt(int i);

    IPaintablePoly clearFaceNormal();

    /**
     * Same as {@link #setupFaceQuad(FaceVertex, FaceVertex, FaceVertex, FaceVertex, EnumFacing)}
     * except also sets nominal face to the given face in the start parameter. 
     * Returns self for convenience.
     */
    IPaintablePoly setupFaceQuad(EnumFacing side, FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, FaceVertex tv3, @Nullable EnumFacing topFace);

    /** 
     * Sets up a quad with human-friendly semantics. <br><br>
     * 
     * topFace establishes a reference for "up" in these semantics. If null, will use default.
     * Depth represents how far recessed into the surface of the face the quad should be. <br><br>
     * 
     * Vertices should be given counter-clockwise.
     * Ordering of vertices is maintained for future references.
     * (First vertex passed in will be vertex 0, for example.) <br><br>
     * 
     * UV coordinates will be based on where rotated vertices project onto the nominal 
     * face for this quad (effectively lockedUV) unless face vertexes have UV coordinates.
     */
    IPaintablePoly setupFaceQuad(FaceVertex vertexIn0, FaceVertex vertexIn1, FaceVertex vertexIn2, FaceVertex vertexIn3, @Nullable EnumFacing topFace);
    
    /** 
     * Sets up a quad with standard semantics.  
     * x0,y0 are at lower left and x1, y1 are top right.
     * topFace establishes a reference for "up" in these semantics.
     * Depth represents how far recessed into the surface of the face the quad should be.<br><br>
     * 
     * Returns self for convenience.<br><br>
     * 
     * @see #setupFaceQuad(FaceVertex, FaceVertex, FaceVertex, FaceVertex, EnumFacing)
     */
    IPaintablePoly setupFaceQuad(float x0, float y0, float x1, float y1, float depth, @Nullable EnumFacing topFace);

    /**
     * Same as {@link #setupFaceQuad(double, double, double, double, double, EnumFacing)}
     * but also sets nominal face with given face in start parameter.  
     * Returns self as convenience.
     */
    IPaintablePoly setupFaceQuad(EnumFacing face, float x0, float y0, float x1, float y1, float depth, @Nullable EnumFacing topFace);

    //TODO use float version
    @Deprecated
    public default IPaintablePoly setupFaceQuad(EnumFacing face, double x0, double y0, double x1, double y1, double depth, @Nullable EnumFacing topFace)
    {
        return this.setupFaceQuad(face, (float)x0, (float)y0, (float)x1, (float)y1, (float)depth, topFace);
    }

    /**
     * Triangular version of {@link #setupFaceQuad(EnumFacing, FaceVertex, FaceVertex, FaceVertex, EnumFacing)}
     */
    IPaintablePoly setupFaceQuad(EnumFacing side, FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, @Nullable EnumFacing topFace);

    /**
     * Triangular version of {@link #setupFaceQuad(FaceVertex, FaceVertex, FaceVertex, FaceVertex, EnumFacing)}
     */
    IPaintablePoly setupFaceQuad(FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, @Nullable EnumFacing topFace);

    IPaintablePoly setLockUV(int layerIndex, boolean lockUV);
    
    @Override
    public IPaintablePoly setNominalFace(EnumFacing face);
    
    @Override
    IPaintablePoly setSurfaceInstance(Surface surface);

    IPaintablePoly setTextureName(int layerIndex, String string);

    IPaintablePoly setShouldContractUVs(int layerIndex, boolean contractUVs);

    @Override
    IPaintablePoly scaleFromBlockCenter(float scaleFactor);

}
