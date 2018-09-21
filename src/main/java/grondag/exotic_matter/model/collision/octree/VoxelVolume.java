package grondag.exotic_matter.model.collision.octree;

import static grondag.exotic_matter.model.collision.octree.OctreeCoordinates.*;

import grondag.exotic_matter.varia.functions.Int3Consumer;

/**
 * Operations on Cartesian representation of voxels 
 * that can happen more efficiently that way. (Filling, mostly)
 *
 */
public class VoxelVolume
{
    public static void loadVolume(VoxelOctree8 voxels, long[] data)
    {
        voxels.visit((index, divisionLevel, isLeaf) ->
        {
            if(isLeaf)
            {
                if(voxels.isFull(index, divisionLevel))
                {
                    int xyz = OctreeCoordinates.indexToXYZ3(index);
                    data[xyz >> 6] |= (1L << (xyz & 63));
                }
                return false;
            }
            return !voxels.isEmpty(index, divisionLevel);
        });
    }
    
    /**
     * Bits true for exterior bits in 8x8x8 volume.
     */
    static final long[] EXTERIOR_MASK_8 = new long[8];
    
    static
    {
        setBit(packedXYZ3(0, 0, 0), EXTERIOR_MASK_8);
        setBit(packedXYZ3(0, 0, 7), EXTERIOR_MASK_8);
        setBit(packedXYZ3(0, 7, 0), EXTERIOR_MASK_8);
        setBit(packedXYZ3(0, 7, 7), EXTERIOR_MASK_8);
        setBit(packedXYZ3(7, 0, 0), EXTERIOR_MASK_8);
        setBit(packedXYZ3(7, 0, 7), EXTERIOR_MASK_8);
        setBit(packedXYZ3(7, 7, 0), EXTERIOR_MASK_8);
        setBit(packedXYZ3(7, 7, 7), EXTERIOR_MASK_8);
        
        for(int i = 1; i < 7; i++)
        {
            setBit(packedXYZ3(0, 0, i), EXTERIOR_MASK_8);
            setBit(packedXYZ3(0, i, 0), EXTERIOR_MASK_8);
            setBit(packedXYZ3(i, 0, 0), EXTERIOR_MASK_8);
            
            setBit(packedXYZ3(7, 0, i), EXTERIOR_MASK_8);
            setBit(packedXYZ3(7, i, 0), EXTERIOR_MASK_8);
            setBit(packedXYZ3(i, 7, 0), EXTERIOR_MASK_8);
            
            setBit(packedXYZ3(0, 7, i), EXTERIOR_MASK_8);
            setBit(packedXYZ3(0, i, 7), EXTERIOR_MASK_8);
            setBit(packedXYZ3(i, 0, 7), EXTERIOR_MASK_8);
            
            setBit(packedXYZ3(7, 7, i), EXTERIOR_MASK_8);
            setBit(packedXYZ3(7, i, 7), EXTERIOR_MASK_8);
            setBit(packedXYZ3(i, 7, 7), EXTERIOR_MASK_8);
        }
        
        for(int i = 1; i < 7; i++)
        {
            for(int j = 1; j < 7; j++)
            {
                setBit(packedXYZ3(i, j, 0), EXTERIOR_MASK_8);
                setBit(packedXYZ3(i, j, 7), EXTERIOR_MASK_8);
                
                setBit(packedXYZ3(i, 0, j), EXTERIOR_MASK_8);
                setBit(packedXYZ3(i, 7, j), EXTERIOR_MASK_8);
                
                setBit(packedXYZ3(0, i, j), EXTERIOR_MASK_8);
                setBit(packedXYZ3(7, i, j), EXTERIOR_MASK_8);
            }
        }
    }
    
    static final ThreadLocal<long[]> workBits = new ThreadLocal<long[]>()
    {
        @Override
        protected long[] initialValue()
        {
            return new long[16];
        }
    };
    
