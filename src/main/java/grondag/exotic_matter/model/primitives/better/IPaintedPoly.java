package grondag.exotic_matter.model.primitives.better;

import com.google.common.collect.ImmutableList.Builder;

import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import grondag.exotic_matter.model.render.QuadBakery;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.client.renderer.block.model.BakedQuad;

public interface IPaintedPoly extends IPoly
{
    int getMaxU(int layerIndex);

    int getMaxV(int layerIndex);

    int getMinU(int layerIndex);

    int getMinV(int layerIndex);

    int layerCount();
    
    String getTextureName(int layerIndex);

    boolean shouldContractUVs(int layerIndex);

    Rotation getRotation(int layerIndex);
    
    public default void addBakedQuadsToBuilder(int layerIndex, Builder<BakedQuad> builder, boolean isItem)
    {
        produceQuads(q -> builder.add(QuadBakery.createBakedQuad(layerIndex, (IPaintedPoly) q, isItem)));
    }

    IPaintedPoly recoloredCopy();
    //final Random r = ThreadLocalRandom.current();
    //(r.nextInt(0x1000000) & 0xFFFFFF) | 0xFF000000

    short glow(int layerIndex, int vertexIndex);

    float u(int layerIndex, int vertexIndex);
    
    float v(int layerIndex, int vertexIndex);

    Vec3f normal(int vertexIndex);
    
}
