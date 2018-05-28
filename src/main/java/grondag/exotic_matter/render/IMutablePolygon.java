package grondag.exotic_matter.render;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4d;
import javax.vecmath.Matrix4f;
import grondag.exotic_matter.render.Surface.SurfaceInstance;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;

public interface IMutablePolygon extends IPolygon
{
    @Override
    public default IMutablePolygon mutableReference()
    {
        return this;
    }

    /**
     * Changes all vertices and quad color to new color and returns itself
     */
    
    // used in mesh construction
    
    public IMutablePolygon replaceColor(int color);
    
    public void setMinU(float minU);

    public void setMaxU(float maxU);

    public void setMinV(float minV);

    public void setMaxV(float maxV);

    /** Using this instead of method on vertex 
     * ensures normals are set correctly for tris.
     */
    void setVertexNormal(int index, float x, float y, float z);

    void setVertexNormal(int index, Vec3f normal);
    
    public void setVertexColor(int index, int vColor);
    
    ///// used in painters
    
    public void setTextureName(@Nullable String textureName);

    public void setRenderPass(RenderPass renderPass);

    public void setFullBrightness(boolean isFullBrightnessIntended);

    /**
     * Assigns UV coordinates to each vertex by projecting vertex
     * onto plane of the quad's face. If the quad is not rotated,
     * then semantics of vertex coordinates matches those of setupFaceQuad.
     * For example, on NSEW faces, "up" (+y) corresponds to the top of the texture.
     * 
     * Assigned values are in the range 0-16, as is conventional for MC.
     */
    public void assignLockedUVCoordinates();
    
    /**
     * Assigns UV coordinates to each vertex by scaling vertex onto
     * the texture simply assuming that vertex uv 0,0 maps to texture
     * uv 0,0 and vertex uv 1,1 maps to texture uv 16, 16. <p>
     * 
     * IOW, multiplies all the vertex uv values by 16 to be
     * consistent with MC texturing.
     * 
     */
    public void assignScaledUVCoordinates();

    /** 
     * Unique scale transformation of all vertex coordinates 
     * using block center (0.5, 0.5, 0.5) as origin.
     */
    public void scaleFromBlockCenter(float scale);

    /**
     * Multiplies this quads color and all vertex color by given value
     */
    public void multiplyColor(int color);

    public void setRotation(Rotation rotation);

    /**
     * Multiplies uvMin/Max by the given factors.
     */
    public void scaleQuadUV(float uScale, float vScale);

    /**
     * Same as {@link #setupFaceQuad(FaceVertex, FaceVertex, FaceVertex, FaceVertex, EnumFacing)}
     * except also sets nominal face to the given face in the start parameter. 
     * Returns self for convenience.
     */
    IMutablePolygon setupFaceQuad(EnumFacing side, FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, FaceVertex tv3, @Nullable EnumFacing topFace);

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
    IMutablePolygon setupFaceQuad(FaceVertex vertexIn0, FaceVertex vertexIn1, FaceVertex vertexIn2, FaceVertex vertexIn3, @Nullable EnumFacing topFace);
    
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
    IMutablePolygon setupFaceQuad(float x0, float y0, float x1, float y1, float depth, EnumFacing topFace);

    /**
     * Same as {@link #setupFaceQuad(double, double, double, double, double, EnumFacing)}
     * but also sets nominal face with given face in start parameter.  
     * Returns self as convenience.
     */
    IMutablePolygon setupFaceQuad(EnumFacing face, float x0, float y0, float x1, float y1, float depth, @Nullable EnumFacing topFace);

    //TODO use float version
    @Deprecated
    public default IMutablePolygon setupFaceQuad(EnumFacing face, double x0, double y0, double x1, double y1, double depth, @Nullable EnumFacing topFace)
    {
        return this.setupFaceQuad(face, (float)x0, (float)y0, (float)x1, (float)y1, (float)depth, topFace);
    }

    /**
     * Triangular version of {@link #setupFaceQuad(EnumFacing, FaceVertex, FaceVertex, FaceVertex, EnumFacing)}
     */
    IMutablePolygon setupFaceQuad(EnumFacing side, FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, @Nullable EnumFacing topFace);

    /**
     * Triangular version of {@link #setupFaceQuad(FaceVertex, FaceVertex, FaceVertex, FaceVertex, EnumFacing)}
     */
    IMutablePolygon setupFaceQuad(FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, @Nullable EnumFacing topFace);

    /** sets surface value and returns self for convenience */
    IMutablePolygon setSurfaceInstance(SurfaceInstance surfaceInstance);
    
    /**
     * Enforces immutability of vertex geometry once a vertex is added
     * by rejecting any attempt to set a vertex that already exists.
     * Rejection is via an assertion, so no overhead in normal use.
     */
    public void addVertex(int index, Vertex vertexIn);

    public default void addVertex(int index, Vec3d point, double u, double v, int color, Vec3d normal)
    {
        this.addVertex(index, (float)point.x, (float)point.y, (float)point.z, (float)u, (float)v, color, (float)normal.x, (float)normal.y, (float)normal.z);
    }
    
    public default void addVertex(int index, Vec3d point, double u, double v, int color)
    {
        this.addVertex(index, (float)point.x, (float)point.y, (float)point.z, (float)u, (float)v, color);
    }
    
    public void addVertex(int index, float x, float y, float z, float u, float v, int color);

    public default void addVertex(int index, float x, float y, float z, float u, float v, int color, Vec3d normal)
    {
        this.addVertex(index, index, y, z, u, v, color, (float)normal.x, (float)normal.y, (float)normal.z);
    }
    
    public void addVertex(int index, float x, float y, float z, float u, float v, int color, Vec3f normal);
    
    public void addVertex(int index, float x, float y, float z, float u, float v, int color, float normalX, float normalY, float normalZ);
    
    void setColor(int color);

    void setLockUV(boolean isLockUV);

    void setShouldContractUVs(boolean shouldContractUVs);

    /**
     * Sets the face to be used for setupFace semantics
     */
    EnumFacing setNominalFace(@Nullable EnumFacing face);

    /** 
     * applies the given transformation to this polygon
     * TODO - switch to float version
     */
    @Deprecated
    public default void transform(Matrix4d m)
    {
        this.transform(new Matrix4f(m));
    }
    
    
    /** applies the given transformation to this polygon*/
    void transform(Matrix4f matrix);

    /**
     * Adds the given offsets to min/max uv
     */
    public void offsetQuadUV(float uOffset, float vOffset);

}
