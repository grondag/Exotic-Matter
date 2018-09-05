package grondag.exotic_matter.model.collision;

import grondag.exotic_matter.model.collision.CollisionBoxList.Builder;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/**
 * Quickly identifies largest filled AABBs within an 8x8x8 voxel volume
 * and outputs those boxes plus any unclaimed voxels. <p>
 * 
 * Exploits representation of each 8x8x1 layer as a single long value to
 * enable fast bit-wise comparisons. <p>
 */
public class BoxFinder
{
    private static final long[] LAYER_PATTERNS;
    private static final int[] MIN_X;
    private static final int[] MAX_X;
    private static final int[] MIN_Y;
    private static final int[] MAX_Y;
    
    
//    private static final LongArrayList[] PATTERNS_BY_÷BIT = new LongArrayList[65];
    private static final LongArrayList[] PATTERNS_BY_AREA = new LongArrayList[65];
    
    static
    {
        LongOpenHashSet patterns = new LongOpenHashSet();
        
        // Exclude single-width shapes. Face join logic in CollisionBoxList will handle those
        for(int xSize = 2; xSize <= 8; xSize++)
        {
            for(int ySize = 2; ySize <= 8; ySize++)
            {
                addPatterns(xSize, ySize, patterns);
            }
        }
        
        LAYER_PATTERNS = patterns.toLongArray();
        MIN_X = new int[patterns.size()];
        MIN_Y = new int[patterns.size()];
        MAX_X = new int[patterns.size()];
        MAX_Y = new int[patterns.size()];
        
        int[][] countByBit = new int[2][64];

        for(int i = 0; i <= 64; i++)
        {
//            PATTERNS_BY_BIT[i] = new LongArrayList();
            PATTERNS_BY_AREA[i] = new LongArrayList();
        }
        final long BIT_27 = 1L << 27;
        
        for(int i = 0; i < LAYER_PATTERNS.length; i++)
        {
            long p = LAYER_PATTERNS[i];
            
            PATTERNS_BY_AREA[Long.bitCount(p)].add(p);
            
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;

            for(int bit = 0; bit < 64; bit++)
            {
                if((p & (1L << bit)) != 0)
                {
                    int x = bit & 7;
                    int y = (bit >> 3) & 7;
                    minX = Math.min(x,  minX);
                    minY = Math.min(y,  minY);
                    maxX = Math.max(x,  maxX);
                    maxY = Math.max(y,  maxY);
                    
                    final int a = (p & BIT_27) == BIT_27 ? 1 : 0;
                    countByBit[a][bit]++;
                }
            }
            
            MIN_X[i] = minX;
            MIN_Y[i] = minY;
            MAX_X[i] = maxX;
            MAX_Y[i] = maxY;
        }
        
        System.out.println(LAYER_PATTERNS.length);
    }
    
    private static void addPatterns(int xSize, int ySize, LongOpenHashSet patterns)
    {
        for(int xOrigin = 0; xOrigin <= 8 - xSize; xOrigin++)
        {
            for(int yOrigin = 0; yOrigin <= 8 - ySize; yOrigin++)
            {
                patterns.add(makePattern(xOrigin, yOrigin, xSize, ySize));
            }
        }
    }

    private static long makePattern(int xOrigin, int yOrigin, int xSize, int ySize)
    {
        long pattern = 0;
        for(int x = 0; x < xSize; x++)
        {
            for(int y = 0; y < ySize; y++)
            {
                pattern |= (1L << bitIndex(xOrigin + x, yOrigin + y));
            }
        }
        return pattern;
    }
    
