package grondag.exotic_matter.render;

import javax.annotation.Nullable;

import grondag.exotic_matter.render.Surface.SurfaceInstance;
import grondag.exotic_matter.varia.BitPacker;
import grondag.exotic_matter.varia.BitPacker.BitElement.BooleanElement;
import grondag.exotic_matter.varia.BitPacker.BitElement.EnumElement;
import grondag.exotic_matter.varia.BitPacker.BitElement.LongElement;
import grondag.exotic_matter.varia.BitPacker.BitElement.NullableEnumElement;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.EnumFacing;

public abstract class AbstractPolygon implements IMutablePolygon
{
    protected static final BitPacker BITPACKER = new BitPacker();
    /**
     * Claim the lower half of our state bits for color.  Actual access can be via direct bitwise math.
     */
    protected static final LongElement COLOR_BITS = BITPACKER.createLongElement(0xFFFFFFFFL);
    
    protected static final NullableEnumElement<EnumFacing> NOMINAL_FACE_BITS = BITPACKER.createNullableEnumElement(EnumFacing.class);
    protected static final EnumElement<Rotation> ROTATION_BITS = BITPACKER.createEnumElement(Rotation.class);
    protected static final EnumElement<RenderPass> RENDERPASS_BITS = BITPACKER.createEnumElement(RenderPass.class);
    protected static final BooleanElement FULLBRIGHT_BITS = BITPACKER.createBooleanElement();
    protected static final BooleanElement LOCKUV_BITS = BITPACKER.createBooleanElement();
    protected static final BooleanElement CONTRACTUV_BITS = BITPACKER.createBooleanElement();

    protected static final long DEFAULT_BITS;
    static
    {
        long defaultBits = 0xFFFFFFFFL; // white
        defaultBits = NOMINAL_FACE_BITS.setValue(null, defaultBits);
        defaultBits = ROTATION_BITS.setValue(Rotation.ROTATE_NONE, defaultBits);
        defaultBits = RENDERPASS_BITS.setValue(RenderPass.SOLID_SHADED, defaultBits);
        defaultBits = FULLBRIGHT_BITS.setValue(false, defaultBits);
        defaultBits = LOCKUV_BITS.setValue(false, defaultBits);
        defaultBits = CONTRACTUV_BITS.setValue(true, defaultBits);
        DEFAULT_BITS = defaultBits;
    }
    
    protected long stateBits = DEFAULT_BITS;
    
    protected float minU = 0;
    protected float maxU = 16;
    protected float minV = 0;
    protected float maxV = 16;

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
        return ROTATION_BITS.getValue(this.stateBits);
    }

    @Override
    public void setRotation(Rotation rotation)
    {
        this.stateBits = ROTATION_BITS.setValue(rotation, this.stateBits);
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
        return FULLBRIGHT_BITS.getValue(this.stateBits);
    }

    @Override
    public void setFullBrightness(boolean isFullBrightness)
    {
        this.stateBits = FULLBRIGHT_BITS.setValue(isFullBrightness, this.stateBits);
    }

    @Override
    public boolean isLockUV()
    {
        return LOCKUV_BITS.getValue(this.stateBits);
    }

    @Override
    public void setLockUV(boolean isLockUV)
    {
        this.stateBits = LOCKUV_BITS.setValue(isLockUV, this.stateBits);
    }

    @Override
    public boolean shouldContractUVs()
    {
        return CONTRACTUV_BITS.getValue(this.stateBits);
    }

    @Override
    public void setShouldContractUVs(boolean shouldContractUVs)
    {
        this.stateBits = CONTRACTUV_BITS.setValue(shouldContractUVs, this.stateBits);
    }

    @Override
    public RenderPass getRenderPass()
    {
        return RENDERPASS_BITS.getValue(this.stateBits);
    }

    @Override
    public void setRenderPass(RenderPass renderPass)
    {
        this.stateBits = RENDERPASS_BITS.setValue(renderPass, this.stateBits);
    }

    
    @Override
    public @Nullable EnumFacing getNominalFace()
    {
        return NOMINAL_FACE_BITS.getValue(this.stateBits);
    }

    @Override
    public EnumFacing setNominalFace(EnumFacing face)
    {
        this.stateBits = NOMINAL_FACE_BITS.setValue(face, this.stateBits);
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
    public void scaleQuadUV(float uScale, float vScale)
    {
        this.minU *= uScale;
        this.maxU *= uScale;
        this.minV *= vScale;
        this.maxV *= vScale;
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
