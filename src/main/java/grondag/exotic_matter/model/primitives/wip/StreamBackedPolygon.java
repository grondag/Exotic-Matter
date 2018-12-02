package grondag.exotic_matter.model.primitives.wip;

import grondag.acuity.api.IRenderPipeline;
import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.primitives.polygon.IPolygon;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import grondag.exotic_matter.world.Rotation;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;

public class StreamBackedPolygon implements IPolygon
{
    protected static final int NO_ADDRESS = -1;
    
    /**
     * Address of our header within the stream.
     * Do not change directly, use {@link #position(int)}
     */
    protected int baseAddress = NO_ADDRESS;
    
    protected PolyEncoder polyEncoder;
    
    /**
     * Stream address where vertex data starts.
     * Set when format changes.
     */
    protected int vertexAddress = NO_ADDRESS;
    
    protected VertexEncoder vertexEncoder;
    
    /**
     * Start of vertex glow data, if present.
     */
    protected int glowAddress = NO_ADDRESS;
    
    protected GlowEncoder glowEncoder;
    
    protected int format;
    
    protected IIntStream stream;
    
    /**
     * Default value should be NO_TAG.
     */
    protected Int2IntOpenHashMap tagData;
    
    /**
     * Default value should be NO_LINK.
     */
    protected Int2IntOpenHashMap linkData;
    
    /**
     * Reads header from given stream address and
     * all subsequent operations reflect data at that 
     * position within the stream.
     */
    protected void moveTo(int baseAddress)
    {
        this.baseAddress = baseAddress;
        setFormat(stream.get(baseAddress));
    }
    
    protected void setFormat(int format)
    {
        this.format = format;
        this.polyEncoder = PolyEncoder.get(format);
        this.vertexEncoder = VertexEncoder.get(format);
        this.glowEncoder = GlowEncoder.get(format);
        this.vertexAddress = polyEncoder.stride();
        this.glowAddress = vertexAddress + vertexEncoder.stride() * vertexCount();
    }

    @Override
    public final boolean isMarked()
    {
        return PolyStreamFormat.isMarked(format);
    }

    @Override
    public final void flipMark()
    {
        setMark(!isMarked());
    }

    @Override
    public final void setMark(boolean isMarked)
    {
        format = PolyStreamFormat.setMarked(format, isMarked);
        stream.set(baseAddress, format);
    }

    @Override
    public final boolean isDeleted()
    {
        return PolyStreamFormat.isDeleted(format);
    }

    @Override
    public final void setDeleted()
    {
        format = PolyStreamFormat.setDeleted(format, true);
        stream.set(baseAddress, format);
    }

    @Override
    public final int getTag()
    {
        return tagData.get(baseAddress);
    }

    @Override
    public final void setTag(int tag)
    {
        tagData.put(baseAddress, tag);
    }

    @Override
    public final int getLink()
    {
        return linkData.get(baseAddress);
    }

    @Override
    public final void setLink(int link)
    {
        linkData.put(baseAddress, link);
    }

    @Override
    public final int vertexCount()
    {
        return PolyStreamFormat.getVertexColorFormat(format);
    }

    @Override
    @Deprecated
    public final Vec3f getPos(int index)
    {
        return Vec3f.create(getVertexX(index), getVertexY(index), getVertexZ(index));
    }

    @Override
    public Vec3f getFaceNormal()
    {
        return polyEncoder.getFaceNormal(stream, baseAddress);
    }

    @Override
    public final EnumFacing getNominalFace()
    {
        return PolyStreamFormat.getNominalFace(format);
    }

    @Override
    public final Surface getSurface()
    {
        return StaticEncoder.getSurface(stream, baseAddress);
    }

    @Override
    @Deprecated
    public final Vec3f getVertexNormal(int vertexIndex)
    {
        return vertexEncoder.hasNormals()
                ? vertexEncoder.getVertexNormal(stream, vertexAddress, vertexIndex)
                : getFaceNormal();
    }

    @Override
    public final boolean hasVertexNormal(int vertexIndex)
    {
        return vertexEncoder.hasNormals()
                ? vertexEncoder.hasVertexNormal(stream, vertexAddress, vertexIndex)
                : false;
    }

