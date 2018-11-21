package grondag.exotic_matter.model.CSG;

import java.util.List;

import grondag.exotic_matter.model.primitives.better.IMutableGeometricVertex;
import grondag.exotic_matter.model.primitives.better.IMutablePoly;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;

// PERF: reuse instances
public class CSGPolygon
{
    public final Vec3f faceNormal;
    public final IMutableGeometricVertex[] vertex;
    public final IMutablePoly original;

    public boolean isInverted;
    
    /**
     * Copies vertices from original.
     */
    public CSGPolygon(IMutablePoly original)
    {
        this(original, original.vertexCount());
        original.claimVertexCopiesToArray(vertex);
    }
    
    /**
     * Copies original reference and current CSG state (faceNormal, inverted) but no vertices.
     */
    public CSGPolygon(CSGPolygon poly, int vCount)
    {
        this.vertex = new IMutableGeometricVertex[vCount];
        this.original = poly.original;
        this.faceNormal = poly.faceNormal;
        this.isInverted = poly.isInverted;
    }
    
    /**
     * Copies original reference and original faceNormal but no vertices.
     */
    private CSGPolygon(IMutablePoly original, final int vCount)
    {
        this.vertex = new IMutableGeometricVertex[vCount];
        this.original = original;
        this.faceNormal = original.getFaceNormal();
    }
    
    /** DO NOT USE */
    @Override
    public CSGPolygon clone()
    {
        throw new UnsupportedOperationException();
    }
    
    public void flip()
    {
        this.isInverted = !this.isInverted;
    }

    /**
     * Does NOT retain any references to our vertices.
     */
    public void addRenderableQuads(List<IMutablePoly> list)
    {
        this.applyInverted().claimCopy().addQuadsToList(list);
    }

    private IMutablePoly applyInverted()
    {
        IMutablePoly result = this.original.claimCopy(this.vertex.length);
        
        final int vCount = this.vertex.length;
        if(this.isInverted)
        {
            result.invertFaceNormal();
            
            //reverse vertex winding order and flip vertex normals
            for(int i = 0, j = vCount - 1; i < vCount; i++, j--)
            {
                result.copyVertex(i, this.vertex[j].flip());
            }
        }
        else
        {
            for(int i = 0; i < vCount; i++)
            {
                result.copyVertex(i, this.vertex[i]);
            }
        }
        
        return result;
    }
    
    public static final int VERTEX_NOT_FOUND = -1;
    
    /**
     * Will return {@link #VERTEX_NOT_FOUND} (-1) if vertex is not part of this polygon.
     */
    public int indexForVertex(IMutableGeometricVertex v)
    {
        for(int i = 0; i < this.vertex.length; i++)
        {
            if(this.vertex[i].pos() == v) return i;
        }
        return VERTEX_NOT_FOUND;
    }
}
