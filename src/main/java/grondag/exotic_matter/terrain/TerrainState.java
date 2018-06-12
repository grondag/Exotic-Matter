package grondag.exotic_matter.terrain;

import javax.annotation.Nullable;

import grondag.exotic_matter.block.ISuperBlock;
import grondag.exotic_matter.model.primitives.QuadHelper;
import grondag.exotic_matter.varia.BitPacker;
import grondag.exotic_matter.varia.Useful;
import grondag.exotic_matter.world.HorizontalCorner;
import grondag.exotic_matter.world.HorizontalFace;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;

public class TerrainState
{
    
    public final static long FULL_BLOCK_STATE_KEY = TerrainState.computeStateKey(12, new int[] {12, 12, 12,12}, new int[] {12, 12, 12, 12}, 0 );
    public final static long EMPTY_BLOCK_STATE_KEY = TerrainState.computeStateKey(1, new int[] {1, 1, 1,1}, new int[] {1, 1, 1, 1}, 1 );

    public final static TerrainState EMPTY_STATE = new TerrainState(EMPTY_BLOCK_STATE_KEY, 0);
    
    /** Eight 6-bit blocks that store a corner and side value,
     * plus 4 bits for center height and 3 bits for offset */
    public final static long STATE_BIT_COUNT = 55;

    public final static long STATE_BIT_MASK = 0x7FFFFFFFFFFFFFL;

    public final static int BLOCK_LEVELS_INT = 12;
    public final static int BLOCK_LEVELS_INT_HALF = BLOCK_LEVELS_INT / 2;
    public final static float BLOCK_LEVELS_FLOAT = (float) BLOCK_LEVELS_INT;
    public final static int MIN_HEIGHT = -23;
    public final static int NO_BLOCK = MIN_HEIGHT - 1;
    public final static int MAX_HEIGHT = 36;
    
    public final static ITerrainBitConsumer<TerrainState> FACTORY = (t, h) -> new TerrainState(t, h);
    /**
     * Want to avoid the synchronization penalty of pooled block pos.
     */
    private static ThreadLocal<BlockPos.MutableBlockPos> mutablePos = new ThreadLocal<BlockPos.MutableBlockPos>()
    {

        @Override
        protected MutableBlockPos initialValue()
        {
            return new BlockPos.MutableBlockPos();
        }
    };
    
    // Use these insted of magic number for filler block meta values
    /** This value is for a height block two below another height block, offset of 2 added to vertex heights*/
//    public final static int FILL_META_DOWN2 = 0;
//    public final static int FILL_META_DOWN1 = 1;
    
    /** This value indicates a top height block, means no offset, no effect on vertex calculations*/
//    public final static int FILL_META_LEVEL = 2;
//    public final static int FILL_META_UP1 = 3;
//    public final static int FILL_META_UP2 = 4;
    

//    /**
//     * Number of possible values for non-center blocks.
//     * Includes negative values, positive values, zero and NO_BLOCK values.
//     */
//    private final static int VALUE_COUNT = -MIN_HEIGHT + MAX_HEIGHT + 1 + 1;

    /** 
     * Returns values -2 through +2 from a triad (3 bits).
     */
    public static int getYOffsetFromTriad(int triad)
    {
        return Math.min(4, triad & 7) - 2;
    }

    /**
     * Stores values from -2  to +2 in a triad (3 bits).
     * Invalid values are handled same as +1.
     */
    public static int getTriadWithYOffset(int offset)
    {
        return Math.min(4, (offset + 2) & 7);
    }

    private final byte centerHeight;
    private final byte sideHeight[] = new byte[4];
    private final byte cornerHeight[] = new byte[4];
    private final byte yOffset;
    private final long stateKey;
    private final int hotness;
    
