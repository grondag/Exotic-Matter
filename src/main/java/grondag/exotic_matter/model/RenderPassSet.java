package grondag.exotic_matter.model;


import static grondag.exotic_matter.render.RenderPass.*;

import grondag.exotic_matter.render.RenderPass;

public enum RenderPassSet
{
    SOLID_S(SOLID_SHADED),
    TRANS_S(TRANSLUCENT_SHADED),
    SOLID_S_TRANS_S(SOLID_SHADED, TRANSLUCENT_SHADED),
    NONE();
    
    public final BlockRenderMode blockRenderMode;
    
    public final RenderLayout renderLayout;
    
    private RenderPassSet(RenderPass... passes)
    {
        this.renderLayout = new RenderLayout(passes);
        
        // if no block render mode matches then must be rendered as TESR
        for(BlockRenderMode mode : BlockRenderMode.values())
        {
            if(mode.renderLayout.renderPassFlags == this.renderLayout.renderPassFlags)
            {
                this.blockRenderMode = mode;
                return;
            }
        }
        this.blockRenderMode = null;
        assert false : "Invalid RenderPassSet";
    }
  
    public boolean canRenderAsNormalBlock()
    {
        return this.blockRenderMode != BlockRenderMode.TESR;
    }
    
    private static class Finder
    {
        private static final RenderPassSet[] LOOKUP = new RenderPassSet[RenderPassSet.values().length];
        
        static
        {
            for(RenderPassSet set : RenderPassSet.values())
            {
                LOOKUP[set.renderLayout.renderPassFlags] = set;
            }
        }
    }

    /**
     * Use BENUMSET_RENDER_PASS to compute flags.
     */
    public static RenderPassSet findByFlags(int flags)
    {
        return Finder.LOOKUP[flags];
    }
}
