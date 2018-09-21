package grondag.exotic_matter.model.collision.octree;

import grondag.exotic_matter.concurrency.SimpleConcurrentCounter;

import static grondag.exotic_matter.model.collision.octree.OctreeCoordinates.*;

/**
 * Voxels of a unit cube to 1/8 (per axis) resolution navigable as an OctTree.  
 * Meant for high-performance voxelization of block models.<p>
 * 
 * Strictly speaking, this is <em>navigable</em> as an Octree which is
 * useful to shortcut tests and other operations, but it is 
 * implemented as a straightforward array of bits (as longs).
 * This is simple and performant for our particular use case.
 */
public class VoxelOctree8 extends AbstractVoxelOctree
{
    public VoxelOctree8()
    {
        super(3);
    }
    
    protected static boolean isEmpty(int index, int divisionLevel, long[] bits)
    {
        // 8 words
        // div 0 - all long words 0 - 7                     
        // div 1 - one long word - xyz                      
        // div 2 - byte within long word xyz : xyz          
        // div 3 - bit within long world xyz : xyz xyz      
        
        switch(divisionLevel)
        {
            case 0:
            {
                for(int i = 0; i < 8; i++)
                    if(bits[i] != 0)  return false;
                return true;
            }
            
            case 1:
                return bits[index] == 0;
                
            case 2:
            {
                final long mask = 0xFFL << (8 * (index & 7));
                return (bits[index >> 3] & mask) == 0;
            }  
            case 3:
            {
                final long mask = 1L << (index & 63);
                return (bits[index >> 6] & mask) == 0;
            }
        }
        assert false : "Bad division level";
        return false;
    }
    
    @Override
    public boolean isEmpty(int index, int divisionLevel)
    {
        return isEmpty(index, divisionLevel, voxelBits);
    }
    
    @Override
    public void clear(int index, int divisionLevel)
    {
        switch(divisionLevel)
        {
            case 0:
            {
                System.arraycopy(ALL_EMPTY, 0, voxelBits, 0, 8);
                return;
            }
            
            case 1:
                voxelBits[index] = 0;
                return;
                
            case 2:
            {
                final long mask = 0xFFL << (8 * (index & 7));
                voxelBits[index >> 3] &= ~mask;
                return;
            }  
            case 3:
            {
                final long mask = 1L << (index & 63);
                voxelBits[index >> 6] &= ~mask;
                return;
            }
        }
        assert false : "Bad division level";
        return;
    }
    
    protected static boolean isFull(int index, int divisionLevel, long[] bits)
    {
        switch(divisionLevel)
        {
            case 0:
            {
                for(int i = 0; i < 8; i++)
                    if(bits[i] != -1L)  return false;
                return true;
            }
            
            case 1:
                return bits[index] == -1L;
                
            case 2:
            {
                final long mask = 0xFFL << (8 * (index & 7));
                return (bits[index >> 3] & mask) == mask;
            }  
            case 3:
            {
                final long mask = 1L << (index & 63);
                return (bits[index >> 6] & mask) == mask;
            }
        }
        assert false : "Bad division level";
        return false;
    }
    
    @Override
    public boolean isFull(int index, int divisionLevel)
    {
        return isFull(index, divisionLevel, this.voxelBits);
    }
    
    //TODO: disable
    SimpleConcurrentCounter interiorFillPerf = new SimpleConcurrentCounter("interiorFillPerf", 1000);
    
    // TODO: remove when implemented in 16X
    @Override
    public void simplify()
    {
        // noop
    }
    
    /**
     * Fills all interior voxels not reachable from an exterior voxel that is already clear.
     * 
     * TODO: remove when implemented in 16X
     */
    @Override
    public void fillInterior()
    {
//        long[] a = new long[8];
//        long[] b = new long[8];
//        
//        VoxelVolume.loadVolume8(this, a);
//        
//        for(int x = 0; x < 8; x++)
//        {
//            for(int y = 0; y < 8; y++)
//            {
//                for(int z = 0; z < 8; z++)
//                {
//                    assert VoxelVolume.isSet(packedXYZ3(x, y, z),  a) == this.isFull(OctreeCoordinates.xyzToIndex3(x, y, z), 3);
//                }
//            }
//        }
//        
//        VoxelVolume.fillVolume8(a, b);
//        
//        System.arraycopy(ALL_FULL, 0, fillBits, 0, 8);
////        interiorFillPerf.startRun();
//        for(int i : EXTERIOR_INDEX_3)
//            fillInteriorInner(i, fillBits, voxelBits);
////        interiorFillPerf.endRun();
//        System.arraycopy(fillBits, 0, voxelBits, 0, 8);
//        
//        for(int x = 0; x < 8; x++)
//        {
//            for(int y = 0; y < 8; y++)
//            {
//                for(int z = 0; z < 8; z++)
//                {
//                    assert VoxelVolume.isSet(packedXYZ3(x, y, z),  b) == this.isFull(OctreeCoordinates.xyzToIndex3(x, y, z), 3);
//                }
//            }
//        }
    }
    
//    private static void fillInteriorInner(final int index, final long[] fillBits, final long[] voxelBits)
//    {
//        final long mask = 0x1L << (index & 63);
//        final int wordIndex = index >> 6;
//        
//        if((fillBits[wordIndex] & mask) != 0 && (voxelBits[wordIndex] & mask) == 0)
//        {
//            fillBits[wordIndex] &= ~mask;
//            
//            final int xyz = indexToXYZ3(index);
//            final int x = xyz & 7;
//            final int y = (xyz >> 3) & 7;
//            final int z = (xyz >> 6) & 7;
//            
//            if(x > 0)
//                fillInteriorInner(xyzToIndex3(packedXYZ3(x - 1, y, z)), fillBits, voxelBits);
//            
//            if(x < 7)
//                fillInteriorInner(xyzToIndex3(packedXYZ3(x + 1, y, z)), fillBits, voxelBits);
//            
//            if(y > 0)
//                fillInteriorInner(xyzToIndex3(packedXYZ3(x, y - 1, z)), fillBits, voxelBits);
//            
//            if(y < 7)
//                fillInteriorInner(xyzToIndex3(packedXYZ3(x, y + 1, z)), fillBits, voxelBits);
//            
//            if(z > 0)
//                fillInteriorInner(xyzToIndex3(packedXYZ3(x, y, z - 1)), fillBits, voxelBits);
//            
//            if(z < 7)
//                fillInteriorInner(xyzToIndex3(packedXYZ3(x, y, z + 1)), fillBits, voxelBits);
//        }
//    }
    
    @Override
    public void setFull(int index, int divisionLevel)
    {
        switch(divisionLevel)
        {
            case 0:
            {
                System.arraycopy(ALL_FULL, 0, voxelBits, 0, 8);
                return;
            }
            
            case 1:
                voxelBits[index] = -1L;
                return;
                
            case 2:
            {
                final long mask = 0xFFL << (8 * (index & 7));
                voxelBits[index >> 3] |= mask;
                return;
            }  
            case 3:
            {
                final long mask = 1L << (index & 63);
                voxelBits[index >> 6] |= mask;
                return;
            }
        }
        assert false : "Bad division level";
        return;
    }
}