package grondag.exotic_matter.model.mesh;

import static grondag.exotic_matter.model.state.ModelStateData.STATE_FLAG_NONE;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import grondag.exotic_matter.block.ISuperBlock;
import grondag.exotic_matter.model.collision.CubeCollisionHandler;
import grondag.exotic_matter.model.collision.ICollisionHandler;
import grondag.exotic_matter.model.painting.PaintLayer;
import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.painting.SurfaceTopology;
import grondag.exotic_matter.model.primitives.CubeInputs;
import grondag.exotic_matter.model.primitives.polygon.IMutablePolygon;
import grondag.exotic_matter.model.primitives.polygon.IPolygon;
import grondag.exotic_matter.model.primitives.stream.IPolyStream;
import grondag.exotic_matter.model.primitives.stream.IWritablePolyStream;
import grondag.exotic_matter.model.primitives.wip.PolyStreams2;
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
    private final IPolyStream cachedQuads;
    
    public CubeMeshFactory()
    {
        super(StateFormat.BLOCK, STATE_FLAG_NONE);
        this.cachedQuads = getCubeQuads();
    }

    @Override
    public void produceShapeQuads(ISuperModelState modelState, Consumer<IMutablePolygon> target)
    {
        if(cachedQuads.isEmpty())
            return;
        
        cachedQuads.origin();
        IPolygon reader = cachedQuads.reader();
        
        do
            target.accept(reader.claimCopy());
        while(cachedQuads.next());
    }
    
    private IPolyStream getCubeQuads()
    {
        CubeInputs cube = new CubeInputs();
        cube.color = 0xFFFFFFFF;
        cube.textureRotation = Rotation.ROTATE_NONE;
        cube.isFullBrightness = false;
        cube.u0 = 0;
        cube.v0 = 0;
        cube.u1 = 1;
        cube.v1 = 1;
        cube.isOverlay = false;
        cube.surfaceInstance = SURFACE_MAIN;
        
        IWritablePolyStream stream = PolyStreams2.claimWritable();
        cube.appendFace(stream, EnumFacing.DOWN);
        cube.appendFace(stream, EnumFacing.UP);
        cube.appendFace(stream, EnumFacing.EAST);
        cube.appendFace(stream, EnumFacing.WEST);
        cube.appendFace(stream, EnumFacing.NORTH);
        cube.appendFace(stream, EnumFacing.SOUTH);
        
        //TODO: remove
        stream.origin();
        assert stream.reader().vertexCount() == 4;
        
        IPolyStream result = stream.releaseAndConvertToReader();
        
        result.origin();
        assert result.reader().vertexCount() == 4;
        
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
