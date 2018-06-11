package grondag.exotic_matter.model.mesh;

import static grondag.exotic_matter.model.state.ModelStateData.STATE_FLAG_HAS_AXIS;
import static grondag.exotic_matter.model.state.ModelStateData.STATE_FLAG_HAS_AXIS_ORIENTATION;
import static grondag.exotic_matter.model.state.ModelStateData.STATE_FLAG_NEEDS_SPECIES;

import java.util.List;

import javax.annotation.Nonnull;
import javax.vecmath.Matrix4f;

import com.google.common.collect.ImmutableList;

import grondag.exotic_matter.block.ISuperBlock;
import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.model.painting.SurfaceTopology;
import grondag.exotic_matter.model.painting.SurfaceType;
import grondag.exotic_matter.model.primitives.IMutablePolygon;
import grondag.exotic_matter.model.primitives.IPolygon;
import grondag.exotic_matter.model.primitives.Poly;
import grondag.exotic_matter.model.state.ISuperModelState;
import grondag.exotic_matter.model.state.StateFormat;
import grondag.exotic_matter.model.varia.ICollisionHandler;
import grondag.exotic_matter.model.varia.SideShape;
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
 
    private List<IPolygon> makeQuads(int meta, Matrix4f matrix)
    {
        float height = (meta + 1) / 16;
        
        IMutablePolygon template = Poly.mutable(4);
        template.setColor(0xFFFFFFFF);
        template.setRotation(Rotation.ROTATE_NONE);
        template.setLockUV(true);

        ImmutableList.Builder<IPolygon> builder = ImmutableList.builder();
        
        IMutablePolygon quad = Poly.mutable(template);
        quad.setSurfaceInstance(TOP_AND_BOTTOM.unitInstance);
        quad.setNominalFace(EnumFacing.UP);
        quad.setupFaceQuad(0, 0, 1, 1, 1-height, EnumFacing.NORTH);
        quad.transform(matrix);
        builder.add(quad);
      
        for(EnumFacing face : EnumFacing.Plane.HORIZONTAL.facings())
        {
            quad = Poly.mutable(template);
            quad.setSurfaceInstance(SIDES.unitInstance);
            quad.setNominalFace(face);
            quad.setupFaceQuad( 0, 0, 1, height, 0, EnumFacing.UP);
            quad.transform(matrix);
            builder.add(quad);
        }
        
        quad = Poly.mutable(template);
        quad.setSurfaceInstance(TOP_AND_BOTTOM.unitInstance);
        quad.setNominalFace(EnumFacing.DOWN);
        quad.setupFaceQuad(0, 0, 1, 1, 0, EnumFacing.NORTH);
        quad.transform(matrix);
        builder.add(quad);
        
        return builder.build();
    }
    
    @Override
    public boolean isAdditive()
    {
        return true;
    }

    @Override
    public @Nonnull List<IPolygon> getShapeQuads(ISuperModelState modelState)
    {
        return this.makeQuads(modelState.getMetaData(), modelState.getMatrix4f());
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
