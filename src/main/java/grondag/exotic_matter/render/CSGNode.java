package grondag.exotic_matter.render;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;

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

import java.util.List;
import java.util.Stack;
import java.util.TreeMap;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableList;

import gnu.trove.map.hash.TLongObjectHashMap;


public class CSGNode
{
    /**
     * RawQuads.
     */
    private List<Poly> quads = new ArrayList<>();
    
    /**
     * Plane used for BSP.
     */
    private @Nullable CSGPlane plane;
    
    /**
     * RawQuads in front of the plane.
     */
    private @Nullable CSGNode front;
    
    /**
     * RawQuads in back of the plane.
     */
    private @Nullable CSGNode back;

    /**
     * Returns root node of BSP tree with given polygons.
     *
     * Polygons in the input are copied so that nothing is mutated.
     */
    public CSGNode(CSGMesh shapeIn)
    {
        for(Poly q : shapeIn)
        {
            if(q.isOnSinglePlane())
            {
                this.addToBSPTree(q.clone().setupCsgMetadata());
            }
            else
            {
                for(Poly t : q.toTris())
                {
                    this.addToBSPTree(t.setupCsgMetadata());
                }
            }
         }
    }

    public void addAll(Iterable<Poly> polys)
    {
        for(Poly p : polys)
        {
            this.addToBSPTree(p);
        }
    }
    
    /**
     * Adds poly to this tree. Meant to be called on root node.
     */
    private void addToBSPTree(final Poly poly)
    {
        Poly p = poly;
        @Nullable CSGNode node  = this;
        
        ArrayDeque<Pair<CSGNode, Poly>> stack = new ArrayDeque<>();
        List<Poly> frontP = new ArrayList<Poly>();
        List<Poly> backP = new ArrayList<Poly>();
        
        while(true)
        {
            if(node.quads.isEmpty())
            {
                node.add(p);
            }
            else
            {
                node.plane.splitQuad(p, node.quads, node.quads, frontP, backP);
                
                if(!frontP.isEmpty())  
                {
                    if(node.front == null) node.front = new CSGNode();
                    for(Poly fp : frontP) stack.push(Pair.of(node.front, fp));
                    frontP.clear();
                }
                
                if(!backP.isEmpty())  
                {
                    if(node.back == null) node.back = new CSGNode();
                    for(Poly bp : backP) stack.push(Pair.of(node.back, bp));
                    backP.clear();
                }
            }
            
            if(stack.isEmpty())  break;
            Pair<CSGNode, Poly> next = stack.pop();
            node = next.getLeft();
            p = next.getRight();
        }
    }

    
    /**
     * Constructor. Creates a node without polygons.
     */
    private CSGNode() { }

    @Override
    public CSGNode clone()
    {
        assert false : "If going to actually use CSGNode.clone() should make it non-recursive.";
    
        CSGNode node = new CSGNode();
        node.plane = this.plane == null ? null : this.plane.clone();
        node.front = this.front == null ? null : this.front.clone();
        node.back = this.back == null ? null : this.back.clone();
        quads.forEach((Poly p) -> {
            node.quads.add(p.clone());
        });
        return node;
    }
    
    /**
     * For testing purposes.  Shouldn't happen.
     */
    protected long getFirstDuplicateQuadID()
    {

        HashSet<Long> ids = new HashSet<Long>();
        for(Poly q : this.allRawQuads())
        {
            if(ids.contains(q.quadID())) return q.quadID();
            ids.add(q.quadID());
        }
        return IPolyProperties.NO_ID;
    }

    /**
     * Converts solid space to empty space and vice verca.
     */
    public void invert()
    {

        if (this.plane == null && quads.isEmpty()) return;

        quads.forEach((quad) -> {
            quad.invert();
        });

        if (this.plane == null)
        {
            // quads can't be empty if we get to here
            this.plane = new CSGPlane(quads.get(0));
        }

        this.plane.flip();

        if (this.front != null) {
            this.front.invert();
        }
        if (this.back != null) {
            this.back.invert();
        }
        CSGNode temp = this.front;
        this.front = this.back;
        this.back = temp;
    }

    /**
     * Recursively removes all polygons in the {@link polygons} list that are
     * contained within this BSP tree.
     *
     * <b>Note:</b> polygons are splitted if necessary.
     *
     * @param quadsIn the polygons to clip
     *
     * @return the cliped list of polygons
     */
    private List<Poly> clipQuads(@Nullable List<Poly> quadsIn)
    {

        if (this.plane == null) {
            return new ArrayList<Poly>(quadsIn);
        }

        List<Poly> frontP = new ArrayList<Poly>();
        List<Poly> backP = new ArrayList<Poly>();

        for (Poly quad : quadsIn) {
            this.plane.splitQuad(quad, frontP, backP, frontP, backP);
        }
        if (this.front != null) {
            frontP = this.front.clipQuads(frontP);
        }
        if (this.back != null) {
            backP = this.back.clipQuads(backP);
        } else {
            backP = new ArrayList<Poly>();
        }

        frontP.addAll(backP);
        return frontP;
    }

