package grondag.exotic_matter.model.primitives.better;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;

import com.google.common.collect.ImmutableList.Builder;

import grondag.acuity.api.IPipelinedVertexConsumer;
import grondag.acuity.api.IRenderPipeline;
import grondag.exotic_matter.ClientProxy;
import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.primitives.FaceVertex;
import grondag.exotic_matter.model.primitives.better.PolygonAccessor.Layer;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import grondag.exotic_matter.varia.BitPacker;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;

public abstract class AbstractPolygon<T extends AbstractPolygon<T>>  implements IPolygon
{
    protected static final BitPacker<AbstractPolygon<?>> BITPACKER = new BitPacker<AbstractPolygon<?>>(p -> p.stateBits, (p, b) -> p.stateBits = b);

    protected static final BitPacker<AbstractPolygon<?>>.EnumElement<EnumFacing> NOMINAL_FACE_BITS = BITPACKER.createEnumElement(EnumFacing.class);
    protected static final BitPacker<AbstractPolygon<?>>.IntElement PIPELINE_INDEX = BITPACKER.createIntElement(1024);
    
    @SuppressWarnings("unchecked")
    protected static final BitPacker<AbstractPolygon<?>>.EnumElement<Rotation>[] ROTATION_BITS = new BitPacker.EnumElement[3];
    
    @SuppressWarnings("unchecked")
    protected static final BitPacker<AbstractPolygon<?>>.EnumElement<BlockRenderLayer>[] RENDERPASS_BITS = new BitPacker.EnumElement[3];
   
    @SuppressWarnings("unchecked")
    protected static final BitPacker<AbstractPolygon<?>>.BooleanElement[] LOCKUV_BIT = new BitPacker.BooleanElement[3];

    @SuppressWarnings("unchecked")
    protected static final BitPacker<AbstractPolygon<?>>.BooleanElement[] EMISSIVE_BIT = new BitPacker.BooleanElement[3];
    
    @SuppressWarnings("unchecked")
    protected static final BitPacker<AbstractPolygon<?>>.BooleanElement[] CONTRACTUV_BITS = new BitPacker.BooleanElement[3];
    
    @SuppressWarnings("unchecked")
    protected static final BitPacker<AbstractPolygon<?>>.IntElement SALT_BITS[] = new BitPacker.IntElement[3];

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

    protected @Nullable Vec3f faceNormal;
    protected Surface surface = Surface.NO_SURFACE;

    protected abstract Layer<T>[] layerAccess();
    
    @Override
    public final EnumFacing getNominalFace()
    {
        return NOMINAL_FACE_BITS.getValue(this);
    }
    
    /** supports mutable interface */
    protected final void setNominalFaceImpl(EnumFacing face)
    {
        NOMINAL_FACE_BITS.setValue(face, this);
    }

    @Override
    public final IRenderPipeline getPipeline()
    {
        return ClientProxy.acuityPipeline(PIPELINE_INDEX.getValue(this));
    }

    /** supports mutable interface */
    protected final void setPipelineImpl(@Nullable IRenderPipeline pipeline)
    {
        final int index = pipeline == null ? 0 : pipeline.getIndex();
        PIPELINE_INDEX.setValue(index, this);
    }
    
    @Override
    public final boolean shouldContractUVs(int layerIndex)
    {
        assert layerIndex < this.layerCount();
        return CONTRACTUV_BITS[layerIndex].getValue(this);
    }

    /** supports mutable interface */
    protected final void setShouldContractUVsImpl(int layerIndex, boolean contractUVs)
    {
        assert layerIndex < this.layerCount();
        CONTRACTUV_BITS[layerIndex].setValue(contractUVs, this);
    }
    
    @Override
    public final Rotation getRotation(int layerIndex)
    {
        assert layerIndex < this.layerCount();
        return ROTATION_BITS[layerIndex].getValue(this);
    }

    /** supports mutable interface */
    protected final void setRotationImpl(int layerIndex, Rotation rotation)
    {
        assert layerIndex < this.layerCount();
        ROTATION_BITS[layerIndex].setValue(rotation, this);
    }
    
    @Override
    public final int getTextureSalt(int layerIndex)
    {
        assert layerIndex < this.layerCount();
        return SALT_BITS[layerIndex].getValue(this);
    }

