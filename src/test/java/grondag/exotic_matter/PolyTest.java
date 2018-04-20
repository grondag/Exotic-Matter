package grondag.exotic_matter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import grondag.exotic_matter.render.FaceVertex;
import grondag.exotic_matter.render.IMutablePolygon;
import grondag.exotic_matter.render.Poly;
import grondag.exotic_matter.render.QuadHelper;
import grondag.exotic_matter.render.Vec3f;
import net.minecraft.util.EnumFacing;

public class PolyTest
{

    @Test
    public void test()
    {
        Vec3f direction;
        Vec3f point;
        IMutablePolygon quad = Poly.mutable(4).setupFaceQuad(EnumFacing.UP, 0, 0, 1, 1, 0.5, EnumFacing.NORTH);
        
        
        // point on plane
        point = new Vec3f(0.5f, 0.5f, 0.5f);
        direction = new Vec3f(0, 123, 0);
        assertTrue(quad.containsPoint(point));
        
        // point on plane outside poly
        point = new Vec3f(-0.5f, 0.5f, 1.5f);
        direction = new Vec3f(0, 123, 0);
        assertFalse(quad.containsPoint(point));
        
        // point with ray intersecting poly
        assertTrue(quad.intersectsWithRay(0.5f, 0.1f, 0.5f, 0, 1, 0));        
        
        // point with ray intersecting plane outside poly
        point = new Vec3f(-32, 0.2f, 27);
        direction = new Vec3f(0, 123, 0);
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