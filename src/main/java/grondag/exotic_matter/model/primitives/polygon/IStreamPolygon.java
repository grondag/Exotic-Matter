package grondag.exotic_matter.model.primitives.polygon;

/**
 * Subset of IPolygon that applies only to polys that are part of a polystream.
 * These operations all involve metadata, and therefore 
 */
public interface IStreamPolygon
{
    public static final int NO_ADDRESS = -1;
    
    public default int streamAddress()
    {
        return NO_ADDRESS;
    }
    
    default boolean isMarked()
    {
        return false;
    }
    
    default void flipMark()
    {
        this.setMark(!this.isMarked());
    }
    
    default void setMark(boolean isMarked)
    {
        throw new UnsupportedOperationException();
    }
    
    default boolean isDeleted()
    {
        return false;
    }
    
    default void setDeleted()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Improbable non-zero value that signifies no link set or link not supported.
     */
    public static final int NO_LINK_OR_TAG = Integer.MIN_VALUE;
    
    default int getTag()
    {
        return NO_LINK_OR_TAG;
    }
    
    default void setTag(int tag)
    {
        throw new UnsupportedOperationException();
    }
    
    
    default int getLink()
    {
        return NO_LINK_OR_TAG;
    }
    
    default void setLink(int link)
    {
        throw new UnsupportedOperationException();
    }    
}
