package grondag.exotic_matter.model;

import grondag.exotic_matter.render.RenderPass;

/**
 * Superblocks rendering characteristics for non-TESR renders.
 * Stored in block instance and determines if renders as normal block 
 * or as TESR. <br><br>
 * 
 * For normal renders, determines which layers are included and of those,
 * which are shaded or flat rendering. Flat is necessary for full brightness renders.
 * 
 * For TESR, look to RenderPassSet for rendering characteristic.
 * 
 * @author grondag
 *
 */
public enum BlockRenderMode
{
    SOLID_SHADED(RenderPass.SOLID_SHADED),
    TRANSLUCENT_SHADED(RenderPass.TRANSLUCENT_SHADED),
    BOTH_SHADED(RenderPass.SOLID_SHADED, RenderPass.TRANSLUCENT_SHADED),
    TESR();
    
    
    /**
     * Sizes quad container - values range from 0 (empty) to 2 (both SOLID and TRANLUCENT)
     */
    public final RenderLayout renderLayout;


    private BlockRenderMode(RenderPass...passes)
    {
        this.renderLayout = new RenderLayout(passes);
    }
 }
