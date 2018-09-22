package grondag.exotic_matter.model.collision.octree;

import static grondag.exotic_matter.model.collision.octree.OctreeCoordinates.ALL_EMPTY;
import static grondag.exotic_matter.model.collision.octree.OctreeCoordinates.packedXYZ3;

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
        // hard-coded for speed
        long[] raw = voxels.rawBits();
        
        final long r0 = raw[0];
        final long r1 = raw[1];
        final long r2 = raw[2];
        final long r3 = raw[3];
        final long r4 = raw[4];
        final long r5 = raw[5];
        final long r6 = raw[6];
        final long r7 = raw[7];
        
        data[0] = ((r0 & 50332416L) >>> 6)
            | ((r0 & 201526275L) << 0)
            | ((r0 & 786444L) << 6)
            | ((r1 & 50332416L) >>> 2)
            | ((r1 & 201526275L) << 4)
            | ((r1 & 786444L) << 10)
            | ((r2 & 50332416L) << 26)
            | ((r2 & 201526275L) << 32)
            | ((r2 & 786444L) << 38)
            | ((r3 & 50332416L) << 30)
            | ((r3 & 201526275L) << 36)
            | ((r3 & 786444L) << 42);
        
        data[1] = ((r0 & 805318656L) >>> 10)
            | ((r0 & 3224420400L) >>> 4)
            | ((r0 & 12583104L) << 2)
            | ((r1 & 805318656L) >>> 6)
            | ((r1 & 3224420400L) << 0)
            | ((r1 & 12583104L) << 6)
            | ((r2 & 805318656L) << 22)
            | ((r2 & 3224420400L) << 28)
            | ((r2 & 12583104L) << 34)
            | ((r3 & 805318656L) << 26)
            | ((r3 & 3224420400L) << 32)
            | ((r3 & 12583104L) << 38);
        
        // constants were derived in a spreadsheet that doesn't support 64-bit integers, thus the ugly
        data[2] = ((r0 & (0x10000000000L | 0x20000000000L | 0x100000000000000L | 0x200000000000000L)) >>> 38)
            | ((r0 & (0x100000000L | 0x200000000L | 0x40000000000L | 0x80000000000L | 0x1000000000000L | 0x2000000000000L | 0x400000000000000L | 0x800000000000000L)) >>> 32)
            | ((r0 & (0x400000000L | 0x800000000L | 0x4000000000000L | 0x8000000000000L)) >>> 26)
            | ((r1 & (0x10000000000L | 0x20000000000L | 0x100000000000000L | 0x200000000000000L)) >>> 34)
            | ((r1 & (0x100000000L | 0x200000000L | 0x40000000000L | 0x80000000000L | 0x1000000000000L | 0x2000000000000L | 0x400000000000000L | 0x800000000000000L)) >>> 28)
            | ((r1 & (0x400000000L | 0x800000000L | 0x4000000000000L | 0x8000000000000L)) >>> 22)
            | ((r2 & (0x10000000000L | 0x20000000000L | 0x100000000000000L | 0x200000000000000L)) >>> 6)
            | ((r2 & (0x100000000L | 0x200000000L | 0x40000000000L | 0x80000000000L | 0x1000000000000L | 0x2000000000000L | 0x400000000000000L | 0x800000000000000L)) << 0)
            | ((r2 & (0x400000000L | 0x800000000L | 0x4000000000000L | 0x8000000000000L)) << 6)
            | ((r3 & (0x10000000000L | 0x20000000000L | 0x100000000000000L | 0x200000000000000L)) >>> 2)
            | ((r3 & (0x100000000L | 0x200000000L | 0x40000000000L | 0x80000000000L | 0x1000000000000L | 0x2000000000000L | 0x400000000000000L | 0x800000000000000L)) << 4)
            | ((r3 & (0x400000000L | 0x800000000L | 0x4000000000000L | 0x8000000000000L)) << 10);
        
        data[3] = ((r0 & (0x100000000000L | 0x200000000000L | 0x1000000000000000L | 0x2000000000000000L)) >>> 42)
            | ((r0 & (0x1000000000L | 0x2000000000L | 0x400000000000L | 0x800000000000L | 0x10000000000000L | 0x20000000000000L | 0x4000000000000000L | 0x08000000000000000L)) >>> 36)
            | ((r0 & (0x4000000000L | 0x8000000000L | 0x40000000000000L | 0x80000000000000L)) >>> 30)
            | ((r1 & (0x100000000000L | 0x200000000000L | 0x1000000000000000L | 0x2000000000000000L)) >>> 38)
            | ((r1 & (0x1000000000L | 0x2000000000L | 0x400000000000L | 0x800000000000L | 0x10000000000000L | 0x20000000000000L | 0x4000000000000000L | 0x08000000000000000L)) >>> 32)
            | ((r1 & (0x4000000000L | 0x8000000000L | 0x40000000000000L | 0x80000000000000L)) >>> 26)
            | ((r2 & (0x100000000000L | 0x200000000000L | 0x1000000000000000L | 0x2000000000000000L)) >>> 10)
            | ((r2 & (0x1000000000L | 0x2000000000L | 0x400000000000L | 0x800000000000L | 0x10000000000000L | 0x20000000000000L | 0x4000000000000000L | 0x08000000000000000L)) >>> 4)
            | ((r2 & (0x4000000000L | 0x8000000000L | 0x40000000000000L | 0x80000000000000L)) << 2)
            | ((r3 & (0x100000000000L | 0x200000000000L | 0x1000000000000000L | 0x2000000000000000L)) >>> 6)
            | ((r3 & (0x1000000000L | 0x2000000000L | 0x400000000000L | 0x800000000000L | 0x10000000000000L | 0x20000000000000L | 0x4000000000000000L | 0x08000000000000000L)) << 0)
            | ((r3 & (0x4000000000L | 0x8000000000L | 0x40000000000000L | 0x80000000000000L)) << 6);
        
        data[4] = ((r4 & (0x100L | 0x200L | 0x1000000L | 0x2000000L)) >>> 6)
            | ((r4 & (0x1L | 0x2L | 0x400L | 0x800L | 0x10000L | 0x20000L | 0x4000000L | 0x8000000L)) << 0)
            | ((r4 & (0x4L | 0x8L | 0x40000L | 0x80000L)) << 6)
            | ((r5 & (0x100L | 0x200L | 0x1000000L | 0x2000000L)) >>> 2)
            | ((r5 & (0x1L | 0x2L | 0x400L | 0x800L | 0x10000L | 0x20000L | 0x4000000L | 0x8000000L)) << 4)
            | ((r5 & (0x4L | 0x8L | 0x40000L | 0x80000L)) << 10)
            | ((r6 & (0x100L | 0x200L | 0x1000000L | 0x2000000L)) << 26)
            | ((r6 & (0x1L | 0x2L | 0x400L | 0x800L | 0x10000L | 0x20000L | 0x4000000L | 0x8000000L)) << 32)
            | ((r6 & (0x4L | 0x8L | 0x40000L | 0x80000L)) << 38)
            | ((r7 & (0x100L | 0x200L | 0x1000000L | 0x2000000L)) << 30)
            | ((r7 & (0x1L | 0x2L | 0x400L | 0x800L | 0x10000L | 0x20000L | 0x4000000L | 0x8000000L)) << 36)
            | ((r7 & (0x4L | 0x8L | 0x40000L | 0x80000L)) << 42);
        
        data[5] = ((r4 & (0x1000L | 0x2000L | 0x10000000L | 0x20000000L)) >>> 10)
            | ((r4 & (0x10L | 0x20L | 0x4000L | 0x8000L | 0x100000L | 0x200000L | 0x40000000L | 0x080000000L)) >>> 4)
            | ((r4 & (0x40L | 0x80L | 0x400000L | 0x800000L)) << 2)
            | ((r5 & (0x1000L | 0x2000L | 0x10000000L | 0x20000000L)) >>> 6)
            | ((r5 & (0x10L | 0x20L | 0x4000L | 0x8000L | 0x100000L | 0x200000L | 0x40000000L | 0x080000000L)) << 0)
            | ((r5 & (0x40L | 0x80L | 0x400000L | 0x800000L)) << 6)
            | ((r6 & (0x1000L | 0x2000L | 0x10000000L | 0x20000000L)) << 22)
            | ((r6 & (0x10L | 0x20L | 0x4000L | 0x8000L | 0x100000L | 0x200000L | 0x40000000L | 0x080000000L)) << 28)
            | ((r6 & (0x40L | 0x80L | 0x400000L | 0x800000L)) << 34)
            | ((r7 & (0x1000L | 0x2000L | 0x10000000L | 0x20000000L)) << 26)
            | ((r7 & (0x10L | 0x20L | 0x4000L | 0x8000L | 0x100000L | 0x200000L | 0x40000000L | 0x080000000L)) << 32)
            | ((r7 & (0x40L | 0x80L | 0x400000L | 0x800000L)) << 38);
        
        data[6] = ((r4 & (0x10000000000L | 0x20000000000L | 0x100000000000000L | 0x200000000000000L)) >>> 38)
            | ((r4 & (0x100000000L | 0x200000000L | 0x40000000000L | 0x80000000000L | 0x1000000000000L | 0x2000000000000L | 0x400000000000000L | 0x800000000000000L)) >>> 32)
            | ((r4 & (0x400000000L | 0x800000000L | 0x4000000000000L | 0x8000000000000L)) >>> 26)
            | ((r5 & (0x10000000000L | 0x20000000000L | 0x100000000000000L | 0x200000000000000L)) >>> 34)
            | ((r5 & (0x100000000L | 0x200000000L | 0x40000000000L | 0x80000000000L | 0x1000000000000L | 0x2000000000000L | 0x400000000000000L | 0x800000000000000L)) >>> 28)
            | ((r5 & (0x400000000L | 0x800000000L | 0x4000000000000L | 0x8000000000000L)) >>> 22)
            | ((r6 & (0x10000000000L | 0x20000000000L | 0x100000000000000L | 0x200000000000000L)) >>> 6)
            | ((r6 & (0x100000000L | 0x200000000L | 0x40000000000L | 0x80000000000L | 0x1000000000000L | 0x2000000000000L | 0x400000000000000L | 0x800000000000000L)) << 0)
            | ((r6 & (0x400000000L | 0x800000000L | 0x4000000000000L | 0x8000000000000L)) << 6)
            | ((r7 & (0x10000000000L | 0x20000000000L | 0x100000000000000L | 0x200000000000000L)) >>> 2)
            | ((r7 & (0x100000000L | 0x200000000L | 0x40000000000L | 0x80000000000L | 0x1000000000000L | 0x2000000000000L | 0x400000000000000L | 0x800000000000000L)) << 4)
            | ((r7 & (0x400000000L | 0x800000000L | 0x4000000000000L | 0x8000000000000L)) << 10);
        
        data[7] = ((r4 & (0x100000000000L | 0x200000000000L | 0x1000000000000000L | 0x2000000000000000L)) >>> 42)
            | ((r4 & (0x1000000000L | 0x2000000000L | 0x400000000000L | 0x800000000000L | 0x10000000000000L | 0x20000000000000L | 0x4000000000000000L | 0x08000000000000000L)) >>> 36)
            | ((r4 & (0x4000000000L | 0x8000000000L | 0x40000000000000L | 0x80000000000000L)) >>> 30)
            | ((r5 & (0x100000000000L | 0x200000000000L | 0x1000000000000000L | 0x2000000000000000L)) >>> 38)
            | ((r5 & (0x1000000000L | 0x2000000000L | 0x400000000000L | 0x800000000000L | 0x10000000000000L | 0x20000000000000L | 0x4000000000000000L | 0x08000000000000000L)) >>> 32)
            | ((r5 & (0x4000000000L | 0x8000000000L | 0x40000000000000L | 0x80000000000000L)) >>> 26)
            | ((r6 & (0x100000000000L | 0x200000000000L | 0x1000000000000000L | 0x2000000000000000L)) >>> 10)
            | ((r6 & (0x1000000000L | 0x2000000000L | 0x400000000000L | 0x800000000000L | 0x10000000000000L | 0x20000000000000L | 0x4000000000000000L | 0x08000000000000000L)) >>> 4)
            | ((r6 & (0x4000000000L | 0x8000000000L | 0x40000000000000L | 0x80000000000000L)) << 2)
            | ((r7 & (0x100000000000L | 0x200000000000L | 0x1000000000000000L | 0x2000000000000000L)) >>> 6)
            | ((r7 & (0x1000000000L | 0x2000000000L | 0x400000000000L | 0x800000000000L | 0x10000000000000L | 0x20000000000000L | 0x4000000000000000L | 0x08000000000000000L)) << 0)
            | ((r7 & (0x4000000000L | 0x8000000000L | 0x40000000000000L | 0x80000000000000L)) << 6);
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
    
    static final long INTERIOR_MASK_XY = 0x007E7E7E7E7E7E00L;

    /**
     * Fills all interior voxels not reachable from an exterior voxel that is already clear.
     * Works by starting with a full volume and "etching" from the outside via simple
     * flood fill until it is stopped by the shell data provided.<p>
     * 
     * To ensure low garbage, requires array be sized to hold two sets of results.
     * Expects source data in lower half.  Output is in upper half.
     */
    static void fillVolume8(long[] data)
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
        
        // flip carved bits to represent filled voxels instead of open
        // needed by the output routine, which looks for set bits
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
