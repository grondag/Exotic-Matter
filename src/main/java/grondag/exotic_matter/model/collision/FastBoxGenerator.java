package grondag.exotic_matter.model.collision;


public class FastBoxGenerator extends AbstractVoxelBuilder
{
    protected FastBoxGenerator()
    {
        super(new JoiningBoxListBuilder(), false);
    }

    @Override
    protected void generateBoxes(ICollisionBoxListBuilder builder)
    {
        genBoxes(voxels, builder);
    }

    private void genBoxes(IVoxelOctree voxels, ICollisionBoxListBuilder builder)
    {
        if(voxels.isEmpty())
            return;
        if(voxels.isFull())
            builder.add(voxels.xMin8(), voxels.yMin8(), voxels.zMin8(), voxels.xMax8(), voxels.yMax8(), voxels.zMax8());
        else if(voxels.hasSubnodes())
            voxels.forEach(v -> genBoxes(v, builder));
    }
}
