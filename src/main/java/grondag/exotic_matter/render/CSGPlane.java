package grondag.exotic_matter.render;

import java.awt.Polygon;

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import grondag.exotic_matter.varia.MicroTimer;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class CSGPlane
{
    protected static AtomicInteger nextInsideLineID = new AtomicInteger(1);
    protected static AtomicInteger nextOutsideLineID = new AtomicInteger(-1);
    
    private float normalX;
    private float normalY;
    private float normalZ;
    
    /**
     * Distance to origin.
     */
    public float dist;
    
    protected int lineID = nextInsideLineID.getAndIncrement();

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

    public CSGPlane(ICSGPolygon quad)
    {
        final Vec3f normal = quad.getFaceNormal();
        this.normalX = normal.x;
        this.normalY = normal.y;
        this.normalZ = normal.z;
        this.dist = normal.dotProduct(quad.getVertex(0).toVec3f());    
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
    private static final int SPANNING = 3;
    

    /**
     * Splits a {@link Polygon} by this plane if needed. After that it puts the
     * polygons or the polygon fragments in the appropriate lists
     * ({@code front}, {@code back}). Coplanar polygons go into either
     * {@code coplanarFront}, {@code coplanarBack} depending on their
     * orientation with respect to this plane. Polygons in front or back of this
     * plane go into either {@code front} or {@code back}.
     *
     * @param quad polygon to split
     * @param coplanarFront "coplanar front" polygons
     * @param coplanarBack "coplanar back" polygons
     * @param front front polygons
     * @param back back polgons
     */
    
    public void splitQuad(
            ICSGPolygon quad,
            List<ICSGPolygon> coplanarFront,
            List<ICSGPolygon> coplanarBack,
            List<ICSGPolygon> front,
            List<ICSGPolygon> back) 
    {
        splitTimer.start();
        splitQuadInner(quad, coplanarFront, coplanarBack, front, back);
        if(splitTimer.stop())
        {
            splitSpanningTimer.reportAndClear();
        }
    }
    private static MicroTimer splitTimer = new MicroTimer("splitQuad", 1000000);
    private void splitQuadInner(
        ICSGPolygon quad,
        List<ICSGPolygon> coplanarFront,
        List<ICSGPolygon> coplanarBack,
        List<ICSGPolygon> front,
        List<ICSGPolygon> back) 
    {
        final int vcount = quad.vertexCount();
        
        // Classify each point as well as the entire polygon
        int polygonType = COPLANAR;
        int types[] = new int[vcount];
        for (int i = 0; i < vcount; i++)
        {
            final Vec3f vert = quad.getVertex(i).toVec3f();
            float t = (vert.x * this.normalX + vert.y * this.normalY + vert.z * this.normalZ) - this.dist;
            
            int type = (t < -QuadHelper.EPSILON) ? BACK : (t > QuadHelper.EPSILON) ? FRONT : COPLANAR;
            polygonType |= type;
            types[i] = type;
        }
        
        // Put the polygon in the correct list, splitting it when necessary.
        switch (polygonType)
        {
            case COPLANAR:
                final Vec3f faceNorm = quad.getFaceNormal();
                float t = faceNorm.x * this.normalX + faceNorm.y * this.normalY + faceNorm.z * this.normalZ;
                (t > 0 ? coplanarFront : coplanarBack).add(quad);
                break;
                
            case FRONT:
                front.add(quad);
                break;
            case BACK:
                back.add(quad);
                break;
            default:
                splitSpanning(quad, vcount, types, front, back);
                break;
        }
    }
    
    private void splitSpanning(ICSGPolygon quad, int vcount, int types[], List<ICSGPolygon> front,
            List<ICSGPolygon> back)
    {
        splitSpanningTimer.start();
        splitSpanningInner(quad, vcount, types, front, back);
        splitSpanningTimer.stop();
    }
    private static MicroTimer splitSpanningTimer = new MicroTimer("splitSpanningQuad", 1000000);
    private void splitSpanningInner(ICSGPolygon quad, int vcount, int types[], List<ICSGPolygon> front,
            List<ICSGPolygon> back)
    {
        List<IPolygonVertex> frontVertex = new ArrayList<>(vcount+1);
        List<IPolygonVertex> backVertex = new ArrayList<>(vcount+1);
        IntList frontLineID = new IntArrayList(vcount+1);
        IntList backLineID = new IntArrayList(vcount+1);
        for (int i = 0; i < vcount; i++) {
            int j = (i + 1) % vcount;
            int iType = types[i];
            int jType = types[j];
            IPolygonVertex iVertex = quad.getVertex(i);
            IPolygonVertex jVertex = quad.getVertex(j);
            int iLineID = quad.getLineID(i);
            
            if (iType != BACK) {
                frontVertex.add(iVertex);
                // if we are splitting at an existing vertex need to use split line
                // if the next vertex is not going into this list
                frontLineID.add(iType == COPLANAR && jType == BACK ? this.lineID : iLineID);
            }
            if (iType != FRONT) {
                backVertex.add(iVertex);
                // if we are splitting at an existing vertex need to use split line
                // if the next vertex is not going into this list
                backLineID.add(iType == COPLANAR && jType == FRONT ? this.lineID : iLineID);
            }
            // Line for interpolated vertex depends on what the next vertex is for this side (front/back).
            // If the next vertex will be included in this side, we are starting the line connecting
            // next vertex with previous vertex and should use line from prev. vertex
            // If the next vertex will NOT be included in this side, we are starting the split line.

            if ((iType | jType) == SPANNING)
            {
                final Vec3f iVec = iVertex.toVec3f();
                final Vec3f tVec = jVertex.toVec3f().subtract(iVec);
                final float iDot = iVec.x * this.normalX + iVec.y * this.normalY + iVec.z * this.normalZ;
                final float tDot = tVec.x * this.normalX + tVec.y * this.normalY + tVec.z * this.normalZ;
                float t = (this.dist - iDot) / tDot;
                IPolygonVertex v = iVertex.interpolate(jVertex, t);
                
                frontVertex.add(v);
                frontLineID.add(jType != FRONT ? this.lineID : iLineID);
                
                backVertex.add(v);
                backLineID.add(jType != BACK ? this.lineID : iLineID);
            }
        }
        
        if (frontVertex.size() >= 3) 
        {
            // forces face normal to be computed if it has not been already
            // this allows it to be copied to the split quad and 
            // and avoids having incomputable face normals due to very small polys.
            quad.getFaceNormal();
            IMutableCSGPolygon frontQuad = Poly.mutableCSG(quad, frontVertex.size());
            frontQuad.setAncestorQuadID(quad.getAncestorQuadIDForDescendant());
            
            for(int i = 0; i < frontVertex.size(); i++)
            {
                frontQuad.addVertex(i, frontVertex.get(i));
                frontQuad.setLineID(i, frontLineID.getInt(i));
            }

            front.add(frontQuad);
        }

        if (backVertex.size() >= 3) 
        {
            // forces face normal to be computed if it has not been already
            // this allows it to be copied to the split quad and 
            // and avoids having incomputable face normals due to very small polys.
            quad.getFaceNormal();
            IMutableCSGPolygon backQuad = Poly.mutableCSG(quad, backVertex.size());
            backQuad.setAncestorQuadID(quad.getAncestorQuadIDForDescendant());

            for(int i = 0; i < backVertex.size(); i++)
            {
                backQuad.addVertex(i, backVertex.get(i));
                backQuad.setLineID(i, backLineID.getInt(i));
            }
            
            back.add(backQuad);               
        }
    }
}
