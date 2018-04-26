package grondag.exotic_matter.render;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CSGPolygon
{
    
    /** special edge ID signifying is an original edge, not a split */
    public static final int IS_AN_ANCESTOR = -1;
    
    /** special edge ID signifying no ID has been set */
    public static final int NO_ID = 0;

    public static final int LINE_NOT_FOUND = -1;
    private static AtomicInteger nextQuadID = new AtomicInteger(1);

    public final Vec3f faceNormal;
    public final Vertex[] vertex;
    public final IPolygon original;
    public final int[] lineID;
    public final int quadID = nextQuadID.incrementAndGet();

    public boolean isInverted;

    public CSGPolygon(IPolygon original)
    {
        this(original, original.vertexCount());
    }
    
    public CSGPolygon(IPolygon original, final int vCount)
    {
        this.vertex = Arrays.copyOf(original.vertexArray(), vCount);
        this.lineID = new int[vCount];
        this.original = original;
        this.faceNormal = original.getFaceNormal();
        this.setupCsgMetadata();
    }
    
    public CSGPolygon(CSGPolygon poly, int vCount)
    {
        this.vertex = new Vertex[vCount];
        this.lineID = new int[vCount];
        this.original = poly.original;
        this.faceNormal = poly.faceNormal;
        this.isInverted = poly.isInverted;
    }

    private void setupCsgMetadata()
    {
        for(int i = 0; i < this.vertex.length; i++)
        {
            this.lineID[i] = CSGPlane.nextOutsideLineID.getAndDecrement();
        }
    }
    
    public void flip()
    {
        this.isInverted = !this.isInverted;
    }


      /**
      * Returns the index of the edge with the given line ID.
      * Returns LINE_NOT_FOUND if it wasn't found.
      */
     public int findLineIndex(long lineID)
     {
         for(int i = 0; i < this.vertex.length; i++)
         {
             if(this.lineID[i] == lineID)
             {
                 return i;
             }
         }
         return LINE_NOT_FOUND;
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
