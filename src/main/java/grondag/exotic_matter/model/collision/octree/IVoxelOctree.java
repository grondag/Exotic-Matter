package grondag.exotic_matter.model.collision.octree;

import java.util.function.Consumer;

/**
 * Specialized octree for tracking occupied voxels in a unit cube divided by 16 on each axis.
 * Intended for collision box generation.
 */
public interface IVoxelOctree
{
    
    public int index();
    
    public int divisionLevel();
}
