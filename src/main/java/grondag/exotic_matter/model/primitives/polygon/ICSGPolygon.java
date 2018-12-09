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
     * Distance to plane, used for CSG splits.<br>
     * Will fail if {@link #isCSG()} is false.
     */
    default public float csgPlaneDistance()
    {
        throw new UnsupportedOperationException();
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
    
    /**
     * Address of the poly that is the first on this node,
     * and which contains front/back metadata.
     * Will fail if {@link #isCSG()} is false.
     */
    default public int getCsgHeadAddress()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * See {@link #getCsgHeadAddress()}
     */
    default public void setCsgHeadAddress(int address)
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * True if this poly is the first on this node,
     * and thus contains front/back metadata.
     * Will fail if {@link #isCSG()} is false.
     */
    default public boolean isCsgHead()
    {
        return getCsgHeadAddress() == this.streamAddress();
    }
    
    /**
     * Address of the head poly that is in front (BSP-wise) of this node.<br>
     * Will be {@link IStreamPolygon#NO_ADDRESS} if no polys are in front.<br>
     * Only populated for the "head" poly.<br>
     * Will fail if {@link #isCSG()} is false.
     */
    default public int getCsgFrontAddress()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * See {@link #getCsgFrontAddress()}
     */
    default public void setCsgFrontAddress(int address)
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Address of the head poly that is in back (BSP-wise) of this node.<br>
     * Will be {@link IStreamPolygon#NO_ADDRESS} if no polys are in back.<br>
     * Only populated for the "head" poly.<br>
     * Will fail if {@link #isCSG()} is false.
     */
    default public int getCsgBackAddress()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * See {@link #getCsgBackAddress()}
     */
    default public void setCsgBackAddress(int address)
    {
        throw new UnsupportedOperationException();
    }
}