    private static final byte SIMPLE_FLAG[] = new byte[4];
    private static final byte SIMPLE_FLAG_TOP = 16;
    private static final byte SIMPLE_FLAG_MOST_SIDES = 32;
    static
    {
        SIMPLE_FLAG[HorizontalFace.EAST.ordinal()] = 1;
        SIMPLE_FLAG[HorizontalFace.WEST.ordinal()] = 2;
        SIMPLE_FLAG[HorizontalFace.NORTH.ordinal()] = 4;
        SIMPLE_FLAG[HorizontalFace.SOUTH.ordinal()] = 8;
    }
    
    /** true if model vertex height calculations current */
    private boolean vertexCalcsDone = false;
    /** cache model vertex height calculations */
    private float midCornerHeight[] = new float[HorizontalCorner.values().length];
    /** cache model vertex height calculations */
    private float farCornerHeight[] = new float[HorizontalCorner.values().length];
    /** cache model vertex height calculations */
    private float midSideHeight[] = new float[HorizontalFace.values().length];
    /** cache model vertex height calculations */
    private float farSideHeight[] = new float[HorizontalFace.values().length];
    
    private float minVertexHeightExcludingCenter;
    private float maxVertexHeightExcludingCenter;
    private float averageVertexHeightIncludingCenter;
    
    private byte simpleFlags = 0;
    
    public final long getStateKey()
    {
        return stateKey;
    }
    
    public final int getHotness()
    {
        return this.hotness;
    }
    
    @Override
    public final int hashCode()
    {
        return (int) Useful.longHash(this.stateKey ^ this.hotness);
    }

    @Override
    public final boolean equals(@Nullable Object obj)
    {
        if(this == obj) return true;
        if(obj == null) return false;
        if(obj instanceof TerrainState)
        {
            final TerrainState other = (TerrainState)obj;
            return other.stateKey == this.stateKey
                    && other.hotness == this.hotness;
        }
        return false;
    }

    public static long computeStateKey(int centerHeightIn, int[] sideHeightIn, int[] cornerHeightIn, int yOffsetIn)
    {
        long stateKey = (centerHeightIn - 1) | getTriadWithYOffset(yOffsetIn) << 4;
        
        int shift = 7;
        for(int i = 0; i < 4; i++)
        {
            stateKey |= ((long)((sideHeightIn[i] - NO_BLOCK)) << shift);
            shift += 6;
            stateKey |= ((long)((cornerHeightIn[i] - NO_BLOCK)) << shift);
            shift += 6;            
        }
        return stateKey;
    }
    
    public TerrainState(int centerHeightIn, int[] sideHeightIn, int[] cornerHeightIn, int yOffsetIn)
    {
        this(computeStateKey(centerHeightIn, sideHeightIn, cornerHeightIn, yOffsetIn), 0);
    }

    public TerrainState(long stateKey, int hotness)
    {
        this.stateKey = stateKey;
        this.hotness = hotness;
        centerHeight = (byte)((stateKey & 0xF) + 1);
        yOffset = (byte) getYOffsetFromTriad((int) ((stateKey >> 4) & 0x7));

        int shift = 7;
        for(int i = 0; i < 4; i++)
        {
            sideHeight[i] = (byte) (((stateKey >> shift) & 63) + NO_BLOCK);
            shift += 6;
            cornerHeight[i] = (byte) (((stateKey >> shift) & 63) + NO_BLOCK);
            shift += 6;
        }        
    }
    

    /**
     * Rendering height of center block ranges from 1 to 12
     * and is stored in state key as values 0-11.
     */
    public final int getCenterHeight()
    {
        return this.centerHeight;
    }

    public final int getCenterHotness()
    {
        return CENTER_HOTNESS.getValue(this.hotness) > 0 ? 255 : 0;
    }
    
    public int getYOffset()
    {
        return this.yOffset;
    }

    // Rendering height of corner and side neighbors ranges 
    // from -24 to 36. 
    public int getSideHeight(HorizontalFace side)
    {
        return this.sideHeight[side.ordinal()];
    }

