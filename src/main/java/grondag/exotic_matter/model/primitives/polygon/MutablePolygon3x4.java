package grondag.exotic_matter.model.primitives.polygon;

import javax.annotation.Nullable;

import grondag.acuity.api.IRenderPipeline;
import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.primitives.FaceVertex;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;

public class MutablePolygon3x4 extends Polygon3x4 implements IMutablePolygon
{
    private int layerCount = 1;
    
    public MutablePolygon3x4 prepare(int layerCount)
    {
        assert layerCount >= 1;
        assert layerCount <= 3;
        this.layerCount = layerCount;
        return this;
    }
    
    @Override
    protected void copyPolyAttributesFrom(IPolygon template)
    {
        super.copyPolyAttributesFrom(template);
        this.setLayerCount(template.layerCount());
    }
    
    @Override
    public final int layerCount()
    {
        return layerCount;
    }

    @Override
    public IMutablePolygon setMaxU(int layerIndex, float maxU)
    {
        super.setMaxUImpl(layerIndex, maxU);
        return this;
    }

    @Override
    public IMutablePolygon setMaxV(int layerIndex, float maxV)
    {
        super.setMaxVImpl(layerIndex, maxV);
        return this;
    }

    @Override
    public IMutablePolygon setMinU(int layerIndex, float minU)
    {
        super.setMinUImpl(layerIndex, minU);
        return this;
    }

    @Override
    public IMutablePolygon setMinV(int layerIndex, float minV)
    {
        super.setMinVImpl(layerIndex, minV);
        return this;
    }

    @Override
    public IMutablePolygon setTextureSalt(int layerIndex, int salt)
    {
        super.setTextureSaltImpl(layerIndex, salt);
        return this;
    }

    @Override
    public IMutablePolygon setLockUV(int layerIndex, boolean lockUV)
    {
        super.setLockUVImpl(layerIndex, lockUV);
        return this;
    }

    @Override
    public IMutablePolygon setTextureName(int layerIndex, String textureName)
    {
        super.setTextureNameImpl(layerIndex, textureName);
        return this;
    }

    @Override
    public IMutablePolygon setRotation(int layerIndex, Rotation rotation)
    {
        super.setRotationImpl(layerIndex, rotation);
        return this;
    }

    @Override
    public IMutablePolygon setShouldContractUVs(int layerIndex, boolean contractUVs)
    {
        super.setShouldContractUVsImpl(layerIndex, contractUVs);
        return this;
    }

    @Override
    public IMutablePolygon setRenderLayer(int layerIndex, BlockRenderLayer layer)
    {
        super.setRenderLayerImpl(layerIndex, layer);
        return this;
    }

    @Override
    public IMutablePolygon setEmissive(int layerIndex, boolean emissive)
    {
        super.setEmissiveImpl(layerIndex, emissive);
        return this;
    }

    @Override
    public IMutablePolygon setVertexColorGlow(int layerIndex, int vertexIndex, int color, int glow)
    {
        super.setVertexColorGlowImpl(layerIndex, vertexIndex, color, glow);
        return this;
    }

    @Override
    public IMutablePolygon setVertexColor(int layerIndex, int vertexIndex, int color)
    {
        super.setVertexColorImpl(layerIndex, vertexIndex, color);
        return this;
    }

    @Override
    public IMutablePolygon setVertexUV(int layerIndex, int vertexIndex, float u, float v)
    {
        super.setVertexUVImpl(layerIndex, vertexIndex, u, v);
        return this;
    }

    @Override
    public IMutablePolygon setVertexGlow(int layerIndex, int vertexIndex, int glow)
    {
        super.setVertexGlowImpl(layerIndex, vertexIndex, glow);
        return this;
    }

    @Override
    public IMutablePolygon setVertexNormal(int vertexIndex, @Nullable Vec3f normal)
    {
        super.setVertexNormalImpl(vertexIndex, normal);
        return this;
    }

