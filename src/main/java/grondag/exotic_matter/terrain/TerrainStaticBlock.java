package grondag.exotic_matter.terrain;

import java.util.List;

import javax.annotation.Nonnull;

import grondag.exotic_matter.ConfigXM;
import grondag.exotic_matter.block.SuperBlockStackHelper;
import grondag.exotic_matter.block.SuperStaticBlock;
import grondag.exotic_matter.model.BlockSubstance;
import grondag.exotic_matter.model.ISuperBlock;
import grondag.exotic_matter.model.ISuperModelState;
import grondag.exotic_matter.model.ModShapes;
import grondag.exotic_matter.model.TerrainBlockHelper;
import grondag.exotic_matter.model.TerrainBlockRegistry;
import grondag.exotic_matter.model.TerrainState;
import grondag.exotic_matter.varia.Useful;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TerrainStaticBlock extends SuperStaticBlock
{
    private final boolean isFiller;
    
    public TerrainStaticBlock(String blockName, BlockSubstance substance, ISuperModelState defaultModelState, boolean isFiller)
    {
        super(blockName, substance, defaultModelState);
        this.isFiller = isFiller;
        this.metaCount = this.isFiller ? 2 : TerrainState.BLOCK_LEVELS_INT;
        
        // make sure proper shape is set
        ISuperModelState modelState = defaultModelState.clone();
        modelState.setShape(this.isFiller ? ModShapes.TERRAIN_FILLER : ModShapes.TERRAIN_HEIGHT);
        this.defaultModelStateBits = modelState.serializeToInts();
    }
    
    /** 
     * This is an egregious hack to avoid performance hit of instanceof.
     * (Based on performance profile results.)
     * Returns true if this is a type of IFlowBlock
     */
    @Override
    public boolean isAssociatedBlock(Block other)
    {
        return other == TerrainBlockHelper.FLOW_BLOCK_INDICATOR || super.isAssociatedBlock(other);
    }

    @Override
    public boolean isFlowFiller()
    {
        return isFiller;
    }

    @Override
    public boolean isFlowHeight()
    {
        return !isFiller;
    }
  
    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
    {
        //see Config.render().enableFaceCullingOnFlowBlocks for explanation
        IBlockState neighborState = blockAccess.getBlockState(pos.offset(side));
        if(ConfigXM.RENDER.enableFaceCullingOnFlowBlocks && TerrainBlockHelper.isFlowBlock(neighborState.getBlock()))
        {
            int myOcclusionKey = this.getOcclusionKey(blockState, blockAccess, pos, side);
            int otherOcclusionKey = ((ISuperBlock)neighborState.getBlock()).getOcclusionKey(neighborState, blockAccess, pos.offset(side), side.getOpposite());
            return myOcclusionKey != otherOcclusionKey;
        }
        else
        {
            return !neighborState.doesSideBlockRendering(blockAccess, pos.offset(side), side.getOpposite());
        }
    }

    @Override
    protected List<ItemStack> createSubItems()
    {
        List<ItemStack> items = super.createSubItems();
        
        for(ItemStack stack : items)
        {
            int meta = stack.getMetadata();
            ISuperModelState modelState = SuperBlockStackHelper.getStackModelState(stack);
            int level = this.isFiller ? TerrainState.BLOCK_LEVELS_INT - 1 : TerrainState.BLOCK_LEVELS_INT - meta;
            int [] quadrants = new int[] {level, level, level, level};
            TerrainState flowState = new TerrainState(level, quadrants, quadrants, 0);
            modelState.setTerrainState(flowState);
            SuperBlockStackHelper.setStackModelState(stack, modelState);
        }
        return items;
    }

    @Override
    public int quantityDropped(IBlockAccess world, BlockPos pos, IBlockState state)
    {
        double volume = 0;
        ISuperModelState modelState = this.getModelStateAssumeStateIsStale(state, world, pos, true);
        for(AxisAlignedBB box : modelState.getShape().meshFactory().collisionHandler().getCollisionBoxes(modelState))
        {
            volume += Useful.volumeAABB(box);
        }

        return (int) Math.min(9, volume * 9);
    }
 
    @Override
    public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos)
    {
        return TerrainBlockHelper.isEmpty(worldIn.getBlockState(pos), worldIn, pos);
    }

    @Override
    public boolean isAir(@Nonnull IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos)
    {
        return TerrainBlockHelper.isEmpty(state, world, pos);
    }
    
    /**
     * Prevent neighboring dynamic blocks from updating geometry by making them static.
     */
    @Override
    public boolean removedByPlayer(@Nonnull IBlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull EntityPlayer player, boolean willHarvest)
    {
        TerrainDynamicBlock.freezeNeighbors(world, pos, state);
        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }
    
    /**
     * Prevent neighboring dynamic blocks from updating geometry by making them static.
     */
    @Override
    public void onBlockPlacedBy(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityLivingBase placer, @Nonnull ItemStack stack)
    {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        TerrainDynamicBlock.freezeNeighbors(worldIn, pos, state);
    }

    /**
     * Convert this block to a dynamic version of itself if one is known.
     */
    public void makeDynamic(IBlockState state, World world, BlockPos pos)
    {
        Block dynamicVersion = TerrainBlockRegistry.TERRAIN_STATE_REGISTRY.getDynamicBlock(this);
        if(dynamicVersion == null || state.getBlock() != this) return;

        world.setBlockState(pos, dynamicVersion.getDefaultState()
                .withProperty(ISuperBlock.META, state.getValue(ISuperBlock.META)), 3);
    }
    
    // setting to false drops AO light value
    @Override
    public boolean isFullCube(IBlockState state)
    {
        // don't have enough information without world access or extended state
        // to determine if is full cube.
        return false;    }

    @Override
    public boolean isOpaqueCube(IBlockState state)
    {
        // don't have enough information without world access or extended state
        // to determine if is full cube.
        return false;
    }
    
    @Override
    public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        return TerrainBlockHelper.shouldBeFullCube(state, world, pos);
    }
    
    @Override
    public boolean getUseNeighborBrightness(IBlockState state)
    {
        return true;
    }
}