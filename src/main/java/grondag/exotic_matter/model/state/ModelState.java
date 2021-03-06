package grondag.exotic_matter.model.state;

import static grondag.exotic_matter.model.state.ModelStateData.STATE_FLAG_HAS_AXIS;
import static grondag.exotic_matter.model.state.ModelStateData.STATE_FLAG_HAS_AXIS_ORIENTATION;
import static grondag.exotic_matter.model.state.ModelStateData.STATE_FLAG_HAS_AXIS_ROTATION;
import static grondag.exotic_matter.model.state.ModelStateData.STATE_FLAG_HAS_SOLID_RENDER;
import static grondag.exotic_matter.model.state.ModelStateData.STATE_FLAG_HAS_TRANSLUCENT_GEOMETRY;
import static grondag.exotic_matter.model.state.ModelStateData.STATE_FLAG_HAS_TRANSLUCENT_RENDER;
import static grondag.exotic_matter.model.state.ModelStateData.STATE_FLAG_NEEDS_CORNER_JOIN;
import static grondag.exotic_matter.model.state.ModelStateData.STATE_FLAG_NEEDS_MASONRY_JOIN;
import static grondag.exotic_matter.model.state.ModelStateData.STATE_FLAG_NEEDS_POS;
import static grondag.exotic_matter.model.state.ModelStateData.STATE_FLAG_NEEDS_SIMPLE_JOIN;
import static grondag.exotic_matter.model.state.ModelStateData.STATE_FLAG_NEEDS_SPECIES;
import static grondag.exotic_matter.model.state.ModelStateData.STATE_FLAG_NEEDS_TEXTURE_ROTATION;
import static grondag.exotic_matter.model.state.ModelStateData.TEST_GETTER_STATIC;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.Matrix4d;
import javax.vecmath.Matrix4f;