    public int getCornerHeight(HorizontalCorner corner)
    {
        return this.cornerHeight[corner.ordinal()];
    }


    
    /**
     * Returns how many filler blocks are needed on top to cover a cut surface.
     * Possible return values are 0, 1 and 2.
     */
    public int topFillerNeeded()
    {
        //filler only applies to level blocks
        if(yOffset != 0) 
            return 0;
        refreshVertexCalculationsIfNeeded();

        double max = 0;
        
        // center vertex does not matter if top is simplified to a single quad
        if(!isTopSimple())
        {
            max = Math.max(max,getCenterVertexHeight());
        }
        
        for(int i = 0; i < 4; i++)
        {
            // side does not matter if side geometry is simplified
            if(!isSideSimple(i))
            {
                max = Math.max(max, this.midSideHeight[i]);
            }
            max = Math.max(max, this.midCornerHeight[i]);
        }
        
        return max > 2.01  ? 2 : max > 1.01 ? 1 : 0;
    }
    
    public boolean isSideSimple(HorizontalFace face)
    {
        return this.isSideSimple(face.ordinal());
    }    
    
    private boolean isSideSimple(int ordinal)
    {
        refreshVertexCalculationsIfNeeded();
        byte flag = SIMPLE_FLAG[ordinal];
        return (this.simpleFlags & flag) == flag;
    }   
    
    /**
     * True if top can be simplified to no more than two tris.  Is true implies all sides are simple.
     * Exception: top is not allowed to be simple if this block is hot, 
     * because that would defeat per-vertex lighting.
     */
    public boolean isTopSimple()
    {
        refreshVertexCalculationsIfNeeded();
        return (this.simpleFlags & SIMPLE_FLAG_TOP) == SIMPLE_FLAG_TOP;
    }
    
    /**
     * True if at least two sides are simple.
     * Exception: sides are not allowed to be simple if this block is hot, 
     * because that would defeat per-vertex lighting.
     */
    public boolean areMostSidesSimple()
    {
        refreshVertexCalculationsIfNeeded();
        return (this.simpleFlags & SIMPLE_FLAG_MOST_SIDES) == SIMPLE_FLAG_MOST_SIDES;
    }
    
    public boolean isFullCube()
    {
        refreshVertexCalculationsIfNeeded();
        double top = 1.0 + yOffset + QuadHelper.EPSILON;
        
        // center vertex does not matter if top is simplified to a single quad
        if(!isTopSimple())
        {
            if(getCenterVertexHeight() < top) return false;
        }
        
        for(int i = 0; i < 4; i++)
        {
            // side does not matter if side geometry is simplified
            if(!isSideSimple(i))
            {
                if(this.midSideHeight[i] < top) return false;
            }

            if(this.midCornerHeight[i] < top) return false;
        }
        return true;
    }

    public boolean isEmpty()
    {
        refreshVertexCalculationsIfNeeded();
        double bottom = 0.0 + yOffset;
        
        // center vertex does not matter if top is simplified to a single quad
        if(!isTopSimple())
        {
            if(getCenterVertexHeight() > bottom) return false;
        }
        
        for(int i = 0; i < 4; i++)
        {
            // side does not matter if side geometry is simplified
            if(!isSideSimple(i))
            {
                if(this.midSideHeight[i] > bottom) return false;
            }
            
            if(this.midCornerHeight[i] > bottom) return false;
        }
        return true;
    }
    
    /** 
     * how much sky light is blocked by this shape. 
     * 0 = none, 14 = most, 255 = all
     */
    public int verticalOcclusion()
    {
        refreshVertexCalculationsIfNeeded();
        double bottom = 0.0 + yOffset;
        
        int aboveCount = 0;
        
        for(int i = 0; i < 4; i++)
        {
            if(this.midSideHeight[i] > bottom) aboveCount++;
            if(this.midCornerHeight[i] > bottom) aboveCount++;
        }        
        
        if(getCenterVertexHeight() > bottom) aboveCount *= 2;
        
        return aboveCount >= 16 ? 255 : aboveCount;
    }
    
