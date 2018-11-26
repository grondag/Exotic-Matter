package grondag.exotic_matter.model.varia;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import grondag.acuity.api.IPipelinedBakedModel;
import grondag.acuity.api.IPipelinedQuadConsumer;
import grondag.exotic_matter.ExoticMatter;
import grondag.exotic_matter.block.ISuperBlock;
import grondag.exotic_matter.block.SuperBlockStackHelper;
import grondag.exotic_matter.block.SuperModelItemOverrideList;
import grondag.exotic_matter.cache.ObjectSimpleCacheLoader;
import grondag.exotic_matter.cache.ObjectSimpleLoadingCache;
import grondag.exotic_matter.model.painting.QuadPaintManager;
import grondag.exotic_matter.model.painting.SurfaceTopology;
import grondag.exotic_matter.model.primitives.QuadHelper;
import grondag.exotic_matter.model.primitives.polygon.IMutablePolygon;
import grondag.exotic_matter.model.primitives.polygon.IPolygon;
import grondag.exotic_matter.model.render.QuadContainer;
import grondag.exotic_matter.model.render.RenderLayout;
import grondag.exotic_matter.model.render.RenderUtil;
import grondag.exotic_matter.model.state.ISuperModelState;
import grondag.exotic_matter.model.varia.SparseLayerMapBuilder.SparseLayerMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class SuperDispatcher
{
    public static final SuperDispatcher INSTANCE = new SuperDispatcher();
    public static final String RESOURCE_BASE_NAME = "super_dispatcher";

    private final SparseLayerMapBuilder[] layerMapBuilders;
    
    public final DispatchDelegate[] delegates;
    
    //custom loading cache is at least 2X faster than guava LoadingCache for our use case
    private final ObjectSimpleLoadingCache<ISuperModelState, SparseLayerMap> modelCache = new ObjectSimpleLoadingCache<ISuperModelState, SparseLayerMap>(new BlockCacheLoader(),  0xFFFF);
    private final ObjectSimpleLoadingCache<ISuperModelState, SimpleItemBlockModel> itemCache = new ObjectSimpleLoadingCache<ISuperModelState, SimpleItemBlockModel>(new ItemCacheLoader(), 0xFFF);
    /** contains quads for use by block damage rendering based on shape only and with appropriate UV mapping*/
    private final ObjectSimpleLoadingCache<ISuperModelState, QuadContainer> damageCache = new ObjectSimpleLoadingCache<ISuperModelState, QuadContainer>(new DamageCacheLoader(), 0x4FF);
    
    private class BlockCacheLoader implements ObjectSimpleCacheLoader<ISuperModelState, SparseLayerMap>
    {
		@Override
		public SparseLayerMap load(ISuperModelState key) {
			
		    RenderLayout renderLayout = key.getRenderLayout();

		    // PERF: make threadlocal
            QuadContainer.Builder[] containers = new QuadContainer.Builder[RenderUtil.RENDER_LAYER_COUNT];
            int containerCount = 0;
            
		    for(BlockRenderLayer layer : RenderUtil.RENDER_LAYERS)
            {
		        if(renderLayout.containsBlockRenderLayer(layer))
		            containers[containerCount++] = new QuadContainer.Builder(layer);
            }
            
		    final int finalCount = containerCount;
		    
		    // PERF: When Acuity is enabled this whole structure is inefficient.  
		    // Also inefficient without because too many checks and adding same polys to mutliple containers.
		    // TODO: No way to handle pipelined polygons that render in both translucent and solid (will there be any?)
		    provideFormattedQuads(key, false, p -> 
		    {
		        for(int i = 0; i < finalCount; i++)
		        {
		            QuadContainer.Builder c = containers[i];
		            if(p.hasRenderLayer(c.layer))
		                c.accept(p);
		        }
		        
		    });
		    
			SparseLayerMap result = layerMapBuilders[renderLayout.ordinal].makeNewMap();
			for(int i = 0; i < finalCount; i++)
            {
                QuadContainer.Builder c = containers[i];
                result.set(c.layer, c.build());
            }
			
			return result;
		}
    }
    
    private class ItemCacheLoader implements ObjectSimpleCacheLoader<ISuperModelState, SimpleItemBlockModel>
    {
		@Override
		public SimpleItemBlockModel load(ISuperModelState key) 
		{
	    	ImmutableList.Builder<BakedQuad> builder = new ImmutableList.Builder<BakedQuad>();
	    	// PERF: create way to generate baked directly from mutable without a transient immutable poly
	    	// will reduce mem allocation overhead
	    	provideFormattedQuads(key, true, p -> 
	    	{
	    	    p.addBakedQuadsToBuilder(null, builder, true);
	    	});
	    	    
			return new SimpleItemBlockModel(builder.build(), true);
		}       
    }
    
    private class DamageCacheLoader implements ObjectSimpleCacheLoader<ISuperModelState, QuadContainer>
    {
        @Override
        public QuadContainer load(ISuperModelState key) 
        {
            QuadContainer.Builder builder = new QuadContainer.Builder(BlockRenderLayer.SOLID);
            key.getShape().meshFactory().produceShapeQuads(key, q ->
            {
                IMutablePolygon mutable = q.claimCopy();
                
                // arbitrary choice - just needs to be a simple non-null texture
                mutable.setTextureName(0, grondag.exotic_matter.init.ModTextures.BLOCK_COBBLE.getSampleTextureName());
             
                // Need to scale UV on non-cubic surfaces to be within a 1 block boundary.
                // This causes breaking textures to be scaled to normal size.
                // If we didn't do this, bigtex block break textures would appear abnormal.
                if(mutable.getSurface().topology == SurfaceTopology.TILED)
                {
                    // This is simple for tiled surface because UV scale is always 1.0
                    mutable.setMinU(0, 0);
                    mutable.setMaxU(0, 1);
                    mutable.setMinV(0, 0);
                    mutable.setMaxV(0, 1);
                }
                
                builder.accept(mutable.toPainted());
                mutable.release();
            });
            return builder.build();
        }       
    }
    
    private SuperDispatcher()
    {
        this.layerMapBuilders = new SparseLayerMapBuilder[RenderLayout.COMBINATION_COUNT];

        for(int i = 0; i < RenderLayout.COMBINATION_COUNT; i++)
        {
            this.layerMapBuilders[i] = new SparseLayerMapBuilder(RenderLayout.ALL_LAYOUTS.get(i).blockLayerList);
        }
        
        this.delegates = new DispatchDelegate[RenderLayout.ALL_LAYOUTS.size()];
        for(RenderLayout layout : RenderLayout.ALL_LAYOUTS)
        {
            DispatchDelegate newDelegate = new DispatchDelegate(layout);
            this.delegates[layout.ordinal] = newDelegate;
        }
    }
    
    public void clear()
    {
            modelCache.clear();
            itemCache.clear();
    }

    public int getOcclusionKey(ISuperModelState modelState, EnumFacing face)
    {
        if(!modelState.getRenderLayout().containsBlockRenderLayer(BlockRenderLayer.SOLID)) return 0;

        SparseLayerMap map = modelCache.get(modelState);
        

        QuadContainer container = map.get(BlockRenderLayer.SOLID);
        if(container == null) 
        {
            ExoticMatter.INSTANCE.warn("Missing model for occlusion key.");
            return 0;
        }
        return container.getOcclusionHash(face);
    }
    
    private void provideFormattedQuads(ISuperModelState modelState, boolean isItem, Consumer<IPolygon> target)
    {
        final Consumer<IMutablePolygon> manager = QuadPaintManager.provideReadyConsumer(modelState, isItem, target);
        modelState.getShape().meshFactory().produceShapeQuads(modelState, manager);
    }
    
    public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack)
    {
        ISuperModelState key = stack.getItem() instanceof CraftingItem
                ? ((CraftingItem)stack.getItem()).modelState
                : SuperBlockStackHelper.getStackModelState(stack);
                
        return key == null ? originalModel : itemCache.get(key);
    }
  
    public DispatchDelegate getDelegate(ISuperBlock block)
    {
        return this.delegates[block.renderLayout().ordinal];
    }
    
    /**
     * Ugly but only used during load. Retrieves delegates for our custom model loader.
     */
    public DispatchDelegate getDelegate(String resourceString)
    {
        int start = resourceString.lastIndexOf(SuperDispatcher.RESOURCE_BASE_NAME) + SuperDispatcher.RESOURCE_BASE_NAME.length();
        int index;
        if(resourceString.contains("item"))
        {
            int end = resourceString.lastIndexOf(".");
            index = Integer.parseInt(resourceString.substring(start, end));
        }
        else
        {
            index = Integer.parseInt(resourceString.substring(start));
        }
        return this.delegates[index];
    }
    
    /**
     * Delegate to use for generic crafting item rendering
     */
    public DispatchDelegate getItemDelegate()
    {
        return this.delegates[RenderLayout.TRANSLUCENT_ONLY.ordinal];
    }
    
    public class DispatchDelegate implements IBakedModel, IModel, IPipelinedBakedModel
    {
        private final String modelResourceString;
        private final RenderLayout blockRenderLayout;
        
        private DispatchDelegate(RenderLayout blockRenderLayout)
        {
            this.modelResourceString = ExoticMatter.INSTANCE.prefixResource(SuperDispatcher.RESOURCE_BASE_NAME  + blockRenderLayout.ordinal);
            this.blockRenderLayout = blockRenderLayout;
        }

        /** only used for block layer version */
        public String getModelResourceString()
        {
            return this.modelResourceString;
        }
        
        /**
         * For the debug renderer - enumerates all painted quads for all layers.
         * Pass in an extended block state.
         */
        public void forAllPaintedQuads(IExtendedBlockState state, Consumer<IPolygon> consumer)
        {
            ISuperModelState modelState = state.getValue(ISuperBlock.MODEL_STATE);
            
            SparseLayerMap map = modelCache.get(modelState);
            for(QuadContainer qc : map.getAll())
            {
                if(qc != null)
                {
                    qc.forEachPaintedQuad(consumer);
                }
            }
        }
        
        @Override
        public boolean mightRenderInLayer(@Nullable BlockRenderLayer forLayer)
        {
            return forLayer == null || this.blockRenderLayout.containsBlockRenderLayer(forLayer);
        }
        
        @Override
        public void produceQuads(@SuppressWarnings("null") IPipelinedQuadConsumer quadConsumer)
        {
            @SuppressWarnings("null")
            ISuperModelState modelState = ((IExtendedBlockState)quadConsumer.blockState()).getValue(ISuperBlock.MODEL_STATE);
            SparseLayerMap map = modelCache.get(modelState);
            QuadContainer qc = map.get(MinecraftForgeClient.getRenderLayer());
            if(qc != null)
            {
                qc.forEachPaintedQuad(null, q -> quadConsumer.accept(q));
                for(EnumFacing face : EnumFacing.VALUES)
                {
                    if(quadConsumer.shouldOutputSide(face))
                            qc.forEachPaintedQuad(face, q -> quadConsumer.accept(q));
                }
            }
        }
        
        @Override
        public TextureAtlasSprite getParticleTexture()
        {
            // should not ever be used
            return Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite();
        }
    
        @Override
        public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand)
        {
            if(state == null) return QuadHelper.EMPTY_QUAD_LIST;
    
            ISuperModelState modelState = ((IExtendedBlockState)state).getValue(ISuperBlock.MODEL_STATE);
            
            BlockRenderLayer layer = MinecraftForgeClient.getRenderLayer();
            
            // If no renderIntent set then probably getting request from block breaking
            if(layer == null)
            {
                QuadContainer qc = damageCache.get(modelState.geometricState());
                return qc.getBakedQuads(side);
            }
            else
            {
                SparseLayerMap map = modelCache.get(modelState);
                QuadContainer container = map.get(layer);
                if(container == null)
                        return QuadHelper.EMPTY_QUAD_LIST;
                return container.getBakedQuads(side);
            }
        }
         
        @Override
        public boolean isAmbientOcclusion()
        {
            return true;
        }
    
        @Override
        public boolean isBuiltInRenderer()
        {
            return false;
        }
        
    	@Override
    	public boolean isGui3d()
    	{
    		return true;
    	}
    
    	@Override
    	public ItemOverrideList getOverrides()
    	{
    		return new SuperModelItemOverrideList(SuperDispatcher.this) ;
    	}
    
    	@Override
        public ItemCameraTransforms getItemCameraTransforms()
        {
            return ItemCameraTransforms.DEFAULT;
        }

        @Override
        public IBakedModel bake(@Nonnull IModelState state, @Nonnull VertexFormat format, @Nonnull Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter)
        {
            return this;
        }
    }
}