package grondag.exotic_matter.model.primitives.polygon;

/**
 * Provides access to pre-computed bounds info for optimized CSG operations.
 *
 */
public interface ICSGPolygon extends IStreamPolygon
{
    /**
     * True if this the ICSGPolygon interface is implemented.
     */
    default public boolean isCSG()
    {
        return false;
    }
    
    /**
     * Pre-computed AABB data. For optimized CSG operations.<br>
     * Will fail if {@link #isCSG()} is false.
     */
    default public float csgMinX()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Pre-computed AABB data. For optimized CSG operations.<br>
     * Will fail if {@link #isCSG()} is false.
     */
    default public float csgMinY()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Pre-computed AABB data. For optimized CSG operations.<br>
     * Will fail if {@link #isCSG()} is false.
     */
    default public float csgMinZ()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Pre-computed AABB data. For optimized CSG operations.<br>
     * Will fail if {@link #isCSG()} is false.
     */
    default public float csgMaxX()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Pre-computed AABB data. For optimized CSG operations.<br>
     * Will fail if {@link #isCSG()} is false.
     */
    default public float csgMaxY()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Pre-computed AABB data. For optimized CSG operations.<br>
     * Will fail if {@link #isCSG()} is false.
     */
    default public float csgMaxZ()
    {
        throw new UnsupportedOperationException();
    }
}
