package grondag.exotic_matter.model.CSG;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import grondag.exotic_matter.model.primitives.polygon.IMutablePolygon;
import grondag.exotic_matter.model.primitives.polygon.IPolygon;
import grondag.exotic_matter.model.primitives.vertex.IMutableVertex;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;

// PERF: reuse instances
public class CSGPolygon
{
    static private AtomicInteger nextID = new AtomicInteger(1);
    
    private final IMutableVertex[] vertex;
    private final IPolygon poly;
    private int originID = nextID.getAndIncrement();
    
    private boolean isInverted;
    
    /**
     * Copies vertices from original.
     */
    public CSGPolygon(IPolygon original)
    {
        this.vertex = new IMutableVertex[original.vertexCount()];
        this.poly = original;
        original.claimVertexCopiesToArray(vertex);
    }
    
    /**
     * Copies original reference and current CSG state (faceNormal, inverted) but no vertices.
     */
    public CSGPolygon(CSGPolygon poly, int vCount)
    {
        this.vertex = new IMutableVertex[vCount];
        this.originID = poly.originID;
        this.poly = poly.poly;
        this.isInverted = poly.isInverted;
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
        this.poly = poly.poly;
        this.originID = poly.originID;
        this.isInverted = poly.isInverted;     
    }
    
    public void flipCSGInverted()
    {
        this.isInverted = !this.isInverted;
    }

    public boolean isCSGInverted()
    {
        return this.isInverted;
    }
    
    /**
     * Does NOT retain any references to our vertices.
     */
    public void addPaintableQuadsToList(List<IMutablePolygon> list)
    {
        IMutablePolygon result = this.applyInverted();
        if(result.toPaintableQuads(list))
            result.release();
    }

    private IMutablePolygon applyInverted()
    {
        IMutablePolygon result = this.poly.claimCopy(this.vertex.length);
        
        final int vCount = this.vertex.length;
        if(this.isInverted)
        {
            result.invertFaceNormal();
            
            //reverse vertex winding order and flip vertex normals
            for(int i = 0, j = vCount - 1; i < vCount; i++, j--)
            {
                result.copyVertexFrom(i, this.vertex[j].flip());
            }
        }
        else
        {
            for(int i = 0; i < vCount; i++)
            {
                result.copyVertexFrom(i, this.vertex[i]);
            }
        }
        
        return result;
    }
    
    public static final int VERTEX_NOT_FOUND = -1;
    
    /**
     * Will return {@link #VERTEX_NOT_FOUND} (-1) if vertex is not part of this polygon.
     */
    public int indexForVertex(Vec3f v)
    {
        final int limit = this.vertexCount();
        for(int i = 0; i < limit; i++)
        {
            if(v.equals(this.getPos(i)))
                return i;
        }
        return VERTEX_NOT_FOUND;
    }

    public Vec3f getFaceNormal()
    {
        return poly.getFaceNormal();
    }
    
    public int vertexCount()
    {
        return this.vertex.length;
    }
    
    public Vec3f getPos(int vertexIndex)
    {
        return vertex[vertexIndex].pos();
    }
    
    public void copyVertexFrom(int targetIndex, CSGPolygon source, int sourceIndex)
    {
        this.vertex[targetIndex] = source.vertex[sourceIndex];
    }
    
    float getVertexX(int vertexIndex)
    {
        return vertex[vertexIndex].x();
    }
    
    float getVertexY(int vertexIndex)
    {
        return vertex[vertexIndex].y();
    }
    
    float getVertexZ(int vertexIndex)
    {
        return vertex[vertexIndex].z();
    }
    
    public void copyInterpolatedVertexFrom(int targetIndex, CSGPolygon from, int fromIndex, CSGPolygon to, int toIndex, float toWeight)
    {
        vertex[targetIndex] = from.vertex[fromIndex].interpolate(to.vertex[toIndex], toWeight);
    }
    
    public int originID()
    {
        return this.originID;
    }
}
