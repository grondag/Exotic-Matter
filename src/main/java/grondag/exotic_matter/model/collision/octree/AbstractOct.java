package grondag.exotic_matter.model.collision.octree;

abstract class AbstractOct implements IVoxelOctree
{
    protected final int index;
    protected final int divisionLevel;
    
    AbstractOct(int index, int divisionLevel)
    {
        this.index = index;
        this.divisionLevel = divisionLevel;
    }
    
    @Override
    public int index() { return this.index; }

    @Override
    public int divisionLevel() { return this.divisionLevel; }
}