package grondag.exotic_matter.model.primitives;


import java.util.Random;

import org.junit.jupiter.api.Test;

class TriangleBoxTestPerf
{
    @Test
    void test()
    {
        for(int i = 0; i < 100; i++)
            doit();
    }
    
    //best
//    true count = 49995645  nanos per call: 79
    
    private void doit()
    {
        final int samples = 100000000;
        long elapsed = 0;
        final Random r = new Random(42);

        final float[] polyData = new float[27];
        int trueCount = 0;
        
        for(int i = 0; i < samples; i++)
        {
            PolyImpl poly = new PolyImpl(3);
            poly.addVertex(0, r.nextFloat(), r.nextFloat(), r.nextFloat(), 0, 0, 0);
            poly.addVertex(1, r.nextFloat(), r.nextFloat(), r.nextFloat(), 0, 0, 0);
            poly.addVertex(2, r.nextFloat(), r.nextFloat(), r.nextFloat(), 0, 0, 0);
            TriangleBoxTest.packPolyData(poly.getVertex(0), poly.getVertex(1), poly.getVertex(2), polyData);
            elapsed -= System.nanoTime();
            if(TriangleBoxTest.triBoxOverlap(0.25f, 0.25f, 0.25f, 0.25f, polyData))
                trueCount++;
            elapsed += System.nanoTime();
        }
        System.out.println("true count = " + trueCount + "  nanos per call: " + elapsed / samples);
        System.out.println("");
    }
}
