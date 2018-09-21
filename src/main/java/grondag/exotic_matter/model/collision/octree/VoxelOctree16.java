package grondag.exotic_matter.model.collision.octree;

import grondag.exotic_matter.concurrency.SimpleConcurrentCounter;

import static grondag.exotic_matter.model.collision.octree.OctreeCoordinates.*;

/**
 * Voxels of a unit cube to 1/16 (per axis) resolution navigable as an OctTree.  
 */
public class VoxelOctree16 extends AbstractVoxelOctree
{
    public VoxelOctree16()
    {
        super(4);
    }
    
    protected static boolean isEmpty(int index, int divisionLevel, long[] bits)
    {
        // 64 words
        // div 0 - all long words 0 - 63                    
        // div 1 - some long words - xyz | 0 - 7            
        // div 2 - one long word - xyz xyz                  
        // div 3 - byte within long word xyz xyz : xyz      
        // div 4 - bit within long world xyz xyz : xyz xyz  
        
        switch(divisionLevel)
        {
            case 0:
            {
                for(int i = 0; i < 64; i++)
                    if(bits[i] != 0)  return false;
                return true;
            }
            
            case 1:
            {
                final int min = index * 8;
                final int max = min + 8;
                for(int i = min; i < max; i++)
                    if(bits[i] != 0)  return false;
                return true;
            }
            
            case 2:
                return bits[index] == 0;
                
            case 3:
            {
                final long mask = 0xFFL << (8 * (index & 7));
                return (bits[index >> 3] & mask) == 0;
            }  
            case 4:
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
                System.arraycopy(ALL_EMPTY, 0, voxelBits, 0, 64);
                return;
            }
            
            case 1:
            {
                System.arraycopy(ALL_EMPTY, 0, voxelBits, index * 8, 8);
                return;
            }
            
            case 2:
                voxelBits[index] = 0;
                return;
                
            case 3:
            {
                final long mask = 0xFFL << (8 * (index & 7));
                voxelBits[index >> 3] &= ~mask;
                return;
            }  
            case 4:
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
                for(int i = 0; i < 64; i++)
                    if(bits[i] != -1L)  return false;
                return true;
            }
            
            case 1:
            {
                final int min = index * 8;
                final int max = min + 8;
                for(int i = min; i < max; i++)
                    if(bits[i] != -1L)  return false;
                return true;
            }
            
            case 2:
                return bits[index] == -1L;
                
            case 3:
            {
                final long mask = 0xFFL << (8 * (index & 7));
                return (bits[index >> 3] & mask) == mask;
            }  
            case 4:
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
    
    /**
     * Fills all interior voxels not reachable from an exterior voxel that is already clear.
     */
    @Override
    public void fillInterior()
    {
        System.arraycopy(ALL_FULL, 0, fillBits, 0, 64);
        for(int i : EXTERIOR_INDEX_4)
            fillInteriorInner(i);
        System.arraycopy(fillBits, 0, voxelBits, 0, 64);
    }
    
    private void fillInteriorInner(int index)
    {
        final long mask = 0x1L << (index & 63);
        final int wordIndex = index >> 6;
        
        if((fillBits[wordIndex] & mask) != 0 && (voxelBits[wordIndex] & mask) == 0)
        {
            fillBits[wordIndex] &= ~mask;
            
            final int xyz = indexToXYZ4(index);
            final int x = xyz & 0xF;
            final int y = (xyz >> 4) & 0xF;
            final int z = (xyz >> 8) & 0xF;
            
            if(x > 0)
                fillInteriorInner(xyzToIndex4(packedXYZ4(x - 1, y, z)));
            
            if(x < 15)
                fillInteriorInner(xyzToIndex4(packedXYZ4(x + 1, y, z)));
            
            if(y > 0)
                fillInteriorInner(xyzToIndex4(packedXYZ4(x, y - 1, z)));
            
            if(y < 15)
                fillInteriorInner(xyzToIndex4(packedXYZ4(x, y + 1, z)));
            
            if(z > 0)
                fillInteriorInner(xyzToIndex4(packedXYZ4(x, y, z - 1)));
            
            if(z < 15)
                fillInteriorInner(xyzToIndex4(packedXYZ4(x, y, z + 1)));
        }
    }
    
    @Override
    public void setFull(int index, int divisionLevel)
    {
        switch(divisionLevel)
        {
            case 0:
            {
                System.arraycopy(ALL_FULL, 0, voxelBits, 0, 64);
                return;
            }
            
            case 1:
            {
                System.arraycopy(ALL_FULL, 0, voxelBits, index * 8, 8);
                return;
            }
            
            case 2:
                voxelBits[index] = -1L;
                return;
                
            case 3:
            {
                final long mask = 0xFFL << (8 * (index & 7));
                voxelBits[index >> 3] |= mask;
                return;
            }  
            case 4:
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