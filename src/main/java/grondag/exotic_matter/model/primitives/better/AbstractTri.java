package grondag.exotic_matter.model.primitives.better;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList.Builder;

import grondag.acuity.api.IPipelinedVertexConsumer;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.BlockRenderLayer;

class AbstractTri extends AbstractPolygon
{
    AbstractTri(Vector3<IMutableVertex> vertices, @Nullable Vector3<Vec3f> normals)
    {
        super(vertices, normals);
    }

    @Override
    public void addPaintableQuadsToList(List<IMutablePolygon> list)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addPaintedQuadsToList(List<IPolygon> list)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void producePaintableQuads(Consumer<IMutablePolygon> consumer)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void producePaintedQuads(Consumer<IPolygon> consumer)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void claimVertexCopiesToArray(IMutableVertex[] vertex)
    {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public int getMaxU(int layerIndex)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMaxV(int layerIndex)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMinU(int layerIndex)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMinV(int layerIndex)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int layerCount()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getTextureName(int layerIndex)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addBakedQuadsToBuilder(int layerIndex, Builder<BakedQuad> builder, boolean isItem)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public IPolygon recoloredCopy()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IMutablePolygon claimCopy(int vertexCount)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void produceVertices(@SuppressWarnings("null") IPipelinedVertexConsumer vertexLighter)
    {
        // TODO Auto-generated method stub
        
    }

    /**
     * This is Acuity-only.  Acuity assumes quad has only a single render layer.
     */
    @Override
    public BlockRenderLayer getRenderLayer()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasRenderLayer(BlockRenderLayer layer)
    {
        // TODO Auto-generated method stub
        return false;
    }

}
