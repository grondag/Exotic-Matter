package grondag.exotic_matter.model.collision;

import java.util.function.IntConsumer;

import grondag.exotic_matter.varia.functions.IAreaBoundsIntFunction;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.longs.LongComparator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/**
 * Static utility methods for BoxFinder
 */
public class BoxFinderUtils
{
    static final long[] AREAS;
    static final int[] VOLUME_KEYS;

    static final long[] EMPTY = new long[64];
    
    static final Slice[][] lookupMinMax = new Slice[8][8];
    
    //TODO: remove
//    static final ArrayList<VolumeFilter> VOLUME_FILTERS;
    
    /**
     * Max is inclusive and equal to max attribute of slice.
     */
    public static Slice sliceByMinMax(int minZ, int maxZ)
    {
        return lookupMinMax[minZ][maxZ];
    }
    
    /**
     * All possible contiguous sections of the Z-axis into 1/8 units.<p>
     * 
     * In retrospect, would probably be better if Slice were simply an 8-bit word instead of an enum
     * but code is working and is "fast enough" for now. Could matter, tho, with LOR.
     */
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
                flags |= (1 << (min + i));
            }
            
            this.layerBits = flags;
        }
    }
    
    static
    {
        for(Slice slice : Slice.values())
        {
            lookupMinMax[slice.min][slice.max] = slice;
        }
        
        LongOpenHashSet patterns = new LongOpenHashSet();
         
        for(int xSize = 1; xSize <= 8; xSize++)
        {
            for(int ySize = 1; ySize <= 8; ySize++)
            {
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
        
        IntArrayList volumes = new IntArrayList();
        for(Slice slice : Slice.values())
        {
            for(int i = 0; i < AREAS.length; i++)
            {
                if(slice.depth * Long.bitCount(AREAS[i]) > 1)
                    volumes.add(volumeKey(slice, i));
            }
        }
        
        VOLUME_KEYS = volumes.toIntArray();
        IntArrays.quickSort(BoxFinderUtils.VOLUME_KEYS, new IntComparator()
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
        
//        VOLUME_FILTERS = createVolumeFilters();
    }
    
    
    static void findBestExclusionBits()
    {
        final IntArrayList[] VOLUMES_BY_BIT = new IntArrayList[512];
        final int[] COUNTS_BY_BIT = new int[512];
        int coverageCount = 0;
        
        for(int i = 0; i < 512; i++)
        {
            VOLUMES_BY_BIT[i] = new IntArrayList();
        }
        
        for(int v = 0; v <VOLUME_KEYS.length; v++)
        {
            final int k = VOLUME_KEYS[v];
            final Slice slice = sliceFromKey(k);
            final long pattern = patternFromKey(k);
            final int vFinal = v;
            
            forEachBit(pattern, i ->  
            {
                final int x = xFromAreaBitIndex(i);
                final int y = yFromAreaBitIndex(i);
                for(int z = slice.min; z <= slice.max; z++)
                {
                    final int n = x | (y << 3) | (z << 6);
                    VOLUMES_BY_BIT[n].add(vFinal);
                    COUNTS_BY_BIT[n]++;
                }
            });
        }
        
        boolean coverage[] = new boolean[VOLUME_KEYS.length];
        
        int firstIndex = -1;
        int bestCount = -1;
        for(int i =  0; i < 512; i++)
        {
            if(COUNTS_BY_BIT[i] > bestCount)
            {
                bestCount = COUNTS_BY_BIT[i];
                firstIndex = i;
            }
        }
        
        coverageCount += bestCount;
        System.out.println("First bit coverage  = " + bestCount);
        
        for(int v : VOLUMES_BY_BIT[firstIndex])
            coverage[v] = true;
        
        int secondIndex = -1;
        bestCount = -1;
        for(int i =  0; i < 512; i++)
        {
            if(i == firstIndex)  continue;
            
            if(COUNTS_BY_BIT[i] > bestCount)
            {
                int c = 0;
                for(int j : VOLUMES_BY_BIT[i])
                    if(!coverage[j]) c++;

                if(c  > bestCount)
                {
                    bestCount = c;
                    secondIndex = i;
                }
            }
            
            for(int v : VOLUMES_BY_BIT[secondIndex])
                coverage[v] = true;
        }
        
        coverageCount += bestCount;
        System.out.println("Second bit coverage  = " + bestCount);
        
        int thirdIndex = -1;
        bestCount = -1;
        for(int i =  0; i < 512; i++)
        {
            if(i == firstIndex || i == secondIndex)  continue;
            
            if(COUNTS_BY_BIT[i] > bestCount)
            {
                int c = 0;
                for(int j : VOLUMES_BY_BIT[i])
                    if(!coverage[j]) c++;

                if(c  > bestCount)
                {
                    bestCount = c;
                    thirdIndex = i;
                }
            }
            
            for(int v : VOLUMES_BY_BIT[thirdIndex])
                coverage[v] = true;
        }
        
        coverageCount += bestCount;
        System.out.println("Third bit coverage  = " + bestCount);
        
        int fourthIndex = -1;
        bestCount = -1;
        for(int i =  0; i < 512; i++)
        {
            if(i == firstIndex || i == secondIndex)  continue;
            
            if(COUNTS_BY_BIT[i] > bestCount)
            {
                int c = 0;
                for(int j : VOLUMES_BY_BIT[i])
                    if(!coverage[j]) c++;

                if(c  > bestCount)
                {
                    bestCount = c;
                    fourthIndex = i;
                }
            }
            
            for(int v : VOLUMES_BY_BIT[fourthIndex])
                coverage[v] = true;
        }
        
        coverageCount += bestCount;
        System.out.println("Fourth bit coverage  = " + bestCount);
        
        System.out.println("Coverge % = " + 100 * coverageCount / VOLUME_KEYS.length);
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
     * Note this total does not count boxes that would be included in the
     * output volume.<p>
     * 
     * Returns 0 if the boxes do not intersect.<p>
     * 
     * Computed as the sum of actor bounds (any axis or side)
     * that are within (not on the edge) of the target volume.  
     * This works because each face within bounds will force 
     * a split of the target box along the plane of the face.
     * 
     * We subtract one from this total because we aren't counting
     * the boxes that would be absorbed by the actor volume.
     */
    static int splitScore(int actorVolIndex, int targetVolIndex)
    {
        final Slice actorSlice = sliceFromKey(actorVolIndex);
        final Slice targetSlice = sliceFromKey(targetVolIndex);

        int result = 0;
        
        // Must be >= or <= on one side of comparison because indexes are voxels and actual face depends on usage (min/max)
        
        if(actorSlice.min > targetSlice.min && actorSlice.min <= targetSlice.max)
            result++;

        if(actorSlice.max >= targetSlice.min && actorSlice.max < targetSlice.max)
            result++;

        result += testAreaBounds(patternFromKey(targetVolIndex), (targetMinX, targetMinY, targetMaxX, targetMaxY) ->
        {
            return testAreaBounds(patternFromKey(actorVolIndex), (actorMinX, actorMinY, actorMaxX, actorMaxY) ->
            {
                int n = 0;
                if(actorMinX > targetMinX && actorMinX <= targetMaxX)
                    n++;
                
                if(actorMaxX >= targetMinX && actorMaxX < targetMaxX)
                    n++;
                
                if(actorMinY > targetMinY && actorMinY <= targetMaxY)
                    n++;
                
                if(actorMaxY >= targetMinY && actorMaxY < targetMaxY)
                    n++;
                
                return n;
            });
        });
        
        return result == 0 ? 0 : result - 1;
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
                long pattern = makePattern(xOrigin, yOrigin, xSize, ySize);
                
//                if(yOrigin + ySize < 8)
//                    assert ((pattern << 8) | pattern) == makePattern(xOrigin, yOrigin, xSize, ySize + 1);
//                
//                if(yOrigin > 0)
//                    assert ((pattern >>> 8) | pattern) == makePattern(xOrigin, yOrigin - 1, xSize, ySize + 1);
//                
//                final int x0 = xOrigin;
//                final int y0 = yOrigin;
//                final int x1 = xOrigin + xSize - 1;
//                final int y1 = yOrigin + ySize - 1;
//                testAreaBounds(pattern, (minX, minY, maxX, maxY) ->
//                {
//                    assert minX == x0;
//                    assert minY == y0;
//                    assert maxX == x1;
//                    assert maxY == y1;
//                    return 0;
//                });
                
                patterns.add(pattern);
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
                pattern |= (1L << areaBitIndex(xOrigin + x, yOrigin + y));
            }
        }
        return pattern;
    }

    static int areaBitIndex(int x, int y)
    {
        return x | (y << 3);
    }
    
    static int xFromAreaBitIndex(int bitIndex)
    {
        return bitIndex & 7;
    }
    
    static int yFromAreaBitIndex(int bitIndex)
    {
        return (bitIndex >>> 3) & 7;
    }
    
    /**
     * Single-pass, bitwise derivation of x, y bounds for given area pattern. 
     * Area does not have to be fully populated to work.
     */
    static int testAreaBounds(long areaPattern, IAreaBoundsIntFunction test)
    {
        if(areaPattern == 0L)
        {
            assert false : "Bad (zero) argument to findAreaBounds";
            return 0;
        }
        
        long xBits = 0;
        
        xBits = areaPattern | (areaPattern >>> 32);
        xBits |= xBits >>> 16;
        xBits |= xBits >>> 8;
        xBits &= 0xFFL;
        
        return test.apply(minX(xBits), minY(areaPattern), maxX(xBits), maxY(areaPattern));
    }
    
    /**
     * For testing. 
     */
    static boolean doAreaBoundsMatch(long areaPattern, int minX, int minY, int maxX, int maxY)
    {
        return testAreaBounds(areaPattern, (x0, y0, x1, y1) ->
        {
            return (x0 == minX && y0 == minY && x1 == maxX && y1 == maxY) ? 1 : 0;
        }) == 1;
    }
    
    private static int minX(long xBits)
    {
        if((xBits & 0b1111) == 0)
        {
            if((xBits & 0b110000) == 0)
                return (xBits & 0b1000000) == 0 ? 7 : 6;
            else
                return (xBits & 0b10000) == 0 ? 5 : 4;
        }
        else
        {
            if((xBits & 0b11) == 0)
                return (xBits & 0b0100) == 0 ? 3 : 2;
            else
                return (xBits & 0b1) == 0 ? 1 : 0;
        }
    }
    
    private static int minY(long yBits)
    {
        if((yBits & 0xFFFFFFFFL) == 0L)
        {
            if((yBits & 0xFFFFFFFFFFFFL) == 0L)
                return (yBits & 0xFFFFFFFFFFFFFFL) == 0L ? 7 : 6;
            else
                return (yBits & 0xFFFFFFFFFFL) == 0L ? 5 : 4;
        }
        else
        {
            if((yBits & 0xFFFFL) == 0L)
                return (yBits & 0xFF0000L) == 0L ? 3 : 2;
            else
                return (yBits & 0xFFL) == 0L ? 1 : 0;
        }
    }
    
    private static int maxX(long xBits)
    {
        if((xBits & 0b11110000) == 0)
        {
            if((xBits & 0b1100) == 0)
                return (xBits & 0b10) == 0 ? 0 : 1;
            else
                return (xBits & 0b1000) == 0 ? 2 : 3;
        }
        else
        {
            if((xBits & 0b11000000) == 0)
                return (xBits & 0b100000) == 0 ? 4 : 5;
            else
                return (xBits & 0b10000000) == 0 ? 6 : 7;
        }
    }
    
    private static int maxY(long yBits)
    {
        if((yBits & 0xFFFFFFFF00000000L) == 0L)
        {
            if((yBits & 0xFFFF0000L) == 0L)
                return (yBits & 0xFF00L) == 0L ? 0 : 1;
            else
                return (yBits & 0xFF000000) == 0L ? 2 : 3;
        }
        else
        {
            if((yBits & 0xFFFF000000000000L) == 0L)
                return (yBits & 0xFF0000000000L) == 0L ? 4 : 5;
            else
                return (yBits & 0xFF00000000000000L) == 0L ? 6 : 7;
        }
    }
    
    /**
     * Encodes a volume key that is naturally sortable by volume. (Larger values imply larger volume).
     */
    static int volumeKey(Slice slice, int patternIndex)
    {
        int volume = volume(slice, patternIndex);
        int result = (volume << 17) | (patternIndex << 6) | slice.ordinal();
        assert result > 0;
        return result;
    }

    static int volume(Slice slice, int patternIndex)
    {
        return slice.depth * Long.bitCount(AREAS[patternIndex]);
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
        return (sliceFromKey(volumeKey0).layerBits & sliceFromKey(volumeKey1).layerBits) != 0
                &&  (patternFromKey(volumeKey0) & patternFromKey(volumeKey1)) != 0L;
    }
    
    static boolean doesVolumeIncludeBit(int volumeKey, int x, int y, int z)
    {
        return (sliceFromKey(volumeKey).layerBits & (1 << z)) != 0
                &&  (patternFromKey(volumeKey) & (1L << areaBitIndex(x, y))) != 0L;
    }
    
    /**
     * True if volume matches the given bounds.<br>
     * Second point coordinates are inclusive.
     */
    static boolean areVolumesSame(int volumeKey, int x0, int y0, int z0, int x1, int y1, int z1)
    {
        return sliceFromKey(volumeKey).min == z0 & sliceFromKey(volumeKey).max == z1
                &&  doAreaBoundsMatch(patternFromKey(volumeKey), x0, y0, x1, y1);
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
            forEachBit32((int)((bits >>> 32) & 0xFFFFFFFFL), 32, consumer);
        }
    }
    
    private static void forEachBit32(int bits, int baseIndex, IntConsumer consumer)
    {
        if(bits != 0)
        {
            forEachBit16((bits & 0xFFFF), baseIndex, consumer);
            forEachBit16(((bits >>> 16) & 0xFFFF), baseIndex + 16, consumer);
        }
    }
    
    private static void forEachBit16(int bits, int baseIndex, IntConsumer consumer)
    {
        if(bits != 0)
        {
            forEachBit8((bits & 0xFF), baseIndex, consumer);
            forEachBit8(((bits >>> 8) & 0xFF), baseIndex + 8, consumer);
        }
    }
    
    private static void forEachBit8(int bits, int baseIndex, IntConsumer consumer)
    {
        if(bits != 0)
        {
            forEachBit4((bits & 0xF), baseIndex, consumer);
            forEachBit4(((bits >>> 4) & 0xF), baseIndex + 4, consumer);
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
    
    public static int bitCount8(int byteValue)
    {
        return bitCount4(byteValue & 0xF) + bitCount4((byteValue >>> 4) & 0xF);
    }
    
    public static int bitCount4(int halfByteValue)
    {
        switch(halfByteValue)
        {
        case 0:
            return 0;
            
        case 1:
        case 2:
        case 4:
        case 8:
            return 1;
            
        case 3:
        case 5:
        case 6:
        case 9:
        case 10:
        case 12:
            return 2;
            
        case 7:
        case 11:
        case 13:
        case 14:
            return 3;

        case 15:
            return 4;
        }
        assert false : "bad bitcount4 value";
        return 0;
    }
}
