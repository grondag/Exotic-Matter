package grondag.exotic_matter.model.primitives.polygon;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import grondag.acuity.api.IRenderPipeline;
import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.primitives.vertex.IPaintableVertex;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;

public abstract class PaintableSubQuad implements IPaintablePolygon
{
    protected float minU = 0;
    protected float maxU = 1;
    protected float minV = 0;
    protected float maxV = 1;

    protected @Nullable String textureName;
    protected Rotation rotation = Rotation.ROTATE_NONE;
    BlockRenderLayer renderPass = BlockRenderLayer.SOLID;
    
    protected PaintableSubQuad()
    {
        final IPaintablePolygon parent = this.getParent();
        // default all settings to parent values
        this.minU = parent.getMinU();
        this.minV = parent.getMinV();
        this.maxU = parent.getMaxU();
        this.maxV = parent.getMaxV();
        this.rotation = parent.getRotation();
    }
    
    @Override
    public abstract int layerIndex();
    
    protected void copyPropertiesFrom(PaintableSubQuad otherSubQuad)
    {
        this.minU = otherSubQuad.minU;
        this.minV = otherSubQuad.minV;
        this.maxU = otherSubQuad.maxU;
        this.maxV = otherSubQuad.maxV;
        this.rotation = otherSubQuad.rotation;
        this.textureName = otherSubQuad.textureName;
    }
    
    protected void copyPropertiesFrom(IPolygon poly)
    {
        this.minU = poly.getMinU();
        this.minV = poly.getMinV();
        this.maxU = poly.getMaxU();
        this.maxV = poly.getMaxV();
        this.rotation = poly.getRotation();
        this.textureName = poly.getTextureName();
    }
    
    @Override
    public IPaintablePolygon paintableCopyWithVertices()
    {
        return getParent().paintableCopyWithVertices().getSubPoly(layerIndex());
    }

    @Override
    public IPaintablePolygon paintableCopy(int vertexCount)
    {
        return getParent().paintableCopy(vertexCount).getSubPoly(layerIndex());
    }

    @Override
    public void setVertex(int i, IPaintableVertex vertex)
    {
        getParent().setVertex(i, vertex);
    }

    @Override
    public IPaintableVertex getPaintableVertex(int i)
    {
        return getParent().getVertex(i).forTextureLayer(layerIndex());
    }
    
    @Override
    public void toPaintableQuads(Consumer<IPaintablePolygon> consumer, boolean isItem)
    {
        getParent().toPaintableQuads(q -> q.getSubPoly(layerIndex()), isItem);
    }

    @Override
    public IPaintablePolygon getSubPoly(int layerIndex)
    {
        return getParent().getSubPoly(layerIndex);
    }
    
    @Override
    public IPaintablePolygon setPipeline(@Nullable IRenderPipeline pipeline)
    {
        return getParent().setPipeline(pipeline).getSubPoly(layerIndex());
    }
    
    @Override
    public void setMinU(float minU)
    {
        this.minU = minU;
    }

    @Override
    public float getMinU()
    {
        return this.minU;
    }

    @Override
    public void setMaxU(float maxU)
    {
        this.maxU = maxU;
    }

    @Override
    public void setMinV(float minV)
    {
        this.minV = minV;
    }

    @Override
    public float getMinV()
    {
        return this.minV;
    }

    @Override
    public void setMaxV(float maxV)
    {
        this.maxV = maxV;
    }

    @Override
    public Surface getSurfaceInstance()
    {
        return getParent().getSurfaceInstance();
    }

    @Override
    public int textureSalt()
    {
        return getParent().textureSalt();
    }

    @Override
    public boolean isLockUV()
    {
        return getParent().isLockUV();
    }

    @Override
    public boolean shouldContractUVs()
    {
        return getParent().shouldContractUVs();
    }
    
    @Override
    public EnumFacing getNominalFace()
    {
        return getParent().getNominalFace();
    }

    @Override
    public Vec3f getFaceNormal()
    {
        return getParent().getFaceNormal();
    }
    
    @Override
    public void setRotation(Rotation rotation)
    {
        this.rotation = rotation;
    }

    @Override
    public Rotation getRotation()
    {
        return this.rotation;
    }

    @Override
    public void setTextureName(@Nullable String textureName)
    {
        this.textureName = textureName;
    }
    
    @Override
    public @Nullable String getTextureName()
    {
        return this.textureName;
    }
    

    @Override
    public int vertexCount()
    {
        return getParent().vertexCount();
    }

    @Override
    public void scaleFromBlockCenter(float scale)
    {
        getParent().scaleFromBlockCenter(scale);
    }

    @Override
    public int layerCount()
    {
        return getParent().layerCount();
    }

    @Override
    public void setRenderLayer(BlockRenderLayer renderPass)
    {
        getParent().setRenderLayer(renderPass);
    }

    @Override
    public float getMaxU()
    {
        return this.maxU;
    }

    @Override
    public float getMaxV()
    {
        return this.maxV;
    }

    @Override
    public boolean isConvex()
    {
        return getParent().isConvex();
    }
    
    @Override
    public @Nullable IRenderPipeline getPipeline()
    {
        return getParent().getPipeline();
    }

    @Override
    public BlockRenderLayer getRenderLayer()
    {
        return getParent().getRenderLayer();
    }
}
