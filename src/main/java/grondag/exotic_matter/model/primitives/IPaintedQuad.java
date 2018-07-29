package grondag.exotic_matter.model.primitives;

import com.google.common.collect.ImmutableList.Builder;

import grondag.acuity.api.IPipelinedQuad;
import net.minecraft.client.renderer.block.model.BakedQuad;

public interface IPaintedQuad extends IPipelinedQuad
{

    void addBakedItemQuadsToBuilder(Builder<BakedQuad> builder);

}
