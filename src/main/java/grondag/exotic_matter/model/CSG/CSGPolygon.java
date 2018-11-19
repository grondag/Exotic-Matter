package grondag.exotic_matter.model.CSG;

import java.util.Arrays;
import java.util.List;

import grondag.exotic_matter.model.primitives.IMutablePolygon;
import grondag.exotic_matter.model.primitives.IPolygon;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import grondag.exotic_matter.model.primitives.vertex.Vertex;

public class CSGPolygon
{
    public final Vec3f faceNormal;
    public final Vertex[] vertex;
    public final IPolygon original;

    public boolean isInverted;
    
    public CSGPolygon(IPolygon original)
    {
        this(original, original.vertexCount());
    }
    
    public CSGPolygon(IPolygon original, final int vCount)
    {
        this.vertex = Arrays.copyOf(original.vertexArray(), vCount);
        this.original = original;
        this.faceNormal = original.getFaceNormal();
    }
    
    @Override
    public CSGPolygon clone()
    {
        return new CSGPolygon(this);
    }
    
    /** specifically for clone */
    private CSGPolygon(CSGPolygon poly)
    {
        this.vertex = Arrays.copyOf(poly.vertex, poly.vertex.length);
        this.original = poly.original;
        this.faceNormal = poly.faceNormal;
        this.isInverted = poly.isInverted;
    }
    
    public CSGPolygon(CSGPolygon poly, int vCount)
    {
        this.vertex = new Vertex[vCount];
        this.original = poly.original;
        this.faceNormal = poly.faceNormal;
        this.isInverted = poly.isInverted;
    }
    
    public void flip()
    {
        this.isInverted = !this.isInverted;
    }

    public void addRenderableQuads(List<IPolygon> list)
    {
        this.applyInverted().addQuadsToList(list, true);
    }

    public IPolygon applyInverted()
    {
        IMutablePolygon result = this.original.mutableCopy(this.vertex.length);
        
        final int vCount = this.vertex.length;
        if(this.isInverted)
        {
            result.invertFaceNormal();
            
            //reverse vertex winding order and flip vertex normals
            for(int i = 0, j = vCount - 1; i < vCount; i++, j--)
            {
                result.addVertex(i, this.vertex[j].flipped());
            }
        }
        else
        {
            for(int i = 0; i < vCount; i++)
            {
                result.addVertex(i, this.vertex[i]);
            }
        }
        
        return result;
    }
    
    public static final int VERTEX_NOT_FOUND = -1;
    
    /**
     * Will return {@link #VERTEX_NOT_FOUND} (-1) if vertex is not part of this polygon.
     */
    public int indexForVertex(Vertex v)
    {
        for(int i = 0; i < this.vertex.length; i++)
        {
            if(this.vertex[i] == v) return i;
        }
        return VERTEX_NOT_FOUND;
    }
}
