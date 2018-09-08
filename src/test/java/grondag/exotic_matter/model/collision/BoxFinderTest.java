package grondag.exotic_matter.model.collision;

import org.junit.jupiter.api.Test;

import grondag.exotic_matter.model.collision.BoxFinderUtils.Slice;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class BoxFinderTest
{

    @Test
    void test()
    {
        forEachBitTest();
        
        for(Slice slice : Slice.values())
        {
            for(int i = 0; i < BoxFinderUtils.AREAS.length; i++)
            {
                int volumeKey = BoxFinderUtils.volumeKey(slice, i);
                
                assert BoxFinderUtils.volumeFromKey(volumeKey) == BoxFinderUtils.volume(slice, i);
                assert BoxFinderUtils.sliceFromKey(volumeKey) == slice;
                assert BoxFinderUtils.patternFromKey(volumeKey) == BoxFinderUtils.AREAS[i];
            }
        }
        
        BoxFinder bf = new BoxFinder();
        
        bf.setFilled(0, 0, 0, 1, 1, 7);
        bf.setFilled(0, 6, 0, 1, 7, 7);
        bf.calcCombined();
        bf.populateMaximalVolumes();
        
        assert bf.volumeCount == 2;
        assert BoxFinderUtils.volumeFromKey(bf.maximalVolumes[0]) == 32;
        assert BoxFinderUtils.volumeFromKey(bf.maximalVolumes[1]) == 32;
        
        bf.setFilled(0, 2, 4, 1, 5, 5);
        bf.calcCombined();
        bf.populateMaximalVolumes();
        
        assert bf.volumeCount == 3;
        assert BoxFinderUtils.volumeFromKey(bf.maximalVolumes[0]) == 32;
        assert BoxFinderUtils.volumeFromKey(bf.maximalVolumes[1]) == 32;
        assert BoxFinderUtils.volumeFromKey(bf.maximalVolumes[2]) == 32;
        
        bf.populateIntersects();
        
        bf.findDisjointSets();
        
        assert bf.disjointSets.size() == 2;
        
        bf.scoreMaximalVolumes();
        
        assert bf.volumeScores[0] == 1;
        assert bf.volumeScores[1] == 1;
        assert bf.volumeScores[2] == 4;
        
        for(Long l : bf.disjointSets)
        {
            bf.explainDisjointSet(l);
        }
    }
    
    void forEachBitTest()
    {
        IntArrayList results = new IntArrayList();
        
        BoxFinderUtils.forEachBit(0xFFFFFFFFFFFFFFFFL, i -> results.add(i));
        
        assert results.size() == 64;
        
        for(int i = 0; i < 64; i++)
        {
            assert results.contains(i);
        }
    }
}
