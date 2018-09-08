package grondag.exotic_matter.model.collision;

import java.util.function.IntConsumer;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.longs.LongComparator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/**
 * Static utility methods for BoxFinder
 */
public class BoxHelper
{
    static final long[] AREAS;
    static final int[] VOLUME_KEYS;
    static final int[] MIN_X;
    static final int[] MAX_X;
    static final int[] MIN_Y;
    static final int[] MAX_Y;
    //    private static final LongArrayList[] PATTERNS_BY_÷BIT = new LongArrayList[65];
    static final LongArrayList[] PATTERNS_BY_AREA = new LongArrayList[65];

    static final long[] EMPTY = new long[64];
    
    static enum Slice
    {
        D1_0(1, 0),
        D1_1(1, 1),
        D1_2(1, 2),
        D1_3(1, 3),
        D1_4(1, 4),
        D1_5(1, 5),
        D1_6(1, 6),
        D1_7(1, 7),
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
        
        /** 
         * Bits are set if Z layer is included. 
         * Used for fast intersection testing.
         */
        public final int layerBits;
        
        private Slice(int depth, int min)
        {
            this.depth = depth;
            this.min = min;
            this.max = min + depth - 1;
            
            int flags = 0;
            for(int i = 0; i < depth; i++)
            {
                flags |= (1 << i);
            }
            
            this.layerBits = flags;
        }
    }
    
    static
    {
        LongOpenHashSet patterns = new LongOpenHashSet();
        
        for(int xSize = 1; xSize <= 8; xSize++)
        {
            for(int ySize = 1; ySize <= 8; ySize++)
            {
                // leave very small patterns for box list to combine via face matching
                if(xSize * ySize < 2) continue;
                
                addPatterns(xSize, ySize, patterns);
            }
        }
        
        AREAS = patterns.toLongArray();
        
        LongArrays.quickSort(AREAS, new LongComparator()
        {
            @Override
            public int compare(@SuppressWarnings("null") Long o1, @SuppressWarnings("null") Long o2)
            {
                return compare(o1.longValue(), o2.longValue());
            }

            @Override
            public int compare(long k1, long k2)
            {
                // note reverse order, want largest first
                return  Integer.compare(Long.bitCount(k2), Long.bitCount(k1));
            }
        });
        
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
        
        for(int i = 0; i < AREAS.length; i++)
        {
            long p = AREAS[i];
            
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
            
            BoxHelper.MIN_X[i] = minX;
            BoxHelper.MIN_Y[i] = minY;
            BoxHelper.MAX_X[i] = maxX;
            BoxHelper.MAX_Y[i] = maxY;
        }
        
        IntArrayList volumes = new IntArrayList();
        for(Slice slice : Slice.values())
        {
            for(int i = 0; i < BoxHelper.AREAS.length; i++)
            {
                volumes.add(volumeKey(slice, i));
            }
        }
        
        VOLUME_KEYS = volumes.toIntArray();
        IntArrays.quickSort(BoxHelper.VOLUME_KEYS, new IntComparator()
        {
            @Override
            public int compare(@SuppressWarnings("null") Integer o1, @SuppressWarnings("null") Integer o2)
            {
                return compare(o1.intValue(), o2.intValue());
            }

            @Override
            public int compare(int k1, int k2)
            {
                // note reverse order, want largest first
                return  Integer.compare(k2, k1);
            }
        });
    }
    
    /**
     * Assumes values are pre-sorted.
     */
    static int intersectIndexUnsafe(int high, int low)
    {
       return high * (high - 1) / 2;
    }