    public float getCenterVertexHeight()
    {
        return (float) getCenterHeight() / BLOCK_LEVELS_FLOAT;
    }

    public float getFarCornerVertexHeight(HorizontalCorner corner)
    {
        refreshVertexCalculationsIfNeeded();
        return farCornerHeight[corner.ordinal()];
    }
    
    public float getMidCornerVertexHeight(HorizontalCorner corner)
    {
        refreshVertexCalculationsIfNeeded();
        return midCornerHeight[corner.ordinal()];
    }
    
    public float getFarSideVertexHeight(HorizontalFace face)
    {
        refreshVertexCalculationsIfNeeded();
        return farSideHeight[face.ordinal()];
    }
    
    public float getMidSideVertexHeight(HorizontalFace face)
    {
        refreshVertexCalculationsIfNeeded();
        return midSideHeight[face.ordinal()];
    }

    private void refreshVertexCalculationsIfNeeded()
    {
        if(vertexCalcsDone) return;
        
        final float centerHeight = this.getCenterVertexHeight();
        
        float max = Float.MIN_VALUE;
        float min = Float.MAX_VALUE;
        float total = centerHeight;
        
        for(HorizontalFace side : HorizontalFace.values())
        {
            final float h = calcMidSideVertexHeight(side);
            total += h;
            if(h > max) max = h;
            if(h < min) min = h;
            
            midSideHeight[side.ordinal()] = h;
            farSideHeight[side.ordinal()] = calcFarSideVertexHeight(side);
            
        }
        
        for(HorizontalCorner corner: HorizontalCorner.values())
        {
            final float h = calcMidCornerVertexHeight(corner);
            
            total += h;
            if(h > max) max = h;
            if(h < min) min = h;
            
            midCornerHeight[corner.ordinal()] = h;
            farCornerHeight[corner.ordinal()] = calcFarCornerVertexHeight(corner);
        }
        
        this.maxVertexHeightExcludingCenter = max;
        this.minVertexHeightExcludingCenter = min;
        this.averageVertexHeightIncludingCenter = total / 9;
        
        //determine if sides and top geometry can be simplified
        //simplification not possible if block is hot - to preserve per-vertex lighting
        if(this.getCenterHotness() == 0)
        {
            boolean topIsSimple = true;
            int simpleSideCount = 0;
            
            for(HorizontalFace side: HorizontalFace.values())
            {
                float avg = midCornerHeight[HorizontalCorner.find(side, side.getLeft()).ordinal()];
                avg += midCornerHeight[HorizontalCorner.find(side, side.getRight()).ordinal()];
                avg /= 2;
                boolean sideIsSimple = Math.abs(avg - midSideHeight[side.ordinal()]) < QuadHelper.EPSILON;
                if(sideIsSimple)
                {
                    this.simpleFlags |= SIMPLE_FLAG[side.ordinal()];
                    simpleSideCount++;
                }
                else
                {
                    topIsSimple = false;
                }
            }
    
            if(simpleSideCount > 1) this.simpleFlags |= SIMPLE_FLAG_MOST_SIDES;
            
            if(topIsSimple)
            {
                float cross1 = (midCornerHeight[HorizontalCorner.NORTH_EAST.ordinal()] + midCornerHeight[HorizontalCorner.SOUTH_WEST.ordinal()]) / 2.0f;
                float cross2 = (midCornerHeight[HorizontalCorner.NORTH_WEST.ordinal()] + midCornerHeight[HorizontalCorner.SOUTH_EAST.ordinal()]) / 2.0f;
                if(Math.abs(cross1 - cross2) < 1.0f) this.simpleFlags |= SIMPLE_FLAG_TOP;
            }
        }
        vertexCalcsDone = true;

    }
   
