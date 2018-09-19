package grondag.exotic_matter.model.collision;

import java.util.List;
import java.util.function.Consumer;

import grondag.exotic_matter.model.collision.octree.IVoxelOctree;
import grondag.exotic_matter.model.primitives.IPolygon;
import grondag.exotic_matter.model.primitives.Vertex;
import net.minecraft.util.math.AxisAlignedBB;

import static grondag.exotic_matter.model.collision.octree.OctreeCoordinates.*;

public class FastBoxGenerator extends AbstractVoxelBuilder implements Consumer<IPolygon>
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
            withBounds8(voxels.index(), voxels.divisionLevel(), (x0, y0, z0, x1, y1, z1) ->
                builder.add(x0, y0, z0, x1, y1, z1));
        else if(voxels.divisionLevel() < 3)
            voxels.forEach(v -> genBoxes(v, builder));
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

    @Override
    protected void acceptTriangleInner(IVoxelOctree v, int maxDivisionLevel)
    {
        // TODO Auto-generated method stub
        super.acceptTriangleInner(v, maxDivisionLevel);
    }
}
