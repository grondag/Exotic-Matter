package grondag.exotic_matter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import grondag.exotic_matter.render.CSGPlane;
import grondag.exotic_matter.render.FaceVertex;
import grondag.exotic_matter.render.IMutablePolygon;
import grondag.exotic_matter.render.Poly;
import grondag.exotic_matter.render.QuadHelper;
import grondag.exotic_matter.render.RenderPass;
import grondag.exotic_matter.render.Vec3f;
import grondag.exotic_matter.render.Vertex;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.EnumFacing;

public class CSGPlanPerf
{

    @Test
    public void test()
    {
        Random r = new Random(45);
        
        CSGPlane plane = new CSGPlane(new Vec3f(r.nextFloat(), r.nextFloat(), r.nextFloat()).normalize(), r.nextFloat());
        
        for(int i = 0; i < 200000000; i++)
        {
            Vertex v = new Vertex(r.nextFloat(), r.nextFloat(), r.nextFloat(), r.nextFloat(), r.nextFloat(), r.nextInt(), null);
            plane.vertexIncrement(v);
        }
    }

}