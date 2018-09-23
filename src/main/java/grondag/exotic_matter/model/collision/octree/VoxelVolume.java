package grondag.exotic_matter.model.collision.octree;

import grondag.exotic_matter.varia.functions.Int3Consumer;

/**
 * Operations on Cartesian representation of voxels 
 * that can happen more efficiently that way. (Filling, mostly)
 *
 */
public class VoxelVolume
{
    public static void forEachSimpleVoxel(long[] data, Int3Consumer consumer)
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
    public static void fillVolume8(long[] data)
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