    //TODO finish or remove
//    private static class SearchNode
//    {
//        /**
//         * Bit this node uses to split the search.
//         */
//        final int bitIndex;
//        
//        /**
//         * If this node splits, the node holding values where our bit = 0.
//         */
//        final @Nullable SearchNode falseNode;
//        
//        /**
//         * If this node splits, the node holding values where our bit = 1.
//         */
//        final @Nullable SearchNode trueNode;
//        
//        /**
//         * If this node is a leaf, the values at this node.
//         */
//        final @Nullable LongArrayList values;
//        
//        /**
//         * Bits set to 1 have been used in parent nodes and should not be used for splits
//         */
//        final long usedBits;
//        
//        SearchNode(LongArrayList values, long usedBits)
//        {
//            this.values = values;
//        }
//    }
    
    private static int bitIndex(int x, int y)
    {
        return x | (y << 3);
    }
    
    private static long[] EMPTY = new long[8];
    
    private long[] voxels = new long[8];
    
    private long[] combined = new long[Slice.values().length];
    
    private static enum Slice
    {
        D2_0(2, 0),
        D2_1(2, 1),
        D2_2(2, 2),
        D2_3(2, 3),
        D2_4(2, 4),
        D2_5(2, 5),
        D2_6(2, 6),
        D3_0(3, 0),
        D3_1(3, 1),
        D3_2(3, 2),
        D3_3(3, 3),
        D3_4(3, 4),
        D3_5(3, 5),
        D4_0(4, 0),
        D4_1(4, 1),
        D4_2(4, 2),
        D4_3(4, 3),
        D4_4(4, 4),
        D5_0(5, 0),
        D5_1(5, 1),
        D5_2(5, 2),
        D5_3(5, 3),
        D6_0(6, 0),
        D6_1(6, 1),
        D6_2(6, 2),
        D7_0(7, 0),
        D7_1(7, 1),
        D8_0(8, 0);
        
        public final int depth;
        public final int min;
        public final int max;
        
        private Slice(int depth, int min)
        {
            this.depth = depth;
            this.min = min;
            this.max = min + depth - 1;
        }
    }
    
    
    public void clear()
    {
        System.arraycopy(EMPTY, 0, voxels, 0, 8);
    }
    
    /**
     * Coordinates must be 0 - 8
     */
    public void setFilled(int x, int y, int z)
    {
        voxels[z] |= (1L << bitIndex(x, y));
    }
    
    public void setEmpty(int x, int y, int z)
    {
        voxels[z] &= ~(1L << bitIndex(x, y));
    }
    
