package grondag.exotic_matter.model.collision;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;

import grondag.exotic_matter.model.primitives.IPolygon;
import grondag.exotic_matter.model.primitives.TriangleBoxTest;
import grondag.exotic_matter.model.primitives.Vertex;
import net.minecraft.util.math.AxisAlignedBB;

public class CollisionBoxGenerator
{
    
    private static void genBoxes2(VoxelOctTree voxels, CollisionBoxListBuilder builder)
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
    
    
}
