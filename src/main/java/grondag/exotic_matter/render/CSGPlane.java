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
import java.util.concurrent.atomic.AtomicLong;

import grondag.exotic_matter.varia.MicroTimer;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.util.math.Vec3d;

public class CSGPlane
{
    protected static AtomicLong nextInsideLineID = new AtomicLong(1);
    protected static AtomicLong nextOutsideLineID = new AtomicLong(-1);
    
    /**
     * Normal vector.
     */
    public Vec3d normal;
    /**
     * Distance to origin.
     */
    public double dist;
    
    protected long lineID = nextInsideLineID.getAndIncrement();

    /**
     * Constructor. Creates a new plane defined by its normal vector and the
     * distance to the origin.
     *
     * @param normal plane normal
     * @param dist distance from origin
     */
    public CSGPlane(Vec3d normal, double dist) {
        this.normal = normal;
        this.dist = dist;
    }

    public CSGPlane(Poly quad)
    {
        this.normal = quad.getFaceNormal();
        this.dist = normal.dotProduct(quad.getVertex(0).toVec3d());    
    }
    
    @Override
    public CSGPlane clone() {
        return new CSGPlane(new Vec3d(normal.x, normal.y, normal.z), dist);
    }

    /**
     * Flips this plane.
     */
    public void flip() {
        normal = normal.scale(-1);
        dist = -dist;
    }

    private static MicroTimer splitTimer = new MicroTimer("splitQuad", 1000000);

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
            Poly quad,
            List<Poly> coplanarFront,
            List<Poly> coplanarBack,
            List<Poly> front,
            List<Poly> back) 
    {
        splitTimer.start();
        splitQuadInner(quad, coplanarFront, coplanarBack, front, back);
        splitTimer.stop();
    }
    private void splitQuadInner(
        Poly quad,
        List<Poly> coplanarFront,
        List<Poly> coplanarBack,
        List<Poly> front,
        List<Poly> back) 
    {
        final int COPLANAR = 0;
        final int FRONT = 1;
        final int BACK = 2;
        final int SPANNING = 3;
        
        // Classify each point as well as the entire polygon into one of the above
        // four classes.
        int polygonType = 0;
        int types[] = new int[quad.vertexCount()];
        for (int i = 0; i < quad.vertexCount(); i++) {
            double t = this.normal.dotProduct(quad.getVertex(i).toVec3d()) - this.dist;
            
            int type = (t < -QuadHelper.EPSILON) ? BACK : (t > QuadHelper.EPSILON) ? FRONT : COPLANAR;
            polygonType |= type;
            types[i] = type;
        }
        
        // Put the polygon in the correct list, splitting it when necessary.
        switch (polygonType) {
            case COPLANAR:
                //System.out.println(" -> coplanar");
                (this.normal.dotProduct(quad.getFaceNormal()) > 0 ? coplanarFront : coplanarBack).add(quad);
                break;
            case FRONT:
                //System.out.println(" -> front");
                front.add(quad);
                break;
            case BACK:
                //System.out.println(" -> back");
                back.add(quad);
                break;
            case SPANNING:
                
                List<Vertex> frontVertex = new ArrayList<Vertex>(quad.vertexCount()+1);
                List<Vertex> backVertex = new ArrayList<Vertex>(quad.vertexCount()+1);
                LongList frontLineID = new LongArrayList(quad.vertexCount()+1);
                LongList backLineID = new LongArrayList(quad.vertexCount()+1);
                for (int i = 0; i < quad.vertexCount(); i++) {
                    int j = (i + 1) % quad.vertexCount();
                    int iType = types[i];
                    int jType = types[j];
                    Vertex iVertex = quad.getVertex(i);
                    Vertex jVertex = quad.getVertex(j);
                    long iLineID = quad.getLineID(i);
                    
                    if (iType != BACK) {
                        frontVertex.add(iVertex);
                        // if we are splitting at an existing vertex need to use split line
                        // if the next vertex is not going into this list
                        frontLineID.add(iType == COPLANAR && jType == BACK ? this.lineID : iLineID);
                    }
                    if (iType != FRONT) {
                        backVertex.add(iType != BACK ? iVertex.clone() : iVertex);
                        // if we are splitting at an existing vertex need to use split line
                        // if the next vertex is not going into this list
                        backLineID.add(iType == COPLANAR && jType == FRONT ? this.lineID : iLineID);
                    }
                    // Line for interpolated vertex depends on what the next vertex is for this side (front/back).
                    // If the next vertex will be included in this side, we are starting the line connecting
                    // next vertex with previous vertex and should use line from prev. vertex
                    // If the next vertex will NOT be included in this side, we are starting the split line.

                    if ((iType | jType) == SPANNING) {
                        float t = (float) ((this.dist - this.normal.dotProduct(iVertex.toVec3d())) / this.normal.dotProduct(jVertex.toVec3d().subtract(iVertex.toVec3d())));
                        Vertex v = iVertex.interpolate(jVertex, t);
                        
                        frontVertex.add(v);
                        frontLineID.add(jType != FRONT ? this.lineID : iLineID);
                        
                        backVertex.add(v.clone());
                        backLineID.add(jType != BACK ? this.lineID : iLineID);
                    }
                }
                if (frontVertex.size() >= 3) 
                {
                    // forces face normal to be computed if it has not been already
                    // this allows it to be copied to the split quad and 
                    // and avoids having incomputable face normals due to very small polys.
                    quad.getFaceNormal();
                    Poly frontQuad = new Poly(quad, frontVertex.size());
                    frontQuad.setAncestorQuadID(quad.getAncestorQuadIDForDescendant());
                    
                    for(int i = 0; i < frontVertex.size(); i++)
                    {
                        frontQuad.addVertex(i, frontVertex.get(i));
                        frontQuad.setLineID(i, frontLineID.getLong(i));
                    }

                    front.add(frontQuad);
                }

                if (backVertex.size() >= 3) 
                {
                    // forces face normal to be computed if it has not been already
                    // this allows it to be copied to the split quad and 
                    // and avoids having incomputable face normals due to very small polys.
                    quad.getFaceNormal();
                    Poly backQuad = new Poly(quad, backVertex.size());
                    backQuad.setAncestorQuadID(quad.getAncestorQuadIDForDescendant());

                    for(int i = 0; i < backVertex.size(); i++)
                    {
                        backQuad.addVertex(i, backVertex.get(i));
                        backQuad.setLineID(i, backLineID.getLong(i));
                    }
                    
                    back.add(backQuad);               
                }
                break;
        }
    }
}
