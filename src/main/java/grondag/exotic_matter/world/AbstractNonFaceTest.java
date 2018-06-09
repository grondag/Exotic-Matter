package grondag.exotic_matter.world;

import grondag.exotic_matter.model.state.ISuperModelState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Base class for block tests that don't care about facing.
 *
 */
public abstract class AbstractNonFaceTest implements IBlockTest
{
    abstract protected boolean testBlock(IBlockAccess world, IBlockState ibs, BlockPos pos, ISuperModelState modelState);

    abstract protected boolean testBlock(IBlockAccess world, IBlockState ibs, BlockPos pos);
    
    @Override
    public boolean testBlock(EnumFacing face, IBlockAccess world, IBlockState ibs, BlockPos pos)
    {
        return this.testBlock(world, ibs, pos);
    }
    
    @Override
    public boolean testBlock(EnumFacing face, IBlockAccess world, IBlockState ibs, BlockPos pos, ISuperModelState modelState)
    {
        return this.testBlock(world, ibs, pos, modelState);
    }

    @Override
    public boolean testBlock(BlockCorner corner, IBlockAccess world, IBlockState ibs, BlockPos pos)
    {
        return this.testBlock(world, ibs, pos);
    }

    @Override
    public boolean testBlock(BlockCorner face, IBlockAccess world, IBlockState ibs, BlockPos pos, ISuperModelState modelState)
    {
        return this.testBlock(world, ibs, pos, modelState);
    }
    
    @Override
    public boolean testBlock(FarCorner corner, IBlockAccess world, IBlockState ibs, BlockPos pos)
    {
        return this.testBlock(world, ibs, pos);
    }
    
    @Override
    public boolean testBlock(FarCorner face, IBlockAccess world, IBlockState ibs, BlockPos pos, ISuperModelState modelState)
    {
        return this.testBlock(world, ibs, pos, modelState);
    }
}