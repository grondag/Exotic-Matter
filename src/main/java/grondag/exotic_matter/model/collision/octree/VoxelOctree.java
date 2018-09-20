package grondag.exotic_matter.model.collision.octree;

import grondag.exotic_matter.concurrency.SimpleConcurrentCounter;

import static grondag.exotic_matter.model.collision.octree.OctreeCoordinates.*;

/**
 * Voxels of a unit cube to 1/16 (per axis) resolution navigable as an OctTree.  
 * Meant for high-performance voxelization of block models.<p>
 * 
 * Strictly speaking, this is <em>navigable</em> as an Octree which is
 * useful to shortcut tests and other operations, but it is 
 * implemented as a straightforward array of bits (as longs).
 * This is simple and performant for our particular use case.
 */
public class VoxelOctree
{
    private final long[] voxelBits = new long[64];
    private final long[] fillBits = new long[64];
    
    private final int maxDivisionLevel;
    
    public VoxelOctree()
    {
        this(true);
    }
    
    /**
     * If isDetailed,  bottom nodes have sub voxels.
     */
    public VoxelOctree(boolean isDetailed)
    {
        maxDivisionLevel = isDetailed ? 4 : 3;
    }
    
    protected static boolean isEmpty(int index, int divisionLevel, long[] bits)
    {
        // 64 words
        // div 0 - all long words 0 - 63                    
        // div 1 - some long words - xyz | 0 - 7            
        // div 2 - one long word - xyz xyz                  
        // div 3 - byte within long word xyz xyz : xyz      
        // div 4 - bit within long world xyz xyz : xyz xyz  
        
        // 8 words
        // div 0 - all long words 0 - 7                     
        // div 1 - one long word - xyz                      
        // div 2 - byte within long word xyz : xyz          
        // div 3 - bit within long world xyz : xyz xyz      
        
        //TODO: will need to handle different array sizes
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
    
    public boolean isEmpty()
    {
        return isEmpty(0, 0);
    }
    
    public boolean isEmpty(int index, int divisionLevel)
    {
        return isEmpty(index, divisionLevel, voxelBits);
    }
    
    public void clear()
    {
        clear(0, 0);
    }
    
    public void clear(int index, int divisionLevel)
    {
        //TODO: will need to handle different array sizes
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
        //TODO: will need to handle different array sizes
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
    
    public boolean isFull()
    {
        return isFull(0, 0);
    }
    
    public boolean isFull(int index, int divisionLevel)
    {
        return isFull(index, divisionLevel, this.voxelBits);
    }
    
    public void visit(IOctreeVisitor visitor)
    {
        if(visitor.visit(0, 0, false))
        {
            visitInner(0, 1, visitor);
            visitInner(1, 1, visitor);
            visitInner(2, 1, visitor);
            visitInner(3, 1, visitor);
            visitInner(4, 1, visitor);
            visitInner(5, 1, visitor);
            visitInner(6, 1, visitor);
            visitInner(7, 1, visitor);
        }
    }
    
    /**
     * Never called for leaf nodes.
     */
    private void visitInner(int index, int divisionLevel, IOctreeVisitor visitor)
    {
        final boolean leaf = divisionLevel == this.maxDivisionLevel;
        if(visitor.visit(index, divisionLevel, leaf) & !leaf)
        {
            final int d = divisionLevel + 1;
            final int i = index << 3;
            
            visitInner(i, d, visitor);
            visitInner(i + 1, d, visitor);
            visitInner(i + 2, d, visitor);
            visitInner(i + 3, d, visitor);
            visitInner(i + 4, d, visitor);
            visitInner(i + 5, d, visitor);
            visitInner(i + 6, d, visitor);
            visitInner(i + 7, d, visitor);
        }
    }
    
    //TODO: disable
    SimpleConcurrentCounter interiorFillPerf = new SimpleConcurrentCounter("interiorFillPerf", 1000);
    
    /**
     * Fills all interior voxels not reachable from an exterior voxel that is already clear.
     */
    public void fillInterior()
    {
        System.arraycopy(ALL_FULL, 0, fillBits, 0, 64);
        if(maxDivisionLevel == 4)
            fillInteriorDetailed();
        else
            fillInteriorCoarse();
        System.arraycopy(fillBits, 0, voxelBits, 0, 64);
    }
    
    private void fillInteriorCoarse()
    {
        interiorFillPerf.startRun();
        for(int i : EXTERIOR_INDEX_3)
            floodClearFill3(i, fillBits, voxelBits);
        interiorFillPerf.endRun();
    }
    
    private static void floodClearFill3(final int index, final long[] fillBits, final long[] voxelBits)
    {
        final long mask = 0xFFL << ((index & 7) << 3);
        final int wordIndex = index >> 3;
        if((fillBits[wordIndex] & mask) != 0 && (voxelBits[wordIndex] & mask) == 0)
        {
            fillBits[wordIndex] &= ~mask;
            
            final int xyz = indexToXYZ3(index);
            final int x = xyz & 7;
            final int y = (xyz >> 3) & 7;
            final int z = (xyz >> 6) & 7;
            
            if(x > 0)
                floodClearFill3(xyzToIndex3(packedXYZ3(x - 1, y, z)), fillBits, voxelBits);
            
            if(x < 7)
                floodClearFill3(xyzToIndex3(packedXYZ3(x + 1, y, z)), fillBits, voxelBits);
            
            if(y > 0)
                floodClearFill3(xyzToIndex3(packedXYZ3(x, y - 1, z)), fillBits, voxelBits);
            
            if(y < 7)
                floodClearFill3(xyzToIndex3(packedXYZ3(x, y + 1, z)), fillBits, voxelBits);
            
            if(z > 0)
                floodClearFill3(xyzToIndex3(packedXYZ3(x, y, z - 1)), fillBits, voxelBits);
            
            if(z < 7)
                floodClearFill3(xyzToIndex3(packedXYZ3(x, y, z + 1)), fillBits, voxelBits);
        }
    }
    
    
    private void fillInteriorDetailed()
    {
        for(int i : EXTERIOR_INDEX_4)
            floodClearFill4(i);
    }
    
    private void floodClearFill4(int index)
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
                floodClearFill4(xyzToIndex4(packedXYZ4(x - 1, y, z)));
            
            if(x < 15)
                floodClearFill4(xyzToIndex4(packedXYZ4(x + 1, y, z)));
            
            if(y > 0)
                floodClearFill4(xyzToIndex4(packedXYZ4(x, y - 1, z)));
            
            if(y < 15)
                floodClearFill4(xyzToIndex4(packedXYZ4(x, y + 1, z)));
            
            if(z > 0)
                floodClearFill4(xyzToIndex4(packedXYZ4(x, y, z - 1)));
            
            if(z < 15)
                floodClearFill4(xyzToIndex4(packedXYZ4(x, y, z + 1)));
        }
    }
    
