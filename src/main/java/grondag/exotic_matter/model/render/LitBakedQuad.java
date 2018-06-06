package grondag.exotic_matter.model.render;

import java.lang.reflect.Field;

import javax.annotation.Nullable;

import grondag.exotic_matter.ExoticMatter;
import grondag.exotic_matter.varia.ReflectionHelper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.IVertexConsumer;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.client.model.pipeline.QuadGatheringTransformer;

public class LitBakedQuad extends BakedQuad
{
    private static @Nullable final Field parentField;
    static
    {
        Field pf = null;
        try
        {
            pf = ReflectionHelper.getAccessibleField(QuadGatheringTransformer.class, "parent");
        }
        catch (NoSuchFieldException e)
        {
            ExoticMatter.INSTANCE.error("Unable to wrap forge lighter, rendering will be borked", e);
        }
        parentField = pf;
    }
    
    private static final ThreadLocal<Wrapper> wrappers = new ThreadLocal<Wrapper>()
    {
        @Override
        protected Wrapper initialValue()
        {
            return new Wrapper();
        }
    };
    
    @SuppressWarnings("null")
    private static class Wrapper implements IVertexConsumer
    {
        private @Nullable IVertexConsumer buffer;
        private @Nullable IVertexConsumer lighter;
        private LitBakedQuad quad;
        private int vertexIndex = 0;
        
        public final void wrap(IVertexConsumer lighter, LitBakedQuad quad) throws IllegalArgumentException, IllegalAccessException
        {
            this.vertexIndex = 0;
            this.buffer = (IVertexConsumer) parentField.get(lighter);
            this.lighter = lighter;
            this.quad = quad;
            parentField.set(lighter, this);
        }
        
        public final void unwrap()
        {
            if(this.buffer != null && this.lighter != null)
            try
            {
                parentField.set(lighter, buffer);
            }
            catch(Exception e) {};
            this.buffer = null;
            this.lighter = null;
        }
        
        @Override
        public VertexFormat getVertexFormat()
        {
            return buffer.getVertexFormat();
        }

        @Override
        public void setQuadTint(int tint)
        {
            buffer.setQuadTint(tint);
        }

        @Override
        public void setQuadOrientation(EnumFacing orientation)
        {
            buffer.setQuadOrientation(orientation);
        }

        @Override
        public void setApplyDiffuseLighting(boolean diffuse)
        {
            buffer.setApplyDiffuseLighting(diffuse);
        }

        @Override
        public void setTexture(TextureAtlasSprite texture)
        {
            buffer.setTexture(texture);
        }

        @Override
        public void put(final int element, float... data)
        {
            VertexFormatElement e = buffer.getVertexFormat().getElement(element);
            
            switch(e.getUsage())
            {
            case COLOR:
                this.quad.interpolateVertexColors(this.vertexIndex, data);
                break;
                
            case UV:
                if(e.getIndex() == 1)
                {
                    final int g = quad.getVertexGlow(vertexIndex);
                    if(g == 0) 
                        ;
                    else if(g == 0xF)
                    {
                        data[0] = QuadBakery.MAX_LIGHT_FLOAT;
                        data[1] = QuadBakery.MAX_LIGHT_FLOAT;
                    }
                    else
                    {
                        final float b = (float)(g * 0x20) / 0xFFFF;
                        data[0] = Math.max(data[0], b);
                        data[1] = Math.max(data[1], b);
                    }
                }
                break;
                
            default:
                break;
            }
            
            if(element == buffer.getVertexFormat().getElementCount() - 1)
                this.vertexIndex++;
            
            buffer.put(element, data);
        }
    }
    
    private final int glowBits;
    
    public LitBakedQuad(int[] vertexDataIn, int tintIndexIn, @Nullable EnumFacing faceIn, TextureAtlasSprite spriteIn, boolean applyDiffuseLighting, VertexFormat format, int glowBits)
    {
        super(vertexDataIn, tintIndexIn, faceIn, spriteIn, applyDiffuseLighting, format);
        assert format == DefaultVertexFormats.BLOCK : "unsupported format for pre-lit quad";
        this.glowBits = glowBits;
    }

    private static boolean wrapError = false;
    
    @Override
    public void pipe(IVertexConsumer consumer)
    {
        if(parentField == null || wrapError)
        {
            super.pipe(consumer);
        }
        else
        {
            boolean didWrap = false;
            Wrapper w = wrappers.get();
            try
            {
                w.wrap(consumer, this);
                didWrap = true;
            }
            catch (Exception e)
            {
                wrapError = true;
                ExoticMatter.INSTANCE.error("Unable to enable glow rendering due to unexpected error. Glow rendering will be disabled.", e);
            }
            super.pipe(consumer);
            if(didWrap) w.unwrap();
        }
    }

    public void interpolateVertexColors(int vertexIndex, float[] colors)
    {
        final int glow = this.getVertexGlow(vertexIndex);
        if(glow == 0) return;
        
        if(glow == 15)
        {
            //The 1 here points to the color element of the block vertex format
            //This is brittle, but easy and fast, and right now that is only format supported for pre-lit quads.
            LightUtil.unpack(this.vertexData, colors, this.format, vertexIndex, 1);
            return;
        }
        
        final float wOriginal = glow / 15f;
        final float wShaded = 1 - wOriginal;
        final float r = colors[0] * wShaded;
        final float g = colors[1] * wShaded;
        final float b = colors[2] * wShaded;
        
        LightUtil.unpack(this.vertexData, colors, this.format, vertexIndex, 1);
        colors[0] = colors[0] * wOriginal + r;
        colors[1] = colors[1] * wOriginal + g;
        colors[2] = colors[2] * wOriginal + b;
    }
    
    /**
     * Returns value 0-15 representing how emmissive the vertex is.
     * 0 means normal lighting, normal shading for diffuse and AO.
     * 15 means full lighting, no shading or coloring for diffuse or AO
     */
    public int getVertexGlow(int vertexIndex)
    {
        return (this.glowBits >> (vertexIndex * 4)) & 0xF;
    }
    
    
    
}
