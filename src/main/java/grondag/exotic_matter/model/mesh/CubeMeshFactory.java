package grondag.exotic_matter.model.mesh;

import static grondag.exotic_matter.model.state.ModelStateData.STATE_FLAG_NONE;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;

import grondag.exotic_matter.block.ISuperBlock;
import grondag.exotic_matter.model.collision.CubeCollisionHandler;
import grondag.exotic_matter.model.collision.ICollisionHandler;
import grondag.exotic_matter.model.painting.PaintLayer;
import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.painting.SurfaceTopology;
import grondag.exotic_matter.model.primitives.CubeInputs;
import grondag.exotic_matter.model.primitives.better.IPolygon;
import grondag.exotic_matter.model.state.ISuperModelState;
import grondag.exotic_matter.model.state.StateFormat;
import grondag.exotic_matter.model.varia.SideShape;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CubeMeshFactory extends ShapeMeshGenerator
{
    private static final Surface SURFACE_MAIN = Surface.builder(SurfaceTopology.CUBIC)
            .withDisabledLayers(PaintLayer.CUT, PaintLayer.LAMP)
            .build();
    
    /** never changes so may as well save it */
    private final List<IPolygon> cachedQuads;
    
    public CubeMeshFactory()
    {
        super(StateFormat.BLOCK, STATE_FLAG_NONE);
        this.cachedQuads = getCubeQuads();
    }

    @Override
    public void produceShapeQuads(ISuperModelState modelState, Consumer<IPolygon> target)
    {
        cachedQuads.forEach(target);
        
    }
    
    private List<IPolygon> getCubeQuads()
    {
        CubeInputs result = new CubeInputs();
        result.color = 0xFFFFFFFF;
        result.textureRotation = Rotation.ROTATE_NONE;
        result.isFullBrightness = false;
        result.u0 = 0;
        result.v0 = 0;
        result.u1 = 1;
        result.v1 = 1;
        result.isOverlay = false;
        result.surfaceInstance = SURFACE_MAIN;
        
        ImmutableList.Builder<IPolygon> builder = ImmutableList.builder();
       
        builder.add(result.makePaintedFace(EnumFacing.DOWN));
        builder.add(result.makePaintedFace(EnumFacing.UP));
        builder.add(result.makePaintedFace(EnumFacing.EAST));
        builder.add(result.makePaintedFace(EnumFacing.WEST));
        builder.add(result.makePaintedFace(EnumFacing.SOUTH));
        builder.add(result.makePaintedFace(EnumFacing.NORTH));
       
        return builder.build();
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
        return 255;
    }

    @Override
    public @Nonnull ICollisionHandler collisionHandler()
    {
        return CubeCollisionHandler.INSTANCE;
    }

    @Override
    public SideShape sideShape(ISuperModelState modelState, EnumFacing side)
    {
        return SideShape.SOLID;
    }

    @Override
    public boolean hasLampSurface(ISuperModelState modelState)
    {
        return true;
    }
}