    /**
     * Makes bottom nodes that are mostly full completely full, or otherwise makes them empty.
     */
    public void simplify()
    {
        if(maxDivisionLevel == 4)
            simplifyDetailed();
        else
            simplifyCoarse();
    }
    
    public void simplifyDetailed()
    {
        final long[] bits = this.voxelBits;
        
        for(int i = 0; i < 64; i++)
        {
            long b = simplifyDetailedInner(bits[i], 0);
            b = simplifyDetailedInner(b, 1);
            b = simplifyDetailedInner(b, 2);
            b = simplifyDetailedInner(b, 3);
            b = simplifyDetailedInner(b, 4);
            b = simplifyDetailedInner(b, 5);
            b = simplifyDetailedInner(b, 6);
            b = simplifyDetailedInner(b, 7);
            bits[i] = b;
        }
    }
    
    private long simplifyDetailedInner(long bits, int shift)
    {
        long mask = 0xFFL << (shift * 8);
        if(Long.bitCount((bits & mask)) >= 4)
            return bits | mask;
        else
            return bits &= ~mask;
    }
      
    
    /**
     * Makes middle nodes that are mostly full completely full, or otherwise makes them empty.
     * Used to generate fast coarse-grained collision boxes.
     */
    private void simplifyCoarse()
    {
        final long[] bits = this.voxelBits;
        
        for(int i = 0; i < 64; i++)
            if(Long.bitCount(bits[i]) >= 32)
                bits[i] = -1L;
            else
                bits[i] = 0;
    }
    
    public void setFull()
    {
        setFull(0, 0);
    }
    
    public void setFull(int index, int divisionLevel)
    {
        //TODO: will need to handle different array sizes
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