    // Remove all polygons in this BSP tree that are inside the other BSP tree
    // `bsp`.
    /**
     * Removes all polygons in this BSP tree that are inside the specified BSP
     * tree ({@code bsp}).
     *
     * <b>Note:</b> polygons are split if necessary.
     *
     * @param bsp bsp that shall be used for clipping
     */
    public void clipTo(CSGNode bsp)
    {
        this.quads = bsp.clipQuads(this.quads);
        
        if (this.front != null) {
            this.front.clipTo(bsp);
        }
        if (this.back != null) {
            this.back.clipTo(bsp);
        }
    }

    /**
     * Returns a list of all polygons in this BSP tree.
     *
     * @return a list of all polygons in this BSP tree
     */
    public List<Poly> allRawQuads()
    {
        List<Poly> localRawQuads = new ArrayList<>(this.quads);
        if (this.front != null) {
            localRawQuads.addAll(this.front.allRawQuads());
        }
        if (this.back != null) {
            localRawQuads.addAll(this.back.allRawQuads());
        }

        return localRawQuads;
    }
    
    /**
     * Returns all quads in this tree recombined as much as possible.
     * Use instead of allRawQuads() for anything to be rendered.
     * Generally only useful on job node!
     * 
     * Will only work if build was called with initCSG parameter = true
     * during initialization of the tree because it uses the information populated then.
     * @return
     */
    public List<Poly> recombinedRawQuads()
    {
        
        //TODO: can we limit comparisons to co-planar quads?  Probably much faster.
        TLongObjectHashMap<ArrayList<Poly>> ancestorBuckets = new TLongObjectHashMap<ArrayList<Poly>>();
        
        this.allRawQuads().forEach((quad) -> 
        {
            if(!ancestorBuckets.contains(quad.getAncestorQuadID()))
            {
                ancestorBuckets.put(quad.getAncestorQuadID(), new ArrayList<Poly>());
            }
            ancestorBuckets.get(quad.getAncestorQuadID()).add(quad);
        });
        
        ArrayList<Poly> retVal = new ArrayList<Poly>();
        ancestorBuckets.valueCollection().forEach((quadList) ->
        {
            retVal.addAll(recombine(quadList));
        });
        
        return retVal;
    }

    
    /**
     * Tries to combine two quads along the given edge. To join, all must be true:
     * 1) shared edge id
     * 2) vertexes in opposite order for each quad match each other
     * 3) quads are both inverted or both not inverted
     * 4) resulting quad has three or four vertices (Tri or Quad)
     * 5) resulting quad is convex
     * 
     * Returns null if quads cannot be joined.
     */
    private @Nullable Poly joinCsgQuads(Poly aQuad, Poly bQuad, long lineID)
    {

        // quads must be same orientation to be joined
        if(aQuad.isInverted() != bQuad.isInverted()) return null;

        final int aStartIndex = aQuad.findLineIndex(lineID);
        // shouldn't happen, but won't work if does
        if(aStartIndex == Poly.LINE_NOT_FOUND) 
            return null;
        final int aEndIndex = aStartIndex + 1 == aQuad.vertexCount() ? 0 : aStartIndex + 1;
        final int aNextIndex = aEndIndex + 1 == aQuad.vertexCount() ? 0 : aEndIndex + 1;
        final int aPrevIndex = aStartIndex == 0 ? aQuad.vertexCount() - 1 : aStartIndex - 1;

        final int bStartIndex = bQuad.findLineIndex(lineID);
        // shouldn't happen, but won't work if does
        if(bStartIndex == Poly.LINE_NOT_FOUND) 
            return null;
        final int bEndIndex = bStartIndex + 1 == bQuad.vertexCount() ? 0 : bStartIndex + 1;
        final int bNextIndex = bEndIndex + 1 == bQuad.vertexCount() ? 0 : bEndIndex + 1;
        final int bPrevIndex = bStartIndex == 0 ? bQuad.vertexCount() - 1 : bStartIndex - 1;
        
        // confirm vertices on either end of vertex match
        if(!aQuad.getVertex(aStartIndex).isCsgEqual(bQuad.getVertex(bEndIndex)))
        {
//            HardScience.log.info("vertex mismatch for LineID = " + lineID + " face = " + aQuad.face);
//            HardScience.log.info("A Start: " + aQuad.getVertex(aStartIndex).toString() );
//            HardScience.log.info("B End: " + bQuad.getVertex(bEndIndex).toString() );
//            HardScience.log.info("B Start: " + bQuad.getVertex(bStartIndex).toString() );
//            HardScience.log.info("A End: " + aQuad.getVertex(aEndIndex).toString() );
            return null;
        }
        if(!aQuad.getVertex(aEndIndex).isCsgEqual(bQuad.getVertex(bStartIndex)))
        {
//            HardScience.log.info("vertex mismatch for LineID = " + lineID);
//            HardScience.log.info("A Start: " + aQuad.getVertex(aStartIndex).toString() );
//            HardScience.log.info("A End: " + aQuad.getVertex(aEndIndex).toString() );
//            HardScience.log.info("B Start: " + bQuad.getVertex(bStartIndex).toString() );
//            HardScience.log.info("B End: " + bQuad.getVertex(bEndIndex).toString() );
            return null;
        }

        final ArrayList<Vertex> joinedVertex = new ArrayList<Vertex>(8);
        final ArrayList<Long> joinedLineID = new ArrayList<Long>(8);
        
        for(int a = 0; a < aQuad.vertexCount(); a++)
        {
            if(a == aStartIndex)
            {
                //if vertex is on the same line as prev and next vertex, leave it out.
                if(!aQuad.getVertex(aStartIndex).isOnLine(aQuad.getVertex(aPrevIndex), bQuad.getVertex(bNextIndex)))
//                if(aQuad.getLineID(aPrevIndex) != bQuad.getLineID(bEndIndex))
                {
                    joinedVertex.add(aQuad.getVertex(aStartIndex));
                    joinedLineID.add(bQuad.getLineID(bEndIndex));
                }

                // add b vertexes except two bQuad vertexes in common with A
                for(int bOffset = 1; bOffset < bQuad.vertexCount() - 1; bOffset++)
                {
                    int b = bEndIndex + bOffset;
                    if(b >= bQuad.vertexCount()) b -= bQuad.vertexCount();
                    joinedVertex.add(bQuad.getVertex(b));
                    joinedLineID.add(bQuad.getLineID(b));
                }
            }
            else if(a == aEndIndex)
            {
                //if vertex is on the same line as prev and next vertex, leave it out.
                if(!aQuad.getVertex(aEndIndex).isOnLine(aQuad.getVertex(aNextIndex), bQuad.getVertex(bPrevIndex)))
//                if(aQuad.getLineID(aEndIndex) != bQuad.getLineID(bPrevIndex))
                {
                    joinedVertex.add(aQuad.getVertex(aEndIndex));
                    joinedLineID.add(aQuad.getLineID(aEndIndex));
                }
            }
            else
            {
                joinedVertex.add(aQuad.getVertex(a));
                joinedLineID.add(aQuad.getLineID(a));
           }
        }   
        
        assert joinedVertex.size() > 2 : "Bad join outcome";
        
        // actually build the new quad!
        Poly joinedQuad = new Poly(aQuad, joinedVertex.size());
        for(int i = 0; i < joinedVertex.size(); i++)
        {
            joinedQuad.addVertex(i, joinedVertex.get(i));
            joinedQuad.setLineID(i, joinedLineID.get(i));
        }

        // must be convex
        if(!joinedQuad.isConvex())
        {
//            HardScience.log.info("Quad not convex");
            return null;
        }
        
//        if(Math.abs(aQuad.getArea() + bQuad.getArea() - joinedQuad.getArea()) > QuadFactory.EPSILON)
//        {
//            HardScience.log.info("area mismatch");
//        }
        
        return joinedQuad;
        
    }
    
