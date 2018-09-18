package grondag.exotic_matter.model.collision.octree;

import java.util.function.Consumer;

import grondag.exotic_matter.concurrency.SimpleConcurrentCounter;

import static grondag.exotic_matter.model.collision.octree.OctreeUtils.*;

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
    /**
     * Points to indexes in voxel data that are edge voxels
     */
    private static final int[] EXTERIOR_VOXEL_INDEX = new int[1352];
    
    /**
     * Points to indexes in bottom data that are edge voxels
     */
    private static final int[] EXTERIOR_BOTTOM_INDEX = new int[1352];
    
    private static final float TOP_SIZE = 0.5f;
    private static final float MIDDLE_SIZE = TOP_SIZE * 0.5f;
    private static final float BOTTOM_SIZE = MIDDLE_SIZE * 0.5f;
    private static final float VOXEL_SIZE = BOTTOM_SIZE * 0.5f;
    
    private static final float TOP_SIZE_HALF = TOP_SIZE * 0.5f;
    private static final float MIDDLE_SIZE_HALF = MIDDLE_SIZE * 0.5f;
    private static final float BOTTOM_SIZE_HALF = BOTTOM_SIZE * 0.5f;
    private static final float VOXEL_SIZE_HALF = VOXEL_SIZE * 0.5f;
    
    static float topOriginX(int topIndex) { return (topIndex & 1) == 0 ? 0 : TOP_SIZE; }
    static float topOriginY(int topIndex) { return ((topIndex >> 1) & 1) == 0 ? 0 : TOP_SIZE; }
    static float topOriginZ(int topIndex) { return ((topIndex >> 2) & 1) == 0 ? 0 : TOP_SIZE; }
    static int parentIndex(int childIndex) { return childIndex / 8; }
    
    static float middleOriginX(int middleIndex)
    { 
        return topOriginX(parentIndex(middleIndex)) + ((middleIndex & 1) == 0 ? 0 : MIDDLE_SIZE);
    }
    static float middleOriginY(int middleIndex)
    { 
        return topOriginY(parentIndex(middleIndex)) + (((middleIndex >> 1) & 1) == 0 ? 0 : MIDDLE_SIZE);
    }
    static float middleOriginZ(int middleIndex)
    { 
        return topOriginZ(parentIndex(middleIndex)) + (((middleIndex >> 2) & 1) == 0 ? 0 : MIDDLE_SIZE);
    }
    
    static float bottomOriginX(int bottomIndex)
    { 
        return middleOriginX(parentIndex(bottomIndex)) + ((bottomIndex & 1) == 0 ? 0 : BOTTOM_SIZE);
    }
    static float bottomOriginY(int bottomIndex)
    { 
        return middleOriginY(parentIndex(bottomIndex)) + (((bottomIndex >> 1) & 1) == 0 ? 0 : BOTTOM_SIZE);
    }
    static float bottomOriginZ(int bottomIndex)
    { 
        return middleOriginZ(parentIndex(bottomIndex)) + (((bottomIndex >> 2) & 1) == 0 ? 0 : BOTTOM_SIZE);
    }
    
    static float voxelOriginX(int voxelIndex)
    { 
        return bottomOriginX(parentIndex(voxelIndex)) + ((voxelIndex & 1) == 0 ? 0 : VOXEL_SIZE);
    }
    static float voxelOriginY(int voxelIndex)
    { 
        return bottomOriginY(parentIndex(voxelIndex)) + (((voxelIndex >> 1) & 1) == 0 ? 0 : VOXEL_SIZE);
    }
    static float voxelOriginZ(int voxelIndex)
    { 
        return bottomOriginZ(parentIndex(voxelIndex)) + (((voxelIndex >> 2) & 1) == 0 ? 0 : VOXEL_SIZE);
    }
    
    static
    {
        final float highEdge = 1 - VOXEL_SIZE;
        int exteriorIndex = 0;
        for(int i = 0; i < 4096; i++)
        {
            final float x = voxelOriginX(i);
            final float y = voxelOriginY(i);
            final float z = voxelOriginZ(i);

            int xyz = packedXYZ4((int)(x / VOXEL_SIZE), (int)(y / VOXEL_SIZE), (int)(z / VOXEL_SIZE));
            
            if(x == 0 || x == highEdge)
            {
                EXTERIOR_VOXEL_INDEX[exteriorIndex++] = xyzToIndex4(xyz);
                continue;
            }
            
            if(y == 0 || y == highEdge)
            {
                EXTERIOR_VOXEL_INDEX[exteriorIndex++] = xyzToIndex4(xyz);
                continue;
            }
            
            if(z == 0 || z == highEdge)
            {
                EXTERIOR_VOXEL_INDEX[exteriorIndex++] = xyzToIndex4(xyz);
                continue;
            }
            
        }
        assert exteriorIndex == 1352;
    
        final float highBottomEdge = 1 - BOTTOM_SIZE;
        int exteriorBottomIndex = 0;
        for(int i = 0; i < 512; i++)
        {
            final float x = bottomOriginX(i);
            final float y = bottomOriginY(i);
            final float z = bottomOriginZ(i);
    
            int xyz = packedXYZ3((int)(x / BOTTOM_SIZE), (int)(y / BOTTOM_SIZE), (int)(z / BOTTOM_SIZE));
            
            if(x == 0 || x == highBottomEdge)
            {
                EXTERIOR_BOTTOM_INDEX[exteriorBottomIndex++] = xyzToIndex3(xyz);
                continue;
            }
            
            if(y == 0 || y == highBottomEdge)
            {
                EXTERIOR_BOTTOM_INDEX[exteriorBottomIndex++] = xyzToIndex3(xyz);
                continue;
            }
            
            if(z == 0 || z == highBottomEdge)
            {
                EXTERIOR_BOTTOM_INDEX[exteriorBottomIndex++] = xyzToIndex3(xyz);
                continue;
            }
            
        }
        assert exteriorBottomIndex == 296;
    }

    private final long[] voxelBits = new long[64];
    private final long[] fillBits = new long[64];
    
    private final Top[] top = new Top[8];
    private final Middle[] middle = new Middle[64];
    private final Bottom[] bottom = new Bottom[512];
    private final Voxel[] voxel = new Voxel[4096];
    
    private final boolean isDetailed;
    
    public VoxelOctree()
    {
        this(true);
    }
    
    /**
     * If isDetailed,  bottom nodes have sub voxels.
     */
    public VoxelOctree(boolean isDetailed)
    {
        this.isDetailed = isDetailed;
        
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
    
    //TODO: disable
    SimpleConcurrentCounter interiorFillPerf = new SimpleConcurrentCounter("interiorFillPerf", 1000);
    
    /**
     * Fills all interior voxels not reachable from an exterior voxel that is already clear.
     */
    public void fillInterior()
    {
        System.arraycopy(ALL_FULL, 0, fillBits, 0, 64);
        if(isDetailed)
            for(int i : EXTERIOR_VOXEL_INDEX)
                voxel[i].floodClearFill();
        else
        {
            interiorFillPerf.startRun();
            for(int i : EXTERIOR_BOTTOM_INDEX)
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
        if(isDetailed)
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
    
    @Override
    public final float voxelSize() { return 1.0f; }
    
    @Override
    public final float voxelSizeHalf() { return 0.5f; }
    
    @Override
    public final float xCenter() { return 0.5f; }

    @Override
    public final float yCenter() { return 0.5f; }
    
    @Override
    public final float zCenter() { return 0.5f; }
    
    private class Top extends AbstractOct
    {
        private Top(int index)
        {
            super(index, topOriginX(index), topOriginY(index), topOriginZ(index));
        }

        @Override
        public Middle subNode(int index)
        {
            return middle[this.index * 8 + index];
        }
        
        @Override
        public final float voxelSize() { return TOP_SIZE; }
        
        @Override
        public final float voxelSizeHalf() { return TOP_SIZE_HALF; }

    }
    
    private class Middle extends AbstractSubOct
    {
        
        private Middle(int index)
        {
            super(index, middleOriginX(index), middleOriginY(index), middleOriginZ(index), index, FULL_BITS, VoxelOctree.this.voxelBits);
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
        
        @Override
        public final float voxelSize() { return MIDDLE_SIZE; }
        
        @Override
        public final float voxelSizeHalf() { return MIDDLE_SIZE_HALF; }
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
            super(index, bottomOriginX(index), bottomOriginY(index), bottomOriginZ(index), bottomDataIndex(index), bottomDataMask(index), VoxelOctree.this.voxelBits);
        }
        
        @Override
        public boolean hasSubnodes()
        {
            return isDetailed;
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
        
        @Override
        public final float voxelSize() { return BOTTOM_SIZE; }
        
        @Override
        public final float voxelSizeHalf() { return BOTTOM_SIZE_HALF; }
        
        
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
            super(index, voxelOriginX(index), voxelOriginY(index), voxelOriginZ(index), index / 64, 0x1L << (index & 63), VoxelOctree.this.voxelBits);
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
        public boolean hasSubnodes()
        {
            return false;
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
        
        @Override
        public final float voxelSize() { return VOXEL_SIZE; }
        
        @Override
        public final float voxelSizeHalf() { return VOXEL_SIZE_HALF; }

        @Override
        public int xMin8() { throw new UnsupportedOperationException(); }

        @Override
        public int xMax8() { throw new UnsupportedOperationException();  }

        @Override
        public int yMin8() { throw new UnsupportedOperationException(); }

        @Override
        public int yMax8() { throw new UnsupportedOperationException(); }

        @Override
        public int zMin8() { throw new UnsupportedOperationException(); }

        @Override
        public int zMax8() { throw new UnsupportedOperationException(); }
    }

    @Override
    public int xMin8() { return 0; }

    @Override
    public int xMax8() { return 8;  }

    @Override
    public int yMin8() { return 0; }

    @Override
    public int yMax8() { return 8; }

    @Override
    public int zMin8() { return 0; }

    @Override
    public int zMax8() { return 8;  }
}