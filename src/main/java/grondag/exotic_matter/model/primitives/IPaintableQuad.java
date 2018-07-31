package grondag.exotic_matter.model.primitives;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import grondag.acuity.api.IPipelinedQuad;
import grondag.acuity.api.IPipelinedVertexConsumer;
import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.render.QuadBakery;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Parts of IPolygon interface needed for potentially mult-layer model/quad baking.<p>
 * 
 * Regarding texture coordinates...<br>
 * UV min and max for quads and vertices prior to baking are generally in the range 0-1. (Not the 0-16 MC convention)
 * 
 * However, see SurfaceTopology for variation in that meaning...  <p>
 * 
 * Note that a "min" UV value in the polygon instance may not be the mathematical minimum
 * because UV values can be flipped for to reverse the texture during rendering.  Same applies to max.<p>
 * 
 * 
 * TODO: split out mutable parts or otherwise clean up the inheritance tree - this is crap
 */
public interface IPaintableQuad extends IPipelinedQuad
{
    
    //UGLY: needs cleanup
    
    @Override
    @SideOnly(value = Side.CLIENT)
    public default void produceVertices(IPipelinedVertexConsumer vertexLighter)
    {
        float[][][] uvData = getUVData(this);
        Vec3f fn = this.getFaceNormal();
        int glow = 0;
        
        vertexLighter.setEmissive(0, this.isEmissive());
        
        for(int i = 0; i < 4; i++)
        {
            IPaintableVertex v = this.getPaintableVertex(i);
            Vec3f n = v.normal();
            if(n == null)
                n =  fn;
            
            //TODO: need to handle for other layers?
            if(v.glow() != glow)
            {
                final int g = v.glow() * 17;
                vertexLighter.setBlockLightMap(g, g, g, 255);
                glow = v.glow();
            }
            
            switch(uvData.length)
            {
            case 1:
                vertexLighter.acceptVertex(v.x(), v.y(), v.z(), n.x, n.y, n.z, v.color(), uvData[0][i][0], uvData[0][i][1]);
                break;
                
            case 2:
                vertexLighter.acceptVertex(v.x(), v.y(), v.z(), n.x, n.y, n.z, v.color(), uvData[0][i][0], uvData[0][i][1],
                        v.forTextureLayer(1).color(), uvData[1][i][0], uvData[1][i][1]);
                break;
            
            case 3:
                vertexLighter.acceptVertex(v.x(), v.y(), v.z(), n.x, n.y, n.z, v.color(), uvData[0][i][0], uvData[0][i][1],
                        v.forTextureLayer(1).color(), uvData[1][i][0], uvData[1][i][1],
                        v.forTextureLayer(2).color(), uvData[2][i][0], uvData[2][i][1]);
                break;
            
            default:
                throw new ArrayIndexOutOfBoundsException();
            }
        }
    }

    public static float[][][] getUVData(IPaintableQuad mainPoly)
    {
        final int layerCount = mainPoly.layerCount();
        
        // UGLY: frequent execution - consider thread-local to avoid excessive garbage?
        final float[][][] uvData = new float[layerCount][4][2];
        
        for(int n = 0; n < layerCount; n++)
        {
            IPaintableQuad poly = mainPoly.getSubQuad(n);
            
            @SuppressWarnings("null")
            final TextureAtlasSprite textureSprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(poly.getTextureName());
            
            final float minU = poly.getMinU();
            final float minV = poly.getMinV();
            
            final float spanU = poly.getMaxU() - minU;
            final float spanV = poly.getMaxV() - minV;
            
            for(int i = 0; i < 4; i++)
            {
                IPaintableVertex v = poly.getPaintableVertex(i);
                uvData[n][i][0] = v.u();
                uvData[n][i][1] = v.v();
            }
            
            // apply texture rotation
            QuadBakery.applyTextureRotation(poly, uvData[n]);
            
            // scale UV coordinates to size of texture sub-region
            for(int v = 0; v < 4; v++)
            {
                uvData[n][v][0] = minU + spanU * uvData[n][v][0];
                uvData[n][v][1] = minV + spanV * uvData[n][v][1];
            }
    
            if(poly.shouldContractUVs())
            {
                QuadBakery.contractUVs(textureSprite, uvData[n]);
            }
            
            final float spriteMinU = textureSprite.getMinU();
            final float spriteSpanU = textureSprite.getMaxU() - spriteMinU;
            final float spriteMinV = textureSprite.getMinV();
            final float spriteSpanV = textureSprite.getMaxV() - spriteMinV;
            
            for(int i = 0; i < 4; i++)
            {
                // doing interpolation here vs using sprite methods to avoid wasteful multiply and divide by 16
                // UGLY: can this be combined with loop above?
                uvData[n][i][0] = spriteMinU + uvData[n][i][0] * spriteSpanU;
                uvData[n][i][1] = spriteMinV + uvData[n][i][1] * spriteSpanV;
            }
        }
        
        return uvData;
    }
    