    /** supports mutable interface */
    protected final void setTextureSaltImpl(int layerIndex, int salt)
    {
        assert layerIndex < this.layerCount();
        SALT_BITS[layerIndex].setValue(salt, this);
    }
    
    @Override
    public final boolean isLockUV(int layerIndex)
    {
        assert layerIndex < this.layerCount();
        return LOCKUV_BIT[layerIndex].getValue(this);
    }

    /** supports mutable interface */
    protected final void setLockUVImpl(int layerIndex, boolean lockUV)
    {
        assert layerIndex < this.layerCount();
        LOCKUV_BIT[layerIndex].setValue(lockUV, this);
    }
    
    @Override
    public final BlockRenderLayer getRenderLayer(int layerIndex)
    {
        assert layerIndex < this.layerCount();
        return RENDERPASS_BITS[layerIndex].getValue(this);
    }
    
    /** supports mutable interface */
    protected final void setRenderLayerImpl(int layerIndex, BlockRenderLayer layer)
    {
        assert layerIndex < this.layerCount();
        RENDERPASS_BITS[layerIndex].setValue(layer, this);
    }
    
    @Override
    public final boolean isEmissive(int layerIndex)
    {
        assert layerIndex < this.layerCount();
        return EMISSIVE_BIT[layerIndex].getValue(this);
    }

