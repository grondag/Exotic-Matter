package grondag.exotic_matter.model.collision;

import java.util.List;
import java.util.function.Consumer;

import grondag.exotic_matter.model.collision.octree.VoxelOctree8;
import grondag.exotic_matter.model.collision.octree.VoxelVolume;
import grondag.exotic_matter.model.primitives.IPolygon;
import grondag.exotic_matter.model.primitives.Vertex;
import net.minecraft.util.math.AxisAlignedBB;

public class FastBoxGenerator extends AbstractVoxelBuilder<VoxelOctree8> implements Consumer<IPolygon>
{
    protected FastBoxGenerator()
    {
        super(new JoiningBoxListBuilder(), new VoxelOctree8());
    }

    @Override
    protected void generateBoxes(ICollisionBoxListBuilder builder)
    {
        VoxelVolume.forEachSimpleVoxel(voxels, (x, y, z) ->
        {
            builder.addSorted(x, y, z, x + 2, y + 2, z + 2);
        });
    }
    
    @Override
    public void accept(@SuppressWarnings("null") IPolygon poly)
    {
        super.accept(poly, 3);
    }


    //TODO: remove below - here for profiling
    
    @Override
    public void prepare()
    {
        // TODO Auto-generated method stub
        super.prepare();
    }

    @Override
    public List<AxisAlignedBB> build()
    {
        // TODO Auto-generated method stub
        return super.build();
    }
    
    @Override
    protected void acceptTriangle(Vertex v0, Vertex v1, Vertex v2, int maxDivisionLevel)
    {
        // TODO Auto-generated method stub
        super.acceptTriangle(v0, v1, v2, maxDivisionLevel);
    }
}
