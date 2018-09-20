package grondag.exotic_matter.model.collision.octree;

abstract class AbstractOct
{
    protected final int index;
    protected final int divisionLevel;
    
    AbstractOct(int index, int divisionLevel)
    {
        this.index = index;
        this.divisionLevel = divisionLevel;
    }
}