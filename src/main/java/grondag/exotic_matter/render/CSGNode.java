package grondag.exotic_matter.render;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

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
import java.util.TreeMap;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;

import gnu.trove.map.hash.TLongObjectHashMap;
import grondag.exotic_matter.varia.SimpleUnorderedArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;


public class CSGNode implements Iterable<Poly>
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

    /**
     * Non-recursive iterator to visit all nodes in this tree.
     */
    private Iterator<CSGNode> allNodes()
    {
        return new AbstractIterator<CSGNode>()
        {
           ArrayDeque<CSGNode> stack = new ArrayDeque<>();
           
           {
               stack.push(CSGNode.this);
           }
           
            @Override
            protected CSGNode computeNext()
            {
                if(stack.isEmpty()) return this.endOfData();
                CSGNode result = stack.pop();
                if(result.front != null) stack.push(result.front);
                if(result.back != null) stack.push(result.back);
                return result;
            }
        };
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
        throw new UnsupportedOperationException();
    }
    
    /**
     * For testing purposes.  Shouldn't happen.
     */
    protected long getFirstDuplicateQuadID()
    {

        IntSet ids = new IntOpenHashSet();
        for(Poly q : this)
        {
            if(ids.contains(q.quadID())) return q.quadID();
            ids.add(q.quadID());
        }
        return IPolyProperties.NO_ID;
    }

    /**
     * Converts solid space to empty space and vice versa
     * for all nodes in this BSP tree
     */
    public void invert()
    {
        Iterator<CSGNode> nodes = this.allNodes();
        
        while(nodes.hasNext())
        {
            // invert node can swap the front/back references in 
            // each node, which are used for iteration, but should
            // not matter because we don't care about the order of
            // iteration - we just need to visit each node
            nodes.next().invertNode();;
        }
    }
    
    private void invertNode()
    {
        if (this.plane == null && quads.isEmpty()) return;

        quads.forEach((quad) -> quad.invert());

        this.plane.flip();

        CSGNode temp = this.front;
        this.front = this.back;
        this.back = temp;
    }

    /**
     * Returns a list excluding input polygons that are
     * contained within this BSP tree, splitting polygons as necessary.<p>
     * 
     * The input list and its contents are not modified, but
     * polys in the input list may be copied to the output list.
     */
    private List<Poly> clipQuads(List<Poly> quadsIn)
    {
        if (this.plane == null) return new ArrayList<Poly>(quadsIn);

        List<Poly> result = new ArrayList<Poly>();
        
        for(Poly p : quadsIn)
        {
            this.clipQuad(p, result);
        }
        
        return result;
    }

    /**
     * Clips the given poly against every node of this tree,
     * adding it or split polygons that are in front of all node planes
     * to the provided list.
     */
    private void clipQuad(final Poly poly, List<Poly> output)
    {
        Poly p = poly;
        @Nullable CSGNode node  = this;
        
        ArrayDeque<Pair<CSGNode, Poly>> stack = new ArrayDeque<>();
        List<Poly> frontP = new ArrayList<Poly>();
        List<Poly> backP = new ArrayList<Poly>();
        
        while(true)
        {
            node.plane.splitQuad(p, frontP, backP, frontP, backP);
            
            if(!frontP.isEmpty())  
            {
                if(node.front == null)
                    output.addAll(frontP);
                else
                    for(Poly fp : frontP) stack.push(Pair.of(node.front, fp));

                frontP.clear();
            }
            
            if(!backP.isEmpty())  
            {
                // not adding back plane polys to the output when
                // we get to leaf nodes is what does the clipping 
                if(node.back != null)
                    for(Poly bp : backP) stack.push(Pair.of(node.back, bp));
                
                backP.clear();
            }
            
            if(stack.isEmpty())  break;
            Pair<CSGNode, Poly> next = stack.pop();
            node = next.getLeft();
            p = next.getRight();
        }
    }

    /**
     * Remove all polygons in this BSP tree that are inside the input BSP tree.
     *
     * This tree is modified, and polygons are split if necessary.
     */
    public void clipTo(CSGNode bsp)
    {
        Iterator<CSGNode> nodes = this.allNodes();
        
        while(nodes.hasNext())
        {
            CSGNode node = nodes.next();
            node.quads = bsp.clipQuads(node.quads);
        }
    }

    
    /**
     * Returns all quads in this tree recombined as much as possible.
     * Use for anything to be rendered.
     * Generally only useful on root node!
     */
    public List<Poly> recombinedRawQuads()
    {
        Int2ObjectOpenHashMap<Collection<Poly>> ancestorBuckets = new Int2ObjectOpenHashMap<Collection<Poly>>();
        
        for(Poly quad : this) 
        {
            if(!ancestorBuckets.containsKey(quad.getAncestorQuadID()))
            {
                ancestorBuckets.put(quad.getAncestorQuadID(), new SimpleUnorderedArrayList<Poly>());
            }
            ancestorBuckets.get(quad.getAncestorQuadID()).add(quad);
        }
        
        ImmutableList.Builder<Poly> retVal = ImmutableList.builder();
        ancestorBuckets.values().forEach((quadList) ->
        {
            retVal.addAll(recombine(quadList));
        });
        
        return retVal.build();
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
            return null;
        }
        if(!aQuad.getVertex(aEndIndex).isCsgEqual(bQuad.getVertex(bStartIndex)))
        {
            return null;
        }

        final ArrayList<Vertex> joinedVertex = new ArrayList<Vertex>(8);
        final IntList joinedLineID = new IntArrayList(8);
        
        // don't try to eliminate co-linear vertices when joining two tris
        // no cases when this is really essential and the line tests fail
        // when joining two very small tris
        final boolean skipLineTest = aQuad.vertexCount() == 3 && bQuad.vertexCount() == 3;
        
        for(int a = 0; a < aQuad.vertexCount(); a++)
        {
            if(a == aStartIndex)
            {
                //if vertex is on the same line as prev and next vertex, leave it out.
                if(skipLineTest || !aQuad.getVertex(aStartIndex).isOnLine(aQuad.getVertex(aPrevIndex), bQuad.getVertex(bNextIndex)))
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
                //if vertex is on the same line as prev and next vertex, leave it out
                //unless we are joining two tris - when we are joining very small tris
                //the line test will fail, and not a problem to end up with a quad for
                //when joining larg
                if(skipLineTest || !aQuad.getVertex(aEndIndex).isOnLine(aQuad.getVertex(aNextIndex), bQuad.getVertex(bPrevIndex)))
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
        
        // actually build the new quad!
        Poly joinedQuad = new Poly(aQuad, joinedVertex.size());
        for(int i = 0; i < joinedVertex.size(); i++)
        {
            joinedQuad.addVertex(i, joinedVertex.get(i));
            joinedQuad.setLineID(i, joinedLineID.getInt(i));
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
    
    
    //TODO: switch to ints and reduce boxing
    
    private Collection<Poly> recombine(Collection<Poly> quadsIn)
    {
        Iterator<Poly> iterator = quadsIn.iterator();
        
        if(!iterator.hasNext()) return quadsIn;
        
        Poly q = iterator.next();
        
        if(q.getAncestorQuadID() == IPolyProperties.IS_AN_ANCESTOR) return quadsIn;
        
        TLongObjectHashMap<Poly> quadMap = new TLongObjectHashMap<Poly>(quadsIn.size());
        TreeMap<Long, TreeMap<Long, Integer>> edgeMap = new TreeMap<Long, TreeMap<Long, Integer>>();
        
//        double totalArea = 0;
        
        do
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
                edgeMap.get(lineID).put((long) q.quadID(), i);
            }
            
            if(iterator.hasNext()) 
                q = iterator.next();
            else 
                break;
            
        } while(true);
        
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
                            quadMap.remove((long)iQuad.quadID());
                            quadMap.remove((long)jQuad.quadID());
                            
                            // add quad to main map
                            quadMap.put((long)joined.quadID(), joined);

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

                                TreeMap<Long, Integer> removeMap = edgeMap.get((long)iQuad.getLineID(n));
                                removeMap.remove((long)iQuad.quadID());
                            }
                            
                            for(int n = 0; n < jQuad.vertexCount(); n++)
                            {
                                // negative line ids represent outside edges - not part of map
                                if(jQuad.getLineID(n) < 0) continue;

                                TreeMap<Long, Integer> removeMap = edgeMap.get((long)jQuad.getLineID(n));
                                removeMap.remove((long)jQuad.quadID());
                            }                            
                            
                            // add quad to edge map
                            for(int n = 0; n < joined.vertexCount(); n++)
                            {
                                // negative line ids represent outside edges - not part of map
                                if(joined.getLineID(n) < 0) continue;

                                if(!edgeMap.containsKey((long)joined.getLineID(n)))
                                {
                                    edgeMap.put((long) joined.getLineID(n), new TreeMap<Long, Integer>());
                                }
                                edgeMap.get((long)joined.getLineID(n)).put((long) joined.quadID(), n);
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
        quadMap.valueCollection().forEach((p) -> retVal.addAll(p.toQuads()));
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

    @Override
    public Iterator<Poly> iterator()
    {
        return new AbstractIterator<Poly>()
        {
           Iterator<CSGNode> nodes = CSGNode.this.allNodes();
           CSGNode node = nodes.next();
           Iterator<Poly> polys = node.quads.iterator();
           
            @Override
            protected Poly computeNext()
            {
                if(polys.hasNext()) return polys.next();
                
                while(nodes.hasNext())
                {
                    node = nodes.next();
                    polys = node.quads.iterator();
                    if(polys.hasNext()) return polys.next();
                }
                
                return this.endOfData();
            }
        };
    }
}
