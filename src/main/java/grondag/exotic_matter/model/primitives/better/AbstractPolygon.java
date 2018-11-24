package grondag.exotic_matter.model.primitives.better;

import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList.Builder;

import grondag.acuity.api.IPipelinedVertexConsumer;
import grondag.acuity.api.IRenderPipeline;
import grondag.exotic_matter.ClientProxy;
import grondag.exotic_matter.model.primitives.vertex.IVec3f;
import grondag.exotic_matter.varia.BitPacker;
import grondag.exotic_matter.varia.structures.ITuple;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;

public abstract class AbstractPolygon  implements IPolygon
{
    protected static final BitPacker<AbstractPolygon> BITPACKER = new BitPacker<AbstractPolygon>(p -> p.stateBits, (p, b) -> p.stateBits = b);

    protected static final BitPacker<AbstractPolygon>.EnumElement<EnumFacing> NOMINAL_FACE_BITS = BITPACKER.createEnumElement(EnumFacing.class);
    protected static final BitPacker<AbstractPolygon>.IntElement PIPELINE_INDEX = BITPACKER.createIntElement(1024);
    
    @SuppressWarnings("unchecked")
    protected static final BitPacker<AbstractPolygon>.EnumElement<Rotation>[] ROTATION_BITS = new BitPacker.EnumElement[3];
    
    @SuppressWarnings("unchecked")
    protected static final BitPacker<AbstractPolygon>.EnumElement<BlockRenderLayer>[] RENDERPASS_BITS = new BitPacker.EnumElement[3];
   
    @SuppressWarnings("unchecked")
    protected static final BitPacker<AbstractPolygon>.BooleanElement[] LOCKUV_BIT = new BitPacker.BooleanElement[3];

    @SuppressWarnings("unchecked")
    protected static final BitPacker<AbstractPolygon>.BooleanElement[] EMISSIVE_BIT = new BitPacker.BooleanElement[3];
    
    @SuppressWarnings("unchecked")
    protected static final BitPacker<AbstractPolygon>.BooleanElement[] CONTRACTUV_BITS = new BitPacker.BooleanElement[3];
    
    @SuppressWarnings("unchecked")
    protected static final BitPacker<AbstractPolygon>.IntElement SALT_BITS[] = new BitPacker.IntElement[3];

    protected static final long DEFAULT_BITS;
    static
    {
        ROTATION_BITS[0] = BITPACKER.createEnumElement(Rotation.class);
        ROTATION_BITS[1] = BITPACKER.createEnumElement(Rotation.class);
        ROTATION_BITS[2] = BITPACKER.createEnumElement(Rotation.class);
        
        RENDERPASS_BITS[0] = BITPACKER.createEnumElement(BlockRenderLayer.class);
        RENDERPASS_BITS[1] = BITPACKER.createEnumElement(BlockRenderLayer.class);
        RENDERPASS_BITS[2] = BITPACKER.createEnumElement(BlockRenderLayer.class);
        
        LOCKUV_BIT[0] = BITPACKER.createBooleanElement();
        LOCKUV_BIT[1] = BITPACKER.createBooleanElement();
        LOCKUV_BIT[2] = BITPACKER.createBooleanElement();
        
        EMISSIVE_BIT[0] = BITPACKER.createBooleanElement();
        EMISSIVE_BIT[1] = BITPACKER.createBooleanElement();
        EMISSIVE_BIT[2] = BITPACKER.createBooleanElement();
        
        CONTRACTUV_BITS[0] = BITPACKER.createBooleanElement();
        CONTRACTUV_BITS[1] = BITPACKER.createBooleanElement();
        CONTRACTUV_BITS[2] = BITPACKER.createBooleanElement();
        
        SALT_BITS[0] = BITPACKER.createIntElement(256);
        SALT_BITS[1] = BITPACKER.createIntElement(256);
        SALT_BITS[2] = BITPACKER.createIntElement(256);
        
        assert BITPACKER.bitLength() <= 64;
        
        long defaultBits = 0;
        defaultBits |= ROTATION_BITS[0].getBits(Rotation.ROTATE_NONE);
        defaultBits |= ROTATION_BITS[1].getBits(Rotation.ROTATE_NONE);
        defaultBits |= ROTATION_BITS[2].getBits(Rotation.ROTATE_NONE);
        
        defaultBits |= RENDERPASS_BITS[0].getBits(BlockRenderLayer.SOLID);
        defaultBits |= RENDERPASS_BITS[1].getBits(BlockRenderLayer.SOLID);
        defaultBits |= RENDERPASS_BITS[2].getBits(BlockRenderLayer.SOLID);
        
        defaultBits |= LOCKUV_BIT[0].getBits(false);
        defaultBits |= LOCKUV_BIT[1].getBits(false);
        defaultBits |= LOCKUV_BIT[2].getBits(false);
        
        defaultBits |= EMISSIVE_BIT[0].getBits(false);
        defaultBits |= EMISSIVE_BIT[1].getBits(false);
        defaultBits |= EMISSIVE_BIT[2].getBits(false);
        
        defaultBits |= CONTRACTUV_BITS[0].getBits(false);
        defaultBits |= CONTRACTUV_BITS[1].getBits(false);
        defaultBits |= CONTRACTUV_BITS[2].getBits(false);
        
        defaultBits |= SALT_BITS[0].getBits(0);
        defaultBits |= SALT_BITS[1].getBits(0);
        defaultBits |= SALT_BITS[2].getBits(0);
        
        DEFAULT_BITS = defaultBits;
    }

