package grondag.exotic_matter.model.primitives.better;

import javax.annotation.Nullable;

import grondag.acuity.api.IRenderPipeline;
import grondag.exotic_matter.ClientProxy;
import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.primitives.vertex.IVec3f;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import grondag.exotic_matter.varia.BitPacker;
import grondag.exotic_matter.varia.structures.AbstractVector;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;

abstract class AbstractPolygon implements IPolygon
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

    protected float minU = 0;
    protected float maxU = 1;
    protected float minV = 0;
    protected float maxV = 1;

    protected @Nullable Vec3f faceNormal;
    protected @Nullable String textureName;
    protected Surface surfaceInstance = Surface.NO_SURFACE;

    protected final AbstractVector<IMutableVertex> vertices;
    
    protected final @Nullable AbstractVector<Vec3f> normals;
    
    AbstractPolygon(AbstractVector<IMutableVertex> vertices, @Nullable AbstractVector<Vec3f> normals)
    {
        this.vertices = vertices;
        this.normals = normals;
    }
    
    @Override
    public final int vertexCount()
    {
        return vertices.size();
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

    @Override
    public final EnumFacing getNominalFace()
    {
        return NOMINAL_FACE_BITS.getValue(this);
    }

    @Override
    public final Surface getSurfaceInstance()
    {
        return surfaceInstance;
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
    
    @SuppressWarnings("null")
    @Override
    public final Vec3f getVertexNormal(int vertexIndex)
    {
        return normals == null ? getFaceNormal() : normals.get(vertexIndex);
    }
    
    @Override
    public final float getVertexX(int vertexIndex)
    {
        return vertices.get(vertexIndex).x();
    }

    @Override
    public final float getVertexY(int vertexIndex)
    {
        return vertices.get(vertexIndex).y();
    }

    @Override
    public final float getVertexZ(int vertexIndex)
    {
        return vertices.get(vertexIndex).z();
    }

    @Override
    public final int getVertexColor(int layerIndex, int vertexIndex)
    {
        return vertices.get(vertexIndex).getColor();
    }

    @Override
    public final int getVertexGlow(int layerIndex, int vertexIndex)
    {
        return vertices.get(vertexIndex).getGlow();
    }

    @Override
    public final float getVertexU(int layerIndex, int vertexIndex)
    {
        return vertices.get(vertexIndex).getU(vertexIndex);
    }

    @Override
    public final float getVertexV(int layerIndex, int vertexIndex)
    {
        return vertices.get(vertexIndex).getV(vertexIndex);
    }

    @Override
    public final IVec3f getPos(int vertexIndex)
    {
        return vertices.get(vertexIndex).pos();
    }
}
