package grondag.exotic_matter.model.collision.octree;

import static grondag.exotic_matter.model.collision.octree.OctreeUtils.*;

abstract class AbstractOct implements IVoxelOctree
{
    protected final int index;
    protected final int divisionLevel;
    protected final float xCenter;
    protected final float yCenter;
    protected final float zCenter;
    protected final int xMin8;
    protected final int xMax8;
    protected final int yMin8;
    protected final int yMax8;
    protected final int zMin8;
    protected final int zMax8;
    
    AbstractOct(int index, int divisionLevel)
    {
        this.index = index;
        this.divisionLevel = divisionLevel;
        final float size = OctreeUtils.voxelSize(divisionLevel);
        final float halfSize = size * 0.5f;
        final int xyz = indexToXYZ(index, divisionLevel);
        final int mask = (1 << divisionLevel) - 1;
        final float xOrigin = (xyz & mask) * size;
        final float yOrigin = ((xyz >> divisionLevel) & mask) * size;
        final float zOrigin = ((xyz >> divisionLevel * 2) & mask) * size;
        
        this.xCenter = xOrigin + halfSize;
        this.yCenter = yOrigin + halfSize;
        this.zCenter = zOrigin + halfSize;
        this.xMin8 = Math.round(xOrigin * 8f);
        this.yMin8 = Math.round(yOrigin * 8f);
        this.zMin8 = Math.round(zOrigin * 8f);
        final int increment = Math.round(size * 8f);
        this.xMax8 = this.xMin8 + increment;
        this.yMax8 = this.yMin8 + increment;
        this.zMax8 = this.zMin8 + increment;
    }
    
    @Override
    public final float voxelRadius()
    {
        return OctreeUtils.voxelRadius(divisionLevel);
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