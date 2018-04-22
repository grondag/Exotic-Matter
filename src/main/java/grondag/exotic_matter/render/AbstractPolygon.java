package grondag.exotic_matter.render;

import javax.annotation.Nullable;

import grondag.exotic_matter.render.Surface.SurfaceInstance;
import grondag.exotic_matter.varia.Color;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.EnumFacing;

public abstract class AbstractPolygon implements IMutablePolygon
{

    protected @Nullable EnumFacing nominalFace;
    protected Rotation rotation = Rotation.ROTATE_NONE;
    protected boolean isFullBrightness = false;
    protected boolean isLockUV = false;
    protected boolean shouldContractUVs = true;
    protected RenderPass renderPass = RenderPass.SOLID_SHADED;
    
    protected int color = Color.WHITE;
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
        
        this.nominalFace = fastOther.nominalFace;
        this.textureName = fastOther.textureName;
        this.rotation = fastOther.rotation;
        this.color = fastOther.color;
        this.isFullBrightness = fastOther.isFullBrightness;
        this.isLockUV = fastOther.isLockUV;
        this.faceNormal = fastOther.faceNormal;
        this.shouldContractUVs = fastOther.shouldContractUVs;
        this.minU = fastOther.minU;
        this.maxU = fastOther.maxU;
        this.minV = fastOther.minV;
        this.maxV = fastOther.maxV;
        this.renderPass = fastOther.renderPass;
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
        return rotation;
    }

    @Override
    public void setRotation(Rotation rotation)
    {
        this.rotation = rotation;
    }

    @Override
    public int getColor()
    {
        return color;
    }

    @Override
    public void setColor(int color)
    {
        this.color = color;
    }

    @Override
    public boolean isFullBrightness()
    {
        return isFullBrightness;
    }

    @Override
    public void setFullBrightness(boolean isFullBrightness)
    {
        this.isFullBrightness = isFullBrightness;
    }

    @Override
    public boolean isLockUV()
    {
        return isLockUV;
    }

    @Override
    public void setLockUV(boolean isLockUV)
    {
        this.isLockUV = isLockUV;
    }

    @Override
    public boolean shouldContractUVs()
    {
        return shouldContractUVs;
    }

    @Override
    public void setShouldContractUVs(boolean shouldContractUVs)
    {
        this.shouldContractUVs = shouldContractUVs;
    }

    @Override
    public RenderPass getRenderPass()
    {
        return renderPass;
    }

    @Override
    public void setRenderPass(RenderPass renderPass)
    {
        this.renderPass = renderPass;
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
    public @Nullable EnumFacing getNominalFace()
    {
        return nominalFace;
    }

    @Override
    public EnumFacing setNominalFace(EnumFacing face)
    {
        this.nominalFace = face;
        return face;
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
