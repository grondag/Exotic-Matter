package grondag.exotic_matter.varia;

import grondag.exotic_matter.model.ISuperBlock;
import grondag.exotic_matter.model.ISuperModelState;
import grondag.exotic_matter.model.varia.AbstractNonFaceTest;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class SuperBlockBorderMatch extends AbstractNonFaceTest
{
    private final ISuperBlock block;
    private final ISuperModelState matchModelState;
    private final boolean isSpeciesPartOfMatch;
    
    /** pass in the info for the block you want to match */
    public SuperBlockBorderMatch(ISuperBlock block, ISuperModelState modelState, boolean isSpeciesPartOfMatch)
    {
        this.block = block;
        this.matchModelState = modelState;
        this.isSpeciesPartOfMatch = isSpeciesPartOfMatch;
    }
    
    /** assumes you want to match block at given position */
    public SuperBlockBorderMatch(IBlockAccess world, IBlockState ibs, BlockPos pos, boolean isSpeciesPartOfMatch)
    {
        this.block = ((ISuperBlock)ibs.getBlock());
        //last param = false prevents recursion - we don't need the full model state (which depends on this logic)
        this.matchModelState = this.block.getModelStateAssumeStateIsCurrent(ibs, world, pos, false);
        this.isSpeciesPartOfMatch = isSpeciesPartOfMatch;
    }
    
    @Override 
    public boolean wantsModelState() { return true; }
    
    @Override
    protected boolean testBlock(IBlockAccess world, IBlockState ibs, BlockPos pos, ISuperModelState modelState)
    {
        return ibs.getBlock() == this.block 
                && this.matchModelState.doShapeAndAppearanceMatch(modelState)
                && (!this.isSpeciesPartOfMatch || !modelState.hasSpecies() || (this.matchModelState.getSpecies() == modelState.getSpecies()));
    }

    @Override
    protected boolean testBlock(IBlockAccess world, IBlockState ibs, BlockPos pos)
    {
        return testBlock(world, ibs, pos, this.block.getModelStateAssumeStateIsCurrent(ibs, world, pos, false));
    }
}