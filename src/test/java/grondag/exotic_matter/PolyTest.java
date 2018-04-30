package grondag.exotic_matter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import grondag.exotic_matter.render.FaceVertex;
import grondag.exotic_matter.render.IMutablePolygon;
import grondag.exotic_matter.render.Poly;
import grondag.exotic_matter.render.QuadHelper;
import grondag.exotic_matter.render.RenderPass;
import grondag.exotic_matter.render.Vec3f;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.EnumFacing;

public class PolyTest
{

    @Test
    public void test()
    {
        Vec3f point;
        IMutablePolygon quad = Poly.mutable(4).setupFaceQuad(EnumFacing.UP, 0, 0, 1, 1, 0.5, EnumFacing.NORTH);
        
        // basic properties
        assert quad.getColor() == 0xFFFFFFFF;
        assert !quad.isFullBrightness();
        assert !quad.isLockUV();
        assert quad.shouldContractUVs();
        assert quad.getNominalFace() == EnumFacing.UP;
        assert quad.getRotation() == Rotation.ROTATE_NONE;
        assert quad.getRenderPass() == RenderPass.SOLID_SHADED;
        
        quad.setFullBrightness(true);
        quad.setLockUV(true);
        quad.setShouldContractUVs(false);
        quad.setNominalFace(null);
        quad.setRotation(Rotation.ROTATE_270);
        quad.setRenderPass(RenderPass.TRANSLUCENT_FLAT);
        quad.setColor(0xFA123456);
        
        assert quad.isFullBrightness();
        assert quad.isLockUV();
        assert !quad.shouldContractUVs();
        assert quad.getNominalFace() == null;
        assert quad.getRotation() == Rotation.ROTATE_270;
        assert quad.getRenderPass() == RenderPass.TRANSLUCENT_FLAT;
        assert quad.getColor() == 0xFA123456;
        
        quad = Poly.mutable(4).setupFaceQuad(EnumFacing.UP, 0, 0, 1, 1, 0.5, EnumFacing.NORTH);
        
        // point on plane
        point = new Vec3f(0.5f, 0.5f, 0.5f);
        assertTrue(quad.containsPoint(point));
        
        // point on plane outside poly
        point = new Vec3f(-0.5f, 0.5f, 1.5f);
        assertFalse(quad.containsPoint(point));
        
        // point with ray intersecting poly
        assertTrue(quad.intersectsWithRay(0.5f, 0.1f, 0.5f, 0, 1, 0));        
        
        // point with ray intersecting plane outside poly
        point = new Vec3f(-32, 0.2f, 27);
        assertFalse(quad.containsPoint(point));
             
        // point with ray facing away from poly
        assertFalse(quad.intersectsWithRay(0.5f, 0.1f, 0.5f, 0, -1, 0));
        
        
        //convexity & area tests
        quad = Poly.mutable(4).setupFaceQuad(EnumFacing.UP, 0, 0, 1, 1, 0.5, EnumFacing.NORTH);
        assertTrue(quad.isConvex());
        assertTrue(Math.abs(quad.getArea() - 1.0) < QuadHelper.EPSILON);
        
        quad = Poly.mutable(3).setupFaceQuad(EnumFacing.UP,
                new FaceVertex(0, 0, 0), 
                new FaceVertex(1, 0, 0), 
                new FaceVertex(1, 1, 0), 
                EnumFacing.NORTH);
        assertTrue(quad.isConvex());
        assertTrue(Math.abs(quad.getArea() - 0.5) < QuadHelper.EPSILON);
        
        quad = Poly.mutable(4).setupFaceQuad(EnumFacing.UP,
                new FaceVertex(0, 0, 0), 
                new FaceVertex(1, 0, 0), 
                new FaceVertex(1, 1, 0), 
                new FaceVertex(0.9f, 0.1f, 0), 
                EnumFacing.NORTH);
        assertFalse(quad.isConvex());
        
        
        // normal facing calculation
        quad = Poly.mutable(4).setupFaceQuad(EnumFacing.UP, 0, 0, 1, 1, 0.5, EnumFacing.NORTH);
        assertTrue(quad.getNormalFace() == EnumFacing.UP);
        
        quad = Poly.mutable(4).setupFaceQuad(EnumFacing.DOWN, 0, 0, 1, 1, 0.5, EnumFacing.NORTH);
        assertTrue(quad.getNormalFace() == EnumFacing.DOWN);
        
        quad = Poly.mutable(4).setupFaceQuad(EnumFacing.EAST, 0, 0, 1, 1, 0.5, EnumFacing.UP);
        assertTrue(quad.getNormalFace() == EnumFacing.EAST);
        
        quad = Poly.mutable(4).setupFaceQuad(EnumFacing.DOWN, 0, 0, 1, 1, 0.5, EnumFacing.NORTH);
        assertTrue(quad.getNormalFace() == EnumFacing.DOWN);
        
        quad = Poly.mutable(4).setupFaceQuad(EnumFacing.SOUTH,
                new FaceVertex(0, 0, 0.1f), 
                new FaceVertex(1, 0, 0.1f), 
                new FaceVertex(1, 1, 0), 
                new FaceVertex(0.9f, 0.1f, 0), 
                EnumFacing.UP);
        assertTrue(quad.getNormalFace() == EnumFacing.SOUTH);

    }

}