    /**
     * Returns the number of maximal volume that target volume
     * must be split into if the actorVolume is chosen for output.
     * Note this total does count boxes that would be included in the
     * output volume but is zero if the boxes do not intersect.<p>
     * 
     * Produces consistent output in the cases of no intersection
     * or full inclusion, but not expected these results will ever be 
     * produced or used because won't be called for non-intersecting volumes
     * (that test happens earlier) and we have eliminated non-maximal 
     * volumes before this test happens.<p>
     * 
     * Returns 0 if the boxes do not intersect.<p>
     * 
     * Computed as the sum of actor bounds (any axis or side)
     * that are within (not on the edge) of the target volume.  
     * This works because each face within bounds will force 
     * a split of the target box along the plane of the face. 
     */
    static int splitScore(int actorVolIndex, int targetVolIndex)
    {
        final Slice actorSlice = sliceFromKey(actorVolIndex);
        final int actorPatternIndex = patternIndexFromKey(actorVolIndex);
        
        final Slice targetSlice = sliceFromKey(targetVolIndex);
        final int targetPatternIndex = patternIndexFromKey(targetVolIndex);

        // TODO: make this a slice/slice lookup and assign X,Y slices to each pattern

        
        int result = 0;
        
        if(actorSlice.min > targetSlice.min && actorSlice.min < targetSlice.max)
            result++;

        if(actorSlice.max > targetSlice.min && actorSlice.max < targetSlice.max)
            result++;

        
        if(MIN_X[actorPatternIndex] > MIN_X[targetPatternIndex] && MIN_X[actorPatternIndex] < MAX_X[targetPatternIndex])
            result++;
        
        if(MAX_X[actorPatternIndex] > MIN_X[targetPatternIndex] && MAX_X[actorPatternIndex] < MAX_X[targetPatternIndex])
            result++;
        
        if(MIN_Y[actorPatternIndex] > MIN_Y[targetPatternIndex] && MIN_Y[actorPatternIndex] < MAX_Y[targetPatternIndex])
            result++;
        
        if(MAX_Y[actorPatternIndex] > MIN_Y[targetPatternIndex] && MAX_Y[actorPatternIndex] < MAX_Y[targetPatternIndex])
            result++;
        
        return result;
    }
    

    /**
     * Validates ordering and sorts if needed.
     */
    static int intersectIndex(int a, int b)
    {
        return a > b ? intersectIndexUnsafe(a, b) : intersectIndexUnsafe(b, a);
    }

    private static void addPatterns(int xSize, int ySize, LongOpenHashSet patterns)
    {
        for(int xOrigin = 0; xOrigin <= 8 - xSize; xOrigin++)
        {
            for(int yOrigin = 0; yOrigin <= 8 - ySize; yOrigin++)
            {
                patterns.add(BoxHelper.makePattern(xOrigin, yOrigin, xSize, ySize));
            }
        }
    }

    static long makePattern(int xOrigin, int yOrigin, int xSize, int ySize)
    {
        long pattern = 0;
        for(int x = 0; x < xSize; x++)
        {
            for(int y = 0; y < ySize; y++)
            {
                pattern |= (1L << BoxHelper.bitIndex(xOrigin + x, yOrigin + y));
            }
        }
        return pattern;
    }

    static int bitIndex(int x, int y)
    {
        return x | (y << 3);
    }

    /**
     * Encodes a volume key that is naturally sortable by volume. (Larger values imply larger volume).
     */
    static int volumeKey(Slice slice, int patternIndex)
    {
        int volume = volume(slice, patternIndex);
        return (volume << 17) | (patternIndex << 6) | slice.ordinal();
    }

    static int volume(Slice slice, int patternIndex)
    {
        int x = MAX_X[patternIndex] - MIN_X[patternIndex] + 1;
        int y = MAX_Y[patternIndex] - MIN_Y[patternIndex] + 1;
        int volume = slice.depth * x * y;
        assert x * y == Long.bitCount(AREAS[patternIndex]);
        return  volume;
    }

    static int volumeFromKey(int volumeKey)
    {
        return (volumeKey >> 17);
    }

    static int patternIndexFromKey(int volumeKey)
    {
        return (volumeKey >> 6) & 2047;
    }

    static long patternFromKey(int volumeKey)
    {
        return AREAS[patternIndexFromKey(volumeKey)];
    }

    static Slice sliceFromKey(int volumeKey)
    {
        return Slice.values()[(volumeKey & 63)];
    }

    /**
     * True if volumes share any voxels, including case where one volume fully includes the other.
     */
    static boolean doVolumesIntersect(int volumeKey0, int volumeKey1)
    {
        return (sliceFromKey(volumeKey0).layerBits & sliceFromKey(volumeKey1).layerBits) != 0L
                &&  (patternFromKey(volumeKey0) & patternFromKey(volumeKey1)) != 0L;
    }

