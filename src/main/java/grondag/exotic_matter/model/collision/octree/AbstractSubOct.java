package grondag.exotic_matter.model.collision.octree;

abstract class AbstractSubOct extends AbstractOct
{
    protected final int dataIndex;
    protected final long bitMask;
    protected final long voxelBits[];
    
    protected AbstractSubOct(int index, float xOrigin, float yOrigin, float zOrigin, int dataIndex, long bitMask, long[] voxelBits)
    {
        super(index, xOrigin, yOrigin, zOrigin);
        this.dataIndex = dataIndex;
        this.bitMask = bitMask;
        this.voxelBits = voxelBits;
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