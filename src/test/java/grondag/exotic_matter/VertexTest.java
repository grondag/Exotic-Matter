package grondag.exotic_matter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;

import grondag.exotic_matter.model.primitives.better.IMutableGeometricVertex;
import grondag.exotic_matter.model.primitives.better.PolyFactory;

class VertexTest
{

    @Test
    void test()
    {
        IMutableGeometricVertex testPoint = PolyFactory.claimMutableVertex(.5f, .5f, .5f);
        
        assertTrue(testPoint.isOnLine(0, 0, 0, 1, 1, 1));
        assertTrue(testPoint.isOnLine(.5f, 0, .5f, .5f, 1, .5f));
        assertFalse(testPoint.isOnLine(.7f, 2, .1f, 0, -1, .25f));
        
        testPoint = PolyFactory.claimMutableVertex(.6f, .4f, .5333333333f);
        assertTrue(testPoint.isOnLine(0.6f, 0.4f, 0.4f, 0.6f, .4f, .6f));
    }

}
