package grondag.exotic_matter.model.collision.octree;

import java.util.function.Consumer;

/**
 * Specialized octree for tracking occupied voxels in a unit cube divided by 16 on each axis.
 * Intended for collision box generation.
 */
public interface IVoxelOctree
{
    /**
     * True if nodes have sub nodes, false if nodes represent the 1/16 voxels.
     */
    public default boolean hasSubnodes()
    {
        return true;
    }
    
    /**
     * In bottom node this simply means each voxel is present.
     * In higher nodes means all subnodes and ultimately all voxels are filled.<p>
     * 
     * Note - default implementation is for parent nodes - must be overridden at voxel level.
     */
    public default boolean isFull()
    {
        return this.subNode(0).isFull()
                && this.subNode(1).isFull()
                && this.subNode(2).isFull()
                && this.subNode(3).isFull()
                && this.subNode(4).isFull()
                && this.subNode(5).isFull()
                && this.subNode(6).isFull()
                && this.subNode(7).isFull();
    }
    
    /**
     * In bottom node this simply means no voxels is filled.
     * In higher nodes means no subnodes and ultimately no voxels are filled.<p>
     * 
     * Note - default implementation is for parent nodes - must be overridden at voxel level.
     */
    public default boolean isEmpty()
    {
        return this.subNode(0).isEmpty()
                && this.subNode(1).isEmpty()
                && this.subNode(2).isEmpty()
                && this.subNode(3).isEmpty()
                && this.subNode(4).isEmpty()
                && this.subNode(5).isEmpty()
                && this.subNode(6).isEmpty()
                && this.subNode(7).isEmpty();
    }
    
    /**
     * Marks node/voxel as full.
     * In higher nodes this is a recursive operation - all subnodes and ultimately all voxels are marked full. <p>
     * 
     * Note - default implementation is for parent nodes - must be overridden at voxel level.
     */
    public default void setFull()
    {
        this.forEach(n -> n.setFull());
    }
    
    /**
     * Marks node/voxel as empty.
     * In higher nodes this is a recursive operation - all subnodes and ultimately all voxels are cleared.
     * 
     * Note - default implementation is for parent nodes - must be overridden at voxel level.
     */
    public default void clear()
    {
        this.forEach(n -> n.clear());
    }
    
    /**
     * True if at least 50% of voxels are completely full. 
     * For lowest (voxel) nodes, is equivalent to {@link #isFull()}
     * @return
     */
    public default boolean isMostlyFull()
    {
        int count = 0;
        
        if(this.subNode(0).isFull())
            count++;
        
        if(this.subNode(1).isFull())
            count++;
        
        if(this.subNode(2).isFull())
            count++;
        
        if(this.subNode(3).isFull())
            if(++count == 4) return true;
        
        if(this.subNode(4).isFull())
            if(++count == 4) return true;
        
        if(count < 1) return false;
        
        if(this.subNode(5).isFull())
            if(++count == 4) return true;

        if(count < 2) return false;

        if(this.subNode(6).isFull())
            if(++count == 4) return true;
        
        if(count < 3) return false;
        
        return this.subNode(7).isFull();
    }
    
    /**
     * If has subnodes returns node at given address.
     * If this node is a voxel, throws an exception.
     */
    public IVoxelOctree subNode(int index);
    
    /**
     * Applies consumer to each subnode if has subnodes.
     * Has no effect otherwise.
     */
    public default void forEach(Consumer<IVoxelOctree> consumer)
    {
        if(this.hasSubnodes())
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
    }
    
    /**
     * Returns a byte (0-255) value with each bit indicating if the
     * subnode corresponding to bit ordinal is full.
     */
    public default int shapeBits()
    {
        int result = this.subNode(0).isFull() ? 1 : 0;
        if(this.subNode(1).isFull()) result |= 2;
        if(this.subNode(2).isFull()) result |= 4;
        if(this.subNode(3).isFull()) result |= 8;
        if(this.subNode(4).isFull()) result |= 16;
        if(this.subNode(5).isFull()) result |= 32;
        if(this.subNode(6).isFull()) result |= 64;
        if(this.subNode(7).isFull()) result |= 128;
        return result;
    }
    
    public float xCenter();
    public float yCenter();
    public float zCenter();
    public float voxelSize();
    public float voxelSizeHalf();
    
    public default float xMin() { return xCenter() - voxelSizeHalf(); }
    public default float xMax() { return xCenter() + voxelSizeHalf(); }
    public default float yMin() { return yCenter() - voxelSizeHalf(); }
    public default float yMax() { return yCenter() + voxelSizeHalf(); }
    public default float zMin() { return zCenter() - voxelSizeHalf(); }
    public default float zMax() { return zCenter() + voxelSizeHalf(); }
    
    /**
     * These return values of 0-8 representing 1/8 split of unit cube.
     * Should throw exception on 1/16 voxels.
     */
    public int xMin8();
    public int xMax8();
    public int yMin8();
    public int yMax8();
    public int zMin8();
    public int zMax8();
}
