package grondag.exotic_matter.model.primitives.polygon;

import javax.annotation.Nullable;

import grondag.acuity.api.IRenderPipeline;
import grondag.exotic_matter.ClientProxy;
import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import grondag.exotic_matter.varia.BitPacker;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;

public abstract class AbstractPolygon implements IMutablePolygon
{
    protected static final BitPacker<AbstractPolygon> BITPACKER = new BitPacker<AbstractPolygon>(p -> p.stateBits, (p, b) -> p.stateBits = b);
    
    protected static final BitPacker<AbstractPolygon>.EnumElement<EnumFacing> NOMINAL_FACE_BITS = BITPACKER.createEnumElement(EnumFacing.class);
    protected static final BitPacker<AbstractPolygon>.EnumElement<Rotation> ROTATION_BITS = BITPACKER.createEnumElement(Rotation.class);
    protected static final BitPacker<AbstractPolygon>.EnumElement<BlockRenderLayer> RENDERPASS_BITS = BITPACKER.createEnumElement(BlockRenderLayer.class);
    protected static final BitPacker<AbstractPolygon>.BooleanElement LOCKUV_BIT = BITPACKER.createBooleanElement();
    protected static final BitPacker<AbstractPolygon>.BooleanElement EMISSIVE_BIT = BITPACKER.createBooleanElement();
    protected static final BitPacker<AbstractPolygon>.BooleanElement CONTRACTUV_BITS = BITPACKER.createBooleanElement();
    protected static final BitPacker<AbstractPolygon>.IntElement SALT_BITS = BITPACKER.createIntElement(256);
    protected static final BitPacker<AbstractPolygon>.IntElement PIPELINE_INDEX = BITPACKER.createIntElement(1024);
    protected static final BitPacker<AbstractPolygon>.BooleanElement EMISSIVE_BIT_2 = BITPACKER.createBooleanElement();
    protected static final BitPacker<AbstractPolygon>.BooleanElement EMISSIVE_BIT_3 = BITPACKER.createBooleanElement();
    
    protected static final long DEFAULT_BITS;
    static
    {
        long defaultBits = 0;
        defaultBits |= ROTATION_BITS.getBits(Rotation.ROTATE_NONE);
        defaultBits |= RENDERPASS_BITS.getBits(BlockRenderLayer.SOLID);
        defaultBits |= LOCKUV_BIT.getBits(false);
        defaultBits |= CONTRACTUV_BITS.getBits(true);
        DEFAULT_BITS = defaultBits;
    }
    
    protected long stateBits = DEFAULT_BITS;
    
    protected float minU = 0;
    protected float maxU = 1;
    protected float minV = 0;
    protected float maxV = 1;

    protected @Nullable Vec3f faceNormal;
    protected @Nullable String textureName;
    protected Surface surfaceInstance = IPolygon.NO_SURFACE;
    
    protected void copyProperties(IPolygon fromObject)
    {
        final AbstractPolygon fastOther = (AbstractPolygon)fromObject;
        
        this.stateBits = fastOther.stateBits;
        this.textureName = fastOther.textureName;
        this.faceNormal = fastOther.faceNormal;
        this.minU = fastOther.minU;
        this.maxU = fastOther.maxU;
        this.minV = fastOther.minV;
        this.maxV = fastOther.maxV;
        this.surfaceInstance = fastOther.surfaceInstance;
    }
    
    @Override
    public @Nullable String getTextureName()
    {
        return textureName;
    }

    @Override
    public void setTextureName(@Nullable String textureName)
    {
        this.textureName = textureName;
    }

    @Override
    public int textureSalt()
    {
        return SALT_BITS.getValue(this);
    }

    @Override
    public void setTextureSalt(int textureSalt)
    {
        SALT_BITS.setValue(textureSalt, this);
    }
    
    @Override
    public Rotation getRotation()
    {
        return ROTATION_BITS.getValue(this);
    }

