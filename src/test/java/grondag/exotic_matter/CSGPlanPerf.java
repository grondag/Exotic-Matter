package grondag.exotic_matter;

import java.util.Random;

import org.junit.Test;

import grondag.exotic_matter.model.CSG.CSGPlane;
import grondag.exotic_matter.model.primitives.better.IMutableVertex;
import grondag.exotic_matter.model.primitives.better.PolyFactory;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;

public class CSGPlanPerf
{

    @Test
    public void test()
    {
        Random r = new Random(45);
        
        Vec3f vec = new Vec3f.Mutable(r.nextFloat(), r.nextFloat(), r.nextFloat()).normalize().toImmutable();
        CSGPlane plane = new CSGPlane(vec, r.nextFloat());
        
        for(int i = 0; i < 200000000; i++)
        {
            IMutableVertex v = PolyFactory.claimMutableVertex();
            v.setPos(r.nextFloat(), r.nextFloat(), r.nextFloat());
            plane.vertexIncrement(v);
        }
    }

}