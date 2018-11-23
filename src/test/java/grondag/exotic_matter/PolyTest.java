package grondag.exotic_matter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import grondag.exotic_matter.model.primitives.FaceVertex;
import grondag.exotic_matter.model.primitives.QuadHelper;
import grondag.exotic_matter.model.primitives.better.IMutablePolygon;
import grondag.exotic_matter.model.primitives.better.PolyFactory;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;

public class PolyTest
{

    @Test
    public void test()
    {
        IMutablePolygon quad = PolyFactory.newPaintable(4).setupFaceQuad(EnumFacing.UP, 0, 0, 1, 1, 0.5, EnumFacing.NORTH);
        
        // basic properties
        assert !quad.isLockUV(0);
        assert quad.shouldContractUVs(0);
        assert quad.getNominalFace() == EnumFacing.UP;
        assert quad.getRotation(0) == Rotation.ROTATE_NONE;
        assert quad.getRenderLayer(0) == BlockRenderLayer.SOLID;
        
        quad.setLockUV(0, true);
        quad.setShouldContractUVs(0, false);
        quad.setNominalFace(EnumFacing.DOWN);
        quad.setRotation(0, Rotation.ROTATE_270);
        quad.setRenderLayer(0, BlockRenderLayer.TRANSLUCENT);
        
        assert quad.isLockUV(0);
        assert !quad.shouldContractUVs(0);
        assert quad.getNominalFace() == EnumFacing.DOWN;
        assert quad.getRotation(0) == Rotation.ROTATE_270;
        assert quad.getRenderLayer(0) == BlockRenderLayer.TRANSLUCENT;
        
        quad = PolyFactory.newPaintable(4).setupFaceQuad(EnumFacing.UP, 0, 0, 1, 1, 0.5, EnumFacing.NORTH);
        
        //convexity & area tests
        quad = PolyFactory.newPaintable(4).setupFaceQuad(EnumFacing.UP, 0, 0, 1, 1, 0.5, EnumFacing.NORTH);
        assertTrue(quad.isConvex());
        assertTrue(Math.abs(quad.getArea() - 1.0) < QuadHelper.EPSILON);
        
        quad = PolyFactory.newPaintable(3).setupFaceQuad(EnumFacing.UP,
                new FaceVertex(0, 0, 0), 
                new FaceVertex(1, 0, 0), 
                new FaceVertex(1, 1, 0), 
                EnumFacing.NORTH);
        assertTrue(quad.isConvex());
        assertTrue(Math.abs(quad.getArea() - 0.5) < QuadHelper.EPSILON);
        
        quad = PolyFactory.newPaintable(4).setupFaceQuad(EnumFacing.UP,
                new FaceVertex(0, 0, 0), 
                new FaceVertex(1, 0, 0), 
                new FaceVertex(1, 1, 0), 
                new FaceVertex(0.9f, 0.1f, 0), 
                EnumFacing.NORTH);
        assertFalse(quad.isConvex());
        
        
        // normal facing calculation
        quad = PolyFactory.newPaintable(4).setupFaceQuad(EnumFacing.UP, 0, 0, 1, 1, 0.5, EnumFacing.NORTH);
        assertTrue(quad.getNormalFace() == EnumFacing.UP);
        
        quad = PolyFactory.newPaintable(4).setupFaceQuad(EnumFacing.DOWN, 0, 0, 1, 1, 0.5, EnumFacing.NORTH);
        assertTrue(quad.getNormalFace() == EnumFacing.DOWN);
        
        quad = PolyFactory.newPaintable(4).setupFaceQuad(EnumFacing.EAST, 0, 0, 1, 1, 0.5, EnumFacing.UP);
        assertTrue(quad.getNormalFace() == EnumFacing.EAST);
        
        quad = PolyFactory.newPaintable(4).setupFaceQuad(EnumFacing.DOWN, 0, 0, 1, 1, 0.5, EnumFacing.NORTH);
        assertTrue(quad.getNormalFace() == EnumFacing.DOWN);
        
        quad = PolyFactory.newPaintable(4).setupFaceQuad(EnumFacing.SOUTH,
                new FaceVertex(0, 0, 0.1f), 
                new FaceVertex(1, 0, 0.1f), 
                new FaceVertex(1, 1, 0), 
                new FaceVertex(0.9f, 0.1f, 0), 
                EnumFacing.UP);
        assertTrue(quad.getNormalFace() == EnumFacing.SOUTH);

    }

}