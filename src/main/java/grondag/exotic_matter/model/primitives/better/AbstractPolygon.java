package grondag.exotic_matter.model.primitives.better;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList.Builder;

import grondag.acuity.api.IRenderPipeline;
import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import grondag.exotic_matter.varia.BitPacker;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;

public abstract class AbstractPolygon implements IPolygon
{
    protected static final BitPacker<AbstractPolygon> BITPACKER = new BitPacker<AbstractPolygon>(p -> p.stateBits, (p, b) -> p.stateBits = b);

    protected static final BitPacker<AbstractPolygon>.EnumElement<EnumFacing> NOMINAL_FACE_BITS = BITPACKER.createEnumElement(EnumFacing.class);
    protected static final BitPacker<AbstractPolygon>.IntElement PIPELINE_INDEX = BITPACKER.createIntElement(1024);
    
    protected static final BitPacker<AbstractPolygon>.EnumElement<Rotation> ROTATION_BITS = BITPACKER.createEnumElement(Rotation.class);
    protected static final BitPacker<AbstractPolygon>.EnumElement<BlockRenderLayer> RENDERPASS_BITS = BITPACKER.createEnumElement(BlockRenderLayer.class);
    protected static final BitPacker<AbstractPolygon>.BooleanElement LOCKUV_BIT = BITPACKER.createBooleanElement();
    protected static final BitPacker<AbstractPolygon>.BooleanElement EMISSIVE_BIT = BITPACKER.createBooleanElement();
    protected static final BitPacker<AbstractPolygon>.BooleanElement CONTRACTUV_BITS = BITPACKER.createBooleanElement();
    protected static final BitPacker<AbstractPolygon>.IntElement SALT_BITS = BITPACKER.createIntElement(256);
    protected static final BitPacker<AbstractPolygon>.BooleanElement EMISSIVE_BIT_2 = BITPACKER.createBooleanElement();
    protected static final BitPacker<AbstractPolygon>.BooleanElement EMISSIVE_BIT_3 = BITPACKER.createBooleanElement();

    protected static final long DEFAULT_BITS;
    static
    {
        assert BITPACKER.bitLength() <= 64;
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
    protected Surface surfaceInstance = Surface.NO_SURFACE;

    @Override
    public Vec3f getFaceNormal()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EnumFacing getNominalFace()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Surface getSurfaceInstance()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addPaintableQuadsToList(List<IMutablePolygon> list)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void addPaintedQuadsToList(List<IPolygon> list)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void producePaintableQuads(Consumer<IMutablePolygon> consumer)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void producePaintedQuads(Consumer<IPolygon> consumer)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void claimVertexCopiesToArray(IMutableVertex[] vertex)
    {
        // TODO Auto-generated method stub

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
    public IRenderPipeline getPipeline()
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
    public boolean shouldContractUVs(int layerIndex)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Rotation getRotation(int layerIndex)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getTextureSalt(int layerIndex)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isLockUV(int layerIndex)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public BlockRenderLayer getRenderLayer(int layerIndex)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
