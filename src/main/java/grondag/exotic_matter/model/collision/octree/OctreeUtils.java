package grondag.exotic_matter.model.collision.octree;

import java.util.Arrays;

public class OctreeUtils
{
    public static final long FULL_BITS = 0xFFFFFFFFFFFFFFFFL;
    public static final long[] ALL_FULL = new long[64];
    public static final long[] ALL_EMPTY = new long[64];
    
    static
    {
        Arrays.fill(ALL_FULL, FULL_BITS);
    }
    
    /**
     * Gives octree index w/ division level 3 from packed 3-bit Cartesian coordinates
     */
    static int xyzToIndex3(final int xyz3)
    {
        //coordinate values are 3 bits each: xxx, yyy, zzz
        //voxel coordinates are interleaved: zyx zyx zyx
        
        // shift all bits of y, z at once to avoid separate shift ops later
        
        final int y = xyz3 >> 2;
        final int z = xyz3 >> 4;
        
        return (xyz3 & 1) | (y & 2) | (z & 4)
         | (((xyz3 & 2) | (y & 4) | (z & 8)) << 2)
         | (((xyz3 & 4) | (y & 8) | (z & 16)) << 4);
    }
    
    /**
     * Gives packed 3-bit Cartesian coordinates from octree index w/ division level 3
     */
    static int indexToXYZ3(final int i3)
    {
        //coordinate values are 3 bits each: xxx, yyy, zzz
        //voxel coordinates are interleaved: zyx zyx zyx
        
        final int j = i3 >> 2;
        final int k = i3 >> 4;
        return ((i3 & 1) | (j & 2) | (k & 4)) 
               | (((i3 & 2) | (j & 4) | (k & 8)) << 2)
               | (((i3 & 4) | (j & 8) | (k & 16)) << 4);
    }
    
    /**
     * Packed 3-bit Cartesian coordinates
     */
    static int packedXYZ3(int x, int y, int z)
    {
        return x | (y << 3) | (z << 6);
    }
    
    static int xyzToIndex3(int x, int y, int z)
    {
        return xyzToIndex3(packedXYZ3(x, y, z));
    }
    
    /**
     * Gives octree index w/ division level 4 from packed 4-bit Cartesian coordinates
     */
    static int xyzToIndex4(final int xyz4)
    {
        //coordinate values are 4 bits each: xxxx, yyyy, zzzz
        //voxel coordinates are interleaved: zyx zyx zyx zyx
        
        // shift all bits of y, z at once to avoid separate shift ops later
        // like so:
        //   xxxx
        //  yyyy
        // zzzz
        
        final int y = xyz4 >> 3;
        final int z = xyz4 >> 6;
        
        return (xyz4 & 1) | (y & 2) | (z & 4)
         | (((xyz4 & 2) | (y & 4) | (z & 8)) << 2)
         | (((xyz4 & 4) | (y & 8) | (z & 16)) << 4)
         | (((xyz4 & 8) | (y & 16) | (z & 32)) << 6);
    }
    
    static int xyzToIndex4(int x, int y, int z)
    {
        return xyzToIndex4(packedXYZ4(x, y, z));
    }
    
    /**
     * Gives packed 4-bit Cartesian coordinates from octree index w/ division level 4
     */
    static int indexToXYZ4(final int i4)
    {
        //coordinate values are 4 bits each: xxxx, yyyy, zzzz
        //voxel coordinates are interleaved: zyx zyx zyx zyx
        
        final int j = i4 >> 2;
        final int k = i4 >> 4;
        final int l = i4 >> 6;
         
        return ((i4 & 1) | (j & 2) | (k & 4) | (l & 8)) 
               | (((i4 & 2) | (j & 4) | (k & 8) | (l & 16)) << 3)
               | (((i4 & 4) | (j & 8) | (k & 16) | (l & 32)) << 6);
    }
    
    /**
     * Packed 4-bit Cartesian coordinates
     */
    static int packedXYZ4(int x, int y, int z)
    {
        return x | (y << 4) | (z << 8);
    }
}
