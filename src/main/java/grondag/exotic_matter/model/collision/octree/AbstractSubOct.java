package grondag.exotic_matter.model.collision.octree;

abstract class AbstractSubOct extends AbstractOct
{
    protected final int dataIndex;
    protected final long bitMask;
    protected final long voxelBits[];
    
    protected AbstractSubOct(int index, int divisionLevel, int dataIndex, long bitMask, long[] voxelBits)
    {
        super(index, divisionLevel);
        this.dataIndex = dataIndex;
        this.bitMask = bitMask;
        this.voxelBits = voxelBits;
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