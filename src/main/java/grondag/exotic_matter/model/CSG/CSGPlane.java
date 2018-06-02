package grondag.exotic_matter.model.CSG;

/**
* Portions reproduced or adapted from JCSG.
* Copyright 2014-2014 Michael Hoffer <info@michaelhoffer.de>. All rights
* reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* 1. Redistributions of source code must retain the above copyright notice,
* this list of conditions and the following disclaimer.
*
* 2. Redistributions in binary form must reproduce the above copyright notice,
* this list of conditions and the following disclaimer in the documentation
* and/or other materials provided with the distribution.
*
* THIS SOFTWARE IS PROVIDED BY Michael Hoffer <info@michaelhoffer.de> "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL Michael Hoffer <info@michaelhoffer.de> OR
* CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
* EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
* PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
* OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
* WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
* OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
* ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*
* The views and conclusions contained in the software and documentation are
* those of the authors and should not be interpreted as representing official
* policies, either expressed or implied, of Michael Hoffer
* <info@michaelhoffer.de>.
*/


import java.awt.Polygon;

import grondag.exotic_matter.model.primitives.QuadHelper;
import grondag.exotic_matter.model.primitives.Vec3f;
import grondag.exotic_matter.model.primitives.Vertex;

public class CSGPlane
{
    
    private float normalX;
    private float normalY;
    private float normalZ;
    
    /**
     * Distance to origin.
     */
    public float dist;

    /**
     * Constructor. Creates a new plane defined by its normal vector and the
     * distance to the origin.
     *
     * @param normal plane normal
     * @param dist distance from origin
     */
    public CSGPlane(Vec3f normal, float dist) {
        this.normalX = normal.x;
        this.normalY = normal.y;
        this.normalZ = normal.z;
        this.dist = dist;
    }

    public CSGPlane(CSGPolygon quad)
    {
        final Vec3f normal = quad.faceNormal;
        this.normalX = normal.x;
        this.normalY = normal.y;
        this.normalZ = normal.z;
        this.dist = normal.dotProduct(quad.vertex[0]);    
    }
    
    private CSGPlane(float x, float y, float z, float dist) {
        this.normalX = x;
        this.normalY = y;
        this.normalZ = z;
        this.dist = dist;
    }
    
    @Override
    public CSGPlane clone()
    {
        return new CSGPlane(this.normalX, this.normalY, this.normalZ, this.dist);
    }

    /**
     * Flips this plane.
     */
    public void flip()
    {
        this.normalX = -this.normalX;
        this.normalY = -this.normalY;
        this.normalZ = -this.normalZ;
        this.dist = -this.dist;
    }

    private static final int COPLANAR = 0;
    private static final int FRONT = 1;
    private static final int BACK = 2;
    
    private static final int BACK_SHIFT = 8;
    private static final int FRONT_INCREMENT = 1;
    private static final int FRONT_MASK = (1 << BACK_SHIFT) - 1;
    private static final int BACK_INCREMENT = 1 << BACK_SHIFT;
    private static final int BACK_MASK = FRONT_MASK << BACK_SHIFT;
    
    public final int vertexIncrement(Vertex v)
    {
//        incrementTimer.start();
//        final int result = vertexIncrementInner(v);
//        incrementTimer.stop();
//        return result;
//    }
//    private static MicroTimer incrementTimer = new MicroTimer("incrementTestTimer", 20000000);
//    private final int vertexIncrementInner(Vertex v)
//    {
        
        final float t = v.x * this.normalX + v.y * this.normalY + v.z * this.normalZ - this.dist;
        
        if(t <= QuadHelper.EPSILON)
        {
            if(t >= -QuadHelper.EPSILON)
                return COPLANAR;
            else
                return BACK_INCREMENT;
        }
        else
        {
            // t > QuadHelper.EPSILON
            return FRONT_INCREMENT;
        }
    }
    
    private final int vertexType(Vertex v)
    {
        final float t = v.x * this.normalX + v.y * this.normalY + v.z * this.normalZ - this.dist;
        
        if(t >= -QuadHelper.EPSILON)
        {
            if(t <= QuadHelper.EPSILON)
                return COPLANAR;
            else
                return FRONT;
        }
        else
        {
            // t < -QuadHelper.EPSILON
            return BACK;
        }
    }
    
    /**
     * Splits a {@link Polygon} by this plane if needed. After that it puts the
     * polygons or the polygon fragments in the appropriate lists
     * ({@code front}, {@code back}). Coplanar polygons go into either
     * {@code coplanarFront}, {@code coplanarBack} depending on their
     * orientation with respect to this plane. Polygons in front or back of this
     * plane go into either {@code front} or {@code back}.
     *
     * @param poly polygon to split
     * @param coplanarFront "coplanar front" polygons
     * @param coplanarBack "coplanar back" polygons
     * @param front front polygons
     * @param back back polgons
     */
    
