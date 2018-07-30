package grondag.exotic_matter.model.primitives;

import java.util.function.Consumer;

import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;

public interface IPaintableQuad
{
    
    //UGLY: needs cleanup
    
    IPaintableQuad getSubQuad(int layerIndex);

    void setMinU(float f);

    float getMinU();

    void setMaxU(float f);

    void setMinV(float f);

    float getMinV();

    void setMaxV(float f);

    Surface getSurfaceInstance();

    int textureSalt();

    boolean isLockUV();

    EnumFacing getNominalFace();

    void setRotation(Rotation object);

    Rotation getRotation();

    void setTextureName(String textureName);

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

    float getMaxU();

    float getMaxV();

    IPaintableQuad paintableCopy();

    IPaintableQuad paintableCopy(int vertexCount);

    void setVertex(int i, IPaintableVertex thisVertex);

    boolean isConvex();

    void toPaintableQuads(Consumer<IPaintableQuad> consumer, boolean b);

}
