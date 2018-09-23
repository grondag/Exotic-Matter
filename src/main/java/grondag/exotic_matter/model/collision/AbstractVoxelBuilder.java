package grondag.exotic_matter.model.collision;


import static grondag.exotic_matter.model.collision.octree.OctreeCoordinates.testCenter;
import static grondag.exotic_matter.model.collision.octree.OctreeCoordinates.voxelRadius;

import java.util.List;

import grondag.exotic_matter.model.collision.octree.AbstractVoxelOctree;
import grondag.exotic_matter.model.primitives.IPolygon;
import grondag.exotic_matter.model.primitives.TriangleBoxTest;
import grondag.exotic_matter.model.primitives.Vertex;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Base class for octree-based collision box generators.
 */
public abstract class AbstractVoxelBuilder<T extends AbstractVoxelOctree>
{
    final float[] polyData = new float[36];
    protected final T voxels;
    protected final ICollisionBoxListBuilder builder;
    
    protected AbstractVoxelBuilder(ICollisionBoxListBuilder builder, T voxels)
    {
        this.builder = builder;
        this.voxels = voxels;
    }
    
    public void prepare()
    {
        voxels.clear();
    }
    
    public List<AxisAlignedBB> build()
    {
        voxels.fillInterior();
        voxels.simplify();
        builder.clear();
        generateBoxes(builder);
        return builder.build();
    }
    
    protected abstract void generateBoxes(ICollisionBoxListBuilder builder);
    
    public void accept(IPolygon poly, int maxDivisionLevel)
    {
        Vertex[] v  = poly.vertexArray();
        
        acceptTriangle(v[0], v[1], v[2], maxDivisionLevel);
        
        if(poly.vertexCount() == 4)
            acceptTriangle(v[0], v[2], v[3], maxDivisionLevel);
    }

    protected void acceptTriangle(Vertex v0, Vertex v1, Vertex v2, int maxDivisionLevel)
    {
        final float[] data = polyData;
        TriangleBoxTest.packPolyData(v0, v1, v2, data);
        voxels.visit((index, divisionLevel, isLeaf) ->
        {
            return testCenter(index, divisionLevel, (x, y, z) -> 
            {
                if(TriangleBoxTest.triBoxOverlap(x, y, z, voxelRadius(divisionLevel), data))
                {
                    if(isLeaf)
                    {
                        voxels.setFull(index, divisionLevel);
                        return false;
                    }
                    else return true;
                }
                else return false;
            });
        });
    }
}
