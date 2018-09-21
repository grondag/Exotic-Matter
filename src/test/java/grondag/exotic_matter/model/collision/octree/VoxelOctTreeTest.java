package grondag.exotic_matter.model.collision.octree;

import org.junit.jupiter.api.Test;

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
        VoxelOctree16 vot = new VoxelOctree16();
        
        assert vot.isEmpty(0, 0);

        vot.clear();
        
        for(int x = 0; x < 16; x++)
        {
            for(int y = 0; y < 16; y++)
            {
                for(int z = 0; z < 16; z++)
                {
                    vot.setFull(OctreeCoordinates.xyzToIndex4(x, y, z), 4);
                }
            }
        }
        
        assert vot.isFull(0, 0);
        
        for(int x = 0; x < 16; x++)
        {
            for(int y = 0; y < 16; y++)
            {
                for(int z = 0; z < 16; z++)
                {
                    vot.clear(OctreeCoordinates.xyzToIndex4(x, y, z), 4);
                }
                
            }
        }
        
       assert vot.isEmpty(0, 0);
        
       vot.clear();
        
        for(int x = 0; x < 16; x += 2)
        {
            for(int y = 0; y < 16; y++)
            {
                for(int z = 0; z < 16; z++)
                {
                    vot.setFull(OctreeCoordinates.xyzToIndex4(x, y, z), 4);
                }
            }
        }
        
        vot.simplify();
        
        assert vot.isFull(0, 0);
    }
    
    private void testCoarse()
    {
        VoxelOctree8 vot = new VoxelOctree8();
        
        assert vot.isEmpty(0, 0);

        vot.clear();
        
        for(int x = 0; x < 8; x++)
        {
            for(int y = 0; y < 8; y++)
            {
                for(int z = 0; z < 8; z++)
                {
                    vot.setFull(OctreeCoordinates.xyzToIndex3(x, y, z), 3);
                }
            }
        }
        
        assert vot.isFull(0, 0);
        
        for(int x = 0; x < 8; x++)
        {
            for(int y = 0; y < 8; y++)
            {
                for(int z = 0; z < 8; z++)
                {
                    vot.clear(OctreeCoordinates.xyzToIndex3(x, y, z), 3);
                }
                
            }
        }
        
       assert vot.isEmpty(0, 0);
        
       vot.clear();
        
        for(int x = 0; x < 8; x += 2)
        {
            for(int y = 0; y < 8; y++)
            {
                for(int z = 0; z < 8; z++)
                {
                    vot.setFull(OctreeCoordinates.xyzToIndex3(x, y, z), 3);
                }
            }
        }
        
        vot.simplify();
        
        assert vot.isFull(0, 0);
    }
}
