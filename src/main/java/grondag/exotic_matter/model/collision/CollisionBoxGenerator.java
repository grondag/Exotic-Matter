package grondag.exotic_matter.model.collision;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;

import grondag.acuity.RunTimer;
import grondag.exotic_matter.model.primitives.IPolygon;
import grondag.exotic_matter.model.primitives.QuadHelper;
import grondag.exotic_matter.model.primitives.TriangleBoxTest;
import grondag.exotic_matter.model.primitives.Vec3f;
import grondag.exotic_matter.model.primitives.Vertex;
import grondag.exotic_matter.model.render.RenderUtil;
import grondag.exotic_matter.varia.VoxelBitField;
import grondag.exotic_matter.varia.VoxelBitField.VoxelBox;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;

public class CollisionBoxGenerator
{

    private final static int STOP_UP = 1 << EnumFacing.UP.ordinal();
    private final static int STOP_DOWN = 1 << EnumFacing.DOWN.ordinal();
    private final static int STOP_EAST = 1 << EnumFacing.EAST.ordinal();
    private final static int STOP_WEST = 1 << EnumFacing.WEST.ordinal();
    private final static int STOP_NORTH = 1 << EnumFacing.NORTH.ordinal();
    private final static int STOP_SOUTH = 1 << EnumFacing.SOUTH.ordinal();

