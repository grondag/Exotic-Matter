package grondag.exotic_matter.model.state;


import javax.annotation.Nullable;

import grondag.exotic_matter.block.ISuperBlock;
import grondag.exotic_matter.model.color.BlockColorMapProvider;
import grondag.exotic_matter.model.color.Translucency;
import grondag.exotic_matter.model.mesh.ModelShape;
import grondag.exotic_matter.model.painting.PaintLayer;
import grondag.exotic_matter.model.texture.TexturePaletteRegistry;
import grondag.exotic_matter.terrain.TerrainState;
import grondag.exotic_matter.varia.BitPacker;
import grondag.exotic_matter.world.CornerJoinBlockStateSelector;
import grondag.exotic_matter.world.IExtraStateFactory;
import grondag.exotic_matter.world.Rotation;
import grondag.exotic_matter.world.SimpleJoin;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class ModelStateData
{
    public static final BitPacker<ModelState> PACKER_0 = new BitPacker<ModelState>(m-> m.bits0, (m, b) -> m.bits0 = b);
    public static final BitPacker<ModelState>.IntElement P0_SHAPE = PACKER_0.createIntElement(ModelShape.MAX_SHAPES);
    @SuppressWarnings("unchecked")
    public static final BitPacker<ModelState>.IntElement[] P0_PAINT_COLOR = (BitPacker<ModelState>.IntElement[]) new BitPacker<?>.IntElement[PaintLayer.DYNAMIC_SIZE];
    public static final BitPacker<ModelState>.BooleanElement P0_AXIS_INVERTED = PACKER_0.createBooleanElement();
    public static final BitPacker<ModelState>.EnumElement<EnumFacing.Axis> P0_AXIS = PACKER_0.createEnumElement(EnumFacing.Axis.class);
    @SuppressWarnings("unchecked")
    public static final BitPacker<ModelState>.BooleanElement[] P0_IS_TRANSLUCENT = (BitPacker<ModelState>.BooleanElement[]) new BitPacker<?>.BooleanElement[PaintLayer.DYNAMIC_SIZE];
    public static final BitPacker<ModelState>.EnumElement<Translucency> P0_TRANSLUCENCY = PACKER_0.createEnumElement(Translucency.class);

    public static final BitPacker<ModelState> PACKER_1 = new BitPacker<ModelState>(m-> m.bits1, (m, b) -> m.bits1 = b);
    @SuppressWarnings("unchecked")
    public static final BitPacker<ModelState>.IntElement[] P1_PAINT_TEXTURE = (BitPacker<ModelState>.IntElement[]) new BitPacker<?>.IntElement[PaintLayer.STATIC_SIZE];
    @SuppressWarnings("unchecked")
    public static final BitPacker<ModelState>.BooleanElement[] P1_PAINT_LIGHT= (BitPacker<ModelState>.BooleanElement[]) new BitPacker<?>.BooleanElement[PaintLayer.DYNAMIC_SIZE];

    /** note that sign bit on packer 2 is reserved to persist static state during serialization */ 
    public static final BitPacker<ModelState> PACKER_2 = new BitPacker<ModelState>(m-> m.bits2, (m, b) -> m.bits2 = b);
    public static final BitPacker<ModelState>.IntElement P2_POS_X = PACKER_2.createIntElement(256);
    public static final BitPacker<ModelState>.IntElement P2_POS_Y = PACKER_2.createIntElement(256);
    public static final BitPacker<ModelState>.IntElement P2_POS_Z = PACKER_2.createIntElement(256);
    /** value semantics are owned by consumer - only constraints are size (39 bits) and does not update from world */
    public static final BitPacker<ModelState>.LongElement P2_STATIC_SHAPE_BITS = PACKER_2.createLongElement(1L << 39);

    public static final BitPacker<ModelState> PACKER_3_BLOCK = new BitPacker<ModelState>(m-> m.bits3, (m, b) -> m.bits3 = b);
    public static final BitPacker<ModelState>.IntElement P3B_SPECIES = PACKER_3_BLOCK.createIntElement(16);
    public static final BitPacker<ModelState>.IntElement P3B_BLOCK_JOIN = PACKER_3_BLOCK.createIntElement(CornerJoinBlockStateSelector.BLOCK_JOIN_STATE_COUNT);
    public static final BitPacker<ModelState>.IntElement P3B_MASONRY_JOIN = PACKER_3_BLOCK.createIntElement(SimpleJoin.STATE_COUNT);
    public static final BitPacker<ModelState>.EnumElement<Rotation> P3B_AXIS_ROTATION = PACKER_3_BLOCK.createEnumElement(Rotation.class);

    public static final BitPacker<ModelState> PACKER_3_FLOW = new BitPacker<ModelState>(m-> m.bits3, (m, b) -> m.bits3 = b);
    public static final BitPacker<ModelState>.LongElement P3F_FLOW_JOIN = PACKER_3_FLOW.createLongElement(TerrainState.STATE_BIT_MASK + 1);

    public static final BitPacker<ModelState> PACKER_3_MULTIBLOCK = new BitPacker<ModelState>(m-> m.bits3, (m, b) -> m.bits3 = b);

    /** used to compare states quickly for border joins  */
    public static final long P0_APPEARANCE_COMPARISON_MASK;
    public static final long P1_APPEARANCE_COMPARISON_MASK;
    public static final long P2_APPEARANCE_COMPARISON_MASK;   

    /** used to compare states quickly for appearance match */
    public static final long P0_APPEARANCE_COMPARISON_MASK_NO_GEOMETRY;

    static
    {
        long borderMask0 = 0;
        long borderMask1 = 0;
        for(int i = 0; i < PaintLayer.STATIC_SIZE; i++)
        {
            P1_PAINT_TEXTURE[i] = PACKER_1.createIntElement(TexturePaletteRegistry.MAX_PALETTES);
        }

        for(int i = 0; i < PaintLayer.DYNAMIC_SIZE; i++)
        {
            P0_PAINT_COLOR[i] = PACKER_0.createIntElement(BlockColorMapProvider.INSTANCE.getColorMapCount()); 
            P0_IS_TRANSLUCENT[i] = PACKER_0.createBooleanElement();
            P1_PAINT_LIGHT[i] = PACKER_1.createBooleanElement(); 

            borderMask0 |= P0_PAINT_COLOR[i].comparisonMask();
            borderMask0 |= P0_IS_TRANSLUCENT[i].comparisonMask();
            borderMask1 |= P1_PAINT_TEXTURE[i].comparisonMask();
            borderMask1 |= P1_PAINT_LIGHT[i].comparisonMask();
        }

        P0_APPEARANCE_COMPARISON_MASK_NO_GEOMETRY = borderMask0
                | P0_TRANSLUCENCY.comparisonMask();

        P0_APPEARANCE_COMPARISON_MASK = P0_APPEARANCE_COMPARISON_MASK_NO_GEOMETRY
                | P0_SHAPE.comparisonMask() 
                | P0_AXIS.comparisonMask()
                | P0_AXIS_INVERTED.comparisonMask();

        P1_APPEARANCE_COMPARISON_MASK = borderMask1;
        P2_APPEARANCE_COMPARISON_MASK = P2_STATIC_SHAPE_BITS.comparisonMask();
    }

    /**
     * Use this as factory for model state block tests that DON'T need to refresh from world.
     */
    public static final IExtraStateFactory TEST_GETTER_STATIC = new IExtraStateFactory()
    {
        @Override
        public @Nullable ISuperModelState get(IBlockAccess worldIn, BlockPos pos, IBlockState state)
        {
            Block block = state.getBlock();
            return (block instanceof ISuperBlock) 
                    ? ((ISuperBlock)block).getModelStateAssumeStateIsCurrent(state, worldIn, pos, false)
                    : null;
        }
    };
    
    /**
     * Use this as factory for model state block tests that DO need to refresh from world.
     */
    public static final IExtraStateFactory TEST_GETTER_DYNAMIC = new IExtraStateFactory()
    {
        @Override
        public @Nullable ISuperModelState get(IBlockAccess worldIn, BlockPos pos, IBlockState state)
        {
            Block block = state.getBlock();
            return (block instanceof ISuperBlock) 
                    ? ((ISuperBlock)block).getModelStateAssumeStateIsCurrent(state, worldIn, pos, true)
                    : null;
        }
    };
    
    public static final BitPacker<ModelState> STATE_PACKER = new BitPacker<ModelState>(m -> m.stateFlags, (m, b) -> m.stateFlags = (int)b);
    
    /**
     * For readability.
     */
    public static final int STATE_FLAG_NONE = 0;

    
    /** see {@link #STATE_FLAG_IS_POPULATED} */
    public static final BitPacker<ModelState>.BooleanElement STATE_BIT_IS_POPULATED = STATE_PACKER.createBooleanElement();
    /* 
     * Enables lazy derivation - set after derivation is complete.
     * NB - check logic assumes that ALL bits are zero for simplicity.
     */
    public static final int STATE_FLAG_IS_POPULATED = (int) STATE_BIT_IS_POPULATED.comparisonMask();

    
    /** see {@link #STATE_FLAG_NEEDS_CORNER_JOIN} */
    public static final BitPacker<ModelState>.BooleanElement STATE_BIT_NEEDS_CORNER_JOIN = STATE_PACKER.createBooleanElement();
    /** 
     * Applies to block-type states.  
     * True if is a block type state and requires full join state.
     */
    public static final int STATE_FLAG_NEEDS_CORNER_JOIN = (int) STATE_BIT_NEEDS_CORNER_JOIN.comparisonMask();

    
    /** see {@link #STATE_FLAG_NEEDS_SIMPLE_JOIN} */
    public static final BitPacker<ModelState>.BooleanElement STATE_BIT_NEEDS_SIMPLE_JOIN = STATE_PACKER.createBooleanElement();
    /** 
     * Applies to block-type states.  
     * True if is a block type state and requires full join state.
     */
    public static final int STATE_FLAG_NEEDS_SIMPLE_JOIN = (int) STATE_BIT_NEEDS_SIMPLE_JOIN.comparisonMask();

    
    /** see {@link #STATE_FLAG_NEEDS_MASONRY_JOIN} */
    public static final BitPacker<ModelState>.BooleanElement STATE_BIT_NEEDS_MASONRY_JOIN = STATE_PACKER.createBooleanElement();
    
    /** 
     * Applies to block-type states.  
     * True if is a block type state and requires masonry join info.
     */
    public static final int STATE_FLAG_NEEDS_MASONRY_JOIN = (int) STATE_BIT_NEEDS_MASONRY_JOIN.comparisonMask();


    /** see {@link #STATE_FLAG_NEEDS_POS} */
    public static final BitPacker<ModelState>.BooleanElement STATE_BIT_NEEDS_POS = STATE_PACKER.createBooleanElement();
    /** 
     * True if position (big-tex) world state is needed. Applies for block and flow state formats.
     */
    public static final int STATE_FLAG_NEEDS_POS = (int) STATE_BIT_NEEDS_POS.comparisonMask();

    
    /** see {@link #STATE_FLAG_NEEDS_SPECIES} */
    public static final BitPacker<ModelState>.BooleanElement STATE_BIT_NEEDS_SPECIES = STATE_PACKER.createBooleanElement();
    public static final int STATE_FLAG_NEEDS_SPECIES = (int) STATE_BIT_NEEDS_SPECIES.comparisonMask();

    
    /** see {@link #STATE_FLAG_HAS_AXIS} */
    public static final BitPacker<ModelState>.BooleanElement STATE_BIT_HAS_AXIS = STATE_PACKER.createBooleanElement();
    public static final int STATE_FLAG_HAS_AXIS = (int) STATE_BIT_HAS_AXIS.comparisonMask();

    
    /** see {@link #STATE_FLAG_NEEDS_TEXTURE_ROTATION} */
    public static final BitPacker<ModelState>.BooleanElement STATE_BIT_NEEDS_TEXTURE_ROTATION = STATE_PACKER.createBooleanElement();
    public static final int STATE_FLAG_NEEDS_TEXTURE_ROTATION = (int) STATE_BIT_NEEDS_TEXTURE_ROTATION.comparisonMask();

    
    /** see {@link #STATE_FLAG_HAS_AXIS_ORIENTATION} */
    public static final BitPacker<ModelState>.BooleanElement STATE_BIT_HAS_AXIS_ORIENTATION = STATE_PACKER.createBooleanElement();
    public static final int STATE_FLAG_HAS_AXIS_ORIENTATION = (int) STATE_BIT_HAS_AXIS_ORIENTATION.comparisonMask();

    /** see {@link #STATE_FLAG_HAS_AXIS_ROTATION} */
    public static final BitPacker<ModelState>.BooleanElement STATE_BIT_HAS_AXIS_ROTATION = STATE_PACKER.createBooleanElement();
    /** Set if shape can be rotated around an axis. Only applies to block models; multiblock models manage this situationally. */
    public static final int STATE_FLAG_HAS_AXIS_ROTATION = (int) STATE_BIT_HAS_AXIS_ROTATION.comparisonMask();


    /** see {@link #STATE_FLAG_HAS_TRANSLUCENT_GEOMETRY} */
    public static final BitPacker<ModelState>.BooleanElement STATE_BIT_HAS_TRANSLUCENT_GEOMETRY = STATE_PACKER.createBooleanElement();
    /** Set if either Base/Cut or Lamp (if present) paint layers are translucent */
    public static final int STATE_FLAG_HAS_TRANSLUCENT_GEOMETRY = (int) STATE_BIT_HAS_TRANSLUCENT_GEOMETRY.comparisonMask();
    
    /** see {@link #STATE_FLAG_HAS_SOLID_RENDER} */
    public static final BitPacker<ModelState>.BooleanElement STATE_BIT_HAS_SOLID_RENDER = STATE_PACKER.createBooleanElement();
    /** True if any layer sould render in the solid block render layer */
    public static final int STATE_FLAG_HAS_SOLID_RENDER = (int) STATE_BIT_HAS_TRANSLUCENT_GEOMETRY.comparisonMask();
    
    /** see {@link #STATE_FLAG_HAS_TRANSLUCENT_RENDER} */
    public static final BitPacker<ModelState>.BooleanElement STATE_BIT_HAS_TRANSLUCENT_RENDER = STATE_PACKER.createBooleanElement();
    /** True if any layer should render in the translucent block render layer */
    public static final int STATE_FLAG_HAS_TRANSLUCENT_RENDER = (int) STATE_BIT_HAS_TRANSLUCENT_GEOMETRY.comparisonMask();
    
    /** use this to turn off flags that should not be used with non-block state formats */
    public static final int STATE_FLAG_DISABLE_BLOCK_ONLY = ~(
            STATE_FLAG_NEEDS_CORNER_JOIN | STATE_FLAG_NEEDS_SIMPLE_JOIN | STATE_FLAG_NEEDS_MASONRY_JOIN
            | STATE_FLAG_NEEDS_SPECIES | STATE_FLAG_NEEDS_TEXTURE_ROTATION);
    
    //hide constructor
    private ModelStateData()
    {
        super();
    }
}
