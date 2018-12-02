package grondag.exotic_matter.varia;

import java.util.Random;

import org.junit.jupiter.api.Test;

class NormalQuantizerTest
{

    @Test
    void test()
    {
        Random r = new Random();
        
        final int RUNS = 10000;
        final float EPSILON = 1f/512;
        
        for(int i = 0; i < RUNS; i++)
        {
            float x = r.nextFloat();
            float y = r.nextFloat();
            float z = r.nextFloat();
            
            int q = NormalQuantizer.pack(x, y, z);
            
            assert Math.abs(x - NormalQuantizer.unpackX(q)) <= EPSILON;
            assert Math.abs(y - NormalQuantizer.unpackY(q)) <= EPSILON;
            assert Math.abs(z - NormalQuantizer.unpackZ(q)) <= EPSILON;
            
        }
    }

}
