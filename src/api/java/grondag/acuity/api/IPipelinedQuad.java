package grondag.acuity.api;


import javax.annotation.Nullable;

import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.client.model.pipeline.BlockInfo;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public interface IPipelinedQuad
{
    @SideOnly(Side.CLIENT)
    public @Nullable IRenderPipeline getPipeline();

    /**
     * Quad must call {@link IPipelinedVertexConsumer#acceptVertex(IPipelinedVertex)} with
     * its vertex information.<p>
     * 
     * For tint, quad (or the model it comes from) is responsible for retrieving and applying block tint 
     * to the vertex colors.  This is done because lighter has no way to know which colors
     * should be modified when there is more than one color/texture layer. And many models don't use it.<p>
     * 
     * You can retrieve the block color from tint with {@link IPipelinedVertexConsumer#getBlockInfo()} 
     * and then {@link BlockInfo#getColorMultiplier(int tint)};
     */
    @SideOnly(Side.CLIENT)
    public void produceVertices(IPipelinedVertexConsumer vertexLighter);

    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer();
}
