package grondag.exotic_matter.model.primitives;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;

public abstract class PaintableSubQuad implements IPaintableQuad
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
        final IPaintableQuad parent = this.getParent();
        // default all settings to parent values
        this.minU = parent.getMinU();
        this.minV = parent.getMinV();
        this.maxU = parent.getMaxU();
        this.maxV = parent.getMaxV();
        this.rotation = parent.getRotation();
    }
    
    protected abstract int layerIndex();
    
    protected void copyPropertiesFrom(PaintableSubQuad otherSubQuad)
    {
        this.minU = otherSubQuad.minU;
        this.minV = otherSubQuad.minV;
        this.maxU = otherSubQuad.maxU;
        this.maxV = otherSubQuad.maxV;
        this.rotation = otherSubQuad.rotation;
        this.textureName = otherSubQuad.textureName;
        this.renderPass = otherSubQuad.renderPass;
    }
    
    @Override
    public IPaintableQuad paintableCopy()
    {
        return getParent().paintableCopy().getSubQuad(layerIndex());
    }

    @Override
    public IPaintableQuad paintableCopy(int vertexCount)
    {
        return getParent().paintableCopy(vertexCount).getSubQuad(layerIndex());
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
    public void toPaintableQuads(Consumer<IPaintableQuad> consumer, boolean isItem)
    {
        getParent().toPaintableQuads(q -> q.getSubQuad(layerIndex()), isItem);
    }

    @Override
    public IPaintableQuad getSubQuad(int layerIndex)
    {
        return getParent().getSubQuad(layerIndex);
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
    public EnumFacing getNominalFace()
    {
        return getParent().getNominalFace();
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
    public void setTextureName(String textureName)
    {
        this.textureName = textureName;
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
    public void setRenderPass(BlockRenderLayer renderPass)
    {
        this.renderPass = renderPass;
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
}