    private List<Poly> recombine(ArrayList<Poly> quadList)
    {
        if(quadList.get(0).getAncestorQuadID() == IPolyProperties.IS_AN_ANCESTOR) return quadList;
        
        TLongObjectHashMap<Poly> quadMap = new TLongObjectHashMap<Poly>(quadList.size());
        TreeMap<Long, TreeMap<Long, Integer>> edgeMap = new TreeMap<Long, TreeMap<Long, Integer>>();
        
//        double totalArea = 0;
        
        for(Poly q : quadList) 
        {
            quadMap.put(q.quadID(), q);
//            totalArea += q.getArea();
            
            // build edge map for inside edges that may be rejoined
            for(int i = 0; i < q.vertexCount(); i++)
            {
                long lineID = q.getLineID(i);
                // negative line ids represent outside edges - no need to rejoin them
                // zero ids are uninitialized edges and should be ignored
                if(lineID <= 0) continue;
                
                if(!edgeMap.containsKey(lineID))
                {
                    edgeMap.put(lineID, new TreeMap<Long, Integer>());
                }
                edgeMap.get(lineID).put(q.quadID(), i);
            }
        }
        
        boolean potentialMatchesRemain = true;
        while(potentialMatchesRemain)
        {
            potentialMatchesRemain = false;
            
            for(Long edgeKey : edgeMap.descendingKeySet())
            {
                TreeMap<Long, Integer> edgeQuadMap = edgeMap.get(edgeKey);
                
                if(edgeQuadMap.isEmpty()) continue;
                
                Long[] edgeQuadIDs = edgeQuadMap.keySet().toArray(new Long[1]);
                if(edgeQuadIDs.length < 2) continue;
                
                for(int i = 0; i < edgeQuadIDs.length - 1; i++)
                {
                    for(int j = i + 1; j < edgeQuadIDs.length; j++)
                    {
                        // Examining two quads that share an edge
                        // to determine if they can be combined.

                        Poly iQuad = quadMap.get(edgeQuadIDs[i]);
                        Poly jQuad = quadMap.get(edgeQuadIDs[j]);
                        
                        if(iQuad == null || jQuad == null) continue;
                        
                        Poly joined = joinCsgQuads(iQuad, jQuad, edgeKey);
                        
                        if(joined != null)
                        {    
                            potentialMatchesRemain = true;
                            
                            // remove quads from main map
                            quadMap.remove(iQuad.quadID());
                            quadMap.remove(jQuad.quadID());
                            
                            // add quad to main map
                            quadMap.put(joined.quadID(), joined);

                            //For debugging
//                            {
//                                double testArea = 0;
//                                for(RawQuad quad : quadMap.valueCollection())
//                                {
//                                    testArea += quad.getArea();
//                                }
//                                if(Math.abs(testArea - totalArea) > QuadFactory.EPSILON)
//                                {
//                                    HardScience.log.info("area mismatch");
//                                }
//                            }
                            
                            // remove quads from edge map
                            for(int n = 0; n < iQuad.vertexCount(); n++)
                            {                
                                // negative line ids represent outside edges - not part of map
                                if(iQuad.getLineID(n) < 0) continue;

                                TreeMap<Long, Integer> removeMap = edgeMap.get(iQuad.getLineID(n));
                                removeMap.remove(iQuad.quadID());
                            }
                            
                            for(int n = 0; n < jQuad.vertexCount(); n++)
                            {
                                // negative line ids represent outside edges - not part of map
                                if(jQuad.getLineID(n) < 0) continue;

                                TreeMap<Long, Integer> removeMap = edgeMap.get(jQuad.getLineID(n));
                                removeMap.remove(jQuad.quadID());
                            }                            
                            
                            // add quad to edge map
                            for(int n = 0; n < joined.vertexCount(); n++)
                            {
                                // negative line ids represent outside edges - not part of map
                                if(joined.getLineID(n) < 0) continue;

                                if(!edgeMap.containsKey(joined.getLineID(n)))
                                {
                                    edgeMap.put(joined.getLineID(n), new TreeMap<Long, Integer>());
                                }
                                edgeMap.get(joined.getLineID(n)).put(joined.quadID(), n);
                            }
                        }
                    }
                }
            }
            
        }
        
//        if(quadMap.size() > 1 && quadList.getFirst().face == EnumFacing.DOWN)
//        {
//            HardScience.log.info("too many");
//        }
        
        ArrayList<Poly> retVal = new ArrayList<Poly>();
        quadMap.valueCollection().forEach((q) -> retVal.addAll(q.toQuads()));
        return retVal;
            
        
    }

