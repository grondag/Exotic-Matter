package grondag.exotic_matter.model.collision;

import java.util.function.Consumer;

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
    private static final long FULL_BITS = 0xFFFFFFFFFFFFFFFFL;
    private static final long[] ALL_FULL = new long[64];
    private static final long[] ALL_EMPTY = new long[64];
    
    /**
     * Points to indexes in voxel data that are edge voxels
     */
    private static final int[] EXTERIOR_VOXEL_INDEX = new int[1352];
    
    /**
     * Maps x,y,z coordinates (as array index) to voxel index (array values). Use helper method to compute array index.
     */
    private static final int[] VOXEL_XYZ_INDEX = new int[4096];
    
    /**
     * Maps voxel index (as array index) to packed xyz coordinates (array values.)
     */
    private static final int[] VOXEL_XYZ_INVERSE_INDEX = new int[4096];
    
    private static int voxelPackedXYZ(int x, int y, int z)
    {
        return x | (y << 4) | (z << 8);
    }
    
    private static int voxelIndex(int x, int y, int z)
    {
        return VOXEL_XYZ_INDEX[voxelPackedXYZ(x, y, z)];
    }
    
    /**
     * Points to indexes in bottom data that are edge voxels
     */
    private static final int[] EXTERIOR_BOTTOM_INDEX = new int[1352];
    
    /**
     * Maps x,y,z coordinates (as array index) to bottom index (array values). Use helper method to compute array index.
     */
    private static final int[] BOTTOM_XYZ_INDEX = new int[4096];
    
    /**
     * Maps bottom index (as array index) to packed xyz coordinates (array values.)
     */
    private static final int[] BOTTOM_XYZ_INVERSE_INDEX = new int[4096];
    
    private static int bottomPackedXYZ(int x, int y, int z)
    {
        return x | (y << 3) | (z << 6);
    }
    
    private static int bottomIndex(int x, int y, int z)
    {
        return BOTTOM_XYZ_INDEX[bottomPackedXYZ(x, y, z)];
    }
    
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
        for(int i = 0; i < 64; i++)
        {
            ALL_FULL[i] = FULL_BITS;
            // yes, I know, just being clear
            ALL_EMPTY[i] = 0;
        }
        
        final float highEdge = 1 - VOXEL_SIZE;
        int exteriorIndex = 0;
        for(int i = 0; i < 4096; i++)
        {
            final float x = voxelOriginX(i);
            final float y = voxelOriginY(i);
            final float z = voxelOriginZ(i);

            int xyz = voxelPackedXYZ((int)(x / VOXEL_SIZE), (int)(y / VOXEL_SIZE), (int)(z / VOXEL_SIZE));
            if(xyz != 0)
            {
                assert VOXEL_XYZ_INDEX[xyz] == 0;
                VOXEL_XYZ_INDEX[xyz] = i;
            }
            VOXEL_XYZ_INVERSE_INDEX[i] = xyz;
            
            if(x == 0 || x == highEdge)
            {
                EXTERIOR_VOXEL_INDEX[exteriorIndex++] = i;
                continue;
            }
            
            if(y == 0 || y == highEdge)
            {
                EXTERIOR_VOXEL_INDEX[exteriorIndex++] = i;
                continue;
            }
            
            if(z == 0 || z == highEdge)
            {
                EXTERIOR_VOXEL_INDEX[exteriorIndex++] = i;
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
    
            int xyz = bottomPackedXYZ((int)(x / BOTTOM_SIZE), (int)(y / BOTTOM_SIZE), (int)(z / BOTTOM_SIZE));
            if(xyz != 0)
            {
                assert BOTTOM_XYZ_INDEX[xyz] == 0;
                BOTTOM_XYZ_INDEX[xyz] = i;
            }
            BOTTOM_XYZ_INVERSE_INDEX[i] = xyz;
            
            if(x == 0 || x == highBottomEdge)
            {
                EXTERIOR_BOTTOM_INDEX[exteriorBottomIndex++] = i;
                continue;
            }
            
            if(y == 0 || y == highBottomEdge)
            {
                EXTERIOR_BOTTOM_INDEX[exteriorBottomIndex++] = i;
                continue;
            }
            
            if(z == 0 || z == highBottomEdge)
            {
                EXTERIOR_BOTTOM_INDEX[exteriorBottomIndex++] = i;
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
        return bottom[BOTTOM_XYZ_INDEX[bottomPackedXYZ(x, y, z)]];
    }
    
    public IVoxelOctree voxel(int x, int y, int z)
    {
        return voxel[VOXEL_XYZ_INDEX[voxelPackedXYZ(x, y, z)]];
    }
    
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
            for(int i : EXTERIOR_BOTTOM_INDEX)
                bottom[i].floodClearFill();
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
    
    private abstract class AbstractOct implements IVoxelOctree
    {
        protected final int index;
        protected final float xCenter;
        protected final float yCenter;
        protected final float zCenter;
        protected final int xMin8;
        protected final int xMax8;
        protected final int yMin8;
        protected final int yMax8;
        protected final int zMin8;
        protected final int zMax8;
        
        private AbstractOct(int index, float xOrigin, float yOrigin, float zOrigin)
        {
            this.index = index;
            this.xCenter = xOrigin + this.voxelSizeHalf();
            this.yCenter = yOrigin + this.voxelSizeHalf();
            this.zCenter = zOrigin + this.voxelSizeHalf();
            this.xMin8 = Math.round(xOrigin * 8f);
            this.yMin8 = Math.round(yOrigin * 8f);
            this.zMin8 = Math.round(zOrigin * 8f);
            final int increment = Math.round(this.voxelSize() * 8f);
            this.xMax8 = this.xMin8 + increment;
            this.yMax8 = this.yMin8 + increment;
            this.zMax8 = this.zMin8 + increment;
        }
        
        @Override
        public float xCenter() { return this.xCenter; }

        @Override
        public float yCenter() { return this.yCenter; }

        @Override
        public float zCenter() { return this.zCenter; }
        
        @Override
        public int xMin8() { return this.xMin8; }

        @Override
        public int xMax8() { return this.xMax8;  }

        @Override
        public int yMin8() { return this.yMin8; }

        @Override
        public int yMax8() { return this.yMax8; }

        @Override
        public int zMin8() { return this.zMin8; }

        @Override
        public int zMax8() { return this.zMax8;  }
    }
    
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
    
    private abstract class AbstractSubOct extends AbstractOct
    {
        protected final int dataIndex;
        protected final long bitMask;
        
        protected AbstractSubOct(int index, float xOrigin, float yOrigin, float zOrigin, int dataIndex, long bitMask)
        {
            super(index, xOrigin, yOrigin, zOrigin);
            this.dataIndex = dataIndex;
            this.bitMask = bitMask;
        }
        
        @Override
        public boolean isFull()
        {
            return (voxelBits[dataIndex] & bitMask) == bitMask;
        }
        
        @Override
        public boolean isEmpty()
        {
            return (voxelBits[dataIndex] & bitMask) == 0L;
        }

        @Override
        public void setFull()
        {
            voxelBits[dataIndex] |= bitMask;
        }

        @Override
        public void clear()
        {
            voxelBits[dataIndex] &= ~bitMask;
        }
    }
    
    private class Middle extends AbstractSubOct
    {
        
        private Middle(int index)
        {
            super(index, middleOriginX(index), middleOriginY(index), middleOriginZ(index), index, FULL_BITS);
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

    private class Bottom extends AbstractSubOct
    {
        private Bottom(int index)
        {
            super(index, bottomOriginX(index), bottomOriginY(index), bottomOriginZ(index), index / 8, 0xFFL << ((index & 7) * 8));
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
            if(this.isEmpty() && (fillBits[dataIndex] & bitMask) != 0)
            {
                fillBits[dataIndex] &= ~bitMask;
                
                final int xyz = BOTTOM_XYZ_INVERSE_INDEX[this.index];
                final int x = xyz & 7;
                final int y = (xyz >> 3) & 7;
                final int z = (xyz >> 6) & 7;
                
                if(x > 0)
                    bottom[bottomIndex(x - 1, y, z)].floodClearFill();
                
                if(x < 7)
                    bottom[bottomIndex(x + 1, y, z)].floodClearFill();    
                
                if(y > 0)
                    bottom[bottomIndex(x, y - 1, z)].floodClearFill();
                
                if(y < 7)
                    bottom[bottomIndex(x, y + 1, z)].floodClearFill();
                
                if(z > 0)
                    bottom[bottomIndex(x, y, z - 1)].floodClearFill();
                
                if(z < 7)
                    bottom[bottomIndex(x, y, z + 1)].floodClearFill();
            }
        }
    }
    
    private class Voxel extends AbstractSubOct
    {
        private Voxel(int index)
        {
            super(index, voxelOriginX(index), voxelOriginY(index), voxelOriginZ(index), index / 64, 0x1L << (index & 63));
        }
        
        private void floodClearFill()
        {
            if(this.isEmpty() && (fillBits[dataIndex] & bitMask) != 0)
            {
                fillBits[dataIndex] &= ~bitMask;
                
                final int xyz = VOXEL_XYZ_INVERSE_INDEX[this.index];
                final int x = xyz & 0xF;
                final int y = (xyz >> 4) & 0xF;
                final int z = (xyz >> 8) & 0xF;
                
                if(x > 0)
                    voxel[voxelIndex(x - 1, y, z)].floodClearFill();
                
                if(x < 15)
                    voxel[voxelIndex(x + 1, y, z)].floodClearFill();    
                
                if(y > 0)
                    voxel[voxelIndex(x, y - 1, z)].floodClearFill();
                
                if(y < 15)
                    voxel[voxelIndex(x, y + 1, z)].floodClearFill();
                
                if(z > 0)
                    voxel[voxelIndex(x, y, z - 1)].floodClearFill();
                
                if(z < 15)
                    voxel[voxelIndex(x, y, z + 1)].floodClearFill();
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