    public static List<AxisAlignedBB> makeCollisionBoxList(Collection<IPolygon> quads)
    {
        if(quads.isEmpty())
        {
            return Collections.emptyList();
        }

        // voxel method
        List<AxisAlignedBB> retVal = makeBoxVoxelMethod(quads);
        //TODO: put back simple method?
//        List<AxisAlignedBB> simple = Collections.singletonList(makeBoxSimpleMethod(quads));

//        double simpleVolume = Useful.volumeAABB(simple);
//        double voxelVolume = Useful.volumeAABB(retVal);
        //use simple volume if voxel gives empty box, or if simple is not much bigger
//        if(voxelVolume == 0.0 || ((simpleVolume - voxelVolume) / voxelVolume) < 0.1)
//        {
//            retVal = simple;
//        }
        return retVal;
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
//        voxels.forEachVoxel(v ->
//        {
//            if(v.isEmpty() && TriangleBoxTest.triBoxOverlap(v.xCenter(), v.yCenter(), v.zCenter(), v.voxelSizeHalf(), data))
//                v.setFull();
//        });
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
        
        // TODO: reimplement simplification
        // use bigger voxels if doesn't inflate the volume too much
//        VoxelBitField simplified =  voxels.simplify();
//        //     HardScience.log.info("v:" + simplified.getFilledRatio() + " d:" + simplified.getDiscardedVolume() + " i:" + simplified.getInflatedVolume());
//        if(simplified.getFilledRatio() > 0 && (simplified.getDiscardedVolume() / simplified.getFilledRatio()) < 0.1 && simplified.getInflatedVolume() < 0.05) 
//        {
//            voxels = simplified;
//        }

        voxels.fillInterior();
        
        voxels.simplify();
        
        genBoxes(voxels, retVal);
        
        
        //TODO: reimplement simplification
//        VoxelBitField boxFodder = voxels.clone();
//        boxFodder.setOrigin(voxels.getBitsPerAxis() / 2, 0, voxels.getBitsPerAxis() / 2);
//        while(!boxFodder.areAllBitsConsumed())
//        {
//            VoxelBox box = boxFodder.getNearestVoxelBox();
//
//            //should never happen, but need to stop loop if it somehow does
//            if(box == null) break; 
//
//            int stoppedFlags = 0;
//
//            while(stoppedFlags < (1 << 6) - 1)
//            {
//                if((stoppedFlags & STOP_EAST) == 0 && !box.expandIfPossible(EnumFacing.EAST)) stoppedFlags |= STOP_EAST;
//                if((stoppedFlags & STOP_SOUTH) == 0 && !box.expandIfPossible(EnumFacing.SOUTH)) stoppedFlags |= STOP_SOUTH;
//                if((stoppedFlags & STOP_UP) == 0 && !box.expandIfPossible(EnumFacing.UP)) stoppedFlags |= STOP_UP;
//                if((stoppedFlags & STOP_WEST) == 0 && !box.expandIfPossible(EnumFacing.WEST)) stoppedFlags |= STOP_WEST;
//                if((stoppedFlags & STOP_NORTH) == 0 && !box.expandIfPossible(EnumFacing.NORTH)) stoppedFlags |= STOP_NORTH;
//                if((stoppedFlags & STOP_DOWN) == 0 && !box.expandIfPossible(EnumFacing.DOWN)) stoppedFlags |= STOP_DOWN;
//            }
//
//            // double to force floating point values in call below - otherwise lower bound is always at 0.
//            double bpa = voxels.getBitsPerAxis();
//
//            retVal.add(new AxisAlignedBB(box.getMinX()/bpa, box.getMinY()/bpa, box.getMinZ()/bpa,
//                    (box.getMaxX()+1.0)/bpa, (box.getMaxY()+1.0)/bpa, (box.getMaxZ()+1.0)/bpa));
//        }

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
    
    private static List<AxisAlignedBB> makeBoxVoxelMethodOld(Collection<IPolygon> quads)
    {

        if(quads.isEmpty()) return Collections.emptyList();
        
        ImmutableList.Builder<AxisAlignedBB> retVal = new ImmutableList.Builder<AxisAlignedBB>();

        VoxelBitField voxels = new VoxelBitField(3);

        
        for(int x = 0; x < 8; x++)
        {
            for(int y = 0; y < 8; y++)
            {
                for(int z = 0; z < 8; z++)
                {
                    if(isVoxelPresent(x, y, z, quads))
                    {
                        voxels.setFilled(x, y, z, true);
                    }
                }
            }
        }

        // Handle privileged case of thin horizontal layer at bottom.
        // Would be better to use Separating Axis Theorem tests to cover more edge cases,
        // but not willing to take time to implement that right now.

        for(IPolygon quad : quads)
        {                  
            if(quad.isOnFace(EnumFacing.DOWN, QuadHelper.EPSILON))
            {
                for(int x = 0; x < 8; x++)
                {
                    for(int z = 0; z < 8; z++)
                    {
                        if(quad.containsPoint(new Vec3f((x+0.5f)/8.0f, 0.0f, (z+0.5f)/8.0f)))
                        {
                            voxels.setFilled(x, 0, z, true);
                        }
                    }
                }
            }
        } 
        
        // use bigger voxels if doesn't inflate the volume too much
        VoxelBitField simplified =  voxels.simplify();
        //     HardScience.log.info("v:" + simplified.getFilledRatio() + " d:" + simplified.getDiscardedVolume() + " i:" + simplified.getInflatedVolume());
        if(simplified.getFilledRatio() > 0 && (simplified.getDiscardedVolume() / simplified.getFilledRatio()) < 0.1 && simplified.getInflatedVolume() < 0.05) 
        {
            voxels = simplified;
        }

        VoxelBitField boxFodder = voxels.clone();
        boxFodder.setOrigin(voxels.getBitsPerAxis() / 2, 0, voxels.getBitsPerAxis() / 2);
        while(!boxFodder.areAllBitsConsumed())
        {
            VoxelBox box = boxFodder.getNearestVoxelBox();

            //should never happen, but need to stop loop if it somehow does
            if(box == null) break; 

            int stoppedFlags = 0;

            while(stoppedFlags < (1 << 6) - 1)
            {
                if((stoppedFlags & STOP_EAST) == 0 && !box.expandIfPossible(EnumFacing.EAST)) stoppedFlags |= STOP_EAST;
                if((stoppedFlags & STOP_SOUTH) == 0 && !box.expandIfPossible(EnumFacing.SOUTH)) stoppedFlags |= STOP_SOUTH;
                if((stoppedFlags & STOP_UP) == 0 && !box.expandIfPossible(EnumFacing.UP)) stoppedFlags |= STOP_UP;
                if((stoppedFlags & STOP_WEST) == 0 && !box.expandIfPossible(EnumFacing.WEST)) stoppedFlags |= STOP_WEST;
                if((stoppedFlags & STOP_NORTH) == 0 && !box.expandIfPossible(EnumFacing.NORTH)) stoppedFlags |= STOP_NORTH;
                if((stoppedFlags & STOP_DOWN) == 0 && !box.expandIfPossible(EnumFacing.DOWN)) stoppedFlags |= STOP_DOWN;
            }

            // double to force floating point values in call below - otherwise lower bound is always at 0.
            double bpa = voxels.getBitsPerAxis();

            retVal.add(new AxisAlignedBB(box.getMinX()/bpa, box.getMinY()/bpa, box.getMinZ()/bpa,
                    (box.getMaxX()+1.0)/bpa, (box.getMaxY()+1.0)/bpa, (box.getMaxZ()+1.0)/bpa));
        }

        return retVal.build();
    }
    
    private static boolean isVoxelPresent(float x, float y, float z, Collection<IPolygon> quads)
    {
        return RenderUtil.isPointEnclosed((x+0.5f)/8f, (y+0.5f)/8f, (z+0.5f)/8f, quads);
    }
    
    private static boolean isVoxelPresent2(float x, float y, float z, float[] polyData)
    {
        x = (x+0.5f)/8f;
        y = (y+0.5f)/8f;
        z = (z+0.5f)/8f;
        return TriangleBoxTest.triBoxOverlap(x, y, z, 0.5f / 8f, polyData);
    }
    
    public static AxisAlignedBB makeBoxSimpleMethod(Collection<IPolygon> quads)
    {
        double minX = 1.0, minY = 1.0, minZ = 1.0, maxX = 0, maxY = 0, maxZ = 0;

        for(IPolygon quad : quads)
        {
            for(int i = 0; i < quad.vertexCount(); i++)
            {
                Vertex v = quad.getVertex(i);
                if(v.x > maxX) maxX = v.x;
                if(v.y > maxY) maxY = v.y;
                if(v.z > maxZ) maxZ = v.z;
                if(v.x < minX) minX = v.x;
                if(v.y < minY) minY = v.y;
                if(v.z < minZ) minZ = v.z;
            }
        }

        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