    public static void forEachSimpleVoxel(VoxelOctree8 voxels, Int3Consumer consumer)
    {
        final long[] data = workBits.get();
        loadVolume(voxels, data);
        fillVolume8(data);
        forEachSimpleVoxelInner(data, consumer);
        System.arraycopy(ALL_EMPTY, 0, data, 0, 16);
    }
    
    static void forEachSimpleVoxelInner(long[] data, Int3Consumer consumer)
    {
        for(int x = 0; x < 8; x += 2)
        {
            for(int y = 0; y < 8; y += 2)
            {
                final long mask = 0b0000001100000011L << (x + y * 8);
                
                for(int z = 0; z < 8; z += 2)
                {
                    int count = Long.bitCount(data[z + 8] & mask) + Long.bitCount(data[z + 9] & mask);
                    if(count >= 4)
                        consumer.accept(x, y, z);
                }
            }
        }
    }
    
    /**
     * Fills all interior voxels not reachable from an exterior voxel that is already clear.
     * Works by starting with a full volume and "etching" from the outside via simple
     * flood fill until it is stopped by the shell data provided.<p>
     * 
     * To ensure low garbage, requires array be sized to hold two sets of results.
     * Expects source data in lower half.  Output is in upper half.
     */
    public static void fillVolume8(long[] data)
    {
        System.arraycopy(ALL_FULL, 0, data, 8, 8);
        
        // corners and edges provide no access to interior, and
        // we can use inputt state for testing face voxels, 
        // so simply copy them
        copyExterior8(data);
        
        // if exterior is clear try interior for each voxel on each face
        for(int i = 1; i < 7; i++)
        {
            for(int j = 1; j < 7; j++)
            {
                if(isClear(packedXYZ3(i, 0, j), data))
                    floodFill8(packedXYZ3(i, 1, j), data);
                
                if(isClear(packedXYZ3(i, 7, j), data))
                    floodFill8(packedXYZ3(i, 6, j), data);
                
                if(isClear(packedXYZ3(i, j, 0), data))
                    floodFill8(packedXYZ3(i, j, 1), data);
                
                if(isClear(packedXYZ3(i, j, 7), data))
                    floodFill8(packedXYZ3(i, j, 6), data);
                
                if(isClear(packedXYZ3(0, i, j), data))
                    floodFill8(packedXYZ3(1, i, j), data);
                
                if(isClear(packedXYZ3(7, i,  j), data))
                    floodFill8(packedXYZ3(6, i, j), data);
            }
        }
    }
    
    /**
     * Assumes template shell in lower half of array and carved bits in upper
     */
    static void floodFill8(int index, long[] data)
    {
        if(isClear(index, data) && isSet(index + 512, data))
        {
            clearBit(index + 512, data);
            
            final int x = index & 7;
            if(x  > 1)
                floodFill8(index - 1, data);
            
            if(x < 6)
                floodFill8(index + 1, data);
            
            final int y = (index >> 3) & 7;
            if(y  > 1)
                floodFill8(index - 8, data);
            
            if(y < 6)
                floodFill8(index + 8, data);
            
            final int z = (index >> 6) & 7;
            if(z  > 1)
                floodFill8(index - 64, data);
            
            if(z < 6)
                floodFill8(index + 64, data);
        }
    }
    
