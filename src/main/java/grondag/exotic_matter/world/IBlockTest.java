package grondag.exotic_matter.world;

import grondag.exotic_matter.block.ISuperBlockAccess;
import grondag.exotic_matter.model.state.ISuperModelState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/** 
 * Used to implement visitor pattern for block-state dependent conditional logic.<p>
 *  
 * Methods that accept model state are purely for optimizatio - prevent lookup
 * of modelstate if it has already been retrieved from world.  No need to implement
 * these if the test does not use model state.<p>
 * 
 * See NeighborBlocks for example of usage.
 */
public interface IBlockTest
{
	public boolean testBlock(EnumFacing face, ISuperBlockAccess world, IBlockState ibs, BlockPos pos);
    public boolean testBlock(BlockCorner corner, ISuperBlockAccess world, IBlockState ibs, BlockPos pos);
    public boolean testBlock(FarCorner corner, ISuperBlockAccess world, IBlockState ibs, BlockPos pos);

	public default boolean wantsModelState() { return false; }
	
	public default boolean testBlock(EnumFacing face, ISuperBlockAccess world, IBlockState ibs, BlockPos pos, ISuperModelState modelState)
	{
	    return testBlock(face, world, ibs, pos);
	}
	
	public default boolean testBlock(BlockCorner corner, ISuperBlockAccess world, IBlockState ibs, BlockPos pos, ISuperModelState modelState)
    {
        return testBlock(corner, world, ibs, pos);
    }
	
	public default boolean testBlock(FarCorner corner, ISuperBlockAccess world, IBlockState ibs, BlockPos pos, ISuperModelState modelState)
    {
        return testBlock(corner, world, ibs, pos);
    }
	
	public default ISuperModelState getExtraState() { return null; }
}