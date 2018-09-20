package grondag.exotic_matter.model.collision.octree;

import java.util.function.Consumer;

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
public class VoxelOctree implements IVoxelOctree
{
    private final long[] voxelBits = new long[64];
    private final long[] fillBits = new long[64];
    
    private final Top[] top = new Top[8];
    private final Middle[] middle = new Middle[64];
    private final Bottom[] bottom = new Bottom[512];
    private final Voxel[] voxel = new Voxel[4096];
    
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
        
        for(int i = 0; i < 8; i++)
            top[i] = new Top(i);
        
        for(int i = 0; i < 64; i++)
            middle[i] = new Middle(i);
        
        for(int i = 0; i < 512; i++)
            bottom[i] = new Bottom(i);
        
        if(isDetailed)
        {
            for(int i = 0; i < 4096; i++)
                voxel[i] = new Voxel(i);
        }
    }
    
    public void forEachVoxel(Consumer<IVoxelOctree> consumer)
    {
        for(Voxel v : voxel)
            consumer.accept(v);
    }
    
    public void forEachBottom(Consumer<IVoxelOctree> consumer)
    {
        for(Bottom b : bottom)
            consumer.accept(b);
    }
    
    public IVoxelOctree bottom(int x, int y, int z)
    {
        return bottom[xyzToIndex3(x, y, z)];
    }
    
    public IVoxelOctree voxel(int x, int y, int z)
    {
        return voxel[xyzToIndex4(x, y, z)];
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
    
    @Override
    public void forEach(Consumer<IVoxelOctree> consumer)
    {
        consumer.accept(this.subNode(0));
        consumer.accept(this.subNode(1));
        consumer.accept(this.subNode(2));
        consumer.accept(this.subNode(3));
        consumer.accept(this.subNode(4));
        consumer.accept(this.subNode(5));
        consumer.accept(this.subNode(6));
        consumer.accept(this.subNode(7));
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
            for(int i : EXTERIOR_INDEX_4)
                voxel[i].floodClearFill();
        else
        {
            interiorFillPerf.startRun();
            for(int i : EXTERIOR_INDEX_3)
                bottom[i].floodClearFill();
            interiorFillPerf.endRun();
        }
        System.arraycopy(fillBits, 0, voxelBits, 0, 64);
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
        for(Bottom b : bottom)
        {
            if(b.isEmpty() || b.isFull())
                continue;
            
            if(b.isMostlyFull())
                b.setFull();
            else
                b.clear();
        }
    }
    
    /**
     * Makes middle nodes that are mostly full completely full, or otherwise makes them empty.
     * Used to generate fast coarse-grained collision boxes.
     */
    private void simplifyCoarse()
    {
        for(Middle m : middle)
        {
            if(m.isEmpty() || m.isFull())
                continue;
            
            if(m.isMostlyFull())
                m.setFull();
            else
                m.clear();
        }
    }
    
    @Override
    public boolean isFull()
    {
        for(int i = 0; i < 64; i++)
        {
            if(voxelBits[i] != FULL_BITS)
                return false;
        }
        return true;
    }

    @Override
    public void setFull()
    {
        System.arraycopy(ALL_FULL, 0, voxelBits, 0, 64);
    }

    @Override
    public void clear()
    {
        System.arraycopy(ALL_EMPTY, 0, voxelBits, 0, 64);
    }
    
    @Override
    public Top subNode(int index)
    {
        return top[index];
    }
    
    private class Top extends AbstractOct
    {
        private Top(int index)
        {
            super(index, 1);
        }

        @Override
        public Middle subNode(int index)
        {
            return middle[this.index * 8 + index];
        }

        @Override
        public void clear()
        {
            // should never be used at this level
            throw new UnsupportedOperationException();
        }

        @Override
        public void setFull()
        {
         // should never be used at this level
            throw new UnsupportedOperationException();
        }
    }
    
    private class Middle extends AbstractSubOct
    {
        
        private Middle(int index)
        {
            super(index, 2, index, FULL_BITS, VoxelOctree.this.voxelBits);
        }

        // methods optimized for storage implementation
        
        @Override
        public boolean isMostlyFull()
        {
            return Long.bitCount(voxelBits[index]) >= 32;
        }

        @Override
        public boolean isFull()
        {
            return voxelBits[index] == FULL_BITS;
        }
        
        @Override
        public boolean isEmpty()
        {
            return voxelBits[index] == 0;
        }

        @Override
        public void setFull()
        {
            voxelBits[index] = FULL_BITS;
        }

        @Override
        public void clear()
        {
            voxelBits[index] = 0;
        }
        
        @Override
        public Bottom subNode(int index)
        {
            return bottom[this.index * 8 + index];
        }
    }

    static int bottomDataIndex(int bottomIndex)
    {
        return bottomIndex >> 3;
    }
    
    static long bottomDataMask(int bottomIndex)
    {
        return 0xFFL << ((bottomIndex & 7) * 8);
    }
    
    private class Bottom extends AbstractSubOct
    {
        private Bottom(int index)
        {
            super(index, 3, bottomDataIndex(index), bottomDataMask(index), VoxelOctree.this.voxelBits);
        }
        
        @Override
        public Voxel subNode(int index)
        {
            return voxel[this.index * 8 + index];
        }
        
        @Override
        public boolean isMostlyFull()
        {
            return Long.bitCount(voxelBits[dataIndex] & bitMask) >= 4;
        }
        
        private void floodClearFill()
        {
            if((fillBits[dataIndex] & bitMask) != 0 && this.isEmpty())
            {
                fillBits[dataIndex] &= ~bitMask;
                
                final Bottom[] bottom = VoxelOctree.this.bottom;
                final int xyz = indexToXYZ3(this.index);
                final int x = xyz & 7;
                final int y = (xyz >> 3) & 7;
                final int z = (xyz >> 6) & 7;
                
                if(x > 0)
                    bottom[xyzToIndex3(packedXYZ3(x - 1, y, z))].floodClearFill();
                
                if(x < 7)
                    bottom[xyzToIndex3(packedXYZ3(x + 1, y, z))].floodClearFill();    
                
                if(y > 0)
                    bottom[xyzToIndex3(packedXYZ3(x, y - 1, z))].floodClearFill();
                
                if(y < 7)
                    bottom[xyzToIndex3(packedXYZ3(x, y + 1, z))].floodClearFill();
                
                if(z > 0)
                    bottom[xyzToIndex3(packedXYZ3(x, y, z - 1))].floodClearFill();
                
                if(z < 7)
                    bottom[xyzToIndex3(packedXYZ3(x, y, z + 1))].floodClearFill();
            }
        }
    }
    
    private class Voxel extends AbstractSubOct
    {
        private Voxel(int index)
        {
            super(index, 4, index / 64, 0x1L << (index & 63), VoxelOctree.this.voxelBits);
        }
        
        private void floodClearFill()
        {
            if(this.isEmpty() && (fillBits[dataIndex] & bitMask) != 0)
            {
                fillBits[dataIndex] &= ~bitMask;
                
                final int xyz = indexToXYZ4(this.index);
                final int x = xyz & 0xF;
                final int y = (xyz >> 4) & 0xF;
                final int z = (xyz >> 8) & 0xF;
                
                if(x > 0)
                    voxel[xyzToIndex4(x - 1, y, z)].floodClearFill();
                
                if(x < 15)
                    voxel[xyzToIndex4(x + 1, y, z)].floodClearFill();    
                
                if(y > 0)
                    voxel[xyzToIndex4(x, y - 1, z)].floodClearFill();
                
                if(y < 15)
                    voxel[xyzToIndex4(x, y + 1, z)].floodClearFill();
                
                if(z > 0)
                    voxel[xyzToIndex4(x, y, z - 1)].floodClearFill();
                
                if(z < 15)
                    voxel[xyzToIndex4(x, y, z + 1)].floodClearFill();
            }
        }

        @Override
        public boolean isMostlyFull()
        {
            return isFull();
        }

        @Override
        public IVoxelOctree subNode(int index)
        {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public int index() { return 0; }

    @Override
    public int divisionLevel() { return 0; }

}