    public void setEmpty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ)
    {
        for(int x = minX; x <= maxX; x++)
        {
            for(int y = minY; y <= maxY; y++)
            {
                for(int z = minZ; z <= maxZ; z++)
                {
                    setEmpty(x, y, z);
                }
            }            
        }
    }
    
    public void outputBoxes(VoxelOctTree voxels, CollisionBoxList.Builder builder)
    {
        loadVoxels(voxels);
        
        while(outputLargest(voxels, builder)) {};
        
        outputRemnants(builder);
        
    }
    
    private boolean outputLargest(VoxelOctTree voxels, Builder builder)
    {
        calcCombined();
        
        Slice bestSlice = null;
        int bestVolume = 0;
        int bestIndex = -1;
        
        for(Slice slice : Slice.values())
        {
            for(int i = 0; i < LAYER_PATTERNS.length; i++)
            {
                long pattern = LAYER_PATTERNS[i];
                
                if((pattern & combined[slice.ordinal()]) == pattern)
                {
                    int v = slice.depth * Long.bitCount(pattern);
                    if(v > bestVolume)
                    {
                        bestVolume = v;
                        bestSlice = slice;
                        bestIndex = i;
                    }
                }
            }
        }
        
        if(bestSlice == null) 
            return false;
        
        setEmpty(MIN_X[bestIndex], MIN_Y[bestIndex], bestSlice.min, MAX_X[bestIndex], MAX_Y[bestIndex], bestSlice.max);
        builder.add(MIN_X[bestIndex], MIN_Y[bestIndex], bestSlice.min, MAX_X[bestIndex] + 1, MAX_Y[bestIndex] + 1, bestSlice.max + 1);
        
        return true;
    }

    private void loadVoxels(VoxelOctTree voxels)
    {
        voxels.forEachBottom(v -> 
        {
            if(v.isFull())
                setFilled(v.xMin8(), v.yMin8(), v.zMin8());
        });
    }
    
    private void calcCombined()
    {
        combined[Slice.D2_0.ordinal()] = voxels[0] & voxels[1];
        combined[Slice.D2_1.ordinal()] = voxels[1] & voxels[2];
        combined[Slice.D2_2.ordinal()] = voxels[2] & voxels[3];
        combined[Slice.D2_3.ordinal()] = voxels[3] & voxels[4];
        combined[Slice.D2_4.ordinal()] = voxels[4] & voxels[5];
        combined[Slice.D2_5.ordinal()] = voxels[5] & voxels[6];
        combined[Slice.D2_6.ordinal()] = voxels[6] & voxels[7];
        
        combined[Slice.D3_0.ordinal()] = combined[Slice.D2_0.ordinal()] & voxels[2];
        combined[Slice.D3_1.ordinal()] = combined[Slice.D2_1.ordinal()] & voxels[3];
        combined[Slice.D3_2.ordinal()] = combined[Slice.D2_2.ordinal()] & voxels[4];
        combined[Slice.D3_3.ordinal()] = combined[Slice.D2_3.ordinal()] & voxels[5];
        combined[Slice.D3_4.ordinal()] = combined[Slice.D2_4.ordinal()] & voxels[6];
        combined[Slice.D3_5.ordinal()] = combined[Slice.D2_5.ordinal()] & voxels[7];
        
        combined[Slice.D4_0.ordinal()] = combined[Slice.D3_0.ordinal()] & voxels[3];
        combined[Slice.D4_1.ordinal()] = combined[Slice.D3_1.ordinal()] & voxels[4];
        combined[Slice.D4_2.ordinal()] = combined[Slice.D3_2.ordinal()] & voxels[5];
        combined[Slice.D4_3.ordinal()] = combined[Slice.D3_3.ordinal()] & voxels[6];
        combined[Slice.D4_4.ordinal()] = combined[Slice.D3_4.ordinal()] & voxels[7];
        
        combined[Slice.D5_0.ordinal()] = combined[Slice.D4_0.ordinal()] & voxels[4];
        combined[Slice.D5_1.ordinal()] = combined[Slice.D4_1.ordinal()] & voxels[5];
        combined[Slice.D5_2.ordinal()] = combined[Slice.D4_2.ordinal()] & voxels[6];
        combined[Slice.D5_3.ordinal()] = combined[Slice.D4_3.ordinal()] & voxels[7];

        combined[Slice.D6_0.ordinal()] = combined[Slice.D5_0.ordinal()] & voxels[5];
        combined[Slice.D6_1.ordinal()] = combined[Slice.D5_1.ordinal()] & voxels[6];
        combined[Slice.D6_2.ordinal()] = combined[Slice.D5_2.ordinal()] & voxels[7];

        combined[Slice.D7_0.ordinal()] = combined[Slice.D6_0.ordinal()] & voxels[6];
        combined[Slice.D7_1.ordinal()] = combined[Slice.D6_1.ordinal()] & voxels[7];

        combined[Slice.D8_0.ordinal()] = combined[Slice.D7_0.ordinal()] & voxels[7];
    }
    
    private void outputRemnants(CollisionBoxList.Builder builder)
    {
        for(int z = 0; z < 8; z++ )
        {
            final long bits = voxels[z];
            if(bits == 0) continue;
            
            for(int i = 0; i < 64; i++)
            {
                if((bits & (1L << i)) != 0L)
                {
                    int x = i & 7;
                    int y = (i >> 3) & 7;
                    builder.addSorted(x, y, z, x + 1, y + 1, z + 1);
                }   
            }
        }
    }
}
