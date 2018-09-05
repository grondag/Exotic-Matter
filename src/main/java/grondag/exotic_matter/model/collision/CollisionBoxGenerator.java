package grondag.exotic_matter.model.collision;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.ImmutableList;

import grondag.exotic_matter.ExoticMatter;
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
    
    private static final ThreadLocal<CollisionBoxList.Builder> boxBuilder = new ThreadLocal<CollisionBoxList.Builder>()
    {
        @Override
        protected CollisionBoxList.Builder initialValue()
        {
            return new CollisionBoxList.Builder();
        }
    };
    
    static AtomicInteger frameCounter = new AtomicInteger();
    
    private static List<AxisAlignedBB> makeBoxVoxelMethod(Collection<IPolygon> quads)
    {
        if(quads.isEmpty()) return ImmutableList.of();

        VoxelOctTree voxels = vot.get();
        voxels.clear();
        
        for(IPolygon poly : quads)
        {
            Vertex[] v  = poly.vertexArray();
            
            makeBoxVoxelMethodInner(v[0], v[1], v[2], voxels);
            
            if(poly.vertexCount() == 4)
                makeBoxVoxelMethodInner(v[0], v[2], v[3], voxels);
        }

        voxels.fillInterior();
        
        voxels.simplify();
        
        CollisionBoxList.Builder builder = boxBuilder.get();
       
        int oldCount = 0;
        if((frameCounter.incrementAndGet() & 255) == 255)
        {
            builder.clear();
            genBoxes(voxels, builder);
            oldCount = builder.size();
        }
        
        builder.clear();
        genBoxes2(voxels, builder);
        
       if(oldCount > 0)
       {
           ExoticMatter.INSTANCE.info("Box count comparison: old method = %d, new method = %d", oldCount, builder.size());
       }
        
        return builder.build();
    }
    
    private static void genBoxes(IVoxelOctTree voxels, CollisionBoxList.Builder builder)
    {
        if(voxels.isEmpty())
            return;
        if(voxels.isFull())
            builder.add(voxels.xMin8(), voxels.yMin8(), voxels.zMin8(), voxels.xMax8(), voxels.yMax8(), voxels.zMax8());
        else if(voxels.hasSubnodes())
            genBoxesInner(voxels, builder);
    }
    
    private static void genBoxes2(VoxelOctTree voxels, CollisionBoxList.Builder builder)
    {
        if(voxels.isEmpty())
            return;
        
        if(voxels.isFull())
            builder.add(voxels.xMin8(), voxels.yMin8(), voxels.zMin8(), voxels.xMax8(), voxels.yMax8(), voxels.zMax8());
        else
        {
            // TODO: make threadlocal or make part of builder
            BoxFinder bf = new BoxFinder();
            bf.outputBoxes(voxels, builder);
        }
    }
    
    private static void genBoxesInner(IVoxelOctTree voxels, CollisionBoxList.Builder builder)
    {
        voxels.forEach(v -> genBoxes(v, builder));
    }
    
    
}
