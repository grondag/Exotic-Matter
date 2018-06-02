package grondag.exotic_matter.terrain;

import grondag.exotic_matter.block.ISuperBlock;
import grondag.exotic_matter.model.primitives.QuadHelper;
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

    public final static TerrainState EMPTY_STATE = new TerrainState(EMPTY_BLOCK_STATE_KEY);
    
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
    
    public long getStateKey()
    {
        return stateKey;
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
        this(computeStateKey(centerHeightIn, sideHeightIn, cornerHeightIn, yOffsetIn));
    }

    public TerrainState(long stateKey)
    {
        this.stateKey = stateKey;
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
    public int getCenterHeight()
    {
        return this.centerHeight;
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
     */
    public boolean isTopSimple()
    {
        refreshVertexCalculationsIfNeeded();
        return (this.simpleFlags & SIMPLE_FLAG_TOP) == SIMPLE_FLAG_TOP;
    }
    
    /**
     * True if at least two sides are simple.
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
    
    public static long getBitsFromWorldStatically(ISuperBlock block, IBlockState state, IBlockAccess world, BlockPos pos)
    {
        return getBitsFromWorldStatically(block.isFlowFiller(), state, world, pos);
    }
   
    private static long getBitsFromWorldStatically(boolean isFlowFiller, IBlockState state, IBlockAccess world, final BlockPos pos)
    {
        int centerHeight;
        int sideHeight[] = new int[4];
        int cornerHeight[] = new int[4];
        int yOffset = 0;
    
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
                return EMPTY_BLOCK_STATE_KEY;
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
    
        centerHeight = neighborHeight[1][1];
    
        for(HorizontalFace side : HorizontalFace.values())
        {
            sideHeight[side.ordinal()] = neighborHeight[side.directionVector.getX() + 1][side.directionVector.getZ() + 1];
        }
    
        for(HorizontalCorner corner : HorizontalCorner.values())
        {
            cornerHeight[corner.ordinal()] = neighborHeight[corner.directionVector.getX() + 1][corner.directionVector.getZ() + 1];
        }
    
        return computeStateKey(centerHeight, sideHeight, cornerHeight, yOffset);
    
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