    @Override
    public IMutablePolygon setVertexNormal(int vertexIndex, float x, float y, float z)
    {
        super.setVertexNormalImpl(vertexIndex, x, y, z);
        return this;
    }

    @Override
    public IMutablePolygon setPipeline(@Nullable IRenderPipeline pipeline)
    {
        super.setPipelineImpl(pipeline);
        return this;
    }

    @Override
    public IMutablePolygon clearFaceNormal()
    {
        super.clearFaceNormalImpl();
        return this;
    }

    @Override
    public IMutablePolygon setupFaceQuad(EnumFacing side, FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, FaceVertex tv3, @Nullable EnumFacing topFace)
    {
        super.setupFaceQuadImpl(side, tv0, tv1, tv2, tv3, topFace);
        return this;
    }

    @Override
    public IMutablePolygon setupFaceQuad(FaceVertex vertexIn0, FaceVertex vertexIn1, FaceVertex vertexIn2, FaceVertex vertexIn3, @Nullable EnumFacing topFace)
    {
        super.setupFaceQuadImpl(vertexIn0, vertexIn1, vertexIn2, vertexIn3, topFace);
        return this;
    }

    @Override
    public IMutablePolygon setupFaceQuad(float x0, float y0, float x1, float y1, float depth, @Nullable EnumFacing topFace)
    {
        super.setupFaceQuadImpl(x0, y0, x1, y1, depth, topFace);
        return this;
    }

    @Override
    public IMutablePolygon setupFaceQuad(EnumFacing face, float x0, float y0, float x1, float y1, float depth, @Nullable EnumFacing topFace)
    {
        super.setupFaceQuadImpl(face, x0, y0, x1, y1, depth, topFace);
        return this;
    }

    @Override
    public IMutablePolygon setupFaceQuad(EnumFacing side, FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, @Nullable EnumFacing topFace)
    {
        super.setupFaceQuadImpl(side, tv0, tv1, tv2, topFace);
        return this;
    }

    @Override
    public IMutablePolygon setupFaceQuad(FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, @Nullable EnumFacing topFace)
    {
        super.setupFaceQuadImpl(tv0, tv1, tv2, topFace);
        return this;
    }

    @Override
    public IMutablePolygon setNominalFace(EnumFacing face)
    {
        super.setNominalFaceImpl(face);
        return this;
    }

    @Override
    public IMutablePolygon invertFaceNormal()
    {
        super.invertFaceNormalImpl();
        return this;
    }

    @Override
    public IMutablePolygon setSurface(Surface surface)
    {
        super.setSurfaceImpl(surface);
        return this;
    }

    @Override
    public void release()
    {
        // NOOP
    }

    @Override
    public IMutablePolygon setVertexLayer(int layerIndex, int vertexIndex, float u, float v, int color, int glow)
    {
        super.setVertexLayerImpl(layerIndex, vertexIndex, u, v, color, glow);
        return this;
    }

    @Override
    public final IMutablePolygon setVertex(int vertexIndex, float x, float y, float z, float u, float v, int color, int glow)
    {
        super.setVertexImpl(vertexIndex, x, y, z, u, v, color, glow);
        return this;
    }

    @Override
    public final IMutablePolygon setVertexPos(int vertexIndex, float x, float y, float z)
    {
        super.setVertexPosImpl(vertexIndex, x, y, z);
        return this;
    }

    @Override
    public final IMutablePolygon setVertexPos(int vertexIndex, Vec3f pos)
    {
        super.setVertexPosImpl(vertexIndex, pos);
        return this;
    }

    @Override
    public IMutablePolygon setLayerCount(int layerCount)
    {
        this.layerCount = layerCount;
        return this;
    }
    
    @Override
    public IMutablePolygon copyVertexFrom(int targetIndex, IPolygon source, int sourceIndex)
    {
        copyVertexFromImpl(targetIndex, source, sourceIndex);
        return this;
    }
}
