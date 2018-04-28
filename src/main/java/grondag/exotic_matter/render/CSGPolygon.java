package grondag.exotic_matter.render;

import java.util.Arrays;
import java.util.List;

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
        
        PolyImpl result = new PolyImpl(this.original, this.vertex.length);
        
        final int vCount = this.vertex.length;
        if(this.isInverted)
        {
            if(result.faceNormal != null) result.faceNormal = result.faceNormal.inverse();
            
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
}
