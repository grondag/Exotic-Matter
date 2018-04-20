package grondag.exotic_matter.render;


public interface IFancyMutablePolygon extends IFancyPolygon, IMutablePolygon
{

    /** Using this instead of method on vertex 
     * ensures normals are set correctly for tris.
     */
    void setVertexNormal(int index, float x, float y, float z);

    void setVertexNormal(int index, Vec3f normal);
    
    public void setVertexColor(int index, int vColor);
}
