package grondag.exotic_matter.model.primitives.better;

import com.google.common.collect.ImmutableList.Builder;

import grondag.exotic_matter.model.render.QuadBakery;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.client.renderer.block.model.BakedQuad;

public interface IPaintedPoly<T extends IPaintedVertex> extends IPoly<T>
{

    int getMaxU(int layerIndex);

    int getMaxV(int layerIndex);

    int getMinU(int layerIndex);

    int getMinV(int layerIndex);

    String getTextureName(int layerIndex);

    boolean shouldContractUVs(int layerIndex);

    Rotation getRotation(int layerIndex);
    
    public default void addBakedQuadsToBuilder(int layerIndex, Builder<BakedQuad> builder, boolean isItem)
    {
        builder.add(QuadBakery.createBakedQuad(layerIndex, this, isItem));
    }

}
