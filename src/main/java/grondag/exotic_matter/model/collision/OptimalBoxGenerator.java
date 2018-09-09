package grondag.exotic_matter.model.collision;

public class OptimalBoxGenerator extends AbstractVoxelBuilder
{
    final BoxFinder bf = new BoxFinder();

    @Override
    protected void generateBoxes(CollisionBoxListBuilder builder)
    {
        if(voxels.isEmpty())
            return;
        
        if(voxels.isFull())
            builder.add(voxels.xMin8(), voxels.yMin8(), voxels.zMin8(), voxels.xMax8(), voxels.yMax8(), voxels.zMax8());
        else
        {
            bf.outputBoxes(voxels, builder);
        }        
    }
}
