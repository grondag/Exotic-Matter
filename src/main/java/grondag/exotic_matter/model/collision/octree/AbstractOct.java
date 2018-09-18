package grondag.exotic_matter.model.collision.octree;

abstract class AbstractOct implements IVoxelOctree
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
    
    AbstractOct(int index, float xOrigin, float yOrigin, float zOrigin)
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