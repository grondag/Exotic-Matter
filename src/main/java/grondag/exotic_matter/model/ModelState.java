package grondag.exotic_matter.model;

import static grondag.exotic_matter.model.ModelStateData.STATE_ENUM_RENDER_PASS_SET;
import static grondag.exotic_matter.model.ModelStateData.STATE_FLAG_HAS_AXIS;
import static grondag.exotic_matter.model.ModelStateData.STATE_FLAG_HAS_AXIS_ORIENTATION;
import static grondag.exotic_matter.model.ModelStateData.STATE_FLAG_HAS_AXIS_ROTATION;
import static grondag.exotic_matter.model.ModelStateData.STATE_FLAG_HAS_TRANSLUCENT_GEOMETRY;
import static grondag.exotic_matter.model.ModelStateData.STATE_FLAG_NEEDS_CORNER_JOIN;
import static grondag.exotic_matter.model.ModelStateData.STATE_FLAG_NEEDS_MASONRY_JOIN;
import static grondag.exotic_matter.model.ModelStateData.STATE_FLAG_NEEDS_POS;
import static grondag.exotic_matter.model.ModelStateData.STATE_FLAG_NEEDS_SIMPLE_JOIN;
import static grondag.exotic_matter.model.ModelStateData.STATE_FLAG_NEEDS_SPECIES;
import static grondag.exotic_matter.model.ModelStateData.STATE_FLAG_NEEDS_TEXTURE_ROTATION;
import static grondag.exotic_matter.model.ModelStateData.TEST_GETTER_STATIC;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4d;
import javax.vecmath.Matrix4f;

