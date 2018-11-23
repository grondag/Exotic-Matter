package grondag.exotic_matter.model.primitives.better;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList.Builder;

import grondag.acuity.api.IPipelinedQuad;
import grondag.acuity.api.IRenderPipeline;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.BlockRenderLayer;

public interface IPaintedPoly extends IPoly, IPipelinedQuad
{
    int getMaxU(int layerIndex);

    int getMaxV(int layerIndex);

    int getMinU(int layerIndex);

    int getMinV(int layerIndex);

    int layerCount();
    
    String getTextureName(int layerIndex);

    boolean shouldContractUVs(int layerIndex);

    Rotation getRotation(int layerIndex);
    
    int getColor(int layerIndex);

    /** 
     * Will return quad color if vertex color not set.
     */
    int getVertexColor(int layerIndex, int vertexIndex);
    
    /** 
     * Will return quad color if vertex color not set.
     */
    int getVertexGlow(int layerIndex, int vertexIndex);
    
    float getVertexU(int layerIndex, int vertexIndex);
    
    float getVertexV(int layerIndex, int vertexIndex);
    
    int getTextureSalt(int layerIndex);

    boolean isLockUV(int layerIndex);

    BlockRenderLayer getRenderLayer(int layerIndex);
    
    /**
     * Adds all quads that belong in the given layer.
     * If layer is null, outputs all quads.
     */
    public default void addBakedQuadsToBuilder(@Nullable BlockRenderLayer layer, Builder<BakedQuad> builder, boolean isItem)
    {
        final int limit = this.layerCount();
        if(limit == 1)
        {
            if(layer == null || this.getRenderLayer(0) == layer)
                addBakedQuadsToBuilder(0, builder, isItem);
        }
        else
        {
            for(int i = 0; i < limit; i++)
            {
                if(layer == null || this.getRenderLayer(i) == layer)
                    addBakedQuadsToBuilder(i, builder, isItem);
            }
        }
    }
    
    public void addBakedQuadsToBuilder(int layerIndex, Builder<BakedQuad> builder, boolean isItem);
//    {
//        produceQuads(q -> builder.add(QuadBakery.createBakedQuad(layerIndex, (IPaintedPoly) q, isItem)));
//    }

    IPaintedPoly recoloredCopy();
    //final Random r = ThreadLocalRandom.current();
    //(r.nextInt(0x1000000) & 0xFFFFFF) | 0xFF000000

    short glow(int layerIndex, int vertexIndex);

    float u(int layerIndex, int vertexIndex);
    
    float v(int layerIndex, int vertexIndex);

    Vec3f normal(int vertexIndex);

    int color(int layerIndex, int vertexIndex);
    
    @Override
    IRenderPipeline getPipeline();
    
    /**
     * Same vertex count. Includes vertex data.
     */
    @Override
    default IPaintablePoly claimCopy()
    {
        return claimCopy(this.vertexCount());
    }
    
    /**
     * Includes vertex data.
     */
    @Override
    IPaintablePoly claimCopy(int vertexCount);

    
    void release();
}