    IPaintableQuad getSubQuad(int layerIndex);

    void setMinU(float f);

    /**
     * See notes on UV coordinates in header
     */
    float getMinU();

    void setMaxU(float f);

    void setMinV(float f);

    /**
     * See notes on UV coordinates in header
     */
    float getMinV();

    void setMaxV(float f);

    Surface getSurfaceInstance();

    /**
     * If non-zero, signals painter to randomize texture on this surface
     * to be different from and not join with adjacent textures.<p>
     * 
     * Enables texture randomization on painted surfaces that don't have position information.
     * Populated by mesh generator if applicable.  Supports values 0-255.
     */
    int textureSalt();

    /** 
     * If true then quad painters will ignore UV coordinates and instead set
     * based on projection of vertices onto the given nominal face.
     * Note that FaceVertex does this by default even if lockUV is not specified.
     * To get unlockedUV coordiates, specificy a face using FaceVertex.UV or FaceVertex.UVColored.
     */
    boolean isLockUV();

    
    public boolean shouldContractUVs();
    
    /**
     * If true and Acuity API is enabled, will signal API that surface is emissive.
     * Per-vertex glow will still be passed as lightmap values.
     */
    public boolean isEmissive();
    
    void setEmissive(boolean isEmissive);
    
    /**
     * Gets the face to be used for setupFace semantics.  
     * Is a general facing but does NOT mean poly is actually on that face.
     */
    EnumFacing getNominalFace();

    public Vec3f getFaceNormal();
    
    void setRotation(Rotation object);

    /** 
     * Causes texture to appear rotated within the frame
     * of this texture. Relies on UV coordinates
     * being in the range 0-1. <br><br>
     * 
     * Rotation happens during quad bake.
     * If lockUV is true, rotation happens after UV
     * coordinates are derived.
     */
    Rotation getRotation();

    void setTextureName(@Nullable String textureName);
    
    @Nullable String getTextureName();

    IMutablePolygon getParent();

    int vertexCount();

    IPaintableVertex getPaintableVertex(int i);

    /** 
     * Unique scale transformation of all vertex coordinates 
     * using block center (0.5, 0.5, 0.5) as origin.
     */
    void scaleFromBlockCenter(float f);

    int layerCount();

    void setRenderPass(BlockRenderLayer renderPass);

    /**
     * Adds given offsets to u,v values of each vertex.
     */
    default void offsetVertexUV(float uShift, float vShift)
    {
        for(int i = 0; i < this.vertexCount(); i++)
        {
            IPaintableVertex v = this.getPaintableVertex(i);
            v = v.withUV(v.u() + uShift, v.v() + vShift);
            
            assert v.u() > -QuadHelper.EPSILON : "vertex uv offset out of bounds"; 
            assert v.u() < 1 + QuadHelper.EPSILON : "vertex uv offset out of bounds"; 
            assert v.v() > -QuadHelper.EPSILON : "vertex uv offset out of bounds"; 
            assert v.v() < 1 + QuadHelper.EPSILON : "vertex uv offset out of bounds";

            this.setVertex(i, v);
        }       
    }

    /**
     * See notes on UV coordinates in header
     */
    float getMaxU();

    /**
     * See notes on UV coordinates in header
     */
    float getMaxV();

    IPaintableQuad paintableCopyWithVertices();

    IPaintableQuad paintableCopy(int vertexCount);

    void setVertex(int i, IPaintableVertex thisVertex);

    /**
     * Returns true if this polygon is convex.
     * All Tris must be.  
     * For quads, confirms that each turn around the quad 
     * goes same way by comparing cross products of edges.
     */
    boolean isConvex();

    void toPaintableQuads(Consumer<IPaintableQuad> consumer, boolean b);

}
