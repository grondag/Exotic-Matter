package grondag.exotic_matter.model.collision.octree;

import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.atomic.LongAdder;

import org.junit.jupiter.api.Test;

import grondag.exotic_matter.ExoticMatter;

class VoxelVolumeTest
{

    @Test
    void test()
    {
        fillTestDirect();
        fillTest();
        perfTest();
        perfTest();
        perfTest();
        perfTest();
        perfTest();
        perfTest();
        perfTest();
        perfTest();
        perfTest();
        perfTest();
    }
    
    void fillTestDirect()
    {
        long[] data = new long[16];
        
        // 00000000
        // 00011100
        // 00100010
        // 01000001
        // 10000010
        // 10001100
        // 01001000
        // 01111100
        final long section = 0b0000000000011100001000100100000110000010100011000100100001111100L;
        
        // 00000000
        // 00011100
        // 00111110
        // 01111111
        // 11111110
        // 11111100
        // 01111000
        // 01111100
        final long cap = 0b0000000000011100001111100111111111111110111111000111100001111100L;
        
        data[0] = 0;
        data[1] = cap;
        data[2] = section;
        data[3] = section;
        data[4] = section;
        data[5] = section;
        data[6] = section;
        data[7] = cap;
        
        VoxelVolume.fillVolume8(data);
        outputCarvedLayers(data);
        assert data[8] == 0;
        assert data[9] == cap;
        assert data[10] == cap;
        assert data[11] == cap;
        assert data[12] == cap;
        assert data[13] == cap;
        assert data[14] == cap;
        assert data[15] == cap;
        
    }
    
    void outputCarvedLayers(long[] data)
    {
        for(int i = 8; i < 16; i++)
            outputLayer(data, i);
    }
    
    void outputLayer(long[] data, int index)
    {
        ExoticMatter.INSTANCE.info("LAYER %d", index);
        long bits = data[index];
        ExoticMatter.INSTANCE.info(Long.toBinaryString(bits & 0xFFL));
        ExoticMatter.INSTANCE.info(Long.toBinaryString((bits >> 8) & 0xFFL));
        ExoticMatter.INSTANCE.info(Long.toBinaryString((bits >> 16) & 0xFFL));
        ExoticMatter.INSTANCE.info(Long.toBinaryString((bits >> 24) & 0xFFL));
        ExoticMatter.INSTANCE.info(Long.toBinaryString((bits >> 32) & 0xFFL));
        ExoticMatter.INSTANCE.info(Long.toBinaryString((bits >> 40) & 0xFFL));
        ExoticMatter.INSTANCE.info(Long.toBinaryString((bits >> 48) & 0xFFL));
        ExoticMatter.INSTANCE.info(Long.toBinaryString((bits >> 56) & 0xFFL));
        ExoticMatter.INSTANCE.info("");
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
    
    
    // best - prior algorithm - suspect diff total due to error was in prior
//    Avg nanos = 21119, check total = 8598240
//    Avg nanos = 21184, check total = 8598240
//    Avg nanos = 21163, check total = 8598240
    
    // best - current bitwise fill algorithm
//    Avg nanos = 12768, check total = 9307710
//    Avg nanos = 12839, check total = 9307710
//    Avg nanos = 12667, check total = 9307710
    
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
