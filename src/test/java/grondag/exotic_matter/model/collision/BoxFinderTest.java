package grondag.exotic_matter.model.collision;

import org.junit.jupiter.api.Test;

import grondag.exotic_matter.model.collision.BoxFinder.Slice;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;

class BoxFinderTest
{

    @Test
    void test()
    {
        for(Slice slice : Slice.values())
        {
            for(int i = 0; i < BoxHelper.AREAS.length; i++)
            {
                int volumeKey = BoxHelper.volumeKey(slice, i);
                
                assert BoxHelper.volumeFromKey(volumeKey) == BoxHelper.volume(slice, i);
                assert BoxHelper.sliceFromKey(volumeKey) == slice;
                assert BoxHelper.patternFromKey(volumeKey) == BoxHelper.AREAS[i];
            }
        }
        
        BoxFinder bf = new BoxFinder();
        
        bf.setFilled(0, 0, 0, 1, 1, 7);
        bf.setFilled(0, 6, 0, 1, 7, 7);
        bf.calcCombined();
        bf.populateMaximalVolumes();
        
        assert bf.maximalVolumes.size() == 2;
        assert BoxHelper.volumeFromKey(bf.maximalVolumes.getInt(0)) == 32;
        assert BoxHelper.volumeFromKey(bf.maximalVolumes.getInt(1)) == 32;
        
        bf.setFilled(0, 2, 4, 1, 5, 5);
        bf.calcCombined();
        bf.populateMaximalVolumes();
        
        assert bf.maximalVolumes.size() == 3;
        assert BoxHelper.volumeFromKey(bf.maximalVolumes.getInt(0)) == 32;
        assert BoxHelper.volumeFromKey(bf.maximalVolumes.getInt(1)) == 32;
        assert BoxHelper.volumeFromKey(bf.maximalVolumes.getInt(2)) == 32;
    }
}
