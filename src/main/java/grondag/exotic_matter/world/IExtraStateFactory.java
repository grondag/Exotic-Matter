package grondag.exotic_matter.world;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public interface IExtraStateFactory<T>
{
    public static final IExtraStateFactory<?> NONE = new IExtraStateFactory<Void>() 
    {
        @Override
        public Void get(IBlockAccess worldIn, BlockPos pos, IBlockState state)
        {
            return null;
        }
    };
            
    public T get(IBlockAccess worldIn, BlockPos pos, IBlockState state);
}
