package grondag.exotic_matter.model.primitives.polygon;

/**
 * Provides access to pre-computed bounds info for optimized CSG operations.
 *
 */
public interface ICSGPolygon
{
    /**
     * Will fail if {@link #hasPrecomputedBounds()} is false.
     */
    default public float planeDistance()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Will fail if {@link #hasPrecomputedBounds()} is false.
     */
    default public float boundsMinX()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Will fail if {@link #hasPrecomputedBounds()} is false.
     */
    default public float boundsMinY()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Will fail if {@link #hasPrecomputedBounds()} is false.
     */
    default public float boundsMinZ()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Will fail if {@link #hasPrecomputedBounds()} is false.
     */
    default public float boundsMaxX()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Will fail if {@link #hasPrecomputedBounds()} is false.
     */
    default public float boundsMaxY()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Will fail if {@link #hasPrecomputedBounds()} is false.
     */
    default public float boundsMaxZ()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * True if this the ICSGPolygon interface is implemented.
     */
    default public boolean hasPrecomputedBounds()
    {
        return false;
    }
   
}
