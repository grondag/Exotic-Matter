package grondag.exotic_matter.model.primitives;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import grondag.acuity.api.IPipelinedQuad;
import grondag.acuity.api.IPipelinedVertexConsumer;
import grondag.acuity.api.IRenderPipeline;
import grondag.acuity.api.TextureFormat;
import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.render.QuadBakery;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Immutable parts of IPolygon interface needed for potentially mult-layer model/quad baking.<p>
 * 
 * Regarding texture coordinates...<br>
 * UV min and max for quads and vertices prior to baking are generally in the range 0-1. (Not the 0-16 MC convention)
 * 
 * However, see SurfaceTopology for variation in that meaning...  <p>
 * 
 * Note that a "min" UV value in the polygon instance may not be the mathematical minimum
 * because UV values can be flipped for to reverse the texture during rendering.  Same applies to max.<p>
 */
public interface IPaintedQuad extends IPipelinedQuad
{

    IPaintableQuad getSubQuad(int layerIndex);
    
    /**
     * See notes on UV coordinates in header
     */
    float getMinU();
    
    /**
     * See notes on UV coordinates in header
     */
    float getMaxU();

    /**
     * See notes on UV coordinates in header
     */
    float getMaxV();
    
    /**
     * See notes on UV coordinates in header
     */
    float getMinV();
    
    public Vec3f getFaceNormal();
    
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
    
    /**
     * Gets the face to be used for setupFace semantics.  
     * Is a general facing but does NOT mean poly is actually on that face.
     */
    EnumFacing getNominalFace();
    
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
    
    @Nullable String getTextureName();

    IMutablePolygon getParent();

    int vertexCount();
    
    int layerCount();

    IPaintableVertex getPaintableVertex(int i);
    
    /**
     * Returns true if this polygon is convex.
     * All Tris must be.  
     * For quads, confirms that each turn around the quad 
     * goes same way by comparing cross products of edges.
     */
    boolean isConvex();

    void toPaintableQuads(Consumer<IPaintableQuad> consumer, boolean b);
    
    IPaintableQuad paintableCopyWithVertices();

    IPaintableQuad paintableCopy(int vertexCount);
    
    /**
     * Convenient shorthand for null check and texture format lookup.
     */
    public default @Nullable TextureFormat textureFormat()
    {
        final IRenderPipeline p = this.getPipeline();
        return p == null ? null : p.textureFormat();
    }
    
    @Override
    @SideOnly(value = Side.CLIENT)
    public default void produceVertices(IPipelinedVertexConsumer vertexLighter)
    {
        float[][][] uvData = IPaintedQuad.getUVData(this);
        Vec3f fn = this.getFaceNormal();
        int glow = 0;
        
        vertexLighter.setEmissive(0, this.isEmissive());
        if(uvData.length > 1)
        {
            vertexLighter.setEmissive(1, this.getSubQuad(1).isEmissive());
            if(uvData.length == 3)
            {
                vertexLighter.setEmissive(2, this.getSubQuad(2).isEmissive());
            }
        }
        
        for(int i = 0; i < 4; i++)
        {
            IPaintableVertex v = this.getPaintableVertex(i);
            Vec3f n = v.normal();
            if(n == null)
                n =  fn;
            
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
    
    static float[][][] getUVData(IPaintedQuad mainPoly)
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

}
