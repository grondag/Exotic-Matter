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
     * Marks node/voxel as full.
     * In higher nodes this is a recursive operation - all subnodes and ultimately all voxels are marked full. <p>
     * 
     * Note - default implementation is for parent nodes - must be overridden at voxel level.
     */
    public void setFull();
    
    
    /**
     * Marks node/voxel as empty.
     * In higher nodes this is a recursive operation - all subnodes and ultimately all voxels are cleared.
     * 
     * Note - default implementation is for parent nodes - must be overridden at voxel level.
     */
    public void clear();
    
    
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
    
    public int index();
    
    public int divisionLevel();
}
