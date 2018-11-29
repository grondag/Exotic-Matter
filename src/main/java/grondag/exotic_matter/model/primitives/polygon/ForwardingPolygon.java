package grondag.exotic_matter.model.primitives.polygon;

import com.google.common.collect.ImmutableList.Builder;

import grondag.acuity.api.IRenderPipeline;
import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;

public class ForwardingPolygon implements IPolygon
{
    public IPolygon wrapped;
    
    @Override
    public int vertexCount()
    {
        return wrapped.vertexCount();
    }

    @Override
    public Vec3f getPos(int index)
    {
        return wrapped.getPos(index);
    }

    @Override
    public BlockRenderLayer getRenderLayer()
    {
        return wrapped.getRenderLayer();
    }

    @Override
    public Vec3f getFaceNormal()
    {
        return wrapped.getFaceNormal();
    }

    @Override
    public EnumFacing getNominalFace()
    {
        return wrapped.getNominalFace();
    }

    @Override
    public Surface getSurface()
    {
        return wrapped.getSurface();
    }

    @Override
    public Vec3f getVertexNormal(int vertexIndex)
    {
        return wrapped.getVertexNormal(vertexIndex);
    }

    @Override
    public boolean hasVertexNormal(int vertexIndex)
    {
        return wrapped.hasVertexNormal(vertexIndex);
    }

    @Override
    public float getVertexNormalX(int vertexIndex)
    {
        return wrapped.getVertexNormalX(vertexIndex);
    }

    @Override
    public float getVertexNormalY(int vertexIndex)
    {
        return wrapped.getVertexNormalY(vertexIndex);
    }

    @Override
    public float getVertexNormalZ(int vertexIndex)
    {
        return wrapped.getVertexNormalZ(vertexIndex);
    }

    @Override
    public float getMaxU(int layerIndex)
    {
        return wrapped.getMaxU(layerIndex);
    }

    @Override
    public float getMaxV(int layerIndex)
    {
        return wrapped.getMaxV(layerIndex);
    }

    @Override
    public float getMinU(int layerIndex)
    {
        return wrapped.getMinU(layerIndex);
    }

    @Override
    public float getMinV(int layerIndex)
    {
        return wrapped.getMinV(layerIndex);
    }

    @Override
    public int layerCount()
    {
        return wrapped.layerCount();
    }

    @Override
    public String getTextureName(int layerIndex)
    {
        return wrapped.getTextureName(layerIndex);
    }

    @Override
    public boolean shouldContractUVs(int layerIndex)
    {
        return wrapped.shouldContractUVs(layerIndex);
    }

    @Override
    public Rotation getRotation(int layerIndex)
    {
        return wrapped.getRotation(layerIndex);
    }

    @Override
    public float getVertexX(int vertexIndex)
    {
        return wrapped.getVertexX(vertexIndex);
    }

    @Override
    public float getVertexY(int vertexIndex)
    {
        return wrapped.getVertexY(vertexIndex);
    }

    @Override
    public float getVertexZ(int vertexIndex)
    {
        return wrapped.getVertexZ(vertexIndex);
    }

    @Override
    public int getVertexColor(int layerIndex, int vertexIndex)
    {
        return wrapped.getVertexColor(layerIndex, vertexIndex);
    }

    @Override
    public int getVertexGlow(int layerIndex, int vertexIndex)
    {
        return wrapped.getVertexGlow(layerIndex, vertexIndex);
    }

    @Override
    public float getVertexU(int layerIndex, int vertexIndex)
    {
        return wrapped.getVertexU(layerIndex, vertexIndex);
    }

    @Override
    public float getVertexV(int layerIndex, int vertexIndex)
    {
        return wrapped.getVertexV(layerIndex, vertexIndex);
    }

    @Override
    public int getTextureSalt()
    {
        return wrapped.getTextureSalt();
    }

    @Override
    public boolean isLockUV(int layerIndex)
    {
        return wrapped.isLockUV(layerIndex);
    }

    @Override
    public boolean hasRenderLayer(BlockRenderLayer layer)
    {
        return wrapped.hasRenderLayer(layer);
    }

    @Override
    public BlockRenderLayer getRenderLayer(int layerIndex)
    {
        return wrapped.getRenderLayer(layerIndex);
    }

    @Override
    public void addBakedQuadsToBuilder(int layerIndex, Builder<BakedQuad> builder, boolean isItem)
    {
        wrapped.addBakedQuadsToBuilder(layerIndex, builder, isItem);
    }

    @Override
    public boolean isEmissive(int textureLayerIndex)
    {
        return wrapped.isEmissive(textureLayerIndex);
    }

    @Override
    public IRenderPipeline getPipeline()
    {
        return wrapped.getPipeline();
    }
}