    private float calcFarCornerVertexHeight(HorizontalCorner corner)
    {
        int heightCorner = getCornerHeight(corner);
        
        if(heightCorner == TerrainState.NO_BLOCK)
        {
            int max = Math.max(Math.max(getSideHeight(corner.face1), getSideHeight(corner.face2)), getCenterHeight());
            heightCorner = max - BLOCK_LEVELS_INT;
        }
       
        return ((float) heightCorner) / BLOCK_LEVELS_FLOAT;
    }
    
    
    private float calcMidCornerVertexHeight(HorizontalCorner corner)
    {
        int heightSide1 = getSideHeight(corner.face1);
        int heightSide2 = getSideHeight(corner.face2);
        int heightCorner = getCornerHeight(corner);
        
        int max = Math.max(Math.max(heightSide1, heightSide2), Math.max(heightCorner, getCenterHeight())) - BLOCK_LEVELS_INT;
                
        if(heightSide1 == TerrainState.NO_BLOCK) heightSide1 = max;
        if(heightSide2 == TerrainState.NO_BLOCK) heightSide2 = max;
        if(heightCorner == TerrainState.NO_BLOCK) heightCorner = max;
        
        float numerator = getCenterHeight() + heightSide1 + heightSide2 + heightCorner;
       
        return numerator / (BLOCK_LEVELS_FLOAT * 4F);
        
    }
    
    /**
     * If at least 3 block touching corner are hot, returns average heat.
     * Returns 0 if 2 or fewer.  This block must be hot to be non-zero.
     */
    public final int midCornerHotness(HorizontalCorner corner)
    {
        final int centerHeat = this.getCenterHotness();
        if(centerHeat == 0) return 0;
        
        final int heatSide1 = SIDE_HOTNESS[corner.face1.ordinal()].getValue(this.hotness);
        final int heatSide2 = SIDE_HOTNESS[corner.face2.ordinal()].getValue(this.hotness);
        final int heatCorner = CORNER_HOTNESS[corner.ordinal()].getValue(this.hotness);
        
        if(heatSide1 == 0)
        {
            if(heatSide2 == 0)
                return 0;
            else
                return heatCorner == 0
                    ? 0
                    : 70; //(centerHeat + heatSide2 + heatCorner + 1) / 3; // + 1 to round up
        }
        else if(heatSide2 == 0)
        {
            // heatside1 is known to be hot at this point
            return heatCorner == 0
                    ? 0
                    : 70; //(centerHeat + heatSide1 + heatCorner + 1) / 3; // + 1 to round up 
        }
        else
        {
            // both sides are hot
            return heatCorner == 0
                    ? 70 //(centerHeat + heatSide1 + heatSide2 + 1) / 3 // + 1 to round up 
                    : 255; //(centerHeat + heatSide1 + heatSide2 + heatCorner + 1) / 4; // + 1 to round up 
        }
    }
    
    /**
     * If both this block and side block are hot, is average heat, rounded up. 
     * Zero otherwise.
     */
    public final int midSideHotness(HorizontalFace face)
    {
        final int centerHeat = this.getCenterHotness();
        if(centerHeat == 0) return 0;
        
        final int heatSide = SIDE_HOTNESS[face.ordinal()].getValue(this.hotness);
        
//        return heatSide == 0
//                ? 0
//                : (heatSide + centerHeat + 1) / 2;
        
        return heatSide == 0
                ? 70
                : 255;
    }
    
    private float calcFarSideVertexHeight(HorizontalFace face)
    {
        return (getSideHeight(face) == TerrainState.NO_BLOCK ? getCenterHeight() - BLOCK_LEVELS_INT: ((float)getSideHeight(face)) / BLOCK_LEVELS_FLOAT);
    }

    private float calcMidSideVertexHeight(HorizontalFace face)
    {
        float sideHeight = getSideHeight(face) == TerrainState.NO_BLOCK ? getCenterHeight() - BLOCK_LEVELS_INT : (float)getSideHeight(face);
        return (sideHeight + (float) getCenterHeight()) / (BLOCK_LEVELS_FLOAT * 2F);
    }

