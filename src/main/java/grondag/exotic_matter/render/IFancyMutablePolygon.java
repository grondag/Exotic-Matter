package grondag.exotic_matter.render;

import net.minecraft.util.math.Vec3d;

public interface IFancyMutablePolygon extends IFancyPolygon, IMutablePolygon
{

    /** Using this instead of method on vertex 
     * ensures normals are set correctly for tris.
     */
    void setVertexNormal(int index, float x, float y, float z);

    void setVertexNormal(int index, Vec3d normal);
    
    public void setVertexColor(int index, int vColor);
}