    /**
     * Copies from lower elements to higher.
     */
    static void copyExterior8(long[] data)
    {
        data[8] = maskedCopy(data[0], data[8], EXTERIOR_MASK_8[0]);
        data[9] = maskedCopy(data[1], data[9], EXTERIOR_MASK_8[1]);
        data[10] = maskedCopy(data[2], data[10], EXTERIOR_MASK_8[2]);
        data[11] = maskedCopy(data[3], data[11], EXTERIOR_MASK_8[3]);
        data[12] = maskedCopy(data[4], data[12], EXTERIOR_MASK_8[4]);
        data[13] = maskedCopy(data[5], data[13], EXTERIOR_MASK_8[5]);
        data[14] = maskedCopy(data[6], data[14], EXTERIOR_MASK_8[6]);
        data[15] = maskedCopy(data[7], data[15], EXTERIOR_MASK_8[7]);
    }
    
//    public static void simplify8(long[] data)
//    {
//        for(int x = 0; x < 8; x += 2)
//        {
//            for(int y = 0; y < 8; y += 2)
//            {
//                for(int z = 0; z < 8; y += 2)
//                {
//                    long mask = (3L << (x + y * 8)) | (3L << (x + y * 8 + 8));
//                    int count = Long.bitCount(data[z] & mask) + Long.bitCount(data[z + 1] & mask);
//                    if(count == 0 || count == 8)
//                        continue;
//                    if(count < 4)
//                        groupClear8(packedXYZ3(x, y, z), data);
//                    else
//                        groupSet8(packedXYZ3(x, y, z), data);
//                }
//            }
//        }
//    }
    
    /**
     * True if count of set voxels in 2x2x2 group with origin at base index >= 4
     */
//    static boolean isMostlyFull8(int baseIndex, long data[])
//    {
//        int count = 0;
//        if(isSet(baseIndex, data)) count++;
//        if(isSet(baseIndex + 1 , data)) count++;
//        if(isSet(baseIndex + 8, data)) count++;
//        
//        if(isSet(baseIndex + 8 + 1, data)) count++;
//        if(count == 4) return true;
//        
//        if(isSet(baseIndex + 64, data)) count++;
//        if(count == 4) return true; if(count == 0) return false;
//        
//        if(isSet(baseIndex + 64 + 1 , data)) count++;
//        if(count == 4) return true; if(count == 1) return false;
//        
//        if(isSet(baseIndex + 64 + 8, data)) count++;
//        if(count == 4) return true; if(count == 2) return false;
//        
//        if(isSet(baseIndex + 64 + 8 + 1, data)) count++;
//        return count == 4;
//    }
    
//    /**
//     * Clears all voxels in 2x2x2 group with origin at base index
//     */
//    static void groupClear8(int baseIndex, long data[])
//    {
//        clearBit(baseIndex, data);
//        clearBit(baseIndex + 1, data);
//        clearBit(baseIndex + 8, data);
//        clearBit(baseIndex + 8 + 1, data);
//        clearBit(baseIndex + 64, data);
//        clearBit(baseIndex + 64 + 1 , data);
//        clearBit(baseIndex + 64 + 8, data);
//        clearBit(baseIndex + 64 + 8 + 1, data);
//    }
    
//    /**
//     * Sets all voxels in 2x2x2 group with origin at base index
//     */
//    static void groupSet8(int baseIndex, long data[])
//    {
//        setBit(baseIndex, data);
//        setBit(baseIndex + 1, data);
//        setBit(baseIndex + 8, data);
//        setBit(baseIndex + 8 + 1, data);
//        setBit(baseIndex + 64, data);
//        setBit(baseIndex + 64 + 1 , data);
//        setBit(baseIndex + 64 + 8, data);
//        setBit(baseIndex + 64 + 8 + 1, data);
//    }
    
    static long maskedCopy(long src, long dest, long mask)
    {
       return (dest & ~mask) | (src & mask);
    }
    
    static void copyBit(int index, long[] src, long[] dest)
    {
        final int i = index >> 6;
        long mask = 1L << (index & 63);
        dest[i] = (dest[i] & ~mask) | (src[i] & mask);
    }
    
    static void setBit(int index, long[] target)
    {
        target[index >> 6] |= (1L << (index & 63));
    }
    
    static void clearBit(int index, long[] target)
    {
        target[index >> 6] &= ~(1L << (index & 63));
    }
    
    static boolean isClear(int index, long[] src)
    {
        return (src[index >> 6] & (1L << (index & 63))) == 0;
    }
    
    static boolean isSet(int index, long[] src)
    {
        return !isClear(index, src);
    }
}
