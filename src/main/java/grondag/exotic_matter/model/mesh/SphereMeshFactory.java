package grondag.exotic_matter.model.mesh;

import static grondag.exotic_matter.model.state.ModelStateData.STATE_FLAG_NONE;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import grondag.exotic_matter.block.ISuperBlock;
import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.painting.SurfaceTopology;
import grondag.exotic_matter.model.painting.SurfaceType;
import grondag.exotic_matter.model.primitives.IMutablePolygon;
import grondag.exotic_matter.model.primitives.IPolygon;
import grondag.exotic_matter.model.primitives.Poly;
import grondag.exotic_matter.model.primitives.QuadHelper;
import grondag.exotic_matter.model.state.ISuperModelState;
import grondag.exotic_matter.model.state.StateFormat;
import grondag.exotic_matter.model.varia.CollisionBoxDispatcher;
import grondag.exotic_matter.model.varia.ICollisionHandler;
import grondag.exotic_matter.model.varia.SideShape;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SphereMeshFactory extends ShapeMeshGenerator implements ICollisionHandler
{
    private static final Surface SURFACE_MAIN = new Surface(SurfaceType.MAIN, SurfaceTopology.TILED);
    
    /** never changes so may as well save it */
    private final Collection<IPolygon> cachedQuads;
    
    public SphereMeshFactory()
    {
        super(StateFormat.BLOCK, STATE_FLAG_NONE, SURFACE_MAIN);
        this.cachedQuads = generateQuads();
    }

    @Override
    public @Nonnull Collection<IPolygon> getShapeQuads(ISuperModelState modelState)
    {
        return cachedQuads;
    }
    
    private Collection<IPolygon> generateQuads()
    {
        IMutablePolygon template = Poly.mutable(4);
        template.setLockUV(false);
        template.setSurfaceInstance(SURFACE_MAIN.unitInstance);
  
        Collection<IPolygon> result = MeshHelper.makeIcosahedron(new Vec3d(.5, .5, .5), 0.6, template, false);
      
        return result;
    }


    @Override
    public boolean isCube(ISuperModelState modelState)
    {
        return false;
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