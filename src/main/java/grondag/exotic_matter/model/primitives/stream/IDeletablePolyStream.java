package grondag.exotic_matter.model.primitives.stream;

/**
 * Polys in the stream can be "deleted", which means
 * they will be skipped by next and nextLink operations.
 * However they still remain in the stream.
 */
public interface IDeletablePolyStream
{
    /**
     * True if poly at reader position is deleted.
     */
    boolean isDeleted();
    
    /**
     * Marks the poly at reader position for deletion.<br>
     * See {@link #setDeleted(int)}
     */
    void setDeleted();
    
    /**
     * True if poly at given address is deleted.
     */
    boolean isDeleted(int address);
    
    /**
     * Marks poly at given address as deleted.
     * Poly remains in collection but will be
     * skipped in next... methods.  Streams
     * that implement this will typically also
     * have IDeletable polygons that will reflect this change.
     */
    void setDeleted(int address);
}
