package grondag.exotic_matter.model;

import static grondag.exotic_matter.model.ModelStateData.STATE_FLAG_NONE;

import java.util.List;

import javax.annotation.Nonnull;

import grondag.exotic_matter.render.CSGMesh;
import grondag.exotic_matter.render.Poly;
import grondag.exotic_matter.render.QuadHelper;
import grondag.exotic_matter.render.SideShape;
import grondag.exotic_matter.render.Surface;
import grondag.exotic_matter.render.SurfaceTopology;
import grondag.exotic_matter.render.SurfaceType;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CSGTestMeshFactory extends ShapeMeshGenerator implements ICollisionHandler
{
    private static final Surface SURFACE_MAIN = new Surface(SurfaceType.MAIN, SurfaceTopology.CUBIC);
    private static final Surface SURFACE_LAMP = new Surface(SurfaceType.LAMP, SurfaceTopology.CUBIC);
    
    /** never changes so may as well save it */
    private final List<Poly> cachedQuads;
    
    public CSGTestMeshFactory()
    {
        super(StateFormat.BLOCK, STATE_FLAG_NONE, SURFACE_MAIN, SURFACE_LAMP);
        this.cachedQuads = getTestQuads();
    }

    @Override
    public @Nonnull List<Poly> getShapeQuads(ISuperModelState modelState)
    {
        return cachedQuads;
    }
    
    private List<Poly> getTestQuads()
    {
        
        Poly template = new Poly();
        template.setLockUV(true);
        template.setSurfaceInstance(SURFACE_MAIN.unitInstance);
  
      
      //union opposite overlapping coplanar faces
//      result = new CSGShape(QuadFactory.makeBox(new AxisAlignedBB(0, .4, .5, 1, 1, 1), template));
//      delta = new CSGShape(QuadFactory.makeBox(new AxisAlignedBB(.3, 0, 0, .7, .6, .5), template));
//      result = result.union(delta);
      
      //union opposite overlapping coplanar faces created by diff
//      result = new CSGShape(QuadFactory.makeBox(new AxisAlignedBB(0.1, 0.1, 0.1, 0.9, 0.9, 0.9), template));
//      delta = new CSGShape(QuadFactory.makeBox(new AxisAlignedBB(0.3, 0.03, 0.5, 0.5, 0.95, 0.7), template));  
//      result = result.difference(delta);
//      delta = new CSGShape(QuadFactory.makeBox(new AxisAlignedBB(0.3, 0, 0, 0.4, .2, 1), template));
//      result = result.union(delta);
      
      // cylinder/cone test
//      result = new CSGShape(QuadFactory.makeCylinder(new Vec3d(.5, 0, .5), new Vec3d(.5, 1, .5), 0.5, 0, template));
      
      // icosahedron (sphere) test
//    result = new CSGShape(QuadFactory.makeIcosahedron(new Vec3d(.5, .5, .5), 0.5, template));

      
      CSGMesh quadsA = new CSGMesh(QuadHelper.makeBox(new AxisAlignedBB(0, 0.4, 0.4, 1.0, 0.6, 0.6), template));
      template.setSurfaceInstance(SURFACE_LAMP.unitInstance);
      CSGMesh quadsB = new CSGMesh(QuadHelper.makeBox(new AxisAlignedBB(0.2, 0, 0.4, 0.6, 1.0, 0.8), template));

//      CSGShape quadsA = new CSGShape(QuadFactory.makeBox(new AxisAlignedBB(0.0, 0.0, 0.0, 1, 1, 1), template));
//      template.color = borderColor.getColorMap(EnumColorMap.BORDER);
//      CSGShape quadsB = new CSGShape(QuadFactory.makeBox(new AxisAlignedBB(0, .1, .45, .05, 0.9, .55), template));

//      CSGMesh result = quadsA.intersect(quadsB);
//      CSGMesh result = quadsA.union(quadsB);
    CSGMesh result = quadsA.difference(quadsB);


    
//      
//      quadsB = new CSGShape(QuadFactory.makeBox(new AxisAlignedBB(0, 0, 0.3, 1, 1, .7), template));
//      result = result.difference(quadsB);
      
//      template.color = borderColor.getColorMap(EnumColorMap.HIGHLIGHT);
//      quadsB = new CSGShape(QuadFactory.makeBox(new AxisAlignedBB(0.2, 0.2, 0, 0.8, 0.8, 1), template));
//      result = result.difference(quadsB);
//
//      template.color = borderColor.getColorMap(EnumColorMap.HIGHLIGHT);
//      quadsB = new CSGShape(QuadFactory.makeBox(new AxisAlignedBB(0, 0, .4, 1, .4, .65), template));
//      result = result.difference(quadsB);
      
//      result.recolor();
      
      return result;
    }


    @Override
    public boolean isCube(ISuperModelState modelState)
    {
        return true;
    }

    @Override
    public boolean rotateBlock(IBlockState blockState, World world, BlockPos pos, EnumFacing axis, ISuperBlock block, ISuperModelState modelState)
    {
        return false;
    }

    @Override
    public int geometricSkyOcclusion(ISuperModelState modelState)
    {
        return 0;
    }

    @Override
    public @Nonnull ICollisionHandler collisionHandler()
    {
        return this;
    }

    @Override
    public SideShape sideShape(ISuperModelState modelState, EnumFacing side)
    {
        return SideShape.MISSING;
    }
    
    @Override
    public List<AxisAlignedBB> getCollisionBoxes(ISuperModelState modelState)
    {
        return CollisionBoxDispatcher.INSTANCE.getCollisionBoxes(modelState);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(ISuperModelState modelState)
    {
        return Block.FULL_BLOCK_AABB;
    }
}
