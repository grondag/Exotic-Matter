package grondag.exotic_matter.model.primitives.stream;

import javax.annotation.Nullable;

import grondag.exotic_matter.model.primitives.polygon.IMutablePolygon;

/**
 * Polystream that allows deletion and partial mutation of polygons already written.  <p>
 *
 *Limitiations
 *<li>Deleted polygons continue to consume RAM.
 *<li>Cannot change vertex count.
 */
public interface IMutablePolyStream
{
    /**
     * Reference to poly at current edit address.
     * When stream first created will point to the first poly in the stream.
     * May NOT point to WIP area.  (Mutable streams are still appendable.)
     */
    @Nullable IMutablePolygon editor();
    
    /**
     * Moves editor to start of stream.<br>
     * Note that WIP is not considered part of stream.
     */
    void editorOrigin();
    
    /**
     * Moves editor to next poly in this stream.  Returns false if already at end.<br>
     * Note that WIP is not considered part of stream.
     */
    boolean editorNext();
    
    /**
     * False if editor at end of stream in build order. <br>
     * By extension, also false if stream is empty.<br>
     * Note that WIP is not considered part of stream. 
     */
    boolean editorHasValue();
    
    /**
     * Address of current editor location. Can be used with {@link #editorMoveTo(int)} to return.
     * Addresses are immutable within this stream instance.<p>
     */
    int editorGetAddress();
    
    /**
     * Moves editor to location specified by address.  Address should be from {@link #getAddress()}.<p>
     * 
     * Will throw exception if address points to WIP on streams still being written.
     */
    void editorMoveTo(int address);
    
    /**
     * Removes polygon at given address.  Will remove it from link chain if part of one.
     * Editor position will be as if {@link #editorNext()} had been called.
     */
    void editorDelete();
    
    /**
     * Removes polygon at given address.  Will remove it from link chain if part of one.
     */
    void delete(int address);
}