import grondag.exotic_matter.ConfigXM;
import grondag.exotic_matter.ExoticMatter;
import grondag.exotic_matter.render.RenderPass;
import grondag.exotic_matter.render.SideShape;
import grondag.exotic_matter.serialization.NBTDictionary;
import grondag.exotic_matter.varia.SuperBlockMasonryMatch;
import grondag.exotic_matter.varia.Useful;
import grondag.exotic_matter.world.CornerJoinBlockState;
import grondag.exotic_matter.world.CornerJoinBlockStateSelector;
import grondag.exotic_matter.world.NeighborBlocks;
import grondag.exotic_matter.world.Rotation;
import grondag.exotic_matter.world.SimpleJoin;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class ModelState implements ISuperModelState
{
    private static final String NBT_MODEL_BITS = NBTDictionary.claim("modelState");
    private static final String NBT_SHAPE = NBTDictionary.claim("texture");
    
    private boolean isStatic;
    private long bits0;
    private long bits1;
    private long bits2;
    private long bits3;

    private int hashCode = -1;

    /** contains indicators derived from shape and painters */
    private int stateFlags;

    public ModelState() { }

    public ModelState(int[] bits)
    {
        this.deserializeFromInts(bits);
    }
    
    public ModelState(long b0, long b1, long b2, long b3)
    {
        bits0 = b0;
        bits1 = b1;
        bits2 = b2;
        bits3 = b3;
    }

    @Override
    public ModelState clone()
    {
        return new ModelState(bits0, bits1, bits2, bits3);
    }

    @Override
    public int[] serializeToInts() 
    {
        int[] result = new int[8];
        result[0] = (int) (bits0 >> 32);
        result[1] = (int) (bits0);

        result[2] = (int) (bits1 >> 32);
        result[3] = (int) (bits1);

        result[4] = (int) (this.isStatic ? (bits2 >> 32) | Useful.INT_SIGN_BIT : (bits2 >> 32));
        result[5] = (int) (bits2);

        result[6] = (int) (bits3 >> 32);
        result[7] = (int) (bits3);
        return result;
    }
    
    /**
     * Note does not reset state flag - do that if calling on an existing instance.
     */
    private void deserializeFromInts(int [] bits)
    {
        // sign on third long word is used to store static indicator
        this.isStatic = (Useful.INT_SIGN_BIT & bits[4]) == Useful.INT_SIGN_BIT;

        this.bits0 = ((long)bits[0]) << 32 | (bits[1] & 0xffffffffL);
        this.bits1 = ((long)bits[2]) << 32 | (bits[3] & 0xffffffffL);
        this.bits2 = ((long)(Useful.INT_SIGN_BIT_INVERSE & bits[4])) << 32 | (bits[5] & 0xffffffffL);
        this.bits3 = ((long)bits[6]) << 32 | (bits[7] & 0xffffffffL);    
    }

    @Override
    public long getBits0() {return this.bits0;}

    @Override
    public long getBits1() {return this.bits1;}

    @Override
    public long getBits2() {return this.bits2;}

    @Override
    public long getBits3() {return this.bits3;}

    private void populateStateFlagsIfNeeded()
    {
        if(this.stateFlags == 0)
        {
            this.stateFlags = ModelStateFlagHelper.getFlags(this);
        }
    }

    private void clearStateFlags()
    {
        if(this.stateFlags != 0) 
        {
            this.stateFlags  = 0;
        }
    }

    @Override
    public boolean isStatic() { return this.isStatic; }

    @Override
    public void setStatic(boolean isStatic) { this.isStatic = isStatic; }

    /**
     * Does NOT consider isStatic in comparison. <br><br>
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj)
    {
        if(this == obj) return true;

        if(obj instanceof ModelState)
        {
            ModelState other = (ModelState)obj;
            return this.bits0 == other.bits0
                    && this.bits1 == other.bits1
                    && this.bits2 == other.bits2
                    && this.bits3 == other.bits3;
        }

        return false;
    }

    @Override
    public boolean equalsIncludeStatic(Object obj)
    {
        if(this == obj) return true;

        if(obj instanceof ModelState)
        {
            ModelState other = (ModelState)obj;
            return this.bits0 == other.bits0
                    && this.bits1 == other.bits1
                    && this.bits2 == other.bits2
                    && this.bits3 == other.bits3
                    && this.isStatic == other.isStatic;
        }

        return false;
    }

    private void invalidateHashCode()
    {
        if(this.hashCode != -1) this.hashCode = -1;
    }

    @Override
    public int hashCode()
    {
        if(hashCode == -1)
        {
            hashCode = (int) Useful.longHash(this.bits0 ^ this.bits1 ^ this.bits2 ^ this.bits3);
        }
        return hashCode;
    }

    @Override
    public ISuperModelState refreshFromWorld(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        //            Output.getLog().info("ModelState.refreshFromWorld static=" + this.isStatic + " @" + pos.toString());
        if(this.isStatic) return this;

        populateStateFlagsIfNeeded();

        if(state.getBlock() instanceof ISuperBlock)
        {
            this.setMetaData(state.getValue(ISuperBlock.META));
        }
        else
        {
            // prevent strangeness - shouldn't get called by non-superblock but modded MC is crazy biz
            return this;
        }
        
        switch(this.getShape().meshFactory().stateFormat)
        {
        case BLOCK:

            if((stateFlags & STATE_FLAG_NEEDS_POS) == STATE_FLAG_NEEDS_POS) refreshBlockPosFromWorld(pos, 255);

            long b3 = bits3;

            NeighborBlocks<ISuperModelState> neighbors = null;

            if((STATE_FLAG_NEEDS_CORNER_JOIN & stateFlags) == STATE_FLAG_NEEDS_CORNER_JOIN)
            {
                neighbors = new NeighborBlocks<>(world, pos, TEST_GETTER_STATIC);
                NeighborBlocks<ISuperModelState>.NeighborTestResults tests = neighbors.getNeighborTestResults(((ISuperBlock)state.getBlock()).blockJoinTest(world, state, pos, this));
                b3 = ModelStateData.P3B_BLOCK_JOIN.setValue(CornerJoinBlockStateSelector.findIndex(tests), b3);
            }
            else if ((STATE_FLAG_NEEDS_SIMPLE_JOIN & stateFlags) == STATE_FLAG_NEEDS_SIMPLE_JOIN)
            {
                neighbors = new NeighborBlocks<>(world, pos, TEST_GETTER_STATIC);
                NeighborBlocks<ISuperModelState>.NeighborTestResults tests = neighbors.getNeighborTestResults(((ISuperBlock)state.getBlock()).blockJoinTest(world, state, pos, this));
                b3 = ModelStateData.P3B_BLOCK_JOIN.setValue(SimpleJoin.getIndex(tests), b3);
            }

            if((STATE_FLAG_NEEDS_MASONRY_JOIN & stateFlags) == STATE_FLAG_NEEDS_MASONRY_JOIN)
            {
                if(neighbors == null) neighbors = new NeighborBlocks<>(world, pos, TEST_GETTER_STATIC);
                NeighborBlocks<ISuperModelState>.NeighborTestResults masonryTests = neighbors.getNeighborTestResults(new SuperBlockMasonryMatch((ISuperBlock) state.getBlock(), this.getSpecies(), pos));
                b3 = ModelStateData.P3B_MASONRY_JOIN.setValue(SimpleJoin.getIndex(masonryTests), b3);
            }

            bits3 = b3;

            break;

        case FLOW:
            // terrain blocks need larger position space to drive texture randomization because doesn't have per-block rotation or version
            if((stateFlags & STATE_FLAG_NEEDS_POS) == STATE_FLAG_NEEDS_POS) refreshBlockPosFromWorld(pos, 255);

            bits3 = ModelStateData.P3F_FLOW_JOIN.setValue(TerrainState.getBitsFromWorldStatically(this, (ISuperBlock)state.getBlock(), state, world, pos), bits3);
            break;

        case MULTIBLOCK:
            break;

        default:
            break;

        }

        this.invalidateHashCode();

        return this;
    }

    /** 
     * Saves world block pos relative to cube boundary specified by mask.
     * Used by BigTex surface painting for texture randomization on non-multiblock shapes.
     */
    private void refreshBlockPosFromWorld(BlockPos pos, int mask)
    {
        long b2 = bits2;
        b2 = ModelStateData.P2_POS_X.setValue((pos.getX() & mask), b2);
        b2 = ModelStateData.P2_POS_Y.setValue((pos.getY() & mask), b2);
        b2 = ModelStateData.P2_POS_Z.setValue((pos.getZ() & mask), b2);
        bits2 = b2;
    }

    ////////////////////////////////////////////////////
    //  PACKER 0 ATTRIBUTES (NOT SHAPE-DEPENDENT)
    ////////////////////////////////////////////////////

    @Override
    public ModelShape<?> getShape()
    {
        return ModelShape.get(ModelStateData.P0_SHAPE.getValue(bits0));
    }

    @Override
    public void setShape(ModelShape<?> shape)
    {
        if(shape.ordinal() != ModelStateData.P0_SHAPE.getValue(bits0))
        {
            bits0 = ModelStateData.P0_SHAPE.setValue(shape.ordinal(), bits0);
            bits2 = ModelStateData.P2_STATIC_SHAPE_BITS.setValue(shape.meshFactory().defaultShapeStateBits, bits2);
            invalidateHashCode();
            clearStateFlags();
        }
    }

    @Override
    public ColorMap getColorMap(PaintLayer layer)
    {
        return BlockColorMapProvider.INSTANCE.getColorMap(ModelStateData.P0_PAINT_COLOR[layer.dynamicIndex].getValue(bits0));
    }

    @Override
    public void setColorMap(PaintLayer layer, ColorMap map)
    {
        bits0 = ModelStateData.P0_PAINT_COLOR[layer.dynamicIndex].setValue(map.ordinal, bits0);
        invalidateHashCode();
    }

    @Override
    public BlockOrientationType orientationType()
    { 
        return getShape().meshFactory().orientationType(this);
    } 
    
    @Override
    public EnumFacing.Axis getAxis()
    {
        return ModelStateData.P0_AXIS.getValue(bits0);
    }

    @Override
    public void setAxis(EnumFacing.Axis axis)
    {
        bits0 = ModelStateData.P0_AXIS.setValue(axis, bits0);
        invalidateHashCode();
    }

    @Override
    public boolean isAxisInverted()
    {
        return ModelStateData.P0_AXIS_INVERTED.getValue(bits0);
    }

    @Override
    public void setAxisInverted(boolean isInverted)
    {
        bits0 = ModelStateData.P0_AXIS_INVERTED.setValue(isInverted, bits0);
        invalidateHashCode();
    }

    @Override
    public boolean isTranslucent(PaintLayer layer)
    {
        return ModelStateData.P0_IS_TRANSLUCENT[layer.dynamicIndex].getValue(bits0);
    }

    @Override
    public void setTranslucent(PaintLayer layer, boolean isTranslucent)
    {
        bits0 =  ModelStateData.P0_IS_TRANSLUCENT[layer.dynamicIndex].setValue(isTranslucent, bits0);
        clearStateFlags();
        invalidateHashCode();
    }

    @Override
    public boolean isMiddleLayerEnabled()
    {
        return this.getTexture(PaintLayer.MIDDLE) != TexturePaletteRegistry.NONE;
    }

    @Override
    public void setMiddleLayerEnabled(boolean isEnabled)
    {
        if(isEnabled && this.getTexture(PaintLayer.MIDDLE) == TexturePaletteRegistry.NONE)
        {
            this.setTexture(PaintLayer.MIDDLE, grondag.exotic_matter.init.ModTextures.BLOCK_NOISE_STRONG);
        }
        else if(!isEnabled && this.getTexture(PaintLayer.MIDDLE) != TexturePaletteRegistry.NONE)
        {
            this.setTexture(PaintLayer.MIDDLE, TexturePaletteRegistry.NONE);
        }
    }

    @Override
    public boolean isOuterLayerEnabled()
    {
        return this.getTexture(PaintLayer.OUTER) != TexturePaletteRegistry.NONE;
    }

    @Override
    public void setOuterLayerEnabled(boolean isEnabled)
    {
        if(isEnabled && this.getTexture(PaintLayer.OUTER) == TexturePaletteRegistry.NONE)
        {
            this.setTexture(PaintLayer.OUTER, grondag.exotic_matter.init.ModTextures.BLOCK_NOISE_STRONG);
        }
        else if(!isEnabled && this.getTexture(PaintLayer.OUTER) != TexturePaletteRegistry.NONE)
        {
            this.setTexture(PaintLayer.OUTER, TexturePaletteRegistry.NONE);
        }
    }

    @Override
    public Translucency getTranslucency()
    {
        return ModelStateData.P0_TRANSLUCENCY.getValue(bits0);
    }

    @Override
    public void setTranslucency(Translucency translucency)
    {
        bits0 = ModelStateData.P0_TRANSLUCENCY.setValue(translucency, bits0);
        invalidateHashCode();
    }

    ////////////////////////////////////////////////////
    //  PACKER 1 ATTRIBUTES (NOT SHAPE-DEPENDENT)
    ////////////////////////////////////////////////////

    @Override
    public ITexturePalette getTexture(PaintLayer layer)
    {
        return TexturePaletteRegistry.get(ModelStateData.P1_PAINT_TEXTURE[layer.ordinal()].getValue(bits1));
    }

    @Override
    public void setTexture(PaintLayer layer, ITexturePalette tex)
    {
        bits1 = ModelStateData.P1_PAINT_TEXTURE[layer.ordinal()].setValue(tex.ordinal(), bits1);
        invalidateHashCode();
        clearStateFlags();
    }

    @Override
    public boolean isFullBrightness(PaintLayer layer)
    {
        return ModelStateData.P1_PAINT_LIGHT[layer.dynamicIndex].getValue(bits1);
    }

    @Override
    public void setFullBrightness(PaintLayer layer, boolean isFullBrightness)
    {
        bits1 = ModelStateData.P1_PAINT_LIGHT[layer.dynamicIndex].setValue(isFullBrightness, bits1);
        clearStateFlags();
        invalidateHashCode();
    }

    ////////////////////////////////////////////////////
    //  PACKER 2 ATTRIBUTES  (NOT SHAPE-DEPENDENT)
    ////////////////////////////////////////////////////

    @Override
    public int getPosX()
    {
        return ModelStateData.P2_POS_X.getValue(bits2);
    }

    @Override
    public void setPosX(int index)
    {
        bits2 = ModelStateData.P2_POS_X.setValue(index, bits2);
        invalidateHashCode();
    }

    @Override
    public int getPosY()
    {
        return ModelStateData.P2_POS_Y.getValue(bits2);
    }

    @Override
    public void setPosY(int index)
    {
        bits2 = ModelStateData.P2_POS_Y.setValue(index, bits2);
        invalidateHashCode();
    }

    @Override
    public int getPosZ()
    {
        return ModelStateData.P2_POS_Z.getValue(bits2);
    }

    @Override
    public void setPosZ(int index)
    {
        bits2 = ModelStateData.P2_POS_Z.setValue(index, bits2);
        invalidateHashCode();
    }

    @Override
    public long getStaticShapeBits()
    {
        return ModelStateData.P2_STATIC_SHAPE_BITS.getValue(bits2);
    }

    @Override
    public void setStaticShapeBits(long bits)
    {
        bits2 = ModelStateData.P2_STATIC_SHAPE_BITS.setValue(bits, bits2);
        invalidateHashCode();
    }

    ////////////////////////////////////////////////////
    //  PACKER 3 ATTRIBUTES  (BLOCK FORMAT)
    ////////////////////////////////////////////////////

    @Override
    public int getSpecies()
    {
        this.populateStateFlagsIfNeeded();

        if(ConfigXM.BLOCKS.debugModelState && !this.hasSpecies())
            ExoticMatter.INSTANCE.warn("getSpecies on model state does not apply for shape");

        return this.hasSpecies() ? ModelStateData.P3B_SPECIES.getValue(bits3) : 0;
    }

    @Override
    public void setSpecies(int species)
    {
        this.populateStateFlagsIfNeeded();

        if(ConfigXM.BLOCKS.debugModelState && !this.hasSpecies())
            ExoticMatter.INSTANCE.warn("setSpecies on model state does not apply for shape");

        if(this.hasSpecies())
        {
            bits3 = ModelStateData.P3B_SPECIES.setValue(species, bits3);
            invalidateHashCode();
        }
    }

    @Override
    public CornerJoinBlockState getCornerJoin()
    {
        if(ConfigXM.BLOCKS.debugModelState)
        {
            populateStateFlagsIfNeeded();
            if((stateFlags & STATE_FLAG_NEEDS_CORNER_JOIN) == 0 || this.getShape().meshFactory().stateFormat != StateFormat.BLOCK)
                ExoticMatter.INSTANCE.warn("getCornerJoin on model state does not apply for shape");
        }

        return CornerJoinBlockStateSelector.getJoinState(MathHelper.clamp(ModelStateData.P3B_BLOCK_JOIN.getValue(bits3), 0, CornerJoinBlockStateSelector.BLOCK_JOIN_STATE_COUNT - 1));
    }

    @Override
    public void setCornerJoin(CornerJoinBlockState join)
    {
        if(ConfigXM.BLOCKS.debugModelState)
        {
            populateStateFlagsIfNeeded();
            if((stateFlags & STATE_FLAG_NEEDS_CORNER_JOIN) == 0 || this.getShape().meshFactory().stateFormat != StateFormat.BLOCK)
                ExoticMatter.INSTANCE.warn("setCornerJoin on model state does not apply for shape");
        }

        bits3 = ModelStateData.P3B_BLOCK_JOIN.setValue(join.getIndex(), bits3);
        invalidateHashCode();
    }

    @Override
    public SimpleJoin getSimpleJoin()
    {
        if(ConfigXM.BLOCKS.debugModelState && this.getShape().meshFactory().stateFormat != StateFormat.BLOCK)
            ExoticMatter.INSTANCE.warn("getSimpleJoin on model state does not apply for shape");


        // If this state is using corner join, join index is for a corner join
        // and so need to derive simple join from the corner join
        populateStateFlagsIfNeeded();
        return ((stateFlags & STATE_FLAG_NEEDS_CORNER_JOIN) == 0)
                ? new SimpleJoin(ModelStateData.P3B_BLOCK_JOIN.getValue(bits3))
                        : getCornerJoin().simpleJoin;
    }

    @Override
    public void setSimpleJoin(SimpleJoin join)
    {
        if(ConfigXM.BLOCKS.debugModelState)
        {
            if(this.getShape().meshFactory().stateFormat != StateFormat.BLOCK)
            {
                ExoticMatter.INSTANCE.warn("Ignored setSimpleJoin on model state that does not apply for shape");
                return;
            }

            populateStateFlagsIfNeeded();
            if((stateFlags & STATE_FLAG_NEEDS_CORNER_JOIN) != 0)
            {
                ExoticMatter.INSTANCE.warn("Ignored setSimpleJoin on model state that uses corner join instead");
                return;
            }
        }

        bits3 = ModelStateData.P3B_BLOCK_JOIN.setValue(join.getIndex(), bits3);
        invalidateHashCode();
    }

    @Override
    public SimpleJoin getMasonryJoin()
    {
        if(ConfigXM.BLOCKS.debugModelState && (this.getShape().meshFactory().stateFormat != StateFormat.BLOCK || (stateFlags & STATE_FLAG_NEEDS_CORNER_JOIN) == 0) || ((stateFlags & STATE_FLAG_NEEDS_MASONRY_JOIN) == 0))
            ExoticMatter.INSTANCE.warn("getMasonryJoin on model state does not apply for shape");

        populateStateFlagsIfNeeded();
        return new SimpleJoin(ModelStateData.P3B_MASONRY_JOIN.getValue(bits3));
    }

    @Override
    public void setMasonryJoin(SimpleJoin join)
    {
        if(ConfigXM.BLOCKS.debugModelState)
        {
            populateStateFlagsIfNeeded();
            if(this.getShape().meshFactory().stateFormat != StateFormat.BLOCK)
            {
                ExoticMatter.INSTANCE.warn("Ignored setMasonryJoin on model state that does not apply for shape");
                return;
            }

            if(((stateFlags & STATE_FLAG_NEEDS_CORNER_JOIN) == 0) || ((stateFlags & STATE_FLAG_NEEDS_MASONRY_JOIN) == 0))
            {
                ExoticMatter.INSTANCE.warn("Ignored setMasonryJoin on model state for which it does not apply");
                return;
            }
        }

        bits3 = ModelStateData.P3B_MASONRY_JOIN.setValue(join.getIndex(), bits3);
        invalidateHashCode();
    }

    @Override
    public Rotation getAxisRotation()
    {
        return ModelStateData.P3B_AXIS_ROTATION.getValue(bits3);
    }

    @Override
    public void setAxisRotation(Rotation rotation)
    {
        populateStateFlagsIfNeeded();
        if(this.getShape().meshFactory().stateFormat != StateFormat.BLOCK)
        {
            if(ConfigXM.BLOCKS.debugModelState) ExoticMatter.INSTANCE.warn("Ignored setAxisRotation on model state that does not apply for shape");
            return;
        }

        if((stateFlags & STATE_FLAG_HAS_AXIS_ROTATION) == 0)
        {
            if(ConfigXM.BLOCKS.debugModelState) ExoticMatter.INSTANCE.warn("Ignored setAxisRotation on model state for which it does not apply");
            return;
        }

        bits3 = ModelStateData.P3B_AXIS_ROTATION.setValue(rotation, bits3);
        invalidateHashCode();
    }

    ////////////////////////////////////////////////////
    //  PACKER 3 ATTRIBUTES  (MULTI-BLOCK FORMAT)
    ////////////////////////////////////////////////////

    @Override
    public long getMultiBlockBits()
    {
        if(ConfigXM.BLOCKS.debugModelState && this.getShape().meshFactory().stateFormat != StateFormat.MULTIBLOCK)
            ExoticMatter.INSTANCE.warn("getMultiBlockBits on model state does not apply for shape");

        return bits3;
    }

    @Override
    public void setMultiBlockBits(long bits)
    {
        if(ConfigXM.BLOCKS.debugModelState && this.getShape().meshFactory().stateFormat != StateFormat.MULTIBLOCK)
            ExoticMatter.INSTANCE.warn("setMultiBlockBits on model state does not apply for shape");

        bits3 = bits;
        invalidateHashCode();
    }

    ////////////////////////////////////////////////////
    //  PACKER 3 ATTRIBUTES  (FLOWING TERRAIN FORMAT)
    ////////////////////////////////////////////////////

    @Override
    public TerrainState getTerrainState()
    {
        if(ConfigXM.BLOCKS.debugModelState && this.getShape().meshFactory().stateFormat != StateFormat.FLOW)
            ExoticMatter.INSTANCE.warn("getTerrainState on model state does not apply for shape");

        return new TerrainState(ModelStateData.P3F_FLOW_JOIN.getValue(bits3));
    }

    @Override
    public void setTerrainState(TerrainState flowState)
    {
        if(ConfigXM.BLOCKS.debugModelState && this.getShape().meshFactory().stateFormat != StateFormat.FLOW)
            ExoticMatter.INSTANCE.warn("setTerrainState on model state does not apply for shape");

        bits3 = ModelStateData.P3F_FLOW_JOIN.setValue(flowState.getStateKey(), bits3);
        invalidateHashCode();
    }


    ////////////////////////////////////////////////////
    //  SHAPE/STATE-DEPENDENT CONVENIENCE METHODS
    ////////////////////////////////////////////////////

    @Override
    public RenderPass getRenderPass(PaintLayer layer)
    {
        boolean needsFlat = this.isFullBrightness(layer);
        
        switch(layer)
        {
        case BASE:
        case CUT:
        case LAMP:
        default:
            if(this.isTranslucent(layer))
            {
                return needsFlat ? RenderPass.TRANSLUCENT_FLAT : RenderPass.TRANSLUCENT_SHADED;
            }
            else
            {
                return needsFlat ? RenderPass.SOLID_FLAT : RenderPass.SOLID_SHADED;
            }
            
        case MIDDLE:
        case OUTER:
            return needsFlat ? RenderPass.TRANSLUCENT_FLAT : RenderPass.TRANSLUCENT_SHADED;
        
        }
    }

    @Override
    public RenderPassSet getRenderPassSet()
    {
       this.populateStateFlagsIfNeeded();
       return STATE_ENUM_RENDER_PASS_SET.getValue(this.stateFlags);
    }
    
    @Override
    public boolean hasAxis()
    {
        this.populateStateFlagsIfNeeded();
        return (this.stateFlags & STATE_FLAG_HAS_AXIS) == STATE_FLAG_HAS_AXIS;
    }

    @Override
    public boolean hasAxisOrientation()
    {
        this.populateStateFlagsIfNeeded();
        return (this.stateFlags & STATE_FLAG_HAS_AXIS_ORIENTATION) == STATE_FLAG_HAS_AXIS_ORIENTATION;
    }

    @Override
    public boolean hasLampSurface()
    {
        return this.getShape().meshFactory().hasLampSurface(this);
    }
    
    @Override
    public boolean hasTranslucentGeometry()
    {
        this.populateStateFlagsIfNeeded();
        return (this.stateFlags & STATE_FLAG_HAS_TRANSLUCENT_GEOMETRY) == STATE_FLAG_HAS_TRANSLUCENT_GEOMETRY;
    }

    @Override
    public boolean hasAxisRotation()
    {
        this.populateStateFlagsIfNeeded();
        return (this.stateFlags & STATE_FLAG_HAS_AXIS_ROTATION) == STATE_FLAG_HAS_AXIS_ROTATION;
    }
    
    @Override
    public boolean hasMasonryJoin()
    {
        this.populateStateFlagsIfNeeded();
        return (this.stateFlags & STATE_FLAG_NEEDS_MASONRY_JOIN) == STATE_FLAG_NEEDS_MASONRY_JOIN;
    }

    @Override
    public boolean hasTextureRotation()
    {
        this.populateStateFlagsIfNeeded();
        return (this.stateFlags & STATE_FLAG_NEEDS_TEXTURE_ROTATION) == STATE_FLAG_NEEDS_TEXTURE_ROTATION;
    }

    @Override
    public boolean hasSpecies()
    {
        this.populateStateFlagsIfNeeded();
        return((this.stateFlags & STATE_FLAG_NEEDS_SPECIES) == STATE_FLAG_NEEDS_SPECIES);
    }

    @Override
    public MetaUsage metaUsage()
    {
        return this.getShape().metaUsage();
    }

    @Override
    public boolean isAxisOrthogonalToPlacementFace() 
    {
        return this.getShape().meshFactory().isAxisOrthogonalToPlacementFace();
    }


    @Override
    public int getMetaData()
    {
        switch(this.metaUsage())
        {
        case SHAPE:
            return this.getShape().meshFactory().getMetaData(this);

        case SPECIES:
            return this.hasSpecies() ? this.getSpecies() : 0;

        case NONE:
        default:
            if(ConfigXM.BLOCKS.debugModelState) ExoticMatter.INSTANCE.warn("ModelState.getMetaData called for inappropriate shape");
            return 0;
        }            
    }

    @Override
    public void setMetaData(int meta)
    {
        switch(this.metaUsage())
        {
        case SHAPE:
            this.getShape().meshFactory().setMetaData(this, meta);
            break;

        case SPECIES:
            if(this.hasSpecies()) this.setSpecies(meta);
            break;

        case NONE:
        default:
            //NOOP
        }            
    }

    @Override
    public boolean isAdditive()
    {
        return this.getShape().meshFactory().isAdditive();
    }
    
    @Override
    public SideShape sideShape(EnumFacing side)
    {
        return getShape().meshFactory().sideShape(this, side);
    }

    @Override
    public boolean isCube()
    {
        return getShape().meshFactory().isCube(this);
    }

    @Override
    public boolean rotateBlock(IBlockState blockState, World world, BlockPos pos, EnumFacing axis, ISuperBlock block)
    {
        return getShape().meshFactory().rotateBlock(blockState, world, pos, axis, block, this);
    }

    @Override
    public int geometricSkyOcclusion()
    {
        return getShape().meshFactory().geometricSkyOcclusion(this);
    }

    @Override
    public boolean doShapeAndAppearanceMatch(ISuperModelState other)
    {
        return (this.bits0 & ModelStateData.P0_APPEARANCE_COMPARISON_MASK) == (other.getBits0() & ModelStateData.P0_APPEARANCE_COMPARISON_MASK)
                && (this.bits1 & ModelStateData.P1_APPEARANCE_COMPARISON_MASK) == (other.getBits1() & ModelStateData.P1_APPEARANCE_COMPARISON_MASK)
                && (this.bits2 & ModelStateData.P2_APPEARANCE_COMPARISON_MASK) == (other.getBits2() & ModelStateData.P2_APPEARANCE_COMPARISON_MASK);
    }

    @Override
    public boolean doesAppearanceMatch(ISuperModelState other)
    {
        return (this.bits0 & ModelStateData.P0_APPEARANCE_COMPARISON_MASK_NO_GEOMETRY) == (other.getBits0() & ModelStateData.P0_APPEARANCE_COMPARISON_MASK_NO_GEOMETRY)
                && (this.bits1 & ModelStateData.P1_APPEARANCE_COMPARISON_MASK) == (other.getBits1() & ModelStateData.P1_APPEARANCE_COMPARISON_MASK);
    }

    @Override
    public ISuperModelState geometricState()
    {
        this.populateStateFlagsIfNeeded();
        ModelState result = new ModelState();
        result.setShape(this.getShape());
        result.setStaticShapeBits(this.getStaticShapeBits());

        switch(this.getShape().meshFactory().stateFormat)
        {
        case BLOCK:
            if(this.hasAxis()) result.setAxis(this.getAxis());
            if(this.hasAxisOrientation()) result.setAxisInverted(this.isAxisInverted());
            if(this.hasAxisRotation()) result.setAxisRotation(this.getAxisRotation());
            if((this.getShape().meshFactory().getStateFlags(this) & STATE_FLAG_NEEDS_CORNER_JOIN) == STATE_FLAG_NEEDS_CORNER_JOIN)
            {
                result.setCornerJoin(this.getCornerJoin());
            }
            else if((this.getShape().meshFactory().getStateFlags(this) & STATE_FLAG_NEEDS_SIMPLE_JOIN) == STATE_FLAG_NEEDS_SIMPLE_JOIN)  
            { 
                result.setSimpleJoin(this.getSimpleJoin());
            }
            break;

        case FLOW:
        case MULTIBLOCK:
            result.bits3 = this.bits3;
            break;

        default:
            break;

        }
        return result;
    }

    @Override
    public List<AxisAlignedBB> collisionBoxes(BlockPos offset)
    {
        return this.getShape().meshFactory().collisionHandler().getCollisionBoxes(this, offset);
    }

    public static @Nullable ModelState deserializeFromNBTIfPresent(NBTTagCompound tag)
    {
        if(tag != null && tag.hasKey(NBT_MODEL_BITS))
        {
            ModelState result = new ModelState();
            result.deserializeNBT(tag);
            return result;
        }
        return null;
    }
    
    @Override
    public EnumFacing rotateFace(EnumFacing face)
    {
        return Transform.rotateFace(this, face);
    }
    
    @Override
    public Matrix4f getMatrix4f()
    {
        return Transform.getMatrix4f(this);
    }
    
    @Override
    public Matrix4d getMatrix4d()
    {
        return new Matrix4d(this.getMatrix4f());
    }
    
    
    @Override
    public void deserializeNBT(NBTTagCompound tag)
    {
        int[] stateBits = tag.getIntArray(NBT_MODEL_BITS);
        if(stateBits == null || stateBits.length != 8)
        {
            ExoticMatter.INSTANCE.warn("Bad or missing data encounter during ModelState NBT deserialization.");
            return;
        }
        this.deserializeFromInts(stateBits);
        
        // shape is serialized by name because registered shapes can change if mods/config change
        ModelShape<?> shape = ModelShape.get(tag.getString(NBT_SHAPE));
        if(shape != null) this.setShape(shape);
        
        // textures serialized by name because registered textures can change if mods/config change
        Arrays.stream(PaintLayer.values()).forEach(l -> deserializeTexture(tag, l));
        
        this.clearStateFlags();
    }

    /**
     * Reads tag and applies to this model state if present.
     * If no tag, does nothing, leaving this instance unchanged.
     */
    private void deserializeTexture(NBTTagCompound tag, PaintLayer layer)
    {
        if(tag.hasKey(layer.tagName))
        {
            this.setTexture(layer, TexturePaletteRegistry.get(tag.getString(layer.tagName)));
        }
    }

    @Override
    public void serializeNBT(NBTTagCompound tag)
    {
        tag.setIntArray(NBT_MODEL_BITS, this.serializeToInts());
        
        // shape is serialized by name because registered shapes can change if mods/config change
        tag.setString(NBT_SHAPE, this.getShape().systemName());
        
        // textures serialized by name because registered textures can change if mods/config change
        Arrays.stream(PaintLayer.values()).forEach(l -> serializeTexture(tag, l));
    }
    
    /**
     * Saves tag if this model state has a non-zero texture in the given layer.
     * Otherwise does nothing.
     */
    private void serializeTexture(NBTTagCompound tag, PaintLayer layer)
    {
        ITexturePalette tex = this.getTexture(layer);
        if(tex != TexturePaletteRegistry.NONE)
        {
            tag.setString(layer.tagName, tex.systemName());
        }
    }

    @Override
    public void fromBytes(PacketBuffer pBuff)
    {
        this.bits0 = pBuff.readLong();
        this.bits1 = pBuff.readLong();
        this.bits2 = pBuff.readLong();
        this.bits3 = pBuff.readLong();
    }

    @Override
    public void toBytes(PacketBuffer pBuff)
    {
        pBuff.writeLong(this.bits0);
        pBuff.writeLong(this.bits1);
        pBuff.writeLong(this.bits2);
        pBuff.writeLong(this.bits3);
    }
}