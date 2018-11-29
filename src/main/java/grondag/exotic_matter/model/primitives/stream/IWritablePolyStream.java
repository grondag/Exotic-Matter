package grondag.exotic_matter.model.primitives.stream;

import grondag.exotic_matter.model.primitives.polygon.IMutablePolygon;

/**
 * Stream that allows appending to end in wip area but
 * is immutable for polygons already created.
 */
public interface IWritablePolyStream extends IPolyStream
{
    /**
     * Holds WIP poly data that will be appended by next call to {@link #append()}.
     * Is reset to defaults when append is called. <p>
     * 
     * DO NOT HOLD A NON-LOCAL REFERENCE TO THIS.
     */
    IMutablePolygon writer();
    
    /**
     * Address that will be used for next appended polygon when append is called.<br>
     * Cannot be used with move... methods until writer is appended.
     */
    int writerAddress();
    
    /**
     * Appends WIP as new poly and resets WIP to default values.
     * Increases size of stream by 1.
     */
    void append();
    
    /**
     * Current poly settings will be used to initialize WIP after append.
     */
    void saveDefaults();
    
    /**
     * Undoes effects of {@link #saveDefaults()} so that defaults are for a new poly stream.
     */
    void clearDefaults();
    
    /**
     * Loads default values into WIP.
     */
    void loadDefaults();
    
    /**
     * Loads WIP with poly at given stream.
     */
    void copyFromAddress(int address);
  
    
    /**
     * Claims a read-only copy of this stream, leaving this writer and stream intact.
     *  New stream will not reflect ongoing changes.
     */
    IPolyStream cloneToReader();
    
    
    /**
     * Locks this stream for changes and returns self as reader reference.
     * Does NOT release this stream because it is still intact but operating as a reader.
     * Any calls to write operations will fail after this is called.
     * Recommended to cast reference to IPolyStreamReader after using.
     */
    IPolyStream convertToReader();

    /**
     * Sets vertex count for current writer. Value can be saved as part of defaults. 
     */
    void setVertexCount(int vertexCount);
    
    /**
     * Sets layer count for current writer. Value can be saved as part of defaults. 
     */
    void setLayerCount(int layerCount);
    
}
