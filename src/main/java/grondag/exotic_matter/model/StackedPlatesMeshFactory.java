package grondag.exotic_matter.model;

import static grondag.exotic_matter.model.ModelStateData.*;

import java.util.List;

import javax.annotation.Nonnull;
import javax.vecmath.Matrix4d;

import com.google.common.collect.ImmutableList;

import grondag.exotic_matter.render.RawQuad;
import grondag.exotic_matter.render.SideShape;
import grondag.exotic_matter.render.Surface;
import grondag.exotic_matter.render.SurfaceTopology;
import grondag.exotic_matter.render.SurfaceType;
import grondag.exotic_matter.varia.Useful;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class StackedPlatesMeshFactory extends ShapeMeshGenerator implements ICollisionHandler
{
    private static Surface TOP_AND_BOTTOM = new Surface(SurfaceType.MAIN, SurfaceTopology.CUBIC);
    private static Surface SIDES = new Surface(SurfaceType.CUT, SurfaceTopology.CUBIC);
    
    public StackedPlatesMeshFactory()
    {
        super(StateFormat.BLOCK, 
                STATE_FLAG_NEEDS_SPECIES | STATE_FLAG_HAS_AXIS | STATE_FLAG_HAS_AXIS_ORIENTATION,
                TOP_AND_BOTTOM, SIDES);
    }
 
    private List<RawQuad> makeQuads(int meta, Matrix4d matrix)
    {
        double height = (meta + 1) / 16.0;
        
        RawQuad template = new RawQuad();
        template.color = 0xFFFFFFFF;
        template.rotation = Rotation.ROTATE_NONE;
        template.isFullBrightness = false;
        template.lockUV = true;

        ImmutableList.Builder<RawQuad> builder = new ImmutableList.Builder<RawQuad>();
        
        RawQuad quad = template.clone();
        quad.surfaceInstance = TOP_AND_BOTTOM.unitInstance;
        quad.setFace(EnumFacing.UP);
        quad.setupFaceQuad(0.0, 0.0, 1.0, 1.0, 1-height, EnumFacing.NORTH);
        builder.add(quad.transform(matrix));
      
        for(EnumFacing face : EnumFacing.Plane.HORIZONTAL.facings())
        {
            quad = template.clone();
            quad.surfaceInstance = SIDES.unitInstance;
            quad.setFace(face);
            quad.setupFaceQuad( 0.0, 0.0, 1.0, height, 0.0, EnumFacing.UP);
            builder.add(quad.transform(matrix));
        }
        
        quad = template.clone();
        quad.surfaceInstance = TOP_AND_BOTTOM.unitInstance;
        quad.setFace(EnumFacing.DOWN);
        quad.setupFaceQuad(0.0, 0.0, 1.0, 1.0, 0.0, EnumFacing.NORTH);
        builder.add(quad.transform(matrix));
        
        return builder.build();
    }
    
    @Override
    public boolean isAdditive()
    {
        return true;
    }

    @Override
    public @Nonnull List<RawQuad> getShapeQuads(ISuperModelState modelState)
    {
        return this.makeQuads(modelState.getMetaData(), modelState.getMatrix4d());
    }

    @Override
    public boolean isCube(ISuperModelState modelState)
    {
        return modelState.getMetaData() == 15;
    }

    @Override
    public boolean rotateBlock(IBlockState blockState, World world, BlockPos pos, EnumFacing axis, ISuperBlock block, ISuperModelState modelState)
    {
        return false;
    }

    @Override
    public int geometricSkyOcclusion(ISuperModelState modelState)
    {
        return modelState.getAxis() == EnumFacing.Axis.Y ? 255 : modelState.getMetaData();
    }

    @Override
    public BlockOrientationType orientationType(ISuperModelState modelState)
    {
        return BlockOrientationType.FACE;
    }
    
    @Override
    public @Nonnull ICollisionHandler collisionHandler()
    {
        return this;
    }

    @Override
    public List<AxisAlignedBB> getCollisionBoxes(ISuperModelState modelState)
    {
        return ImmutableList.of(getCollisionBoundingBox(modelState));
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(ISuperModelState modelState)
    {
        return Useful.makeRotatedAABB(0, 0, 0, 1, (modelState.getMetaData() + 1) / 16f, 1, modelState.getMatrix4f());
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox(ISuperModelState modelState)
    {
        return getCollisionBoundingBox(modelState);
    }

    @Override
    public SideShape sideShape(ISuperModelState modelState, EnumFacing side)
    {
        if(modelState.getMetaData() ==15) return SideShape.SOLID;
        
        if(side.getAxis() == modelState.getAxis())
        {
            return (side.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE) == modelState.isAxisInverted()
                    ? SideShape.SOLID : SideShape.MISSING;
        }
        else
        {
            return modelState.getMetaData() > 8 ? SideShape.PARTIAL : SideShape.MISSING;
        }
    }
    
    @Override
    public int getMetaData(ISuperModelState modelState)
    {
        return (int) (modelState.getStaticShapeBits() & 0xF);
    }

    @Override
    public void setMetaData(ISuperModelState modelState, int meta)
    {
        modelState.setStaticShapeBits(meta);
    }
    
}