    @Override
    public final float getVertexNormalX(int vertexIndex)
    {
        return vertexEncoder.hasNormals()
                ? vertexEncoder.getVertexNormalX(stream, vertexAddress, vertexIndex)
                : polyEncoder.getFaceNormalX(stream, baseAddress);
    }

    @Override
    public final float getVertexNormalY(int vertexIndex)
    {
        return vertexEncoder.hasNormals()
                ? vertexEncoder.getVertexNormalY(stream, vertexAddress, vertexIndex)
                : polyEncoder.getFaceNormalY(stream, baseAddress);
    }

    @Override
    public final float getVertexNormalZ(int vertexIndex)
    {
        return vertexEncoder.hasNormals()
                ? vertexEncoder.getVertexNormalZ(stream, vertexAddress, vertexIndex)
                : polyEncoder.getFaceNormalZ(stream, baseAddress);
    }

    @Override
    public final float getMaxU(int layerIndex)
    {
        return polyEncoder.getMaxU(stream, baseAddress, layerIndex);
    }

    @Override
    public final float getMaxV(int layerIndex)
    {
        return polyEncoder.getMaxV(stream, baseAddress, layerIndex);
    }

    @Override
    public final float getMinU(int layerIndex)
    {
        return polyEncoder.getMinU(stream, baseAddress, layerIndex);
    }

    @Override
    public final float getMinV(int layerIndex)
    {
        return polyEncoder.getMinV(stream, baseAddress, layerIndex);
    }

    @Override
    public final int layerCount()
    {
        return PolyStreamFormat.getLayerCount(format);
    }

    @Override
    public final String getTextureName(int layerIndex)
    {
        return polyEncoder.getTextureName(stream, baseAddress, layerIndex);
    }

    @Override
    public final boolean shouldContractUVs(int layerIndex)
    {
        return StaticEncoder.shouldContractUVs(stream, baseAddress, layerIndex);
    }

    @Override
    public final Rotation getRotation(int layerIndex)
    {
        return StaticEncoder.getRotation(stream, baseAddress, layerIndex);
    }

    @Override
    public final float getVertexX(int vertexIndex)
    {
        return vertexEncoder.getVertexX(stream, vertexAddress, vertexIndex);
    }

    @Override
    public final float getVertexY(int vertexIndex)
    {
        return vertexEncoder.getVertexY(stream, vertexAddress, vertexIndex);
    }

    @Override
    public final float getVertexZ(int vertexIndex)
    {
        return vertexEncoder.getVertexZ(stream, vertexAddress, vertexIndex);
    }

    @Override
    public final int getVertexColor(int layerIndex, int vertexIndex)
    {
        return vertexEncoder.hasColor()
                ? vertexEncoder.getVertexColor(stream, vertexAddress, layerIndex, vertexIndex)
                : polyEncoder.getVertexColor(stream, baseAddress, layerIndex);
    }

    @Override
    public final int getVertexGlow(int vertexIndex)
    {
        return glowEncoder.getGlow(stream, glowAddress, vertexIndex);
    }

    @Override
    public final float getVertexU(int layerIndex, int vertexIndex)
    {
        return vertexEncoder.getVertexU(stream, vertexAddress, layerIndex, vertexIndex);
    }

    @Override
    public final float getVertexV(int layerIndex, int vertexIndex)
    {
        return vertexEncoder.getVertexV(stream, vertexAddress, layerIndex, vertexIndex);
    }

    @Override
    public final int getTextureSalt()
    {
        return StaticEncoder.getTextureSalt(stream, baseAddress);
    }

    @Override
    public final boolean isLockUV(int layerIndex)
    {
        return StaticEncoder.isLockUV(stream, baseAddress, layerIndex);
    }

    @Override
    public final boolean hasRenderLayer(BlockRenderLayer layer)
    {
        return StaticEncoder.hasRenderLayer(stream, baseAddress, layer);
    }

    @Override
    public BlockRenderLayer getRenderLayer(int layerIndex)
    {
        return StaticEncoder.getRenderLayer(stream, baseAddress, layerIndex);
    }

    @Override
    public boolean isEmissive(int layerIndex)
    {
        return StaticEncoder.isEmissive(stream, baseAddress, layerIndex);
    }

    @Override
    public IRenderPipeline getPipeline()
    {
        return StaticEncoder.getPipeline(stream, baseAddress);
    }

}