    /**
     * Adds poly to this node, establishing the plane for this node
     * if it was not already determined. <p>
     * 
     * ASSUMES the given poly is coplanar with the plane of this node
     * if plane is already set.
     */
    private void add(Poly poly)
    {
        if(this.plane == null) this.plane = new CSGPlane(poly);
        this.quads.add(poly);
    }
    
//    /**
//     * Build a BSP tree out of {@code polygons}. When called on an existing
//     * tree, the new polygons are filtered down to the bottom of the tree and
//     * become new nodes there. Each set of polygons is partitioned using the
//     * start polygon (no heuristic is used to pick a good split).
//     *
//     * @param quadsIn polygons used to build the BSP
//     */
//    public final void build(List<Poly> quadsIn)
//    {
//        if (quadsIn.isEmpty()) 
//        {
//            return;
//        }
//
//        if (this.plane == null) {
//            this.plane = new CSGPlane(quadsIn.get(0));
//        }
//
//        List<Poly> frontP = new ArrayList<Poly>();
//        List<Poly> backP = new ArrayList<Poly>();
//
//        // parallel version does not work here
//        quadsIn.forEach((quad) -> {
//            this.plane.splitQuad(quad.clone(), this.quads, this.quads, frontP, backP);
//        });
//
//        if (!frontP.isEmpty()) {
//            if (this.front == null) {
//                this.front = new CSGNode();
//            }
//            this.front.build(frontP);
//        }
//        if (!backP.isEmpty()) {
//            if (this.back == null) {
//                this.back = new CSGNode();
//            }
//            this.back.build(backP);
//        }
//    }
    
}
