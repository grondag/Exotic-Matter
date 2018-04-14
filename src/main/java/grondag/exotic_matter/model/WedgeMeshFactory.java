package grondag.exotic_matter.model;

import java.util.List;

import javax.annotation.Nonnull;
import javax.vecmath.Matrix4d;
import com.google.common.collect.ImmutableList;

import grondag.exotic_matter.render.FaceVertex;
import grondag.exotic_matter.render.Poly;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.EnumFacing;

public class WedgeMeshFactory extends AbstractWedgeMeshFactory
{
    @Override
    public @Nonnull List<Poly> getShapeQuads(ISuperModelState modelState)
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

        ImmutableList.Builder<Poly> builder = new ImmutableList.Builder<Poly>();
        
        Poly quad = template.clone();
        quad.setSurfaceInstance(BACK_AND_BOTTOM_INSTANCE);
        quad.setNominalFace(EnumFacing.NORTH);
        quad.setupFaceQuad(0, 0, 1, 1, 0, EnumFacing.UP);
        builder.add(quad.transform(matrix));
      
        quad = template.clone();
        quad.setSurfaceInstance(BACK_AND_BOTTOM_INSTANCE);
        quad.setNominalFace(EnumFacing.EAST);
        quad.setupFaceQuad(0, 0, 1, 1, 0, EnumFacing.UP);
        builder.add(quad.transform(matrix));
        
        quad = template.clone();
        quad.setSurfaceInstance(SIDE_INSTANCE);
        quad.setNominalFace(EnumFacing.UP);
        quad.setupFaceQuad(EnumFacing.UP,
                new FaceVertex(0, 1, 0),
                new FaceVertex(1, 0, 0),
                new FaceVertex(1, 1, 0), 
                EnumFacing.NORTH);
        builder.add(quad.transform(matrix));
        
        quad = template.clone();
        quad.setSurfaceInstance(SIDE_INSTANCE);
        quad.setNominalFace(EnumFacing.DOWN);
        quad.setupFaceQuad(EnumFacing.DOWN,
                new FaceVertex(0, 0, 0),
                new FaceVertex(1, 1, 0),
                new FaceVertex(0, 1, 0), 
                EnumFacing.NORTH);
        builder.add(quad.transform(matrix));
        
        quad = template.clone();
        quad.setSurfaceInstance(TOP_INSTANCE);
        quad.setNominalFace(EnumFacing.SOUTH);
        quad.setupFaceQuad(EnumFacing.SOUTH,
                new FaceVertex(0, 0, 1),
                new FaceVertex(1, 0, 0),
                new FaceVertex(1, 1, 0), 
                new FaceVertex(0, 1, 1), 
                EnumFacing.UP);
        builder.add(quad.transform(matrix));
        
        return builder.build();
    }
}
