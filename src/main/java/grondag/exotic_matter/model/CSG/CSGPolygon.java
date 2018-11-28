package grondag.exotic_matter.model.CSG;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import grondag.exotic_matter.model.primitives.polygon.IMutablePolygon;
import grondag.exotic_matter.model.primitives.polygon.IPolygon;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;

// PERF: reuse instances
public class CSGPolygon
{
    static private AtomicInteger nextID = new AtomicInteger(1);
    
    private final IMutablePolygon poly;
    private int originID = nextID.getAndIncrement();
    private boolean isInverted;
    
    /**
     * Includes vertices from original.
     */
    public CSGPolygon(IPolygon original)
    {
        this.poly = original.claimCopy();
    }
    
    /**
     * Copies original reference and current CSG state (faceNormal, inverted) but no vertices.
     */
    public CSGPolygon(CSGPolygon poly, int vCount)
    {
        this.originID = poly.originID;
        this.poly = poly.poly.claimCopy(vCount);
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
        IMutablePolygon result = this.poly.claimCopy();
        
        if(this.isInverted)
            result.flip();
        
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
        return this.poly.vertexCount();
    }
    
    public Vec3f getPos(int vertexIndex)
    {
        return this.poly.getPos(vertexIndex);
    }
    
    public void copyVertexFrom(int targetIndex, CSGPolygon source, int sourceIndex)
    {
        this.poly.copyVertexFrom(targetIndex, source.poly, sourceIndex);
    }
    
    float getVertexX(int vertexIndex)
    {
        return this.poly.getVertexX(vertexIndex);
    }
    
    float getVertexY(int vertexIndex)
    {
        return this.poly.getVertexY(vertexIndex);
    }
    
    float getVertexZ(int vertexIndex)
    {
        return this.poly.getVertexZ(vertexIndex);
    }
    
    public void copyInterpolatedVertexFrom(int targetIndex, CSGPolygon from, int fromIndex, CSGPolygon to, int toIndex, float toWeight)
    {
        this.poly.copyInterpolatedVertexFrom(targetIndex, from.poly, fromIndex, to.poly, toIndex, toWeight);
    }
    
    public int originID()
    {
        return this.originID;
    }
}
