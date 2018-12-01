package grondag.exotic_matter.model.primitives.stream;

public interface ILinkedPolyStream
{
    /**
     * Sets link for this poly at the current reader.  Used to create sublists within a stream.<p>
     * 
     * This, along with {@link #setMark(boolean)} are the only 
     * mutating options allowed on a stream reader.<p>
     * 
     * Not meant for concurrent use.
     */
    void setLink(int linkAddress);
    
    /**
     * Sets link for the poly at the target address.  See {@link #setLink(int)}
     */
    void setLink(int targetAddress, int linkAddress);

    /**
     * True if reader at end of link chain or not in a link chain.
     */
    boolean hasLink();
    
    /**
     * Clears any link set by {@link #setLink(int)}
     */
    void clearLink();
    
    /**
     * Moves reader to next linked poly, if a link exists.  Returns false if already at end of link chain.
     */
    boolean nextLink();
}
