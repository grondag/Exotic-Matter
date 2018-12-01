package grondag.exotic_matter.model.primitives.stream;

import grondag.exotic_matter.model.primitives.polygon.IPolygon;

public interface IPolyStream
{
    /**
     * Number of polys in stream.  Does not include unappended WIP.
     */
    int size();
    
    default boolean isEmpty()
    {
        return size() == 0;
    }
    
    /**
     * Reference to poly at current read address.<br>
     * When stream first created will point to the first poly in the stream.<br>
     * Returns null if at end or first poly has not been written.
     */
    IPolygon reader();
    
    /**
     * Moves reader to start of stream.<br>
     * Note that WIP is not considered part of stream.
     */
    IPolyStream origin();
    
    /**
     * Moves reader to next poly in this stream.  Returns false if already at end.<br>
     * Note that WIP is not considered part of stream.
     */
    boolean next();
    
    /**
     * False if reader at end of stream in build order. <br>
     * By extension, also false if stream is empty.<br>
     * Note that WIP is not considered part of stream. 
     */
    boolean hasValue();
    
    /**
     * Address of current reader location. Can be used with {@link #moveTo(int)} to return.
     * Addresses are immutable within this stream instance.<p>
     */
    int getAddress();
    
    /**
     * Moves reader to location specified by address.  Address should be from {@link #getAddress()}.<p>
     * 
     * Will throw exception if address points to WIP on streams still being written.
     */
    void moveTo(int address);
    
    void release();
    
    
    /**
     * Virtual read-only reference to an existing poly in this stream.
     * Use for interpolation and other poly-poly operations.
     * Does not affect and not affected by read/write/update cursors.<p>
     * 
     * DO NOT STORE A REFERENCE.  Meant only for use in a local operation.
     * Calls  to {@link #movePolyA(int)} will produce new values.
     */
    IPolygon polyA();
    
    /**
     * Sets address for {@link #polyA()} and returns same as convenience.
     */
    IPolygon movePolyA(int address);
    
    /**
     * Secondary instance of {@link #polyA()}. For interpolation.
     */
    IPolygon polyB();
    
    /**
     * Secondary instance of {@link #movePolyA(int)}.  For interpolation.
     */
    IPolygon movePolyB(int address);
    
    /**
     * Claims a writable copy of this stream, leaving this stream intact.
     *  New stream has no connection with this stream.
     */
    IWritablePolyStream cloneToWritable();
}