    public void splitQuad(CSGPolygon poly, CSGSplitAcceptor target)  
    {
//        tagCounter.addTo(poly.original.getTag(), 1);
//        splitTimer.start();
//        splitQuadInner(poly, target);
//        if(splitTimer.stop())
//        {
//            splitSpanningTimer.reportAndClear();
//        }
//    }
//    public static MicroTimer splitTimer = new MicroTimer("splitQuad", 100000000);
//    public static Object2IntOpenHashMap<String> tagCounter = new Object2IntOpenHashMap<>();
//
//    public static void printTagCounts()
//    {
//        tagCounter.entrySet().stream().sequential().sorted(new Comparator<Entry<String, Integer>>() {
//
//            @Override
//            public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2)
//            {
//                return o2.getValue().compareTo(o1.getValue());
//            }}).forEach((Entry<String, Integer> e) -> System.out.println(e.getKey() + ", " + e.getValue()));
//        tagCounter.clear();
//    }
//    private void splitQuadInner(CSGPolygon poly, CSGSplitAcceptor target) 
//    {
        int combinedCount = 0;
        
        for (Vertex v : poly.vertex)
        {
            combinedCount += this.vertexIncrement(v);
        }
        
        // Put the polygon in the correct list, splitting it when necessary.
        if((combinedCount & FRONT_MASK) == 0)
        {
            if(combinedCount == 0)
            {
                // coplanar
                final Vec3f faceNorm = poly.faceNormal;
                float t = faceNorm.x * this.normalX + faceNorm.y * this.normalY + faceNorm.z * this.normalZ;
                if(poly.isInverted) t = -t;
                
                if(t > 0) 
                        target.acceptCoplanarFront(poly);
                else 
                        target.acceptCoplanarBack(poly);
            }
            else target.acceptBack(poly);
        }
        else
        {
            // frontcount > 0
            if((combinedCount & BACK_MASK) == 0)
                target.acceptFront(poly);
            else
            {
                splitSpanning(poly, combinedCount, target);
            }
        }
    }
    
    private void splitSpanning(CSGPolygon poly, int combinedCount, CSGSplitAcceptor target)
    {
//        splitSpanningTimer.start();
//        splitSpanningInner(poly, combinedCount, target);
//        splitSpanningTimer.stop();
//    }
//    public static MicroTimer splitSpanningTimer = new MicroTimer("splitSpanningQuad", 10000000);
//    private void splitSpanningInner(CSGPolygon poly, int combinedCount, CSGSplitAcceptor target)
//    {
        final int vcount = poly.vertex.length;
        
        final Vertex[] verts = poly.vertex;
        
        CSGPolygon frontQuad = new CSGPolygon(poly, (combinedCount & FRONT_MASK) + 2);
        int iFront = 0;

        CSGPolygon backQuad = new CSGPolygon(poly, ((combinedCount & BACK_MASK) >> BACK_SHIFT) + 2);
        int iBack = 0;


        Vertex iVertex = verts[vcount - 1];
        int iType = this.vertexType(iVertex);
        
        for (int j = 0; j < vcount; j++)
        {
            final Vertex jVertex = verts[j];
            final int jType = this.vertexType(jVertex);
            
            switch(iType * 3 + jType)
            {
                case 0:
                    // I COPLANAR - J COPLANAR
                    frontQuad.vertex[iFront++] = iVertex;
                    backQuad.vertex[iBack++] = iVertex;
                    break;
                    
                case 1:
                    // I COPLANAR - J FRONT
                    frontQuad.vertex[iFront++] = iVertex;
                    backQuad.vertex[iBack++] = iVertex;

                    break;
                    
                case 2:
                    // I COPLANAR - J BACK
                    frontQuad.vertex[iFront++] = iVertex;
                    backQuad.vertex[iBack++] = iVertex;
                    break;
                    
                    
                case 3:
                    // I FRONT - J COPLANAR
                    frontQuad.vertex[iFront++] = iVertex;
                    break;
                    
                case 4:
                    // I FRONT - J FRONT
                    frontQuad.vertex[iFront++] = iVertex;
                    break;
                    
                case 5:
                {
                    // I FRONT - J BACK
                    frontQuad.vertex[iFront++] = iVertex;

                    // Line for interpolated vertex depends on what the next vertex is for this side (front/back).
                    // If the next vertex will be included in this side, we are starting the line connecting
                    // next vertex with previous vertex and should use line from prev. vertex
                    // If the next vertex will NOT be included in this side, we are starting the split line.
                    
                    final Vec3f tVec = jVertex.subtract(iVertex);
                    final float iDot = iVertex.x * this.normalX + iVertex.y * this.normalY + iVertex.z * this.normalZ;
                    final float tDot = tVec.x * this.normalX + tVec.y * this.normalY + tVec.z * this.normalZ;
                    float t = (this.dist - iDot) / tDot;
                    Vertex v = iVertex.interpolate(jVertex, t);
                    
                    frontQuad.vertex[iFront++] = v;
                    backQuad.vertex[iBack++] = v;
                    
                    break;
                }  
                    
                case 6:
                    // I BACK- J COPLANAR
                    backQuad.vertex[iBack++] = iVertex;
                    break;
                    
                case 7:
                {
                    // I BACK - J FRONT
                    backQuad.vertex[iBack++] = iVertex;
                    
                    // see notes for 5
                    
                    final Vec3f tVec = jVertex.subtract(iVertex);
                    final float iDot = iVertex.x * this.normalX + iVertex.y * this.normalY + iVertex.z * this.normalZ;
                    final float tDot = tVec.x * this.normalX + tVec.y * this.normalY + tVec.z * this.normalZ;
                    float t = (this.dist - iDot) / tDot;
                    Vertex v = iVertex.interpolate(jVertex, t);
                    
                    frontQuad.vertex[iFront++] = v;
                    backQuad.vertex[iBack++] = v;
                    
                    break;
                }
                
                case 8:
                    // I BACK - J BACK
                    backQuad.vertex[iBack++] = iVertex;
                    break;
           
            }
            
            iVertex = jVertex;
            iType = jType;
        }
        
        target.acceptFront(frontQuad);
        target.acceptBack(backQuad);               
    }
}