    @Override
    public String toString()
    {
        String retval = "CENTER=" + this.getCenterHeight();
        for(HorizontalFace side: HorizontalFace.values())
        {
            retval += " " + side.name() + "=" + this.getSideHeight(side);
        }
        for(HorizontalCorner corner: HorizontalCorner.values())
        {
            retval += " " + corner.name() + "=" + this.getCornerHeight(corner);
        }
        retval += " Y-OFFSET=" + yOffset;
        return retval;
    }

    public int concavity()
    {
        int count = 0;
        
        final int center = this.getCenterHeight();
        for(HorizontalFace side: HorizontalFace.values())
        {
            count += Math.max(0, this.getSideHeight(side) - center) / 12;
        }
        for(HorizontalCorner corner: HorizontalCorner.values())
        {
            count += Math.max(0, this.getCornerHeight(corner) - center) / 12;
        }
        return count;
    }
    
    public int spread()
    {
        
        final int center = this.getCenterHeight();
        int min = center;
        int max = center;
        for(HorizontalFace side: HorizontalFace.values())
        {
            int h = this.getSideHeight(side);
            if(h > max)
                max = h;
            else if(h < min)
                min = h;
        }
        for(HorizontalCorner corner: HorizontalCorner.values())
        {
            int h = this.getCornerHeight(corner);
            if(h > max)
                max = h;
            else if(h < min)
                min = h;
        }
        return max-min;
    }
    
    public int divergence()
    {
        
        final int center = this.getCenterHeight();
        int div = 0;
        for(HorizontalFace side: HorizontalFace.values())
        {
            div += Math.abs(this.getSideHeight(side) - center);
        }
        for(HorizontalCorner corner: HorizontalCorner.values())
        {
            div += Math.abs(this.getCornerHeight(corner) - center);
        }
        return div;
    }
    
    public static <T> T produceBitsFromWorldStatically(ISuperBlock block, IBlockState state, IBlockAccess world, BlockPos pos, ITerrainBitConsumer<T> consumer)
    {
        return produceBitsFromWorldStatically(block.isFlowFiller(), state, world, pos, consumer);
    }
    
    @SuppressWarnings("null")
    public static final BitPacker<Void> HOTNESS_PACKER = new BitPacker<Void>(null, null);
    public static final BitPacker<Void>.IntElement CENTER_HOTNESS = HOTNESS_PACKER.createIntElement(6); 
    @SuppressWarnings("unchecked")
    public static final BitPacker<Void>.IntElement[] CORNER_HOTNESS = (BitPacker<Void>.IntElement[]) new BitPacker<?>.IntElement[4];
    @SuppressWarnings("unchecked")
    public static final BitPacker<Void>.IntElement[] SIDE_HOTNESS = (BitPacker<Void>.IntElement[]) new BitPacker<?>.IntElement[4];
   
    static
    {
        SIDE_HOTNESS[HorizontalFace.NORTH.ordinal()] = HOTNESS_PACKER.createIntElement(6); 
        SIDE_HOTNESS[HorizontalFace.EAST.ordinal()] = HOTNESS_PACKER.createIntElement(6); 
        SIDE_HOTNESS[HorizontalFace.SOUTH.ordinal()] = HOTNESS_PACKER.createIntElement(6); 
        SIDE_HOTNESS[HorizontalFace.WEST.ordinal()] = HOTNESS_PACKER.createIntElement(6); 
        
        CORNER_HOTNESS[HorizontalCorner.NORTH_EAST.ordinal()] = HOTNESS_PACKER.createIntElement(6); 
        CORNER_HOTNESS[HorizontalCorner.NORTH_WEST.ordinal()] = HOTNESS_PACKER.createIntElement(6); 
        CORNER_HOTNESS[HorizontalCorner.SOUTH_EAST.ordinal()] = HOTNESS_PACKER.createIntElement(6); 
        CORNER_HOTNESS[HorizontalCorner.SOUTH_WEST.ordinal()] = HOTNESS_PACKER.createIntElement(6);
    }
    
