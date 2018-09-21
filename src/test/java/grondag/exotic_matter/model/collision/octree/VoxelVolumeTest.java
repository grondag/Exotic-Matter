package grondag.exotic_matter.model.collision.octree;

import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.atomic.LongAdder;

import org.junit.jupiter.api.Test;

import grondag.exotic_matter.concurrency.SimpleConcurrentCounter;

class VoxelVolumeTest
{

    @Test
    void test()
    {
        fillTest();
        perfTest();
        perfTest();
        perfTest();
        perfTest();
        perfTest();
    }
    
    void fillTest()
    {
        VoxelOctree8 vot = new VoxelOctree8();

        // hollow box in middle
        // from 2, 2, 2 thru 6, 6, 6
        for(int i = 2; i < 7; i++)
        {
            for(int j = 2; j < 7; j++)
            {
                vot.setFull(OctreeCoordinates.xyzToIndex3(i, j, 2), 3);
                vot.setFull(OctreeCoordinates.xyzToIndex3(i, j, 6), 3);

                vot.setFull(OctreeCoordinates.xyzToIndex3(i, 2, j), 3);
                vot.setFull(OctreeCoordinates.xyzToIndex3(i, 6, j), 3);

                vot.setFull(OctreeCoordinates.xyzToIndex3(2, i, j), 3);
                vot.setFull(OctreeCoordinates.xyzToIndex3(6, i, j), 3);
            }
        }

//        VoxelVolume.loadVolume(vot, data);
//        VoxelVolume.fillVolume8(data);

        // track 4 x 4 x 4 
        // should see 1, 1, 1 thru 3, 3, 3 filled
        // because voxels at 6 will be simplified to include face voxels
        // Except: corner voxels at high edges won't be - only 1 or 2 subvoxels in them
        BitSet bits = new BitSet();
        VoxelVolume.forEachSimpleVoxel(vot, (x, y, z) ->
        {
            x = x / 2;
            y = y / 2;
            z = z / 2;
            System.out.println(String.format("Simple vox @ %d, %d, %d", x, y, z));
            bits.set(x | (y << 2) | (z << 4));
        });
        
        for(int x = 0; x < 4; x++)
        {
            for(int y = 0; y < 4; y++)
            {
                for(int z = 0; z < 4; z++)
                {
                    int x2 = x * 2;
                    int y2 = y * 2;
                    int z2 = z * 2;
                    int count = isIn8(x2, y2, z2)
                            + isIn8(x2 + 1, y2, z2)
                            
                            + isIn8(x2, y2 + 1, z2)
                            + isIn8(x2 + 1, y2 + 1, z2)
                            
                            + isIn8(x2, y2, z2 + 1)
                            + isIn8(x2 + 1, y2, z2 + 1)
                            
                            + isIn8(x2, y2 + 1, z2 + 1)
                            + isIn8(x2 + 1, y2 + 1, z2 + 1);
;
                    
                    assert bits.get(x | (y << 2) | (z << 4)) == count >= 4;
                }
            }
        }
    }

    int isIn8(int x, int y, int z)
    {
        return (x < 2 || x > 6 || y < 2 || y > 6 || z < 2 || z > 6) ? 0 : 1;
    }
    
    
    // best
//    Avg nanos = 21770, check total = 8598240
//    Avg nanos = 21610, check total = 8598240
//    Avg nanos = 22143, check total = 8598240
    void perfTest()
    {
        final VoxelOctree8 vot = new VoxelOctree8();
        final Random r = new Random(42);
        final long start = System.nanoTime();
        final int SAMPLE_COUNT = 100000;
        LongAdder adder = new LongAdder();
        
        for(int i = 0; i < SAMPLE_COUNT; i++)
        {
            final int vCount = 64 + r.nextInt(256);
            for(int j = 0; j < vCount; j++)
                vot.setFull(OctreeCoordinates.xyzToIndex3(r.nextInt(7), r.nextInt(7), r.nextInt(7)), 3);
            
            VoxelVolume.forEachSimpleVoxel(vot, (x, y, z) ->
            {
                adder.add(x + y + z);
            });
            vot.clear();
        }
        
        System.out.println(String.format("Avg nanos = %d, check total = %d", (System.nanoTime() - start) / SAMPLE_COUNT, adder.longValue()));
        
    }
}
