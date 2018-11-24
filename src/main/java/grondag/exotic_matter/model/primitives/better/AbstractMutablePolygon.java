package grondag.exotic_matter.model.primitives.better;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;

import grondag.acuity.api.IRenderPipeline;
import grondag.exotic_matter.model.primitives.FaceVertex;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import grondag.exotic_matter.world.Rotation;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;

public abstract class AbstractMutablePolygon extends AbstractPolygon implements IMutablePolygon
{
    @Override
    public final IMutablePolygon setNominalFace(EnumFacing face)
    {
        NOMINAL_FACE_BITS.setValue(face, this);
        return this;
    }

    @Override
    public final IMutablePolygon setMaxU(int layerIndex, float maxU)
    {
        getLayerProps().get(layerIndex).setMaxU(maxU);
        return this;
    }

    @Override
    public final IMutablePolygon setMaxV(int layerIndex, float maxV)
    {
        getLayerProps().get(layerIndex).setMaxV(maxV);
        return this;
    }

    @Override
    public final IMutablePolygon setMinU(int layerIndex, float minU)
    {
        getLayerProps().get(layerIndex).setMinU(minU);
        return this;
    }

    @Override
    public final IMutablePolygon setMinV(int layerIndex, float minV)
    {
        getLayerProps().get(layerIndex).setMinV(minV);
        return this;
    }

    @Override
    public final IMutablePolygon setTextureSalt(int layerIndex, int salt)
    {
        SALT_BITS[layerIndex].setValue(salt, this);
        return this;
    }

    @Override
    public final IMutablePolygon setLockUV(int layerIndex, boolean lockUV)
    {
        LOCKUV_BIT[layerIndex].setValue(lockUV, this);
        return this;
    }

    @Override
    public final IMutablePolygon setTextureName(int layerIndex, String textureName)
    {
        getLayerProps().get(layerIndex).setTextureName(textureName);
        return this;
    }

    @Override
    public final IMutablePolygon setRotation(int layerIndex, Rotation rotation)
    {
        ROTATION_BITS[layerIndex].setValue(rotation, this);
        return this;
    }

    @Override
    public final IMutablePolygon setShouldContractUVs(int layerIndex, boolean contractUVs)
    {
        CONTRACTUV_BITS[layerIndex].setValue(contractUVs, this);
        return this;
    }

    @Override
    public final IMutablePolygon setRenderLayer(int layerIndex, BlockRenderLayer layer)
    {
        RENDERPASS_BITS[layerIndex].setValue(layer, this);
        return this;
    }

    @Override
    public final IMutablePolygon setEmissive(int layerIndex, boolean isEmissive)
    {
        EMISSIVE_BIT[layerIndex].setValue(isEmissive, this);
        return this;
    }

    @Override
    public final IMutablePolygon setPipeline(@Nullable IRenderPipeline pipeline)
    {
        final int index = pipeline == null ? 0 : pipeline.getIndex();
        PIPELINE_INDEX.setValue(index, this);
        return this;
    }
    
    @Override
    public final IMutablePolygon setVertexColorGlow(int layerIndex, int vertexIndex, int color, int glow)
    {
        this.getVertices().get(vertexIndex).setColorGlow(layerIndex, color, glow);
        return this;
    }

    @Override
    public final IMutablePolygon setVertexColor(int layerIndex, int vertexIndex, int color)
    {
        this.getVertices().get(vertexIndex).setColor(layerIndex, color);
        return this;
    }

    @Override
    public final IMutablePolygon setVertexUV(int layerIndex, int vertexIndex, float u, float v)
    {
        this.getVertices().get(vertexIndex).setUV(layerIndex, u, v);
        return this;
    }

    @Override
    public final IMutablePolygon setVertexGlow(int layerIndex, int vertexIndex, int glow)
    {
        this.getVertices().get(vertexIndex).setGlow(layerIndex, glow);
        return this;
    }

    @Override
    public final IMutablePolygon setVertexNormal(int vertexIndex, Vec3f normal)
    {
        this.getVertices().get(vertexIndex).setNormal(normal);
        return this;
    }

    @Override
    public final IMutablePolygon setVertexNormal(int vertexIndex, float x, float y, float z)
    {
        this.getVertices().get(vertexIndex).setNormal(x, y, z);
        return this;
    }
    
    @Override
    public IMutablePolygon copyVertexFrom(int targetIndex, IPolygon source, int sourceIndex)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IMutablePolygon copyInterpolatedVertexFrom(int targetIndex, IPolygon source0, int vertexIndex0, IPolygon source1, int vertexIndex1, float weight)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IMutablePolygon copyVertexFrom(int vertexIndex, IMutableVertex source)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IMutablePolygon setupFaceQuad(EnumFacing side, FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, FaceVertex tv3, EnumFacing topFace)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IMutablePolygon setupFaceQuad(FaceVertex vertexIn0, FaceVertex vertexIn1, FaceVertex vertexIn2, FaceVertex vertexIn3, EnumFacing topFace)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IMutablePolygon setupFaceQuad(float x0, float y0, float x1, float y1, float depth, EnumFacing topFace)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IMutablePolygon setupFaceQuad(EnumFacing face, float x0, float y0, float x1, float y1, float depth, EnumFacing topFace)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IMutablePolygon setupFaceQuad(EnumFacing side, FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, EnumFacing topFace)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IMutablePolygon setupFaceQuad(FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, EnumFacing topFace)
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public IMutablePolygon scaleFromBlockCenter(float scaleFactor)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IPolygon toPainted()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean toPaintableQuads(Consumer<IMutablePolygon> consumer)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean toPaintableTris(Consumer<IMutablePolygon> consumer)
    {
        // TODO Auto-generated method stub
        return false;
    }
    
    @Override
    public IPolygon assignLockedUVCoordinates(int layerIndex)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IMutablePolygon transform(Matrix4f matrix)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IMutablePolygon setVertex(int vertexIndex, float x, float y, float z, float u, float v, int color)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IMutablePolygon setVertex(int vertexIndex, float x, float y, float z, float u, float v, int color, float normX, float normY, float normZ)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