    /** supports mutable interface */
    protected final void setEmissiveImpl(int layerIndex, boolean emissive)
    {
        assert layerIndex < this.layerCount();
        EMISSIVE_BIT[layerIndex].setValue(emissive, this);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public final float getMaxU(int layerIndex)
    {
        return layerAccess()[layerIndex].uMaxGetter.get((T) this);
    }

    /** supports mutable interface */
    @SuppressWarnings("unchecked")
    protected final void setMaxUImpl(int layerIndex, float maxU)
    {
        layerAccess()[layerIndex].uMaxSetter.set((T) this, maxU);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final float getMaxV(int layerIndex)
    {
        return layerAccess()[layerIndex].vMaxGetter.get((T) this);
    }
    
    /** supports mutable interface */
    @SuppressWarnings("unchecked")
    protected final void setMaxVImpl(int layerIndex, float maxV)
    {
        layerAccess()[layerIndex].vMaxSetter.set((T) this, maxV);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final float getMinU(int layerIndex)
    {
        return layerAccess()[layerIndex].uMinGetter.get((T) this);
    }
    
    /** supports mutable interface */
    @SuppressWarnings("unchecked")
    protected final void setMinUImpl(int layerIndex, float minU)
    {
        layerAccess()[layerIndex].uMinSetter.set((T) this, minU);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final float getMinV(int layerIndex)
    {
        return layerAccess()[layerIndex].vMinGetter.get((T) this);
    }

    /** supports mutable interface */
    @SuppressWarnings("unchecked")
    protected final void setMinVImpl(int layerIndex, float minV)
    {
        layerAccess()[layerIndex].vMinSetter.set((T) this, minV);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public final String getTextureName(int layerIndex)
    {
        return layerAccess()[layerIndex].textureGetter.get((T) this);
    }
 
    /** supports mutable interface */
    @SuppressWarnings("unchecked")
    protected final void setTextureNameImpl(int layerIndex, String textureName)
    {
        layerAccess()[layerIndex].textureSetter.set((T) this, textureName);
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
    public final Vec3f getFaceNormal()
    {
        Vec3f result = this.faceNormal;
        if(result == null)
        {
            result = computeFaceNormal();
            this.faceNormal = result;
        }
        return result;
    }
    
    /** supports mutable interface */
    protected final void invertFaceNormalImpl()
    {
        final Vec3f norm = this.faceNormal;
        if(norm != null)
            this.faceNormal = Vec3f.create(-norm.x(), -norm.y(), -norm.z());
    }
    

    /** supports mutable interface */
    protected final void clearFaceNormalImpl()
    {
        this.faceNormal = null;
    }


    @Override
    public final Surface getSurface()
    {
        return surface;
    }
    
    /** supports mutable interface */
    protected final void setSurfaceImpl(Surface surface)
    {
        this.surface = surface;
    }

    
    /** supports mutable interface */
    protected abstract void setVertexImpl(int vertexIndex, float x, float y, float z, float u, float v, int color);

    /** supports mutable interface */
    protected abstract void setVertexImpl(int vertexIndex, float x, float y, float z, float u, float v, int color, float normX, float normY, float normZ);

    /** supports mutable interface */
    protected abstract void setVertexNormalImpl(int vertexIndex, Vec3f normal);

    /** supports mutable interface */
    protected abstract void setVertexNormalImpl(int vertexIndex, float x, float y, float z);
    
    /** supports mutable interface */
    protected abstract void setVertexColorGlowImpl(int layerIndex, int vertexIndex, int color, int glow);

    /** supports mutable interface */
    protected abstract void setVertexColorImpl(int layerIndex, int vertexIndex, int color);

    /** supports mutable interface */
    protected abstract void setVertexUVImpl(int layerIndex, int vertexIndex, float u, float v);

    /** supports mutable interface */
    protected abstract void setVertexUImpl(int layerIndex, int vertexIndex, float u);
    
    /** supports mutable interface */
    protected abstract void setVertexVImpl(int layerIndex, int vertexIndex, float v);
    
    /** supports mutable interface */
    protected abstract void setVertexGlowImpl(int layerIndex, int vertexIndex, int glow);

    
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
    
    /** supports mutable interface */
    protected final void scaleFromBlockCenterImpl(float scaleFactor)
    {
        // TODO Auto-generated method stub
    }

    /** supports mutable interface */
    protected IPolygon toPaintedImpl()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    /** supports mutable interface */
    protected boolean toPaintableQuadsImpl(Consumer<IMutablePolygon> consumer)
    {
        // TODO Auto-generated method stub
        return false;
    }
    
    /** supports mutable interface */
    protected boolean toPaintableTrisImpl(Consumer<IMutablePolygon> consumer)
    {
        // TODO Auto-generated method stub
        return false;
    }

    /** supports mutable interface */
    protected final void assignLockedUVCoordinatesImpl(int layerIndex)
    {
        // TODO Auto-generated method stub
    }

    /** supports mutable interface */
    protected final void transformImpl(Matrix4f matrix)
    {
        // TODO Auto-generated method stub
    }

    /** supports mutable interface */
    protected final void setupFaceQuadImpl(EnumFacing side, FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, FaceVertex tv3, @Nullable EnumFacing topFace)
    {
        // TODO Auto-generated method stub
    }

    /** supports mutable interface */
    protected final void setupFaceQuadImpl(FaceVertex vertexIn0, FaceVertex vertexIn1, FaceVertex vertexIn2, FaceVertex vertexIn3, @Nullable EnumFacing topFace)
    {
        // TODO Auto-generated method stub
    }

    /** supports mutable interface */
    protected final void setupFaceQuadImpl(float x0, float y0, float x1, float y1, float depth, @Nullable EnumFacing topFace)
    {
        // TODO Auto-generated method stub
    }

    /** supports mutable interface */
    protected final void setupFaceQuadImpl(EnumFacing face, float x0, float y0, float x1, float y1, float depth, @Nullable EnumFacing topFace)
    {
        // TODO Auto-generated method stub
    }

    /** supports mutable interface */
    protected final void setupFaceQuadImpl(EnumFacing side, FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, @Nullable EnumFacing topFace)
    {
        // TODO Auto-generated method stub
    }

    /** supports mutable interface */
    protected final void setupFaceQuadImpl(FaceVertex tv0, FaceVertex tv1, FaceVertex tv2, @Nullable EnumFacing topFace)
    {
        // TODO Auto-generated method stub
    }

    /** supports mutable interface */
    protected final void copyVertexFromImpl(int targetIndex, IPolygon source, int sourceIndex)
    {
        // TODO Auto-generated method stub
    }

    /** supports mutable interface */
    protected final void copyInterpolatedVertexFromImpl(int targetIndex, IPolygon source0, int vertexIndex0, IPolygon source1, int vertexIndex1, float weight)
    {
        // TODO Auto-generated method stub
    }

    /** supports mutable interface */
    protected final void copyVertexFromImpl(int vertexIndex, IMutableVertex source)
    {
        // TODO Auto-generated method stub
    }
}