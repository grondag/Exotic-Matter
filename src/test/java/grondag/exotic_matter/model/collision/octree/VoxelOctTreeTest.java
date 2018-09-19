package grondag.exotic_matter.model.collision.octree;

import org.junit.jupiter.api.Test;

import grondag.exotic_matter.model.collision.octree.IVoxelOctree;
import grondag.exotic_matter.model.collision.octree.VoxelOctree;

class VoxelOctTreeTest
{

    @Test
    void test()
    {
       testDetailed();
       testCoarse();
    }
    
    private void testDetailed()
    {
        VoxelOctree vot = new VoxelOctree(true);
        
        assert vot.isEmpty();

        testBox(vot);
        
        vot.clear();
        
        for(int x = 0; x < 16; x++)
        {
            for(int y = 0; y < 16; y++)
            {
                for(int z = 0; z < 16; z++)
                {
                    vot.voxel(x, y, z).setFull();
                }
            }
        }
        
        assert vot.isFull();
        
        for(int x = 0; x < 16; x++)
        {
            for(int y = 0; y < 16; y++)
            {
                for(int z = 0; z < 16; z++)
                {
                    vot.voxel(x, y, z).clear();
                }
                
            }
        }
        
       assert vot.isEmpty();
        
       vot.clear();
        
        for(int x = 0; x < 16; x += 2)
        {
            for(int y = 0; y < 16; y++)
            {
                for(int z = 0; z < 16; z++)
                {
                    vot.voxel(x, y, z).setFull();
                }
            }
        }
        
        vot.simplify();
        
        assert vot.isFull();
    }
    
    private void testBox(IVoxelOctree t)
    {
        for(int i = 0; i < 8; i++)
        {
            IVoxelOctree st = t.subNode(i);
//            assert st.xMin() >= t.xMin();
//            assert st.xMax() <= t.xMax();
//            assert st.yMin() >= t.yMin();
//            assert st.yMax() <= t.yMax();
//            assert st.zMin() >= t.zMin();
//            assert st.zMax() <= t.zMax();
            if(st.divisionLevel() < 4)
                testBox(st);
        }
    }
    
    private void testCoarse()
    {
        VoxelOctree vot = new VoxelOctree(false);
        
        assert vot.isEmpty();

        testBox(vot);
        
        vot.clear();
        
        for(int x = 0; x < 8; x++)
        {
            for(int y = 0; y < 8; y++)
            {
                for(int z = 0; z < 8; z++)
                {
                    vot.bottom(x, y, z).setFull();
                }
            }
        }
        
        assert vot.isFull();
        
        for(int x = 0; x < 8; x++)
        {
            for(int y = 0; y < 8; y++)
            {
                for(int z = 0; z < 8; z++)
                {
                    vot.bottom(x, y, z).clear();
                }
                
            }
        }
        
       assert vot.isEmpty();
        
       vot.clear();
        
        for(int x = 0; x < 8; x += 2)
        {
            for(int y = 0; y < 8; y++)
            {
                for(int z = 0; z < 8; z++)
                {
                    vot.bottom(x, y, z).setFull();
                }
            }
        }
        
        vot.simplify();
        
        assert vot.isFull();
    }
}
