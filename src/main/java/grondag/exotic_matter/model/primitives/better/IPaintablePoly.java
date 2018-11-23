package grondag.exotic_matter.model.primitives.better;

import java.util.Collection;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import grondag.acuity.api.IRenderPipeline;
import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.primitives.FaceVertex;
import grondag.exotic_matter.model.primitives.QuadHelper;
import grondag.exotic_matter.world.Rotation;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;

public interface IPaintablePoly  extends IMutablePoly, IPaintedPoly
{
    /**
     * Sets all attributes that are available in the source vertex.
     * DOES NOT retain a reference to the input vertex.
     */
    IPaintablePoly copyVertexFrom(int vertexIndex, IPaintedVertex source);
    
    /**
     * Sets all attributes that are available in this poly in the source vertex.
     * If target vertex is null, allocates a new stand-alone vertex.
     * DOES NOT retain a reference to the target vertex.
     */
    IPaintableVertex copyVertexTo(int vertexIndex, @Nullable IPaintedVertex target);
    
    /**
     * Copies all attributes that are available in the source poly.
     * DOES NOT retain a reference to the input poly.
     */
    IPaintablePoly copyVertexFrom(int targetIndex, IPaintedPoly source, int sourceIndex);
    
    /**
     * Interpolates all attributes that are available in the source poly.
     * Weight = 0 gives source 0, weight = 1 gives source 1, with values 
     * in between giving blended results.
     * DOES NOT retain a reference to either input poly.
     */
    IPaintablePoly copyInterpolatedVertexFrom(int targetIndex, IPaintedPoly source0, int vertexIndex0, IPaintedPoly source1, int vertexIndex1, float weight);

    
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

    IPaintablePoly setTextureSalt(int layerIndex, int salt);

    IPaintablePoly setLockUV(int layerIndex, boolean lockUV);

    IPaintablePoly setTextureName(int layerIndex, String textureName);
    
    IPaintablePoly setRotation(int layerIndex, Rotation rotation);

    IPaintablePoly setShouldContractUVs(int layerIndex, boolean contractUVs);
    
    IPaintablePoly setRenderLayer(int layerIndex, BlockRenderLayer layer);
    
    IPaintedPoly setEmissive(int textureLayerIndex, boolean emissive);
    
    /**
     * glow is clamped to allowed values
     */
    IPaintablePoly setVertexColorGlow(int layerIndex, int vertexIndex, int color, int glow);
    
    IPaintablePoly setVertexColor(int layerIndex, int vertexIndex, int color);
    
    IPaintablePoly setVertexUV(int layerIndex, int vertexIndex, float u, float v);
    
    default IPaintablePoly setVertexU(int layerIndex, int vertexIndex, float u)
    {
        return this.setVertexUV(layerIndex, vertexIndex, u, this.getVertexV(layerIndex, vertexIndex));
    }
    
    default IPaintablePoly setVertexV(int layerIndex, int vertexIndex, float v)
    {
        return this.setVertexUV(layerIndex, vertexIndex, this.getVertexU(layerIndex, vertexIndex), v);
    }
    
    /**
     * glow is clamped to allowed values
     */
    IPaintablePoly setVertexGlow(int layerIndex, int vertexIndex, int glow);
    
    IPaintedPoly setPipeline(IRenderPipeline pipeline);
    
    @Override
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

    
    @Override
    public IPaintablePoly setNominalFace(EnumFacing face);
    
    @Override
    IPaintablePoly setSurfaceInstance(Surface surface);

    @Override
    IPaintablePoly scaleFromBlockCenter(float scaleFactor);

    IPaintedPoly toPainted();

    /**
     * If this poly is a tri or a convex quad, simply passes to consumer and returns false.<p>
     * 
     * If it a concave quad or higher-order polygon, generates new paintables that split this poly
     * into convex quads or tris. If a split occurs, returns true. This instance will be unmodified.<p>
     * 
     * Return value of true signals to release this poly if it is no longer needed for processing.
     */
    boolean toPaintableQuads(Consumer<IPaintablePoly> consumer);
    
    /**
     * WARNING: releases all polys in the input collection. <br>
     * DO NOT RETAIN REFERENCES TO ANY INPUTS. <br>
     * Returns a new Does NOT split non-quads to quads.
     */
    static ImmutableList<IPaintedPoly> paintAndRelease(Collection<IPaintablePoly> from)
    {
        ImmutableList.Builder<IPaintedPoly> builder = ImmutableList.builder();

        for(IPaintablePoly p : from)
        {
            builder.add(p.toPainted());
            p.release();
        }
        return builder.build();
    }
    
    /**
     * Assigns locked UV coordinates in all layers that require them.
     */
    default IPaintedPoly assignAllLockedUVCoordinates()
    {
        final int layerCount = this.layerCount();
        if(layerCount == 1)
        {
            if(isLockUV(0))
                assignLockedUVCoordinates(0);
        }
        else 
        {
            for(int i = 0; i < layerCount; i++)
            {
                if(isLockUV(i))
                    assignLockedUVCoordinates(i);
            }
        }
        return this;
    }
    
    /**
     *  if lockUV is on, derive UV coords by projection
     *  of vertex coordinates on the plane of the quad's face
     */
    IPaintedPoly assignLockedUVCoordinates(int layerIndex);

    /**
     * Adds given offsets to u,v values of each vertex.
     */
    default IPaintedPoly offsetVertexUV(int layerIndex, float uShift, float vShift)
    {
        for(int i = 0; i < this.vertexCount(); i++)
        {
            final float u = this.getVertexU(layerIndex, i) + uShift;
            final float v = this.getVertexV(layerIndex, i) + vShift;
            
            assert u > -QuadHelper.EPSILON : "vertex uv offset out of bounds"; 
            assert u < 1 + QuadHelper.EPSILON : "vertex uv offset out of bounds"; 
            assert v > -QuadHelper.EPSILON : "vertex uv offset out of bounds"; 
            assert v < 1 + QuadHelper.EPSILON : "vertex uv offset out of bounds";

            this.setVertexUV(layerIndex, i, u, v);
        }      
        return this;
    }
}