    /**
     * True if volumes share no voxels.
     */
    static boolean areVolumesDisjoint(int volumeKey0, int volumeKey1)
    {
        return (sliceFromKey(volumeKey0).layerBits & sliceFromKey(volumeKey1).layerBits) == 0
                ||  (patternFromKey(volumeKey0) & patternFromKey(volumeKey1)) == 0L;
    }

    /**
     * True if the "big" volume fully includes the "small" volume.
     * False is the volumes are the same volume, if "small" volume
     * is actually larger, or if the small volume contains any voxels
     * not part of the big volume.
     */
    static boolean isVolumeIncluded(int bigKey, int smallKey)
    {
        // big volume must be larger than and distinct from the small volume
        if(volumeFromKey(bigKey) <= volumeFromKey(smallKey))
            return false;
        
        final int smallSliceBits  = sliceFromKey(smallKey).layerBits;
        if((sliceFromKey(bigKey).layerBits & smallSliceBits) != smallSliceBits)
            return false;
    
        final long smallPattern =  patternFromKey(smallKey);
        return ((patternFromKey(bigKey) & smallPattern) == smallPattern);
    }
    
    static void forEachBit(long bits, IntConsumer consumer)
    {
        if(bits != 0)
        {
            forEachBit32((int)(bits & 0xFFFFFFFFL), 0, consumer);
            forEachBit32((int)((bits >> 32) & 0xFFFFFFFFL), 32, consumer);
        }
    }
    
    private static void forEachBit32(int bits, int baseIndex, IntConsumer consumer)
    {
        if(bits != 0)
        {
            forEachBit16((bits & 0xFFFF), baseIndex, consumer);
            forEachBit16(((bits >> 16) & 0xFFFF), baseIndex + 16, consumer);
        }
    }
    
    private static void forEachBit16(int bits, int baseIndex, IntConsumer consumer)
    {
        if(bits != 0)
        {
            forEachBit8((bits & 0xFF), baseIndex, consumer);
            forEachBit8(((bits >> 8) & 0xFF), baseIndex + 8, consumer);
        }
    }
    
    private static void forEachBit8(int bits, int baseIndex, IntConsumer consumer)
    {
        if(bits != 0)
        {
            forEachBit4((bits & 0xF), baseIndex, consumer);
            forEachBit4(((bits >> 4) & 0xF), baseIndex + 4, consumer);
        }
    }
    
    private static void forEachBit4(int bits, int baseIndex, IntConsumer consumer)
    {
        switch(bits)
        {
        case 0:
            break;
            
        case 1:
            consumer.accept(baseIndex);
            break;
            
        case 2:
            consumer.accept(baseIndex + 1);
            break;
            
        case 3:
            consumer.accept(baseIndex);
            consumer.accept(baseIndex + 1);
            break;
            
        case 4:
            consumer.accept(baseIndex + 2);
            break;
            
        case 5:
            consumer.accept(baseIndex);
            consumer.accept(baseIndex + 2);
            break;

        case 6:
            consumer.accept(baseIndex + 1);
            consumer.accept(baseIndex + 2);
            break;
        
        case 7:
            consumer.accept(baseIndex);
            consumer.accept(baseIndex + 1);
            consumer.accept(baseIndex + 2);
            break;

        case 8:
            consumer.accept(baseIndex + 3);
            break;

        case 9:
            consumer.accept(baseIndex);
            consumer.accept(baseIndex + 3);
            break;
            
        case 10:
            consumer.accept(baseIndex + 1);
            consumer.accept(baseIndex + 3);
            break;

        case 11:
            consumer.accept(baseIndex);
            consumer.accept(baseIndex + 1);
            consumer.accept(baseIndex + 3);        
            break;

        case 12:
            consumer.accept(baseIndex + 2);
            consumer.accept(baseIndex + 3);
            break;

        case 13:
            consumer.accept(baseIndex);
            consumer.accept(baseIndex + 2);
            consumer.accept(baseIndex + 3);
            break;

        case 14:
            consumer.accept(baseIndex + 1);
            consumer.accept(baseIndex + 2);
            consumer.accept(baseIndex + 3);
            break;
        
        case 15:
            consumer.accept(baseIndex);
            consumer.accept(baseIndex + 1);
            consumer.accept(baseIndex + 2);
            consumer.accept(baseIndex + 3);
            break;
        }
    }
}