    private static <T> T produceBitsFromWorldStatically(boolean isFlowFiller, IBlockState state, IBlockAccess world, final BlockPos pos, ITerrainBitConsumer<T> consumer)
    {
        int sideHeight[] = new int[4];
        int cornerHeight[] = new int[4];
        int yOffset = 0;
        
        long hotness = 0;
    
        BlockPos.MutableBlockPos mPos = mutablePos.get();
        
        //        HardScience.log.info("flowstate getBitsFromWorld @" + pos.toString());
    
        int yOrigin = pos.getY();
        IBlockState originState = state;
        if(isFlowFiller)
        {
            int offset = TerrainBlockHelper.getYOffsetFromState(state);
            yOrigin -= offset;
            yOffset = offset;
            
            mPos.setPos(pos.getX(), pos.getY() - offset, pos.getZ());
            originState = world.getBlockState(mPos);
            if(!TerrainBlockHelper.isFlowHeight(originState.getBlock()))
            {
                return consumer.apply(EMPTY_BLOCK_STATE_KEY, 0);
            }
        }
        else
        {
            // If under another flow height block, handle similar to filler block.
            // Not a perfect fix if they are stacked, but shouldn't normally be.
            //                HardScience.log.info("flowstate is height block");
    
            // try to use block above as height origin
            mPos.setPos(pos.getX(), pos.getY() + 2, pos.getZ());
            originState = world.getBlockState(mPos);
            if(TerrainBlockHelper.isFlowHeight(originState.getBlock()))
            {
                yOrigin += 2;
                yOffset = -2;
                //                    HardScience.log.info("origin 2 up");
            }
            else
            {
                mPos.setPos(pos.getX(), pos.getY() + 1, pos.getZ());
                originState = world.getBlockState(mPos);
                if(TerrainBlockHelper.isFlowHeight(originState.getBlock()))
                {
                    yOrigin += 1;
                    yOffset = -1;
                    //                        HardScience.log.info("origin 1 up");
                }
                else
                {
                    // didn't work, handle as normal height block
                    originState = state;
                    //                        HardScience.log.info("origin self");
                }
            }
        }
    
        int[][] neighborHeight = new int[3][3];
        neighborHeight[1][1] = TerrainBlockHelper.getFlowHeightFromState(originState);
        final int centerHeight = neighborHeight[1][1];

        if(centerHeight > 0)
        {
            hotness = CENTER_HOTNESS.setValue(TerrainBlockHelper.getHotness(originState.getBlock()), hotness);
        }
        final boolean hasHotness = hotness != 0;
        
        for(int x = 0; x < 3; x++)
        {
            for(int z = 0; z < 3; z++)
            {
                if(x == 1 && z == 1 ) continue;
                mPos.setPos(pos.getX() - 1 + x, yOrigin, pos.getZ() - 1 + z);
    
                // use cache if available
                neighborHeight[x][z] = TerrainBlockHelper.getFlowHeight(world, mPos);
    
            }
        }
    
    
        for(HorizontalFace side : HorizontalFace.values())
        {
            final int x = side.directionVector.getX();
            final int z = side.directionVector.getZ();
            final int h = neighborHeight[x + 1][z + 1];
            sideHeight[side.ordinal()] = h;
            if(h != TerrainState.NO_BLOCK && hasHotness)
            {
                final int y = yOrigin -2 + (h - TerrainState.MIN_HEIGHT) / TerrainState.BLOCK_LEVELS_INT;
                mPos.setPos(pos.getX() + x, y, pos.getZ() + z);
                IBlockState hotState = world.getBlockState(mPos);
                final int heat = TerrainBlockHelper.getHotness(hotState.getBlock());
                if(heat != 0)
                    hotness = SIDE_HOTNESS[side.ordinal()].setValue(heat, hotness);
            }
        }
    
        for(HorizontalCorner corner : HorizontalCorner.values())
        {
            final int x = corner.directionVector.getX();
            final int z = corner.directionVector.getZ();
            final int h = neighborHeight[x + 1][z + 1];
            cornerHeight[corner.ordinal()] = h;
            
            if(h != TerrainState.NO_BLOCK && hasHotness)
            {
                final int y = yOrigin -2 + (h - TerrainState.MIN_HEIGHT) / TerrainState.BLOCK_LEVELS_INT;
                mPos.setPos(pos.getX() + x, y, pos.getZ() + z);
                IBlockState hotState = world.getBlockState(mPos);
                final int heat = TerrainBlockHelper.getHotness(hotState.getBlock());
                if(heat != 0)
                    hotness = CORNER_HOTNESS[corner.ordinal()].setValue(heat, hotness);
            }
        }
    
        return consumer.apply(computeStateKey(centerHeight, sideHeight, cornerHeight, yOffset), (int)hotness);
    
    }

    
    /** 
     * Pass in pos with Y of flow block for which we are getting data.
     * Returns relative flow height based on blocks 2 above through 2 down.
     * Gets called frequently, thus the use of mutable pos.
     */
    public static int getFlowHeight(IBlockAccess world, MutableBlockPos pos)
    {
        pos.setY(pos.getY() + 2);;
        IBlockState state = world.getBlockState(pos);
        int h = TerrainBlockHelper.getFlowHeightFromState(state);
        if(h > 0) return 2 * BLOCK_LEVELS_INT + h;
    
        pos.setY(pos.getY() - 1);
        state = world.getBlockState(pos);
        h = TerrainBlockHelper.getFlowHeightFromState(state);
        if(h > 0) return BLOCK_LEVELS_INT + h;
    
        pos.setY(pos.getY() - 1);
        state = world.getBlockState(pos);
        h = TerrainBlockHelper.getFlowHeightFromState(state);
        if(h > 0) return h;
    
        pos.setY(pos.getY() - 1);
        state = world.getBlockState(pos);
        h = TerrainBlockHelper.getFlowHeightFromState(state);
        if(h > 0) return -BLOCK_LEVELS_INT + h;
    
        pos.setY(pos.getY() - 1);
        state = world.getBlockState(pos);
        h = TerrainBlockHelper.getFlowHeightFromState(state);
        if(h > 0) return -2 * BLOCK_LEVELS_INT + h;
    
        return NO_BLOCK;
    }

