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
//        fillVolume8(data);
        fillVolume8New(data);
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
    
    final static int X_MAX = 7;
    final static int Y_MAX = 7 << 3;
    final static int Z_MAX = 7 << 6;
    
    final static int X_INC = 1;
    final static int Y_INC = 1 << 3;
    final static int Z_INC = 1 << 6;
    
    final static int X_BIT = 1;
    final static int Y_BIT = 2;
    final static int Z_BIT = 4;
    
    final static int XY_BITS = X_BIT | Y_BIT;
    final static int XZ_BITS = X_BIT | Z_BIT;
    final static int YZ_BITS = Y_BIT | Z_BIT;
    
    final static int XYZ_BITS = X_BIT | Y_BIT | Z_BIT;
    
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
                int base = packedXYZ3(i, 0, j);
                if(isClear(base, data))
                    floodFill8(base + Y_INC, data);
                
                base += Y_MAX;
                if(isClear(base, data))
                    floodFill8(base - Y_INC, data);
                
                base = packedXYZ3(i, j, 0);
                if(isClear(base, data))
                    floodFill8(base + Z_INC, data);
                
                base += Z_MAX;
                if(isClear(base, data))
                    floodFill8(base - Z_INC, data);
                
                base = packedXYZ3(0, i, j);
                if(isClear(base, data))
                    floodFill8(base + X_INC, data);
                
                base += X_MAX;
                if(isClear(base, data))
                    floodFill8(base - X_INC, data);
            }
        }
    }
   
    static final long INTERIOR_MASK_XY = 0x007E7E7E7E7E7E00L;

    
    /**
     * Fast bitwise exterior carve operation.
     */
    static void fillVolume8New(long[] data)
    {
        // during processing, 1 bits in high words represent open voxels
        
        // open voxels in Z end slices are definitely open
        data[8] = ~data[0];
        
        // any voxels accessible from edge of x,y slice are also open
        data[9] = ~(data[1] | INTERIOR_MASK_XY);
        data[10] = ~(data[2] | INTERIOR_MASK_XY);
        data[11] = ~(data[3] | INTERIOR_MASK_XY);
        data[12] = ~(data[4] | INTERIOR_MASK_XY);
        data[13] = ~(data[5] | INTERIOR_MASK_XY);
        data[14] = ~(data[6] | INTERIOR_MASK_XY);
        
        data[15] = ~data[7];
        
        boolean didUpdate = true;
        
        while(didUpdate)
            didUpdate = fillZ(data) || fillXY(data);
        
        // PERF: could avoid this when done by inverting during read
        for(int i = 8; i < 16; i++)
            data[i] = ~data[i];
    }
    
    /**
     * Exploits fact that coarse (8x8x8) voxels for a single
     * Z-axis slice fit within a single long word.
     */
    static boolean fillXY(long[] data)
    {
        boolean didFill = false;
        for(int index = 0; index < 8; index++)
        {
            // get carved current state
            long opens = data[8 + index];
            
            // if no open voxels, nothing to propagate
            if(opens == 0L) continue;
            
            // propagate open voxels left, right, up and down, ignoring voxels already open in current state
            opens = ((opens << 1) | (opens >>> 1) | (opens << 8) | (opens >>> 8)) & ~opens;
                
            // if nothing new to carve, move on
            if(opens == 0L) continue;
                
            // remove voxels that are solid in template
            opens &= ~data[index];
                
            // if nothing new to carve, move on
            if(opens == 0L) continue;
                
            // finally, propagate open voxels into carved results
            data[8 + index] |= opens;
            didFill = true;
        }
        return didFill;
    }
    
    
    static boolean fillZ(long[] data)
    {
        boolean didUpdate = false;
        for(int z = 0; z < 5; z++)
        {
            // get open voxels in previous and following layer, ignoring voxels already open in this layer
            long opens = (data[8 + z] | data[10 + z]) & ~data[9 + z];
            
            // if no open voxels, nothing to propagate into this layer
            if(opens == 0L) continue;
            
            // remove voxels that are solid in template
            opens &= ~data[1 + z];
            
            // if nothing new to carve, move on
            if(opens == 0L) continue;
            
            // finally, propagate open voxels into target layer
            data[9 + z] |= opens;
            
            didUpdate = true;
        }
        return didUpdate;
    }
    
    /**
     * Returns shell slice with voxels cleared according to rules
     */
    static long fillXY(long xySlice)
    {
        // example input
        // XXXXXXXX--
        // XXXXXXXX--
        // ----XXXXXX
        // ----XXXXXX
        // ----XXXXXX
        // --XXXXXX--
        // --XXXXXX--
        
        // outside edges always match input
        long result = xySlice | INTERIOR_MASK_XY;
        
        // result is now this
        // XXXXXXXX--
        // XXXXXXXXX-
        // -XXXXXXXXX
        // -XXXXXXXXX
        // -XXXXXXXXX
        // -XXXXXXXX-
        // --XXXXXX--
        
        
        // mask outside and shift in
        // top first
        
        long opens = ~result;
        // gives this
        // --------XX
        // ---------X
        // X---------
        // X---------
        // X---------
        // X--------X
        // XX------XX
        long openMask = (opens & 0x7E000000000000L) >>> 8;
        openMask |= ((opens & 0x7E) << 8);
        openMask |= ((opens & 0x0080808080808000L) >>> 1);
        openMask |= ((opens & 0x0001010101010100L) << 1);
        
        // mask looks like this now
        // ----------
        // --------X-
        // -X--------
        // -X--------
        // -X------X-
        // ----------
        // ----------
        
        // if open in previous radius and also open in shell then is open
        // in our example, mask still looks the same
        openMask &= ~xySlice;
        
        // if no more can be open check for convex fill
        if(openMask == 0) return fillXYInner(result);
        
        result &= ~(openMask | xySlice);
        return result;
    }
    
    static long fillXYInner(long partialFill)
    {
        return 0;
    }
    
    /**
     * Assumes template shell in lower half of array and carved bits in upper
     */
    static void floodFill8(int index, long[] data)
    {
        if(isSet(index + 512, data) && isClear(index, data))
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
