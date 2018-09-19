package grondag.exotic_matter.model.collision;

import java.util.List;
import java.util.function.Consumer;

import grondag.exotic_matter.model.collision.octree.IVoxelOctree;
import grondag.exotic_matter.model.collision.octree.VoxelOctree;
import grondag.exotic_matter.model.primitives.IPolygon;
import grondag.exotic_matter.model.primitives.TriangleBoxTest;
import grondag.exotic_matter.model.primitives.Vertex;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Base class for octree-based collision box generators.
 */
public abstract class AbstractVoxelBuilder implements Consumer<IPolygon>
{
    final float[] polyData = new float[27];
    protected final VoxelOctree voxels;
    protected final ICollisionBoxListBuilder builder;
    
    protected AbstractVoxelBuilder(ICollisionBoxListBuilder builder, boolean isFast)
    {
        this.builder = builder;
        this.voxels = new VoxelOctree(isFast);
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
    
    @Override
    public void accept(@SuppressWarnings("null") IPolygon poly)
    {
        Vertex[] v  = poly.vertexArray();
        
        acceptTriangle(v[0], v[1], v[2]);
        
        if(poly.vertexCount() == 4)
            acceptTriangle(v[0], v[2], v[3]);
    }

    protected void acceptTriangle(Vertex v0, Vertex v1, Vertex v2)
    {
        final float[] data = polyData;
        TriangleBoxTest.packPolyData(v0, v1, v2, data);
        acceptTriangleInner(voxels);
    }
    
    protected void acceptTriangleInner(IVoxelOctree v)
    {
        final float[] data = polyData;
        if(TriangleBoxTest.triBoxOverlap(v.xCenter(), v.yCenter(), v.zCenter(), v.voxelRadius(), data))
        {
            if(v.hasSubnodes())
                v.forEach(sv -> acceptTriangleInner(sv));
            else
                v.setFull();
        }
    }
}