    protected long stateBits = DEFAULT_BITS;

    protected abstract ITuple<IMutableVertex> getVertices();

    protected abstract ITuple<IVec3f> getNormals();

    protected abstract ITuple<IPolygonLayerProperties> getLayerProps();
    
    @Override
    public final int vertexCount()
    {
        return getVertices().size();
    }
    
    @Override
    public final EnumFacing getNominalFace()
    {
        return NOMINAL_FACE_BITS.getValue(this);
    }

    @Override
    public final IRenderPipeline getPipeline()
    {
        return ClientProxy.acuityPipeline(PIPELINE_INDEX.getValue(this));
    }

    @Override
    public final boolean shouldContractUVs(int layerIndex)
    {
        assert layerIndex < this.layerCount();
        return CONTRACTUV_BITS[layerIndex].getValue(this);
    }

    @Override
    public final Rotation getRotation(int layerIndex)
    {
        assert layerIndex < this.layerCount();
        return ROTATION_BITS[layerIndex].getValue(this);
    }

    @Override
    public final int getTextureSalt(int layerIndex)
    {
        assert layerIndex < this.layerCount();
        return SALT_BITS[layerIndex].getValue(this);
    }

    @Override
    public final boolean isLockUV(int layerIndex)
    {
        assert layerIndex < this.layerCount();
        return LOCKUV_BIT[layerIndex].getValue(this);
    }

    @Override
    public final BlockRenderLayer getRenderLayer(int layerIndex)
    {
        assert layerIndex < this.layerCount();
        return RENDERPASS_BITS[layerIndex].getValue(this);
    }
    
    @Override
    public final boolean isEmissive(int layerIndex)
    {
        assert layerIndex < this.layerCount();
        return EMISSIVE_BIT[layerIndex].getValue(this);
    }
    
    @Override
    public final IVec3f getVertexNormal(int vertexIndex)
    {
        return getNormals() == null ? getFaceNormal() : getNormals().get(vertexIndex);
    }
    
    @Override
    public final float getVertexX(int vertexIndex)
    {
        return getVertices().get(vertexIndex).x();
    }

    @Override
    public final float getVertexY(int vertexIndex)
    {
        return getVertices().get(vertexIndex).y();
    }

    @Override
    public final float getVertexZ(int vertexIndex)
    {
        return getVertices().get(vertexIndex).z();
    }

    @Override
    public final int getVertexColor(int layerIndex, int vertexIndex)
    {
        return getVertices().get(vertexIndex).getColor(layerIndex);
    }

    @Override
    public final int getVertexGlow(int layerIndex, int vertexIndex)
    {
        return getVertices().get(vertexIndex).getGlow(layerIndex);
    }

    @Override
    public final float getVertexU(int layerIndex, int vertexIndex)
    {
        return getVertices().get(vertexIndex).getU(vertexIndex);
    }

    @Override
    public final float getVertexV(int layerIndex, int vertexIndex)
    {
        return getVertices().get(vertexIndex).getV(vertexIndex);
    }

    @Override
    public final IVec3f getPos(int vertexIndex)
    {
        return getVertices().get(vertexIndex).pos();
    }

    
    @Override
    public final float getMaxU(int layerIndex)
    {
        return getLayerProps().get(layerIndex).getMaxU();
    }

    @Override
    public final float getMaxV(int layerIndex)
    {
        return getLayerProps().get(layerIndex).getMaxV();
    }

    @Override
    public final float getMinU(int layerIndex)
    {
        return getLayerProps().get(layerIndex).getMinU();
    }

    @Override
    public final float getMinV(int layerIndex)
    {
        return getLayerProps().get(layerIndex).getMinV();
    }

    @Override
    public final int layerCount()
    {
        return getLayerProps().size();
    }

    @Override
    public final String getTextureName(int layerIndex)
    {
        return getLayerProps().get(layerIndex).getTextureName();
    }

    /**
     * This is Acuity-only.  Acuity assumes quad has only a single render layer.
     */
    @Override
    public final BlockRenderLayer getRenderLayer()
    {
        return getRenderLayer(0);
    }

    @Override
    public final boolean hasRenderLayer(BlockRenderLayer layer)
    {
        if(getRenderLayer(0) == layer)
            return true;
        
        final int count = this.layerCount();
        return (count > 1 && getRenderLayer(1) == layer)
               || (count == 3 && getRenderLayer(2) == layer);
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
    
    @Override
    public final void addPaintableQuadsToList(List<IMutablePolygon> list)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public final void addPaintedQuadsToList(List<IPolygon> list)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public final void producePaintableQuads(Consumer<IMutablePolygon> consumer)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public final void producePaintedQuads(Consumer<IPolygon> consumer)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public final void claimVertexCopiesToArray(IMutableVertex[] vertex)
    {
        // TODO Auto-generated method stub
        
    }
}
