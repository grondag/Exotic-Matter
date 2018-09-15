package grondag.exotic_matter.model.collision;

import org.junit.jupiter.api.Test;

class VoxelOctTreeTest
{

    @Test
    void test()
    {
        VoxelOctree vot = new VoxelOctree();
        
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
            assert st.xMin() >= t.xMin();
            assert st.xMax() <= t.xMax();
            assert st.yMin() >= t.yMin();
            assert st.yMax() <= t.yMax();
            assert st.zMin() >= t.zMin();
            assert st.zMax() <= t.zMax();
            if(st.hasSubnodes())
                testBox(st);
        }
    }
}
