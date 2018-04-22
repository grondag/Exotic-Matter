package grondag.exotic_matter.render;

/**
 * Polygon tagged with CSG metadata for use in CSG operations.
 * Also supports mutating CSG operations.
 *
 */
public interface ICSGPolygon extends IPolygon
{
    /** special edge ID signifying is an original edge, not a split */
    public static final int IS_AN_ANCESTOR = -1;
    
    /** special edge ID signifying no ID has been set */
    public static final int NO_ID = 0;

    public static final int LINE_NOT_FOUND = -1;
    
    @Override
    public default boolean isCSG()
    {
        return true;
    }
    
    @Override
    public default ICSGPolygon csgReference()
    {
        return this;
    }
    
    public int quadID();
    
    public void setLineID(int index, int lineID);
    
    public int getLineID(int index);
    
    public void setAncestorQuadID(int ancestorQuadID);
    
    public int getAncestorQuadID();

    public int getAncestorQuadIDForDescendant();
    
    public boolean isInverted();

    public void flip();
    
    /**
     * If this polygon is inverted, returns a copy of with a reversed winding order 
     * and inverted face normal. Vertex normals are also flipped if present. The copy
     * is not marked as inverted.<p>
     * 
     * If this polygon is not inverted, returns self.<p>
     * 
     * Intended for use at end of CSG operations, to make polygons renderable.  CSG 
     * operations can simply use inverted indicator but rendering requires 
     * correct winding order and normals.<p>
     * 
     * Should be called <em>after</em> rejoining coplanar splits because wipes out edge metadata.
     */
    public IPolygon applyInverted();
    
    
    /**
     * Returns the index of the edge with the given line ID.
     * Returns LINE_NOT_FOUND if, uh, you know, it wasn't checked.
     */
    public default int findLineIndex(long lineID)
    {
        for(int i = 0; i < this.vertexCount(); i++)
        {
            if(this.getLineID(i) == lineID)
            {
                return i;
            }
        }
        return LINE_NOT_FOUND;
    }
}
