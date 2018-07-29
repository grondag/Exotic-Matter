package grondag.exotic_matter.model.varia;

import java.util.List;

import javax.annotation.Nullable;

import grondag.exotic_matter.model.render.QuadContainer;
import net.minecraft.util.BlockRenderLayer;


/**
 * Super lightweight version of EnumMap for Block layers to QuadContainer. Only stores values for keys that are used.
 */
public class SparseLayerMapBuilder
{
    private final int[] layerIndices = new int[BlockRenderLayer.values().length];
    private final int size;
    public final BlockRenderLayer[] layers;
    
    public SparseLayerMapBuilder(List<BlockRenderLayer> layers)
    {
        this.size = layers.size();
        this.layers = layers.toArray(new BlockRenderLayer[layers.size()]);
        int counter = 0;
        
        for(BlockRenderLayer l: layers)
        {
            layerIndices[l.ordinal()] = counter++;
        }
    }
    
    public SparseLayerMap makeNewMap()
    {
        if(this.size == 1)
            return new SparseLayerSingletonMap();
        
        else if(this.size == 2 && this.layers[0] == BlockRenderLayer.SOLID && this.layers[1] == BlockRenderLayer.TRANSLUCENT)
            return new SparseLayerSolidTransMap();
        
        else 
            return new SparseLayerArrayMap();
    }
    
    public abstract class SparseLayerMap
    {
        public abstract @Nullable QuadContainer get(BlockRenderLayer layer);
        
        public abstract void set(BlockRenderLayer layer, QuadContainer value);
        
        public abstract QuadContainer[] getAll();
    }
    
    private class SparseLayerArrayMap extends SparseLayerMap
    {
        private final QuadContainer[] values = new QuadContainer[size];
        
        private SparseLayerArrayMap()
        {
            //NOOP - just making it private
        }
        
        @Override
        public @Nullable QuadContainer get(BlockRenderLayer layer)
        {
            return values[layerIndices[layer.ordinal()]];
        }
        
        @Override
        public void set(BlockRenderLayer layer, QuadContainer value)
        {
            values[layerIndices[layer.ordinal()]] = value;
        }
        
        @Override
        public QuadContainer[] getAll()
        {
            return this.values;
        }
    }
     
    private class SparseLayerSingletonMap extends SparseLayerMap
    {
        private @Nullable QuadContainer value;
        
        private SparseLayerSingletonMap()
        {
            //NOOP - just making it private
        }
        
        @Override
        public @Nullable QuadContainer get(BlockRenderLayer layer)
        {
            return value;
        }
        
        @Override
        public void set(BlockRenderLayer layer, QuadContainer value)
        {
            this.value = value;
        }
        
        @Override
        public QuadContainer[] getAll()
        {
            return new QuadContainer[] { this.value };
        }
    }
    
    /**
     * Optimized for most common (currently only) non-singleton case/
     */
    private class SparseLayerSolidTransMap extends SparseLayerMap
    {
        private @Nullable QuadContainer solid;
        private @Nullable QuadContainer translucent;
        
        private SparseLayerSolidTransMap()
        {
            // Keep private
        }
        
        @Override
        public @Nullable QuadContainer get(BlockRenderLayer layer)
        {
            switch(layer)
            {
            case SOLID:
                return this.solid;
                
            case TRANSLUCENT:
                return this.translucent;
                
            default:
                return null;
            }
        }
        
        @Override
        public void set(BlockRenderLayer layer, QuadContainer value)
        {
            switch(layer)
            {
            case SOLID:
                this.solid = value;
                
            case TRANSLUCENT:
                this.translucent = value;
                
            default:
                //NOOP
            }
        }
        
        @Override
        public QuadContainer[] getAll()
        {
            return new QuadContainer[] { this.solid, this.translucent };
        }
    }
}