import grondag.exotic_matter.ConfigXM;
import grondag.exotic_matter.ExoticMatter;
import grondag.exotic_matter.block.ISuperBlock;
import grondag.exotic_matter.block.ISuperBlockAccess;
import grondag.exotic_matter.model.mesh.BlockOrientationType;
import grondag.exotic_matter.model.mesh.ModelShape;
import grondag.exotic_matter.model.mesh.ModelShapes;
import grondag.exotic_matter.model.painting.PaintLayer;
import grondag.exotic_matter.model.painting.VertexProcessor;
import grondag.exotic_matter.model.painting.VertexProcessors;
import grondag.exotic_matter.model.primitives.Transform;
import grondag.exotic_matter.model.render.RenderLayout;
import grondag.exotic_matter.model.render.RenderLayoutProducer;
import grondag.exotic_matter.model.texture.ITexturePalette;
import grondag.exotic_matter.model.texture.TexturePaletteRegistry;
import grondag.exotic_matter.model.varia.SideShape;
import grondag.exotic_matter.serialization.NBTDictionary;
import grondag.exotic_matter.terrain.TerrainState;
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
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class ModelState implements ISuperModelState
{
    private static final String NBT_MODEL_BITS = NBTDictionary.claim("modelState");
    private static final String NBT_SHAPE = NBTDictionary.claim("shape");
    /**
     * Stores string containing registry names of textures, vertex processors
     */
    private static final String NBT_LAYERS = NBTDictionary.claim("layers");
    
    /**
     * Removes model state from the tag if present.
     */
    public static final void clearNBTValues(@Nullable NBTTagCompound tag)
    {
        if(tag == null) return;
        tag.removeTag(NBT_MODEL_BITS);
        tag.removeTag(NBT_SHAPE);
        tag.removeTag(NBT_LAYERS);
    }
    
    private boolean isStatic = false;
    protected long coreBits;
    protected long shapeBits1;
    protected long shapeBits0;
    
    // default to white, full alpha
    protected long layerBitsBase = 0xFFFFFFFFL;
    protected long layerBitsLamp = 0xFFFFFFFFL;
    protected long layerBitsMiddle = 0xFFFFFFFFL;
    protected long layerBitsOuter = 0xFFFFFFFFL;
    protected long layerBitsCut = 0xFFFFFFFFL;

    private int hashCode = -1;

    /** contains indicators derived from shape and painters */
    protected int stateFlags;

    public ModelState() { }

    public ModelState(int[] bits)
    {
        this.deserializeFromInts(bits);
    }
    
    public ModelState(
            long coreBits, 
            long shapeBits0, 
            long shapeBits1, 
            long layerBitsBase,
            long layerBitsCut,
            long layerBitsLamp,
            long layerBitsMiddle,
            long layerBitsOuter)
    {
        this.coreBits = coreBits;
        this.shapeBits0 = shapeBits0;
        this.shapeBits1 = shapeBits1;
        this.layerBitsBase = layerBitsBase;
        this.layerBitsCut = layerBitsCut;
        this.layerBitsLamp = layerBitsLamp;
        this.layerBitsMiddle = layerBitsMiddle;
        this.layerBitsOuter = layerBitsOuter;
    }

    @Override
    public ModelState clone()
    {
        return new ModelState(coreBits, shapeBits0, shapeBits1, layerBitsBase, layerBitsCut, layerBitsLamp, layerBitsMiddle, layerBitsOuter);
    }

    @Override
    public final int[] serializeToInts() 
    {
        int[] result = new int[16];
        result[0] = (int) (this.isStatic ? (coreBits >> 32) | Useful.INT_SIGN_BIT : (coreBits >> 32));
        result[1] = (int) (coreBits);

        result[2] = (int) (shapeBits1 >> 32);
        result[3] = (int) (shapeBits1);

        result[4] = (int) (shapeBits0 >> 32);
        result[5] = (int) (shapeBits0);
        
        result[6] = (int) (layerBitsBase >> 32);
        result[7] = (int) (layerBitsBase);
        
        result[8] = (int) (layerBitsCut >> 32);
        result[9] = (int) (layerBitsCut);
        
        result[10] = (int) (layerBitsLamp >> 32);
        result[11] = (int) (layerBitsLamp);
        
        result[12] = (int) (layerBitsMiddle >> 32);
        result[13] = (int) (layerBitsMiddle);
        
        result[14] = (int) (layerBitsOuter >> 32);
        result[15] = (int) (layerBitsOuter);
        
        return result;
    }
    
    /**
     * Note does not reset state flag - do that if calling on an existing instance.
     */
    private void deserializeFromInts(int [] bits)
    {
        // sign on first long word is used to store static indicator
        this.isStatic = (Useful.INT_SIGN_BIT & bits[0]) == Useful.INT_SIGN_BIT;

        this.coreBits = ((long)(Useful.INT_SIGN_BIT_INVERSE & bits[0])) << 32 | (bits[1] & 0xffffffffL);
        this.shapeBits1 = ((long)bits[2]) << 32 | (bits[3] & 0xffffffffL);
        this.shapeBits0 = ((long)bits[4]) << 32 | (bits[5] & 0xffffffffL);
        
        this.layerBitsBase = ((long)bits[6]) << 32 | (bits[7] & 0xffffffffL); 
        this.layerBitsCut = ((long)bits[8]) << 32 | (bits[9] & 0xffffffffL); 
        this.layerBitsLamp = ((long)bits[10]) << 32 | (bits[11] & 0xffffffffL); 
        this.layerBitsMiddle = ((long)bits[12]) << 32 | (bits[13] & 0xffffffffL); 
        this.layerBitsOuter = ((long)bits[14]) << 32 | (bits[15] & 0xffffffffL); 
    }

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
    public boolean equals(@Nullable Object obj)
    {
        if(this == obj) return true;

        if(obj instanceof ModelState)
        {
            ModelState other = (ModelState)obj;
            return this.coreBits == other.coreBits
                    && this.shapeBits0 == other.shapeBits0
                    && this.shapeBits1 == other.shapeBits1
                    && this.layerBitsBase == other.layerBitsBase
                    && this.layerBitsCut == other.layerBitsCut
                    && this.layerBitsLamp == other.layerBitsLamp
                    && this.layerBitsMiddle == other.layerBitsMiddle
                    && this.layerBitsOuter == other.layerBitsOuter;
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
            return this.isStatic == other.isStatic
                    && this.coreBits == other.coreBits
                    && this.shapeBits0 == other.shapeBits0
                    && this.shapeBits1 == other.shapeBits1
                    && this.layerBitsBase == other.layerBitsBase
                    && this.layerBitsCut == other.layerBitsCut
                    && this.layerBitsLamp == other.layerBitsLamp
                    && this.layerBitsMiddle == other.layerBitsMiddle
                    && this.layerBitsOuter == other.layerBitsOuter;
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
            hashCode = (int) Useful.longHash(
                    this.coreBits ^ this.shapeBits0 ^ this.shapeBits1
                    ^ this.layerBitsBase ^ this.layerBitsCut ^ this.layerBitsLamp 
                    ^ this.layerBitsMiddle ^ this.layerBitsOuter);
        }
        return hashCode;
    }

    @SuppressWarnings("null")
    @Override
    public ISuperModelState refreshFromWorld(IBlockState state, ISuperBlockAccess world, BlockPos pos)
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

            NeighborBlocks neighbors = null;

            if((STATE_FLAG_NEEDS_CORNER_JOIN & stateFlags) == STATE_FLAG_NEEDS_CORNER_JOIN)
            {
                neighbors = new NeighborBlocks(world, pos, TEST_GETTER_STATIC);
                NeighborBlocks.NeighborTestResults tests = neighbors.getNeighborTestResults(((ISuperBlock)state.getBlock()).blockJoinTest(world, state, pos, this));
                ModelStateData.BLOCK_JOIN.setValue(CornerJoinBlockStateSelector.findIndex(tests), this);
            }
            else if ((STATE_FLAG_NEEDS_SIMPLE_JOIN & stateFlags) == STATE_FLAG_NEEDS_SIMPLE_JOIN)
            {
                neighbors = new NeighborBlocks(world, pos, TEST_GETTER_STATIC);
                NeighborBlocks.NeighborTestResults tests = neighbors.getNeighborTestResults(((ISuperBlock)state.getBlock()).blockJoinTest(world, state, pos, this));
                ModelStateData.BLOCK_JOIN.setValue(SimpleJoin.getIndex(tests), this);
            }

            if((STATE_FLAG_NEEDS_MASONRY_JOIN & stateFlags) == STATE_FLAG_NEEDS_MASONRY_JOIN)
            {
                if(neighbors == null) neighbors = new NeighborBlocks(world, pos, TEST_GETTER_STATIC);
                NeighborBlocks.NeighborTestResults masonryTests = neighbors.getNeighborTestResults(new SuperBlockMasonryMatch((ISuperBlock) state.getBlock(), this.getSpecies(), pos));
                ModelStateData.MASONRY_JOIN.setValue(SimpleJoin.getIndex(masonryTests), this);
            }

            break;

        case FLOW:
            // terrain blocks need larger position space to drive texture randomization because doesn't have per-block rotation or version
            if((stateFlags & STATE_FLAG_NEEDS_POS) == STATE_FLAG_NEEDS_POS) refreshBlockPosFromWorld(pos, 255);
            TerrainState.produceBitsFromWorldStatically((ISuperBlock)state.getBlock(), state, world, pos, (t, h) ->
            { 
                ModelStateData.FLOW_JOIN.setValue(t, this);
                ModelStateData.EXTRA_SHAPE_BITS.setValue(h, this);
                return null;
            });
            
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
        ModelStateData.POS_X.setValue((pos.getX() & mask), this);
        ModelStateData.POS_Y.setValue((pos.getY() & mask), this);
        ModelStateData.POS_Z.setValue((pos.getZ() & mask), this);
    }

    ////////////////////////////////////////////////////
    //  PACKER 0 ATTRIBUTES (NOT SHAPE-DEPENDENT)
    ////////////////////////////////////////////////////

    @Override
    public ModelShape<?> getShape()
    {
        return ModelShapes.get(ModelStateData.SHAPE.getValue(this));
    }

    @Override
    public void setShape(ModelShape<?> shape)
    {
        if(shape.ordinal() != ModelStateData.SHAPE.getValue(this))
        {
            ModelStateData.SHAPE.setValue(shape.ordinal(), this);
            ModelStateData.EXTRA_SHAPE_BITS.setValue(shape.meshFactory().defaultShapeStateBits, this);
            invalidateHashCode();
            clearStateFlags();
        }
    }

    @Override
    public final int getColorARGB(PaintLayer layer)
    {
        final int alpha = this.isTranslucent(layer)
                ? ModelStateData.PAINT_ALPHA[layer.ordinal()].getValue(this)
                : 0xFF;
        return (alpha << 24)  | ModelStateData.PAINT_COLOR[layer.ordinal()].getValue(this); 
    }

    @Override
    public final void setColorRGB(PaintLayer layer, int rgb)
    {
        ModelStateData.PAINT_COLOR[layer.ordinal()].setValue(rgb & 0xFFFFFF, this);
        invalidateHashCode();
    }

    @Override
    public final int getAlpha(PaintLayer layer)
    {
        return ModelStateData.PAINT_ALPHA[layer.ordinal()].getValue(this);
    }
    
    @Override
    public final void setAlpha(PaintLayer layer, int translucency)
    {
        ModelStateData.PAINT_ALPHA[layer.ordinal()].setValue(translucency & 0xFF, this);
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
        return ModelStateData.AXIS.getValue(this);
    }

    @Override
    public void setAxis(EnumFacing.Axis axis)
    {
        ModelStateData.AXIS.setValue(axis, this);
        invalidateHashCode();
    }

    @Override
    public boolean isAxisInverted()
    {
        return ModelStateData.AXIS_INVERTED.getValue(this);
    }

    @Override
    public void setAxisInverted(boolean isInverted)
    {
        ModelStateData.AXIS_INVERTED.setValue(isInverted, this);
        invalidateHashCode();
    }

    @Override
    public boolean isLayerEnabled(PaintLayer layer)
    {
        return this.getTexture(layer) != TexturePaletteRegistry.NONE;
    }

    @Override
    public void disableLayer(PaintLayer layer)
    {
        this.setTexture(layer, TexturePaletteRegistry.NONE);
    }

    @Override
    public boolean isTranslucent(PaintLayer layer)
    {
        return ModelStateData.PAINT_IS_TRANSLUCENT[layer.ordinal()].getValue(this);
    }

    @Override
    public void setTranslucent(PaintLayer layer, boolean isTranslucent)
    {
        ModelStateData.PAINT_IS_TRANSLUCENT[layer.ordinal()].setValue(isTranslucent, this);
        clearStateFlags();
        invalidateHashCode();
    }
    
    ////////////////////////////////////////////////////
    //  PACKER 1 ATTRIBUTES (NOT SHAPE-DEPENDENT)
    ////////////////////////////////////////////////////

    @Override
    public ITexturePalette getTexture(PaintLayer layer)
    {
        return TexturePaletteRegistry.get(ModelStateData.PAINT_TEXTURE[layer.ordinal()].getValue(this));
    }

    @Override
    public void setTexture(PaintLayer layer, @Nonnull ITexturePalette tex)
    {
        ModelStateData.PAINT_TEXTURE[layer.ordinal()].setValue(tex.ordinal(), this);
        invalidateHashCode();
        clearStateFlags();
    }

    @Override
    public void setVertexProcessor(PaintLayer layer, VertexProcessor vp)
    {
        ModelStateData.PAINT_VERTEX_PROCESSOR[layer.ordinal()].setValue(vp.ordinal, this);
        invalidateHashCode();        
    }
    
    @Override
    public VertexProcessor getVertexProcessor(PaintLayer layer)
    {
        return VertexProcessors.get(ModelStateData.PAINT_VERTEX_PROCESSOR[layer.ordinal()].getValue(this));
    }
    
    @Override
    public boolean isEmissive(PaintLayer layer)
    {
        return ModelStateData.PAINT_EMISSIVE[layer.ordinal()].getValue(this);
    }

    @Override
    public void setEmissive(PaintLayer layer, boolean isEmissive)
    {
        ModelStateData.PAINT_EMISSIVE[layer.ordinal()].setValue(isEmissive, this);
        clearStateFlags();
        invalidateHashCode();
    }

    ////////////////////////////////////////////////////
    //  PACKER 2 ATTRIBUTES  (NOT SHAPE-DEPENDENT)
    ////////////////////////////////////////////////////

    @Override
    public int getPosX()
    {
        return ModelStateData.POS_X.getValue(this);
    }

    @Override
    public void setPosX(int index)
    {
        ModelStateData.POS_X.setValue(index, this);
        invalidateHashCode();
    }

    @Override
    public int getPosY()
    {
        return ModelStateData.POS_Y.getValue(this);
    }

    @Override
    public void setPosY(int index)
    {
        ModelStateData.POS_Y.setValue(index, this);
        invalidateHashCode();
    }

    @Override
    public int getPosZ()
    {
        return ModelStateData.POS_Z.getValue(this);
    }

    @Override
    public void setPosZ(int index)
    {
        ModelStateData.POS_Z.setValue(index, this);
        invalidateHashCode();
    }

    @Override
    public long getStaticShapeBits()
    {
        return ModelStateData.EXTRA_SHAPE_BITS.getValue(this);
    }

    @Override
    public void setStaticShapeBits(long bits)
    {
        ModelStateData.EXTRA_SHAPE_BITS.setValue(bits, this);
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

        return this.hasSpecies() ? ModelStateData.SPECIES.getValue(this) : 0;
    }

    @Override
    public void setSpecies(int species)
    {
        this.populateStateFlagsIfNeeded();

        if(ConfigXM.BLOCKS.debugModelState && !this.hasSpecies())
            ExoticMatter.INSTANCE.warn("setSpecies on model state does not apply for shape");

        if(this.hasSpecies())
        {
            ModelStateData.SPECIES.setValue(species, this);
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

        return CornerJoinBlockStateSelector.getJoinState(MathHelper.clamp(ModelStateData.BLOCK_JOIN.getValue(this), 0, CornerJoinBlockStateSelector.BLOCK_JOIN_STATE_COUNT - 1));
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

        ModelStateData.BLOCK_JOIN.setValue(join.getIndex(), this);
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
                ? new SimpleJoin(ModelStateData.BLOCK_JOIN.getValue(this))
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

        ModelStateData.BLOCK_JOIN.setValue(join.getIndex(), this);
        invalidateHashCode();
    }

    @Override
    public SimpleJoin getMasonryJoin()
    {
        if(ConfigXM.BLOCKS.debugModelState && (this.getShape().meshFactory().stateFormat != StateFormat.BLOCK || (stateFlags & STATE_FLAG_NEEDS_CORNER_JOIN) == 0) || ((stateFlags & STATE_FLAG_NEEDS_MASONRY_JOIN) == 0))
            ExoticMatter.INSTANCE.warn("getMasonryJoin on model state does not apply for shape");

        populateStateFlagsIfNeeded();
        return new SimpleJoin(ModelStateData.MASONRY_JOIN.getValue(this));
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

        ModelStateData.MASONRY_JOIN.setValue(join.getIndex(), this);
        invalidateHashCode();
    }

    @Override
    public Rotation getAxisRotation()
    {
        return ModelStateData.AXIS_ROTATION.getValue(this);
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

        ModelStateData.AXIS_ROTATION.setValue(rotation, this);
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

        return shapeBits0;
    }

    @Override
    public void setMultiBlockBits(long bits)
    {
        if(ConfigXM.BLOCKS.debugModelState && this.getShape().meshFactory().stateFormat != StateFormat.MULTIBLOCK)
            ExoticMatter.INSTANCE.warn("setMultiBlockBits on model state does not apply for shape");

        shapeBits0 = bits;
        invalidateHashCode();
    }

    ////////////////////////////////////////////////////
    //  PACKER 3 ATTRIBUTES  (FLOWING TERRAIN FORMAT)
    ////////////////////////////////////////////////////


    @Override
    public long getTerrainStateKey()
    {
        assert this.getShape().meshFactory().stateFormat == StateFormat.FLOW : "getTerrainState on model state does not apply for shape";
        return ModelStateData.FLOW_JOIN.getValue(this);
    }
    
    @Override
    public int getTerrainHotness()
    {
        assert this.getShape().meshFactory().stateFormat == StateFormat.FLOW : "getTerrainState on model state does not apply for shape";
        return (int)ModelStateData.EXTRA_SHAPE_BITS.getValue(this);
    }

    @Override
    public void setTerrainStateKey(long terrainStateKey)
    {
        assert this.getShape().meshFactory().stateFormat == StateFormat.FLOW : "getTerrainState on model state does not apply for shape";
        ModelStateData.FLOW_JOIN.setValue(terrainStateKey, this);
    }

    @Override
    public TerrainState getTerrainState()
    {
        assert this.getShape().meshFactory().stateFormat == StateFormat.FLOW : "getTerrainState on model state does not apply for shape";
        return new TerrainState(ModelStateData.FLOW_JOIN.getValue(this), (int)ModelStateData.EXTRA_SHAPE_BITS.getValue(this));
    }
   
    @Override
    public void setTerrainState(TerrainState flowState)
    {
        assert this.getShape().meshFactory().stateFormat == StateFormat.FLOW : "getTerrainState on model state does not apply for shape";
        ModelStateData.FLOW_JOIN.setValue(flowState.getStateKey(), this);
        ModelStateData.EXTRA_SHAPE_BITS.setValue(flowState.getHotness(), this);
        invalidateHashCode();
    }


    ////////////////////////////////////////////////////
    //  SHAPE/STATE-DEPENDENT CONVENIENCE METHODS
    ////////////////////////////////////////////////////

    @Override
    public BlockRenderLayer getRenderPass(PaintLayer layer)
    {
        switch(layer)
        {
            case BASE:
            case CUT:
            case LAMP:
            default:
                return this.isTranslucent(layer) ? BlockRenderLayer.TRANSLUCENT : BlockRenderLayer.SOLID;
                
            case MIDDLE:
            case OUTER:
            {
                this.populateStateFlagsIfNeeded();
                if(ExoticMatter.proxy.isAcuityEnabled())
                    // report solid if multi-layer solid render is enabled and applicable
                    return (this.stateFlags & STATE_FLAG_HAS_SOLID_RENDER) == STATE_FLAG_HAS_SOLID_RENDER ? BlockRenderLayer.SOLID : BlockRenderLayer.TRANSLUCENT;
                else 
                    return BlockRenderLayer.TRANSLUCENT;
            }
        }
    }
    
    @Override
    public final RenderLayout getRenderLayout()
    {
        return getRenderLayoutProducer().renderLayout();
    }
    
    @Override
    public final RenderLayoutProducer getRenderLayoutProducer()
    {
        this.populateStateFlagsIfNeeded();
        
        if((this.stateFlags & STATE_FLAG_HAS_SOLID_RENDER) == STATE_FLAG_HAS_SOLID_RENDER)
        {
            if((this.stateFlags & STATE_FLAG_HAS_TRANSLUCENT_RENDER) == STATE_FLAG_HAS_TRANSLUCENT_RENDER)
            {
                // models with solid base textures can render in solid layer if Acuity is enabled
                return (this.stateFlags & STATE_FLAG_HAS_TRANSLUCENT_GEOMETRY) == 0
                        ? RenderLayoutProducer.ALWAYS_BOTH
                        : RenderLayoutProducer.DEPENDS;
            }
            else 
                return RenderLayoutProducer.ALWAYS_SOLID;
        }
        else 
            return ((this.stateFlags & STATE_FLAG_HAS_TRANSLUCENT_RENDER) == STATE_FLAG_HAS_TRANSLUCENT_RENDER)
                ? RenderLayoutProducer.ALWAYS_TRANSLUCENT
                : RenderLayoutProducer.ALWAYS_NONE;
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
    public final boolean doShapeAndAppearanceMatch(ISuperModelState other)
    {
        final ModelState o = (ModelState)other;
        return (this.coreBits & ModelStateData.SHAPE_COMPARISON_MASK_0) == (o.coreBits & ModelStateData.SHAPE_COMPARISON_MASK_0)
                && (this.shapeBits1 & ModelStateData.SHAPE_COMPARISON_MASK_1) == (o.shapeBits1 & ModelStateData.SHAPE_COMPARISON_MASK_1)
                && this.layerBitsBase == o.layerBitsBase
                && this.layerBitsCut == o.layerBitsCut
                && this.layerBitsLamp == o.layerBitsLamp
                && this.layerBitsMiddle == o.layerBitsMiddle
                && this.layerBitsOuter == o.layerBitsOuter;
    }

    @Override
    public boolean doesAppearanceMatch(ISuperModelState other)
    {
        final ModelState o = (ModelState)other;
        return this.layerBitsBase == o.layerBitsBase
                && this.layerBitsCut == o.layerBitsCut
                && this.layerBitsLamp == o.layerBitsLamp
                && this.layerBitsMiddle == o.layerBitsMiddle
                && this.layerBitsOuter == o.layerBitsOuter;
    }

    // PERF: bottleneck for Pyroclasm
    @Override
    public ISuperModelState geometricState()
    {
        this.populateStateFlagsIfNeeded();
        ModelState result = new ModelState();
        result.setShape(this.getShape());

        switch(this.getShape().meshFactory().stateFormat)
        {
        case BLOCK:
            result.setStaticShapeBits(this.getStaticShapeBits());
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
            ModelStateData.FLOW_JOIN.setValue(ModelStateData.FLOW_JOIN.getValue(this), result);
            break;
            
        case MULTIBLOCK:
            result.shapeBits0 = this.shapeBits0;
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
        if(tag.hasKey(NBT_MODEL_BITS))
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
    public void deserializeNBT(@Nullable NBTTagCompound tag)
    {
        if(tag == null)
            return;
        
        int[] stateBits = tag.getIntArray(NBT_MODEL_BITS);
        if(stateBits.length != 16)
        {
            ExoticMatter.INSTANCE.warn("Bad or missing data encounter during ModelState NBT deserialization.");
            return;
        }
        this.deserializeFromInts(stateBits);
        
        // shape is serialized by name because registered shapes can change if mods/config change
        ModelShape<?> shape = ModelShapes.get(tag.getString(NBT_SHAPE));
        if(shape != null)
            ModelStateData.SHAPE.setValue(shape.ordinal(), this);
        
        // textures and vertex processors serialized by name because registered can change if mods/config change
        String layers = tag.getString(NBT_LAYERS);
        if(layers.isEmpty())
        {
            String[] names = layers.split(",");
            if(names.length != 0)
            {
                int i = 0;
                for(PaintLayer l : PaintLayer.VALUES)
                {
                    if(ModelStateData.PAINT_TEXTURE[l.ordinal()].getValue(this) != 0)
                    {
                        ITexturePalette tex = TexturePaletteRegistry.get(names[i++]);
                        ModelStateData.PAINT_TEXTURE[l.ordinal()].setValue(tex.ordinal(), this);
                        if(i == names.length) break;
                    }
                    
                    if(ModelStateData.PAINT_VERTEX_PROCESSOR[l.ordinal()].getValue(this) != 0)
                    {
                        VertexProcessor vp = VertexProcessors.get(names[i++]);
                        ModelStateData.PAINT_VERTEX_PROCESSOR[l.ordinal()].setValue(vp.ordinal, this);
                        if(i == names.length) break;
                    }
                }
            }
        }
        this.clearStateFlags();
    }

    @Override
    public void serializeNBT(NBTTagCompound tag)
    {
        tag.setIntArray(NBT_MODEL_BITS, this.serializeToInts());
        
        // shape is serialized by name because registered shapes can change if mods/config change
        tag.setString(NBT_SHAPE, this.getShape().systemName());
        
        // textures and vertex processors serialized by name because registered can change if mods/config change
        StringBuilder layers = new StringBuilder();
        for(PaintLayer l : PaintLayer.VALUES)
        {
            if(ModelStateData.PAINT_TEXTURE[l.ordinal()].getValue(this) != 0)
            {
                if(layers.length() != 0)
                    layers.append(",");
                layers.append(this.getTexture(l).systemName());
            }
            
            if(ModelStateData.PAINT_VERTEX_PROCESSOR[l.ordinal()].getValue(this) != 0)
            {
                if(layers.length() != 0)
                    layers.append(",");
                layers.append(this.getVertexProcessor(l).registryName);
            }
        }
        if(layers.length() != 0)
            tag.setString(NBT_LAYERS, layers.toString());
    }

    @Override
    public void fromBytes(PacketBuffer pBuff)
    {
        this.coreBits = pBuff.readLong();
        this.shapeBits0 = pBuff.readLong();
        this.shapeBits1 = pBuff.readVarLong();
        this.layerBitsBase = pBuff.readVarLong();
        this.layerBitsCut = pBuff.readVarLong();
        this.layerBitsLamp = pBuff.readVarLong();
        this.layerBitsMiddle = pBuff.readVarLong();
        this.layerBitsOuter = pBuff.readVarLong();
    }

    @Override
    public void toBytes(PacketBuffer pBuff)
    {
        pBuff.writeLong(this.coreBits);
        pBuff.writeLong(this.shapeBits0);
        pBuff.writeVarLong(this.shapeBits1);
        pBuff.writeVarLong(this.layerBitsBase);
        pBuff.writeVarLong(this.layerBitsCut);
        pBuff.writeVarLong(this.layerBitsLamp);
        pBuff.writeVarLong(this.layerBitsMiddle);
        pBuff.writeVarLong(this.layerBitsOuter);
    }
}