    /**
     * Amount of lava, in fluid levels, that should be retained on top of this block.
     * Designed to promote smooth terrain generation by acting similar to a box filter.<p>
     * 
     * Computed based on slopes of lines from side and corner middle verticesto center vertex.<p>
     * 
     * Return values are clamped to the range from 1 level to 18 levels (1.5 blocks)
     */
    public int retentionLevels()
    {
        refreshVertexCalculationsIfNeeded();
        
        final float center = this.getCenterVertexHeight();

        final float max = this.maxVertexHeightExcludingCenter;
        final float min = this.minVertexHeightExcludingCenter;
        final float avg = this.averageVertexHeightIncludingCenter;
        
        final float drop = max - min;
        
        // no drop gives one half block of retention
        if(drop == 0) return BLOCK_LEVELS_INT_HALF;
        
        /** essentially the distance from a box filter result */
        final float diffFromAvgLevels = (avg - center) * BLOCK_LEVELS_INT;
        
        int result;
        
        if(center <= min)
        {
            // center is (or shares) lowest point
            result = Math.round(diffFromAvgLevels) + BLOCK_LEVELS_INT_HALF;
        }
        else
        {
            // center is part of a slope
            result = drop < 1
                    ? Math.round(diffFromAvgLevels + (1 - drop) * BLOCK_LEVELS_INT_HALF)
                    : Math.round(diffFromAvgLevels);
        }
        
        return MathHelper.clamp(result, 1, BLOCK_LEVELS_INT + BLOCK_LEVELS_INT_HALF);
    }
}