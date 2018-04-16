package grondag.exotic_matter.render;

import java.util.concurrent.ThreadLocalRandom;

public interface IPolygonList extends Iterable<IPolygon>
{
    /**
     * Copies number of vertices and quad-level format. Does not copy vertices.
     */
    public IMutablePolygon mutable(IPolygon template);
    
    /**
     * Copies poly-level attributes and allocates the given number of vertices. Does not copy vertices.
     */
    public IMutablePolygon mutable(IPolygon template, int vertexCount);
    
    /**
     * Creates new, blank mutable poly with the given number of vertices. 
     */
    public IMutablePolygon mutable(int vertexCount);
    
    /**
     * Copies poly-level attributes AND copies vertices.
     */
    public IMutablePolygon mutableCopyOf(IPolygon template);

    public int size();

    public boolean isEmpty();
    
    /**
     * Randomly recolors all the quads as an aid to debugging.
     * Will fail if quads in this list are not mutable.
     */
    public default void recolor()
    {
        this.forEach((IPolygon quad) -> quad.mutableReference().replaceColor((ThreadLocalRandom.current().nextInt(0x1000000) & 0xFFFFFF) | 0xFF000000));
    }
}
