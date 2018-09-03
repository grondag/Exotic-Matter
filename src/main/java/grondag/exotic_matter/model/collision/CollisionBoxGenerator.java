package grondag.exotic_matter.model.collision;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;

import grondag.acuity.RunTimer;
import grondag.exotic_matter.model.primitives.IPolygon;
import grondag.exotic_matter.model.primitives.TriangleBoxTest;
import grondag.exotic_matter.model.primitives.Vertex;
import net.minecraft.util.math.AxisAlignedBB;

public class CollisionBoxGenerator
{
    public static List<AxisAlignedBB> makeCollisionBoxList(Collection<IPolygon> quads)
    {
        return quads.isEmpty()
                ? ImmutableList.of()
                : makeBoxVoxelMethod(quads);
    }

    private static final ThreadLocal<float[]> polyData = new ThreadLocal<float[]>()
    {
        @Override
        protected float[] initialValue()
        {
            return new float[27];
        }
    };
    
    private static void makeBoxVoxelMethodInner(Vertex v0, Vertex v1, Vertex v2, VoxelOctTree voxels)
    {
        float[] data = polyData.get();
        TriangleBoxTest.packPolyData(v0, v1, v2, data);
        makeBoxVoxelMethodFill(voxels, data);
    }
    
    private static void makeBoxVoxelMethodFill(IVoxelOctTree v, float[] data)
    {
        if(TriangleBoxTest.triBoxOverlap(v.xCenter(), v.yCenter(), v.zCenter(), v.voxelSizeHalf(), data))
        {
            if(v.hasSubnodes())
                v.forEach(sv -> makeBoxVoxelMethodFill(sv, data));
            else
                v.setFull();
        }
    }
    
    private static final ThreadLocal<VoxelOctTree> vot = new ThreadLocal<VoxelOctTree>()
    {
        @Override
        protected VoxelOctTree initialValue()
        {
            return new VoxelOctTree();
        }
    };
    
    private static List<AxisAlignedBB> makeBoxVoxelMethod(Collection<IPolygon> quads)
    {
        if(quads.isEmpty()) return Collections.emptyList();
        
        ImmutableList.Builder<AxisAlignedBB> retVal = new ImmutableList.Builder<AxisAlignedBB>();

        VoxelOctTree voxels = vot.get();
        voxels.clear();
        
        RunTimer.THREADED_5000.start();
        for(IPolygon poly : quads)
        {
            Vertex[] v  = poly.vertexArray();
            
            makeBoxVoxelMethodInner(v[0], v[1], v[2], voxels);
            
            if(poly.vertexCount() == 4)
                makeBoxVoxelMethodInner(v[0], v[2], v[3], voxels);
        }
        RunTimer.THREADED_5000.finish();

        voxels.fillInterior();
        
        voxels.simplify();
        
        genBoxes(voxels, retVal);

        return retVal.build();
    }
    
    private static void genBoxes(IVoxelOctTree voxels, ImmutableList.Builder<AxisAlignedBB> builder)
    {
            if(voxels.isEmpty())
                return;
            if(voxels.isFull())
                builder.add(new AxisAlignedBB(voxels.xMin(), voxels.yMin(), voxels.zMin(), voxels.xMax(), voxels.yMax(), voxels.zMax()));
            else if(voxels.hasSubnodes())
                voxels.forEach(v -> genBoxes(v, builder));
    }
}
