package grondag.exotic_matter.model.render;

import grondag.exotic_matter.model.primitives.IPaintableQuad;
import grondag.exotic_matter.model.primitives.IPolygon;
import grondag.exotic_matter.model.primitives.vertex.Vertex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class QuadBakery
{
    //Still fuzzy on how the lightmap coordinates work, but this does the job.
    //It mimics the lightmap that would be returned from a block in full brightness.
    public static final int MAX_LIGHT = 15 * 0x20;
    public static final float MAX_LIGHT_FLOAT = (float)MAX_LIGHT / 0xFFFF;
    public static final float[] LIGHTMAP_FULLBRIGHT = {MAX_LIGHT_FLOAT, MAX_LIGHT_FLOAT};
    
    /**
     * Temporary Workaround for Forge #5073
     */
    private static final VertexFormat ITEM_ALTERNATE;
    
    static
    {
        ITEM_ALTERNATE = new VertexFormat();
        ITEM_ALTERNATE.addElement(DefaultVertexFormats.POSITION_3F);
        ITEM_ALTERNATE.addElement(DefaultVertexFormats.COLOR_4UB);
        ITEM_ALTERNATE.addElement(DefaultVertexFormats.NORMAL_3B);
        ITEM_ALTERNATE.addElement(DefaultVertexFormats.PADDING_1B);
        ITEM_ALTERNATE.addElement(DefaultVertexFormats.TEX_2F);
    }
    
    /**
     * Creates a baked quad - does not mutate the given instance.
     * Will use ITEM vertex format if forceItemFormat is true.
     * Use this for item models.  Doing so will disable pre-baked lighting
     * and cause the quad to include normals.<p>
     * 
     * Expects that lightmaps are represented by vertex glow bits.
     * For example, if the quad is full brightness, then glow should be 255 for all vertices.
     * Any transformation to alpha or lightmap that uses glow bits should already
     * be applied by painer before this is called.
     */
    @SuppressWarnings("null")
    public static BakedQuad createBakedQuad(IPolygon raw, boolean forceItemFormat)
    {
        final float spanU = raw.getMaxU() - raw.getMinU();
        final float spanV = raw.getMaxV() - raw.getMinV();
        
        final TextureAtlasSprite textureSprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(raw.getTextureName());
        
        //Dimensions are vertex 0-4 and u/v 0-1.
        float[][] uvData = new float[4][2];
        float[][] normalData = new float[4][3];
        float[] faceNormal = raw.getFaceNormalArray();   
        
        int glowBits =  0;
        for(int v = 0; v < 4; v++)
        {
            final Vertex vertex = raw.getVertex(v);
            if(vertex.glow() != 0)
            { 
                // round to nearest 0-15
                int g = (vertex.glow()  + 9) / 17;
                glowBits |= (g << (v * 4));
            }
            
            uvData[v][0] = vertex.u();
            uvData[v][1] = vertex.v();
            
            normalData[v] = vertex.normal() == null
                    ? faceNormal
                    : vertex.normal().toArray();
        }
        
        // apply texture rotation
        applyTextureRotation(raw, uvData);
        
        // scale UV coordinates to size of texture sub-region
        for(int v = 0; v < 4; v++)
        {
            uvData[v][0] = raw.getMinU() + spanU * uvData[v][0];
            uvData[v][1] = raw.getMinV() + spanV * uvData[v][1];
        }

        if(raw.shouldContractUVs())
        {
            contractUVs(textureSprite, uvData);
        }

        int[] vertexData = new int[28];

        /**
         * The item vertex consumer expects to get Item vertex format. (Includes normal.)
         * But to render lightmap we have to use Block format, which uses two bytes that
         * would normally be used for normals to contain brightness information.
         * Note that this means any per-vertex normals generated by meshes will 
         * not be used if the quad is full brightness and not being rendered as an item.
         * This should be OK, because we generally don't care about shading for full-brightness render.
         */
        VertexFormat format = forceItemFormat || glowBits == 0
//                ? net.minecraft.client.renderer.vertex.DefaultVertexFormats.ITEM
                ? ITEM_ALTERNATE
                : net.minecraft.client.renderer.vertex.DefaultVertexFormats.BLOCK;
        
        final float spriteMinU = textureSprite.getMinU();
        final float spriteSpanU = textureSprite.getMaxU() - spriteMinU;
        final float spriteMinV = textureSprite.getMinV();
        final float spriteSpanV = textureSprite.getMaxV() - spriteMinV;
        
        for(int v = 0; v < 4; v++)
        {
            for(int e = 0; e < format.getElementCount(); e++)
            {
                switch(format.getElement(e).getUsage())
                {
                case POSITION:
                    LightUtil.pack(raw.getVertex(v).pos().toArray(), vertexData, format, v, e);
                    break;

                case NORMAL: 
                {
                    LightUtil.pack(normalData[v], vertexData, format, v, e);
                    break;
                }
                case COLOR:
                {
                    final int color = raw.getVertex(v).color();
                    float[] colorRGBA = new float[4];
                    colorRGBA[0] = ((float) (color >> 16 & 0xFF)) / 255f;
                    colorRGBA[1] = ((float) (color >> 8 & 0xFF)) / 255f;
                    colorRGBA[2] = ((float) (color  & 0xFF)) / 255f;
                    colorRGBA[3] = ((float) (color >> 24 & 0xFF)) / 255f;
                    LightUtil.pack(colorRGBA, vertexData, format, v, e);
                    break;
                }
                case UV: 
                    if(format.getElement(e).getIndex() == 0)
                    {
                        // This block handles the normal case: texture UV coordinates
                        float[] interpolatedUV = new float[2];
                        
                        // doing interpolation here vs using sprite methods to avoid wasteful multiply and divide by 16
                        interpolatedUV[0] = spriteMinU + uvData[v][0] * spriteSpanU;
                        interpolatedUV[1] = spriteMinV + uvData[v][1] * spriteSpanV;
                        LightUtil.pack(interpolatedUV, vertexData, format, v, e);
                    }
                    else
                    {
                        // There are 2 UV elements when we are using a BLOCK vertex format
                        // The 2nd accepts pre-baked lightmaps.  
                        float[] lightMap = new float[2];

                        final float glow = (float)(((glowBits >> (v * 4)) & 0xF) * 0x20) / 0xFFFF;
                                
                        lightMap[0] = glow;
                        lightMap[1] = glow;

                        LightUtil.pack(lightMap, vertexData, format, v, e);
                    }
                    break;

                default:
                    // NOOP, padding or weirdness
                }
            }
        }
        
        BakedQuad quad = format == ITEM_ALTERNATE
                ? new CachedBakedQuad(vertexData, -1, raw.getActualFace(), textureSprite, true, format)
                : new LitBakedQuad(vertexData, normalData, -1, raw.getActualFace(), textureSprite, true, format, glowBits);
        
        return QuadCache.INSTANCE.getCachedQuad(quad);
    }
    
    public static void applyTextureRotation(IPaintableQuad raw, float[][] uvData)
    {
       switch(raw.getRotation())
       {
       case ROTATE_NONE:
       default:
           break;
           
       case ROTATE_90:
           for(int i = 0; i < 4; i++)
           {
               float uOld = uvData[i][0];
               float vOld = uvData[i][1];
               uvData[i][0] = vOld;
               uvData[i][1] = 1 - uOld;
           }
           break;

       case ROTATE_180:
           for(int i = 0; i < 4; i++)
           {
               float uOld = uvData[i][0];
               float vOld = uvData[i][1];
               uvData[i][0] = 1 - uOld;
               uvData[i][1] = 1 - vOld;
           }
           break;
       
       case ROTATE_270:
           for(int i = 0; i < 4; i++)
           {
               float uOld = uvData[i][0];
               float vOld = uvData[i][1];
               uvData[i][0] = 1 - vOld;
               uvData[i][1] = uOld;
           }
        break;
       
       }
    }

    /**
     * UV shrinkage amount to prevent visible seams
     */
    public static final float UV_EPS = 1f / 0x100;
    
     /**
     * Prevents visible seams along quad boundaries due to slight overlap
     * with neighboring textures or empty texture buffer.
     * Borrowed from Forge as implemented by Fry in UnpackedBakedQuad.build().
     * Array dimensions are vertex 0-3, u/v 0-1
     */
    public static void contractUVs(TextureAtlasSprite textureSprite, float[][] uvData)
    {
        float tX = textureSprite.getOriginX() / textureSprite.getMinU();
        float tY = textureSprite.getOriginY() / textureSprite.getMinV();
        float tS = tX > tY ? tX : tY;
        float ep = 1f / (tS * 0x100);

        //uve refers to the uv element number in the format
        //we will always have uv data directly
        float center[] = new float[2];

        for(int v = 0; v < 4; v++)
        {
            center[0] += uvData[v][0] / 4;
            center[1] += uvData[v][1] / 4;
        }

        for(int v = 0; v < 4; v++)
        {
            for (int i = 0; i < 2; i++)
            {
                float uo = uvData[v][i];
                float un = uo * (1 - UV_EPS) + center[i] * UV_EPS;
                float ud = uo - un;
                float aud = ud;
                if(aud < 0) aud = -aud;
                if(aud < ep) // not moving a fraction of a pixel
                {
                    float udc = uo - center[i];
                    if(udc < 0) udc = -udc;
                    if(udc < 2 * ep) // center is closer than 2 fractions of a pixel, don't move too close
                    {
                        un = (uo + center[i]) / 2;
                    }
                    else // move at least by a fraction
                    {
                        un = uo + (ud < 0 ? ep : -ep);
                    }
                }
                uvData[v][i] = un;
            }
        }
    }
}
