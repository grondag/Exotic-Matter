package grondag.exotic_matter.model.primitives;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

import org.junit.jupiter.api.Test;

class PointInPolygonTestPerf
{

    @Test
    void test()
    {
        doit();
        doit();
        doit();
    }
    
    //best 
//    nanos per call: 120
//    nanos per call: 106
//    nanos per call: 104

//    nanos per call: 120
//    nanos per call: 103
//    nanos per call: 102
    
    private void doit()
    {
        final int samples = 10000000;
        long elapsed = 0;
        final Random r = new Random(5);
        
        for(int i = 0; i < samples; i++)
        {
            PolyImpl poly = new PolyImpl(3);
            poly.addVertex(0, r.nextFloat(), r.nextFloat(), r.nextFloat(), 0, 0, 0);
            poly.addVertex(1, r.nextFloat(), r.nextFloat(), r.nextFloat(), 0, 0, 0);
            poly.addVertex(2, r.nextFloat(), r.nextFloat(), r.nextFloat(), 0, 0, 0);
            Vec3f p = new Vec3f(r.nextFloat(), r.nextFloat(), r.nextFloat());
            elapsed -= System.nanoTime();
            PointInPolygonTest.isPointInPolygon3(p, poly);
            elapsed += System.nanoTime();
        }
        System.out.println("nanos per call: " + elapsed / samples);
    }
}
