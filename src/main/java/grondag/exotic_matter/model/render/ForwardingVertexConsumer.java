package grondag.exotic_matter.model.render;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.IVertexConsumer;

/**
 * Forge has a class like this, but wrapped consumer here is non-final.
 */
public abstract class ForwardingVertexConsumer implements IVertexConsumer
{
    protected @Nullable IVertexConsumer wrapped;
    
    @Override
    public VertexFormat getVertexFormat()
    {
        return wrapped.getVertexFormat();
    }

    @Override
    public void setQuadTint(int tint)
    {
        wrapped.setQuadTint(tint);
    }

    @Override
    public void setQuadOrientation(EnumFacing orientation)
    {
        wrapped.setQuadOrientation(orientation);
    }

    @Override
    public void setApplyDiffuseLighting(boolean diffuse)
    {
        wrapped.setApplyDiffuseLighting(diffuse);
    }

    @Override
    public void setTexture(TextureAtlasSprite texture)
    {
        wrapped.setTexture(texture);
    }

    @Override
    public void put(final int element, float... data)
    {
        wrapped.put(element, data);
    }
}
