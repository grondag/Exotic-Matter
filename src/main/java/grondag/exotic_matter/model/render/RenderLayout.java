package grondag.exotic_matter.model.render;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import grondag.exotic_matter.varia.BinaryEnumSet;
import net.minecraft.util.BlockRenderLayer;

/**
 * Allows static access during enum initialization.
 *
 */
public class RenderLayout
{
    public static final BinaryEnumSet<BlockRenderLayer> BENUMSET_BLOCK_RENDER_LAYER = new BinaryEnumSet<BlockRenderLayer>(BlockRenderLayer.class);
    
    private static final RenderLayout[] lookup = new RenderLayout[BENUMSET_BLOCK_RENDER_LAYER.combinationCount()];
    
    static
    {
        for(int i = 0; i < lookup.length; i ++)
        {
            lookup[i] = new RenderLayout(BENUMSET_BLOCK_RENDER_LAYER.getValuesForSetFlags(i));
        }
    }
    
    public static ImmutableList<RenderLayout> ALL_LAYOUTS = ImmutableList.copyOf(lookup);
    
    public static RenderLayout find(BlockRenderLayer...passes)
    {
        return lookup[BENUMSET_BLOCK_RENDER_LAYER.getFlagsForIncludedValues(passes)];
    }
    
    public static final RenderLayout SOLID_ONLY = lookup[BENUMSET_BLOCK_RENDER_LAYER.getFlagForValue(BlockRenderLayer.SOLID)];
    public static final RenderLayout TRANSLUCENT_ONLY = lookup[BENUMSET_BLOCK_RENDER_LAYER.getFlagForValue(BlockRenderLayer.TRANSLUCENT)];
    public static final RenderLayout SOLID_AND_TRANSLUCENT = lookup[BENUMSET_BLOCK_RENDER_LAYER.getFlagsForIncludedValues(BlockRenderLayer.SOLID, BlockRenderLayer.TRANSLUCENT)];
    public static final RenderLayout NONE = lookup[BENUMSET_BLOCK_RENDER_LAYER.getFlagsForIncludedValues()];
    
    /**
     * Sizes quad container
     */
    public final int blockLayerCount;
    
    public final int blockLayerFlags;
    
    public final List<BlockRenderLayer> blockLayerList;
    
    private final int[] blockLayerContainerIndexes = new int[BlockRenderLayer.values().length];
    
    private final BlockRenderLayer[] containerLayers;
    
    private RenderLayout(BlockRenderLayer... passes)
    {
        this.containerLayers = passes;
        this.blockLayerFlags = BENUMSET_BLOCK_RENDER_LAYER.getFlagsForIncludedValues(passes);
        this.blockLayerList =  ImmutableList.copyOf(passes);
        this.blockLayerCount = this.blockLayerList.size();
                
        Arrays.fill(blockLayerContainerIndexes, -1);
        
        for(int i = 0; i < this.blockLayerCount; i++)
        {
            this.blockLayerContainerIndexes[passes[i].ordinal()] = i;
        }
    }

    public final boolean containsBlockRenderLayer(BlockRenderLayer layer)
    {
        return BENUMSET_BLOCK_RENDER_LAYER.isFlagSetForValue(layer, this.blockLayerFlags);
    }
    
    /**
     * If block layer is present returns container index (0 or 1) where quads for the layer should be kept.
     * Returns -1 if layer not present.
     */
    public final int containerIndexFromBlockRenderLayer(BlockRenderLayer layer)
    {
        return this.blockLayerContainerIndexes[layer.ordinal()];
    }
    
    /**
     * Returns null if given container index is out of range.
     */
    @Nullable
    public final BlockRenderLayer BlockRenderLayerFromContainerIndex(int containerIndex)
    {
        if(containerIndex < 0 || containerIndex >= this.blockLayerCount)
        {
            return null;
        }
        else
        {
            return this.containerLayers[containerIndex];
        }
    }
}
