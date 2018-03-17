package grondag.exotic_matter.render;

import grondag.exotic_matter.ConfigXM;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class QuadBakery
{
    /**
     * Creates a baked quad - does not mutate the given instance.
     * Will use ITEM vertex format if forceItemFormat is true.
     * Use this for item models.  Doing so will disable pre-baked lighting
     * and cause the quad to include normals.
     */
    public static BakedQuad createBakedQuad(RawQuad raw, boolean forceItemFormat)
    {
        float spanU = raw.maxU - raw.minU;
        float spanV = raw.maxV - raw.minV;
        
        TextureAtlasSprite textureSprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(raw.textureName);
        
        //Dimensions are vertex 0-4 and u/v 0-1.
        float[][] uvData = new float[4][2];
        for(int v = 0; v < 4; v++)
        {
            uvData[v][0] = (float) raw.getVertex(v).u;
            uvData[v][1] = (float) raw.getVertex(v).v;
        }
        
        // apply texture rotation
        applyTextureRotation(raw, uvData);
        
        // scale UV coordinates to size of texture sub-region
        for(int v = 0; v < 4; v++)
        {
            uvData[v][0] = raw.minU + spanU * uvData[v][0] / 16F;
            uvData[v][1] = raw.minV + spanV * uvData[v][1] / 16F;
        }

        if(raw.shouldContractUVs)
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
        VertexFormat format = !forceItemFormat && (raw.isFullBrightness || raw.surfaceInstance.isLampGradient)
                ? net.minecraft.client.renderer.vertex.DefaultVertexFormats.BLOCK
                : net.minecraft.client.renderer.vertex.DefaultVertexFormats.ITEM;
        
        float[] faceNormal = raw.getFaceNormalArray();          
        
        for(int v = 0; v < 4; v++)
        {

            for(int e = 0; e < format.getElementCount(); e++)
            {
                switch(format.getElement(e).getUsage())
                {
                case POSITION:
                    LightUtil.pack(raw.getVertex(v).xyzToFloatArray(), vertexData, format, v, e);
                    break;

                case NORMAL: 
                    LightUtil.pack(raw.getVertex(v).hasNormal() ? raw.getVertex(v).normalToFloatArray() : faceNormal, vertexData, format, v, e);
                    break;

                case COLOR:
                    float shade;
                    if(!raw.isFullBrightness && ConfigXM.RENDER.enableCustomShading && !raw.surfaceInstance.isLampGradient())
                    {
                        Vec3d surfaceNormal = raw.getVertex(v).hasNormal() ? raw.getVertex(v).getNormal() : raw.getFaceNormal();
                        shade = ConfigXM.RENDER.minAmbientLight + 
                                (float) ((surfaceNormal.dotProduct(ConfigXM.Render.lightingNormal) + 1) * ConfigXM.Render.normalLightFactor);
                    }
                    else
                    {
                        shade = 1.0F;
                    }
                    float[] colorRGBA = new float[4];
                    colorRGBA[0] = ((float) (raw.getVertex(v).color >> 16 & 0xFF)) * shade / 255f;
                    colorRGBA[1] = ((float) (raw.getVertex(v).color >> 8 & 0xFF)) * shade / 255f;
                    colorRGBA[2] = ((float) (raw.getVertex(v).color  & 0xFF)) * shade / 255f;
                    colorRGBA[3] = ((float) (raw.getVertex(v).color >> 24 & 0xFF)) / 255f;
                    LightUtil.pack(colorRGBA, vertexData, format, v, e);
                    break;

                case UV: 
                    if(format.getElement(e).getIndex() == 1)
                    {
                        // There are 2 UV elements when we are using a BLOCK vertex format
                        // that accepts pre-baked lightmaps.  Assuming here that the 
                        // intention is for full brightness. (Don't have a way to pass something dimmer.)
                        float[] fullBright = new float[2];

                        //Don't really understand how brightness format works, but this does the job.
                        //It mimics the lightmap that would be returned from a block in full brightness.
                        fullBright[0] = (float)(15 * 0x20) / 0xFFFF;
                        fullBright[1] = (float)(15 * 0x20) / 0xFFFF;

                        LightUtil.pack(fullBright, vertexData, format, v, e);
                    }
                    else
                    {
                        // This block handles the normal case: texture UV coordinates
                        float[] interpolatedUV = new float[2];
                        interpolatedUV[0] = textureSprite.getInterpolatedU(uvData[v][0]);
                        interpolatedUV[1] = textureSprite.getInterpolatedV(uvData[v][1]);
                        LightUtil.pack(interpolatedUV, vertexData, format, v, e);
                    }
                    break;

                default:
                    // NOOP, padding or weirdness
                }
            }
        }

        boolean applyDiffuseLighting = !raw.isFullBrightness
                && !raw.surfaceInstance.isLampGradient()  
                && !ConfigXM.RENDER.enableCustomShading;
        
        return QuadCache.INSTANCE.getCachedQuad(new CachedBakedQuad(vertexData, raw.face, textureSprite, applyDiffuseLighting, 
                format));
    }
    
    private static void applyTextureRotation(RawQuad raw, float[][] uvData)
    {
       switch(raw.rotation)
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
               uvData[i][1] = 16 - uOld;
           }
           break;

       case ROTATE_180:
           for(int i = 0; i < 4; i++)
           {
               float uOld = uvData[i][0];
               float vOld = uvData[i][1];
               uvData[i][0] = 16 - uOld;
               uvData[i][1] = 16 - vOld;
           }
           break;
       
       case ROTATE_270:
           for(int i = 0; i < 4; i++)
           {
               float uOld = uvData[i][0];
               float vOld = uvData[i][1];
               uvData[i][0] = 16 - vOld;
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
