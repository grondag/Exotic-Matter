package grondag.exotic_matter.model.primitives;

import javax.annotation.Nullable;

import grondag.exotic_matter.model.painting.Surface.SurfaceInstance;
import grondag.exotic_matter.varia.BitPacker;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;

public abstract class AbstractPolygon implements IMutablePolygon
{
    protected static final BitPacker<AbstractPolygon> BITPACKER = new BitPacker<AbstractPolygon>(p -> p.stateBits, (p, b) -> p.stateBits = b);
    
    /**
     * Claim the lower half of our state bits for color.  Actual access can be via direct bitwise math.
     */
    protected static final BitPacker<AbstractPolygon>.LongElement COLOR_BITS = BITPACKER.createLongElement(0xFFFFFFFFL);
    
    protected static final BitPacker<AbstractPolygon>.NullableEnumElement<EnumFacing> NOMINAL_FACE_BITS = BITPACKER.createNullableEnumElement(EnumFacing.class);
    protected static final BitPacker<AbstractPolygon>.EnumElement<Rotation> ROTATION_BITS = BITPACKER.createEnumElement(Rotation.class);
    protected static final BitPacker<AbstractPolygon>.EnumElement<BlockRenderLayer> RENDERPASS_BITS = BITPACKER.createEnumElement(BlockRenderLayer.class);
    protected static final BitPacker<AbstractPolygon>.BooleanElement FULLBRIGHT_BITS = BITPACKER.createBooleanElement();
    protected static final BitPacker<AbstractPolygon>.BooleanElement LOCKUV_BITS = BITPACKER.createBooleanElement();
    protected static final BitPacker<AbstractPolygon>.BooleanElement CONTRACTUV_BITS = BITPACKER.createBooleanElement();

    protected static final long DEFAULT_BITS;
    static
    {
        long defaultBits = 0xFFFFFFFFL; // white
        defaultBits |= NOMINAL_FACE_BITS.getBits(null);
        defaultBits |= ROTATION_BITS.getBits(Rotation.ROTATE_NONE);
        defaultBits |= RENDERPASS_BITS.getBits(BlockRenderLayer.SOLID);
        defaultBits |= FULLBRIGHT_BITS.getBits(false);
        defaultBits |= LOCKUV_BITS.getBits(false);
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
    protected SurfaceInstance surfaceInstance = IPolygon.NO_SURFACE;
    
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
    public int getColor()
    {
        return (int)(this.stateBits & 0xFFFFFFFFL);
    }

    @Override
    public void setColor(int color)
    {
        this.stateBits = (color & 0xFFFFFFFFL) | (this.stateBits & 0xFFFFFFFF00000000L);
    }

    @Override
    public boolean isFullBrightness()
    {
        return FULLBRIGHT_BITS.getValue(this);
    }

    @Override
    public void setFullBrightness(boolean isFullBrightness)
    {
        FULLBRIGHT_BITS.setValue(isFullBrightness, this);
    }

    @Override
    public boolean isLockUV()
    {
        return LOCKUV_BITS.getValue(this);
    }

    @Override
    public void setLockUV(boolean isLockUV)
    {
        LOCKUV_BITS.setValue(isLockUV, this);
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
    public BlockRenderLayer getRenderPass()
    {
        return RENDERPASS_BITS.getValue(this);
    }

    @Override
    public void setRenderPass(BlockRenderLayer renderPass)
    {
        RENDERPASS_BITS.setValue(renderPass, this);
    }

    
    @Override
    public @Nullable EnumFacing getNominalFace()
    {
        return NOMINAL_FACE_BITS.getValue(this);
    }

    @Override
    public EnumFacing setNominalFace(EnumFacing face)
    {
        NOMINAL_FACE_BITS.setValue(face, this);
        return face;
    }
    
    @Override
    public SurfaceInstance getSurfaceInstance()
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
        if(this.faceNormal == null) this.faceNormal = computeFaceNormal();
        return this.faceNormal;
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
        if(norm != null) this.faceNormal = norm.inverse();
    }
    
    @Override
    public IMutablePolygon setSurfaceInstance(SurfaceInstance surfaceInstance)
    {
        this.surfaceInstance = surfaceInstance;
        return this;
    }
    
    @Override
    public void multiplyColor(int color)
    {
        this.setColor(QuadHelper.multiplyColor(this.getColor(), color));
    }
}