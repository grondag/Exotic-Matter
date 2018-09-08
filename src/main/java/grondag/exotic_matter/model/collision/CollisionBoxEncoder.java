package grondag.exotic_matter.model.collision;

import net.minecraft.util.EnumFacing;

/**
 * Static methods for encoding and decoding 1/8 ^ 3 AABB within a unit
 * cube into/from primitive values.  These are used to reduce garbage and improve LOR in implementation.
 */
public class CollisionBoxEncoder
{
    @SuppressWarnings("unused")
    private static final int MIN_X_SHIFT = 0;
    private static final int MIN_Y_SHIFT = 4;
    private static final int MIN_Z_SHIFT = 8;
    private static final int MAX_X_SHIFT = 12;
    private static final int MAX_Y_SHIFT = 16;
    private static final int MAX_Z_SHIFT = 20;

    @SuppressWarnings("unused")
    private static final int AXIS_SHIFT = 0;
    private static final int DEPTH_SHIFT = 4;
    private static final int MIN_A_SHIFT = 8;
    private static final int MIN_B_SHIFT = 12;
    private static final int MAX_A_SHIFT = 16;
    private static final int MAX_B_SHIFT = 20;
    
    /**
     * Encodes an AABB within a unit cube sliced into eights on each axis.
     * Values must be 0-8.  Values do not need to be sorted but cannot be equal.
     */
    static int boxKey(int x0, int y0, int z0, int x1, int y1, int z1)
    {
        int swap;
        
        if(x1 < x0)
        {
            swap = x0;
            x0 = x1;
            x1 = swap;
        }   
        
        if(y1 < y0)
        {
            swap = y0;
            y0 = y1;
            y1 = swap;
        }
        
        if(z1 < z0)
        {
            swap = z0;
            z0 = z1;
            z1 = swap;
        }
        
        return boxKeySorted(x0, y0, z0, x1, y1, z1);
    }
    
    /**
     * Encodes an AABB within a unit cube sliced into eights on each axis.
     * Values must be 0-8.  This version requires that min & max be pre-sorted on each axis.
     * If you don't have pre-sorted values, use {@link #boxKey(int, int, int, int, int, int)}.
     */
    static int boxKeySorted(int minX, int minY, int minZ, int maxX, int maxY, int maxZ)
    {
        return minX | (minY << MIN_Y_SHIFT) | (minZ << MIN_Z_SHIFT)
                | (maxX << MAX_X_SHIFT) | (maxY << MAX_Y_SHIFT) | (maxZ << MAX_Z_SHIFT);
    }
    
    static int minX(int boxKey)
    {
        return boxKey & 0xF;
    }
    
    static int maxX(int boxKey)
    {
        return (boxKey >> MAX_X_SHIFT) & 0xF;
    }
    
    static int minY(int boxKey)
    {
        return (boxKey >> MIN_Y_SHIFT) & 0xF;
    }
    
    static int maxY(int boxKey)
    {
        return (boxKey >> MAX_Y_SHIFT) & 0xF;
    }
    
    static int minZ(int boxKey)
    {
        return (boxKey >> MIN_Z_SHIFT) & 0xF;
    }
    
    static int maxZ(int boxKey)
    {
        return (boxKey >> MAX_Z_SHIFT) & 0xF;
    }
    
    static int boxVolume(int boxKey)
    {
        return(   (maxX(boxKey) - minX(boxKey))
                * (maxY(boxKey) - minY(boxKey))
                * (maxZ(boxKey) - minZ(boxKey)));
    }
    
    /**
     * Returns box key representing combined AABB of both keys.
     * Intended for use when both boxes share a face.
     */
    static int combineBoxes(int boxKey0, int boxKey1)
    {
        return boxKeySorted(
                Math.min(minX(boxKey0), minX(boxKey1)),
                Math.min(minY(boxKey0), minY(boxKey1)),
                Math.min(minZ(boxKey0), minZ(boxKey1)),
                Math.max(maxX(boxKey0), maxX(boxKey1)),
                Math.max(maxY(boxKey0), maxY(boxKey1)),
                Math.max(maxZ(boxKey0), maxZ(boxKey1)));
    }
    
    /**
     * Key components (LSB to MSB) are axis, plane depth (on-axis coordinate), 
     * planar min a, b and planar max a, b.<p>
     * 
     * There is no "front-back" - coplanar bounds should be equal if 2d min/max equal.
     */
    private static int faceKey(EnumFacing face, int depth, int minA, int minB, int maxA, int maxB)
    {
        return face.getAxis().ordinal() | (depth << DEPTH_SHIFT)
                | (minA << MIN_A_SHIFT) | (minB << MIN_B_SHIFT) 
                | (maxA << MAX_A_SHIFT) | (maxB << MAX_B_SHIFT);
    }
    
    static int faceKey(EnumFacing face, int boxKey)
    {
        switch(face)
        {
        case UP:
            return faceKey(face, maxY(boxKey), minX(boxKey), minZ(boxKey), maxX(boxKey), maxZ(boxKey));
            
        case DOWN:
            return faceKey(face, minY(boxKey), minX(boxKey), minZ(boxKey), maxX(boxKey), maxZ(boxKey));
            
        case EAST:
            return faceKey(face, maxX(boxKey), minY(boxKey), minZ(boxKey), maxY(boxKey), maxZ(boxKey));
            
        case WEST:
            return faceKey(face, minX(boxKey), minY(boxKey), minZ(boxKey), maxY(boxKey), maxZ(boxKey));
            
        case SOUTH:
            return faceKey(face, maxZ(boxKey), minX(boxKey), minY(boxKey), maxX(boxKey), maxY(boxKey));

        case NORTH:
            return faceKey(face, minZ(boxKey), minX(boxKey), minY(boxKey), maxX(boxKey), maxY(boxKey));
        }
        
        throw new IndexOutOfBoundsException();
    }
}
