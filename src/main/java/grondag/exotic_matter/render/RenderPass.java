package grondag.exotic_matter.render;

import net.minecraft.util.BlockRenderLayer;

/**
 * Does not always imply separate passes.  
 * If using TESR flat and shaded can render in same pass.
 */
public enum RenderPass
{
    SOLID_SHADED(BlockRenderLayer.SOLID),
    TRANSLUCENT_SHADED(BlockRenderLayer.TRANSLUCENT);
    
    public final BlockRenderLayer blockRenderLayer;
    
    private RenderPass(BlockRenderLayer brl)
    {
        this.blockRenderLayer = brl;
    }
}
