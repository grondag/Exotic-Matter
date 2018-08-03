package grondag.exotic_matter.model.primitives;

import javax.annotation.Nullable;

import grondag.acuity.api.IRenderPipeline;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.BlockRenderLayer;

/**
 * Mutable parts of IPolygon interface needed for potentially mult-layer model/quad baking.<p>
 * 
 * See note in IPainteQuad header Regarding texture coordinates...<br>
 */
public interface IPaintableQuad extends IPaintedQuad
{
    void setMinU(float f);

    void setMaxU(float f);

    void setMinV(float f);

    void setMaxV(float f);

    void setEmissive(boolean isEmissive);

    void setRotation(Rotation object);

    void setTextureName(@Nullable String textureName);

    /** 
     * Unique scale transformation of all vertex coordinates 
     * using block center (0.5, 0.5, 0.5) as origin.
     */
    void scaleFromBlockCenter(float f);

    void setRenderLayer(BlockRenderLayer renderPass);

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

    void setVertex(int i, IPaintableVertex thisVertex);
    
    /** sets acuity render pipeline and returns self for convenience */
    IPaintableQuad setPipeline(@Nullable IRenderPipeline pipeline);

}
