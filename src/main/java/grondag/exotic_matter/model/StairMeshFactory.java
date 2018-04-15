package grondag.exotic_matter.model;

import java.util.List;

import javax.annotation.Nonnull;
import javax.vecmath.Matrix4d;

import com.google.common.collect.ImmutableList;

import grondag.exotic_matter.render.IPolygon;
import grondag.exotic_matter.render.Poly;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.EnumFacing;

public class StairMeshFactory extends AbstractWedgeMeshFactory
{
    @Override
    public @Nonnull List<IPolygon> getShapeQuads(ISuperModelState modelState)
    {
        // Axis for this shape is along the face of the sloping surface
        // Four rotations x 3 axes gives 12 orientations - one for each edge of a cube.
        // Default geometry is Y orthogonalAxis with full sides against north/east faces.

        Matrix4d matrix = modelState.getMatrix4d();
        
        Poly template = new Poly();
        template.setColor(0xFFFFFFFF);
        template.setRotation(Rotation.ROTATE_NONE);
        template.setFullBrightness(false);
        template.setLockUV(true);

        ImmutableList.Builder<IPolygon> builder = ImmutableList.builder();
        
        Poly quad = template.clone();
        quad.setSurfaceInstance(BACK_AND_BOTTOM_INSTANCE);
        quad.setNominalFace(EnumFacing.NORTH);
        quad.setupFaceQuad(0, 0, 1, 1, 0, EnumFacing.UP);
        quad = quad.transform(matrix);
        builder.add(quad);
      
        quad = template.clone();
        quad.setSurfaceInstance(BACK_AND_BOTTOM_INSTANCE);
        quad.setNominalFace(EnumFacing.EAST);
        quad.setupFaceQuad(0, 0, 1, 1, 0, EnumFacing.UP);
        quad = quad.transform(matrix);
        builder.add(quad);
        
        // Splitting sides into three quadrants vs one long strip plus one long quadrant
        // is necessary to avoid AO lighting artifacts.  AO is done by vertex, and having
        // a T-junction tends to mess about with the results.
        
        quad = template.clone();
        quad.setSurfaceInstance(SIDE_INSTANCE);
        quad.setupFaceQuad(EnumFacing.UP, 0.0, 0.5, 0.5, 1.0, 0.0, EnumFacing.NORTH);
        quad = quad.transform(matrix);
        builder.add(quad);
        
        quad = template.clone();
        quad.setSurfaceInstance(SIDE_INSTANCE);
        quad.setupFaceQuad(EnumFacing.UP, 0.5, 0.5, 1.0, 1.0, 0.0, EnumFacing.NORTH);
        quad = quad.transform(matrix);
        builder.add(quad);
        
        quad = template.clone();
        quad.setSurfaceInstance(SIDE_INSTANCE);
        quad.setupFaceQuad(EnumFacing.UP, 0.5, 0.0, 1.0, 0.5, 0.0, EnumFacing.NORTH);
        quad = quad.transform(matrix);
        builder.add(quad);
        
        // Splitting sides into three quadrants vs one long strip plus one long quadrant
        // is necessary to avoid AO lighting artifacts.  AO is done by vertex, and having
        // a T-junction tends to mess about with the results.
        
        quad = template.clone();
        quad.setSurfaceInstance(SIDE_INSTANCE);
        quad.setupFaceQuad(EnumFacing.DOWN, 0.0, 0.5, 0.5, 1.0, 0.0, EnumFacing.NORTH);
        quad = quad.transform(matrix);
        builder.add(quad);
        
        quad = template.clone();
        quad.setSurfaceInstance(SIDE_INSTANCE);
        quad.setupFaceQuad(EnumFacing.DOWN, 0.5, 0.5, 1.0, 1.0, 0.0, EnumFacing.NORTH);
        quad = quad.transform(matrix);
        builder.add(quad);
        
        quad = template.clone();
        quad.setSurfaceInstance(SIDE_INSTANCE);
        quad.setupFaceQuad(EnumFacing.DOWN, 0.0, 0.0, 0.5, 0.5, 0.0, EnumFacing.NORTH);
        quad = quad.transform(matrix);
        builder.add(quad);
        
        
        
        quad = template.clone();
        quad.setSurfaceInstance(SIDE_INSTANCE);
        quad.setupFaceQuad(EnumFacing.SOUTH, 0.5, 0.0, 1.0, 1.0, 0.0, EnumFacing.UP);
        quad = quad.transform(matrix);
        builder.add(quad);
        
        quad = template.clone();
        quad.setSurfaceInstance(TOP_INSTANCE);
        quad.setupFaceQuad(EnumFacing.SOUTH, 0.0, 0.0, 0.5, 1.0, 0.5, EnumFacing.UP);
        quad = quad.transform(matrix);
        builder.add(quad);
        
        quad = template.clone();
        quad.setSurfaceInstance(SIDE_INSTANCE);
        quad.setupFaceQuad(EnumFacing.WEST, 0.0, 0.0, 0.5, 1.0, 0.0, EnumFacing.UP);
        quad = quad.transform(matrix);
        builder.add(quad);
        
        quad = template.clone();
        quad.setSurfaceInstance(TOP_INSTANCE);
        quad.setupFaceQuad(EnumFacing.WEST, 0.5, 0.0, 1.0, 1.0, 0.5, EnumFacing.UP);
        quad = quad.transform(matrix);
        builder.add(quad);
        
        return builder.build();
    }
}
