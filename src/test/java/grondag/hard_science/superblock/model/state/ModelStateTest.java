package grondag.hard_science.superblock.model.state;

import org.junit.Test;

import grondag.exotic_matter.ExoticMatter;
import grondag.exotic_matter.init.ModTextures;
import grondag.exotic_matter.model.color.BlockColorMapProvider;
import grondag.exotic_matter.model.color.Translucency;
import grondag.exotic_matter.model.mesh.ModShapes;
import grondag.exotic_matter.model.mesh.ModelShape;
import grondag.exotic_matter.model.painting.PaintLayer;
import grondag.exotic_matter.model.render.RenderLayout;
import grondag.exotic_matter.model.state.ModelState;
import grondag.exotic_matter.model.state.ModelStateData;
import grondag.exotic_matter.world.CornerJoinBlockStateSelector;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;

public class ModelStateTest
{

    @Test
    public void test()
    {
        ExoticMatter.INSTANCE.info("Max shapes within current format: " + ModelShape.MAX_SHAPES);
        ExoticMatter.INSTANCE.info("core bits length = "  + ModelStateData.PACKER_CORE.bitLength());
        ExoticMatter.INSTANCE.info("layer bits length = "  + ModelStateData.PACKER_LAYER_BASE.bitLength());
        assert ModelStateData.PACKER_LAYER_BASE.bitLength() == ModelStateData.PACKER_LAYER_CUT.bitLength();
        assert ModelStateData.PACKER_LAYER_BASE.bitLength() == ModelStateData.PACKER_LAYER_LAMP.bitLength();
        assert ModelStateData.PACKER_LAYER_BASE.bitLength() == ModelStateData.PACKER_LAYER_MIDDLE.bitLength();
        assert ModelStateData.PACKER_LAYER_BASE.bitLength() == ModelStateData.PACKER_LAYER_OUTER.bitLength();
        
        ExoticMatter.INSTANCE.info("block shape bits length = "  + ModelStateData.PACKER_SHAPE_BLOCK.bitLength());
        ExoticMatter.INSTANCE.info("flow shape bits length = "  + ModelStateData.PACKER_SHAPE_FLOW.bitLength());
        ExoticMatter.INSTANCE.info("extra shape bits length = "  + ModelStateData.PACKER_SHAPE_EXTRA.bitLength());
        
        
        // sign bit on third long is used to store static indicator
        assert(ModelStateData.PACKER_SHAPE_EXTRA.bitLength() < 64);
        
        ModelState state = new ModelState();
        
        state.setShape(ModShapes.COLUMN_SQUARE);
        state.setStatic(true);

        state.setOuterLayerEnabled(true);
        state.setColorMap(PaintLayer.BASE, BlockColorMapProvider.INSTANCE.getColorMap(5));
        state.setColorMap(PaintLayer.OUTER, BlockColorMapProvider.INSTANCE.getColorMap(7));
        state.setFullBrightness(PaintLayer.LAMP, true);
        state.setTexture(PaintLayer.BASE, ModTextures.BLOCK_NOISE_STRONG);
        state.setTexture(PaintLayer.LAMP, ModTextures.BLOCK_COBBLE);
        state.setTexture(PaintLayer.MIDDLE, ModTextures.BLOCK_NOISE_SUBTLE_ZOOM);
        state.setTexture(PaintLayer.OUTER, ModTextures.BIGTEX_TEST1);
        state.setAxis(EnumFacing.Axis.Z);
        state.setTranslucent(PaintLayer.MIDDLE, true);
        state.setTranslucency(Translucency.SHADED);
        state.setPosX(3);
        state.setPosY(7);
        state.setPosZ(15);
        state.setSpecies(13);
        state.setCornerJoin(CornerJoinBlockStateSelector.getJoinState(69));
        state.setAxisInverted(true);
        state.setStaticShapeBits(879579585L);
        
        NBTTagCompound persistedState = state.serializeNBT();
        
        ModelState reloadedState = new ModelState();
        reloadedState.deserializeNBT(persistedState);
        
        assert(state.equals(reloadedState));
        assert(state.hashCode() == reloadedState.hashCode());
        
        assert(reloadedState.getShape() == ModShapes.COLUMN_SQUARE);
        assert(reloadedState.isStatic());
        assert(reloadedState.isOuterLayerEnabled());
        assert(reloadedState.isMiddleLayerEnabled());
        assert(reloadedState.getColorMap(PaintLayer.BASE) == BlockColorMapProvider.INSTANCE.getColorMap(5));
        assert(reloadedState.getColorMap(PaintLayer.OUTER) == BlockColorMapProvider.INSTANCE.getColorMap(7));
        assert(reloadedState.isFullBrightness(PaintLayer.LAMP));
        assert(reloadedState.getTexture(PaintLayer.BASE) == ModTextures.BLOCK_NOISE_STRONG);
        assert(reloadedState.getTexture(PaintLayer.LAMP) == ModTextures.BLOCK_COBBLE);
        assert(reloadedState.getTexture(PaintLayer.MIDDLE) == ModTextures.BLOCK_NOISE_SUBTLE_ZOOM);
        assert(reloadedState.getTexture(PaintLayer.OUTER) == ModTextures.BIGTEX_TEST1);
        assert(reloadedState.getAxis()) == EnumFacing.Axis.Z;
        assert(reloadedState.getTranslucency()) == Translucency.SHADED;
        assert(reloadedState.getPosX() == 3);
        assert(reloadedState.getPosY() == 7);
        assert(reloadedState.getPosZ() == 15);
        assert(reloadedState.getSpecies() == 13);
        assert(reloadedState.getCornerJoin() == CornerJoinBlockStateSelector.getJoinState(69));
        assert(reloadedState.getSimpleJoin().getIndex() == CornerJoinBlockStateSelector.getJoinState(69).simpleJoin.getIndex());
        assert(reloadedState.isAxisInverted());
        assert(reloadedState.getStaticShapeBits() == 879579585L);
        RenderLayout rps = reloadedState.getRenderLayout();
        assert(rps.containsBlockRenderLayer(BlockRenderLayer.SOLID) == true);
        assert(rps.containsBlockRenderLayer(BlockRenderLayer.CUTOUT) == false);
        assert(rps.containsBlockRenderLayer(BlockRenderLayer.CUTOUT_MIPPED) == false);
        assert(rps.containsBlockRenderLayer(BlockRenderLayer.TRANSLUCENT) == true);
    }

}