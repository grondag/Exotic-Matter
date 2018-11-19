package grondag.exotic_matter.model.render;


import com.google.common.collect.ImmutableList.Builder;
import grondag.acuity.api.IPipelinedQuad;
import grondag.exotic_matter.model.primitives.IGeometricVertexConsumer;
import grondag.exotic_matter.model.primitives.INormalVertexConsumer;
import grondag.exotic_matter.model.primitives.IPolygon;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import grondag.exotic_matter.model.primitives.vertex.Vertex;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;

/**
 * TODO: finish
 * WIP interface that would replace IPolygon in SuperDispatcher
 * Intent would be to store compressed (short) values and index to cachable values (surfaces, for example.)
 */
public interface IRenderableQuad extends IPipelinedQuad
{

    public void produceGeometricVertices(IGeometricVertexConsumer consumer);

    public EnumFacing getActualFace();

    public Vec3f getFaceNormal();
    
    public default void addBakedQuadsToBuilder(Builder<BakedQuad> builder, boolean isItem)
    {
        builder.add(QuadBakery.createBakedQuad((IPolygon) this, isItem));
    }

    public void produceNormalVertices(INormalVertexConsumer consumer);
//    {
//        Vec3f faceNorm = this.getFaceNormal();
//        
//        this.forEachVertex(v -> 
//        {
//            if(v.normal == null)
//                consumer.acceptVertex(v.x, v.y, v.z, faceNorm.x, faceNorm.y, faceNorm.z);
//            else
//                consumer.acceptVertex(v.x, v.y, v.z, v.normal.x, v.normal.y, v.normal.z);
//        });
//    }
    
    public float getMaxV();

    public float getMinU();

    public float getMinV();

    public float getMaxU();

    public String getTextureName();

    public float[] getFaceNormalArray();

    public Vertex getVertex(int v);

    public boolean shouldContractUVs();

    Rotation getRotation();
    
    public static IRenderableQuad fromMutable(IPolygon mutable)
    {
        return null;
    }
}
