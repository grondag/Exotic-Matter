package grondag.exotic_matter.model.collision;

import java.util.List;
import java.util.function.Consumer;

import grondag.exotic_matter.model.collision.octree.VoxelOctree8;
import grondag.exotic_matter.model.primitives.IPolygon;
import grondag.exotic_matter.model.primitives.Vertex;
import net.minecraft.util.math.AxisAlignedBB;

import static grondag.exotic_matter.model.collision.octree.OctreeCoordinates.*;

public class FastBoxGenerator extends AbstractVoxelBuilder<VoxelOctree8> implements Consumer<IPolygon>
{
    protected FastBoxGenerator()
    {
        super(new JoiningBoxListBuilder(), new VoxelOctree8());
    }

    @Override
    protected void generateBoxes(ICollisionBoxListBuilder builder)
    {
        voxels.visit((index, divisionLevel, isLeaf) ->
        {
            if(voxels.isEmpty(index, divisionLevel))
                return false;
            
            if(voxels.isFull(index, divisionLevel))
            {
                withBounds8(index, divisionLevel, (x0, y0, z0, x1, y1, z1) ->
                    builder.add(x0, y0, z0, x1, y1, z1));
                return false;
            }
            
            return true;
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