    @Override
    public void setRotation(Rotation rotation)
    {
        ROTATION_BITS.setValue(rotation, this);
    }

    @Override
    public boolean isEmissive()
    {
        return EMISSIVE_BIT.getValue(this);
    }

    @Override
    public void setEmissive(boolean isEmissive)
    {
        EMISSIVE_BIT.setValue(isEmissive, this);
    }

    @Override
    public boolean isLockUV()
    {
        return LOCKUV_BIT.getValue(this);
    }

    @Override
    public void setLockUV(boolean isLockUV)
    {
        LOCKUV_BIT.setValue(isLockUV, this);
    }

    @Override
    public boolean shouldContractUVs()
    {
        return CONTRACTUV_BITS.getValue(this);
    }

    @Override
    public void setShouldContractUVs(boolean shouldContractUVs)
    {
        CONTRACTUV_BITS.setValue(shouldContractUVs, this);
    }

    @Override
    public BlockRenderLayer getRenderLayer()
    {
        return RENDERPASS_BITS.getValue(this);
    }

    @Override
    public void setRenderLayer(BlockRenderLayer renderPass)
    {
        RENDERPASS_BITS.setValue(renderPass, this);
    }

    
    @Override
    public EnumFacing getNominalFace()
    {
        return NOMINAL_FACE_BITS.getValue(this);
    }

    @Override
    public void setNominalFace(EnumFacing face)
    {
        NOMINAL_FACE_BITS.setValue(face, this);
    }
    
    @Override
    public Surface getSurfaceInstance()
    {
        return surfaceInstance;
    }

    @Override
    public float getMinU()
    {
        return minU;
    }

    @Override
    public void setMinU(float minU)
    {
        this.minU = minU;
    }

    @Override
    public float getMaxU()
    {
        return maxU;
    }

    @Override
    public void setMaxU(float maxU)
    {
        this.maxU = maxU;
    }

    @Override
    public float getMinV()
    {
        return minV;
    }

    @Override
    public void setMinV(float minV)
    {
        this.minV = minV;
    }

    @Override
    public float getMaxV()
    {
        return maxV;
    }

    @Override
    public void setMaxV(float maxV)
    {
        this.maxV = maxV;
    }
    
    @Override
    public final void scaleQuadUV(float uScale, float vScale)
    {
        this.minU *= uScale;
        this.maxU *= uScale;
        this.minV *= vScale;
        this.maxV *= vScale;
    }
    
    @Override
    public final void offsetQuadUV(float uOffset, float vOffset)
    {
        this.minU += uOffset;
        this.maxU += uOffset;
        this.minV += vOffset;
        this.maxV += vOffset;        
    }
    @Override
    public boolean hasFaceNormal()
    {
        return  this.faceNormal != null;
    }
    
    @Override
    public Vec3f getFaceNormal()
    {
        Vec3f result = this.faceNormal;
        if(result == null)
        {
            result = computeFaceNormal();
            this.faceNormal = result;
        }
        return result;
    }
    
    @Override
    public void clearFaceNormal()
    {
        this.faceNormal = null;
    }
    
    @Override
    public void invertFaceNormal()
    {
        final Vec3f norm = this.faceNormal;
        if(norm != null)
            this.faceNormal = Vec3f.create(-norm.x(), -norm.y(), -norm.z());
    }
    
    @Override
    public IMutablePolygon setSurfaceInstance(Surface surfaceInstance)
    {
        this.surfaceInstance = surfaceInstance;
        return this;
    }
    
    @Override
    public @Nullable IRenderPipeline getPipeline()
    {
        return ClientProxy.acuityPipeline(PIPELINE_INDEX.getValue(this));
    }
    
    @Override
    public IMutablePolygon setPipeline(@Nullable IRenderPipeline pipeline)
    {
        final int index = pipeline == null ? 0 : pipeline.getIndex();
        PIPELINE_INDEX.setValue(index, this);
        return this;
    }
}
