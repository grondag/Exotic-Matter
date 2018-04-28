package grondag.exotic_matter.render;

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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import grondag.exotic_matter.varia.SimpleUnorderedArrayList;


public class CSGNode implements Iterable<CSGPolygon>
{
    /**
     * RawQuads.
     */
    private List<CSGPolygon> quads = new ArrayList<>();
    
    /**
     * Plane used for BSP.
     */
    CSGPlane plane;
    
    /**
     * RawQuads in front of the plane.
     */
    @Nullable CSGNode front;
    
    /**
     * RawQuads in back of the plane.
     */
    @Nullable CSGNode back;

    /**
     * Returns root node of BSP tree with given polygons. <p>
     * 
     * Polygons must be coplanar. (not  validated)<br>
     * Input collection must have at least one polygon or will crash.
     * Polygons in the input are copied so that nothing is mutated.<br>
     */
    public CSGNode(Collection<IPolygon> shapeIn)
    {
        Iterator<IPolygon> it = shapeIn.iterator();
        CSGPolygon poly = new CSGPolygon(it.next());
        this.plane = new CSGPlane(poly);
        
        CSGSplitAcceptor.CoFrontBack target = new CSGSplitAcceptor.CoFrontBack();
        
        while(true)
        {
            target.splitPolyStartingWith(poly, this);
            
            
            if(it.hasNext())
                poly = new CSGPolygon(it.next());
            else break;
        }
    }

    /**
     * Non-recursive iterator to visit all nodes in this tree.
     */
    private Iterator<CSGNode> allNodes()
    {
        return new Iterator<CSGNode>()
        {
           private @Nullable CSGNode next;
           ArrayDeque<CSGNode> stack = new ArrayDeque<>();
           
           {
               stack.push(CSGNode.this);
               next = computeNext();
           }
           
            protected CSGNode computeNext()
            {
                if(stack.isEmpty()) return null;
                CSGNode result = stack.pop();
                if(result.front != null) stack.push(result.front);
                if(result.back != null) stack.push(result.back);
                return result;
            }

            @Override
            public boolean hasNext()
            {
                return next != null;
            }

            @Override
            public CSGNode next()
            {
                CSGNode result = next;
                this.next = computeNext();
                return result;
            }
        };
    }

    /** 
     * Meant to be called on root node
     */
    public void addAll(Iterable<CSGPolygon> polys)
    {
        CSGSplitAcceptor.CoFrontBack target = new CSGSplitAcceptor.CoFrontBack();
        
        for(CSGPolygon p : polys)
        {
            target.splitPolyStartingWith(p, this);
        }
    }

    
    CSGNode(CSGPolygon firstPoly)
    { 
        this.plane = new CSGPlane(firstPoly);
        this.quads.add(firstPoly);        
    }

    @Override
    public CSGNode clone()
    {
        throw new UnsupportedOperationException();
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
        this.quads.forEach(q -> q.flip());
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
    private List<CSGPolygon> clipQuads(List<CSGPolygon> quadsIn)
    {
        CSGSplitAcceptor.ClipAcceptor target = new CSGSplitAcceptor.ClipAcceptor();
        
        for(CSGPolygon p : quadsIn)
        {
            target.splitPolyStartingWith(p, this);
        }
        
        return target.output();
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
     * 
     * TODO: add a prediction mechanism in the CSG operations
     * to determine if it is worth calling this each time
     * The average poly reduction is between 10 and 20%, but that
     * may  not be evenly spread across all the invocations.
     * If could predict from the input meshes when it would be 
     * useful would save the cost of setup.
     */
    public List<IPolygon> recombinedRenderableQuads()
    {
        IdentityHashMap<IPolygon, Collection<CSGPolygon>> ancestorBuckets = new IdentityHashMap<>();
        
        for(CSGPolygon quad : this) 
        {
            Collection<CSGPolygon> list =  ancestorBuckets.get(quad.original);
            if(list == null)
            {
                list = new SimpleUnorderedArrayList<CSGPolygon>();
                ancestorBuckets.put(quad.original, list);
            }
            list.add(quad);
        }
        
        ArrayList<IPolygon> retVal = new ArrayList<>();
        ancestorBuckets.values().forEach((quadList) ->
        {
            for(CSGPolygon p : recombine(quadList))
            {
                p.addRenderableQuads(retVal);
            }
        });
        
        return retVal;
    }

    
    /**
     * Tries to combine two quads along the given edge. To join, all must be true:
     * 1) shared edge id
     * 2) vertexes in opposite order for each quad match each other
     * 3) quads are both inverted or both not inverted
     * 
     * NO LONGER REQUIRED...
     * 4) resulting quad has three or four vertices (Tri or Quad)
     * 5) resulting quad is convex
     * 
     * Expected now that higher-order and concave polygons will be split before they are rendered.
     * This must be handled downstream. Doing it this way may allow rejoins where intermediate
     * results would not pass these tests.<p>
     * 
     * Returns null if quads cannot be joined.
     */
//    private @Nullable CSGPolygon joinCsgPolys(CSGPolygon aQuad, CSGPolygon bQuad, long lineID)
//    {
//
//        // quads must be same orientation to be joined
//        if(aQuad.isInverted != bQuad.isInverted) return null;
//
//        final int aStartIndex = aQuad.findLineIndex(lineID);
//        // shouldn't happen, but won't work if does
//        if(aStartIndex == CSGPolygon.LINE_NOT_FOUND) 
//            return null;
//        final int aSize = aQuad.vertex.length;
//        final int aEndIndex = aStartIndex + 1 == aSize ? 0 : aStartIndex + 1;
//        final int aNextIndex = aEndIndex + 1 == aSize ? 0 : aEndIndex + 1;
//        final int aPrevIndex = aStartIndex == 0 ? aSize - 1 : aStartIndex - 1;
//
//        final int bStartIndex = bQuad.findLineIndex(lineID);
//        // shouldn't happen, but won't work if does
//        if(bStartIndex == CSGPolygon.LINE_NOT_FOUND) 
//            return null;
//        final int bSize = bQuad.vertex.length;
//        final int bEndIndex = bStartIndex + 1 == bSize ? 0 : bStartIndex + 1;
//        final int bNextIndex = bEndIndex + 1 == bSize ? 0 : bEndIndex + 1;
//        final int bPrevIndex = bStartIndex == 0 ? bSize - 1 : bStartIndex - 1;
//        
//        // confirm vertices on either end of vertex match
//        if(!aQuad.vertex[aStartIndex].isCsgEqual(bQuad.vertex[bEndIndex]))
//        {
//            return null;
//        }
//        if(!aQuad.vertex[aEndIndex].isCsgEqual(bQuad.vertex[bStartIndex]))
//        {
//            return null;
//        }
//
//        final ArrayList<Vertex> joinedVertex = new ArrayList<>(8);
//        final IntList joinedLineID = new IntArrayList(8);
//        
//        // don't try to eliminate co-linear vertices when joining two tris
//        // no cases when this is really essential and the line tests fail
//        // when joining two very small tris
//        final boolean skipLineTest = aSize == 3 && bSize == 3;
//        
//        for(int a = 0; a < aSize; a++)
//        {
//            if(a == aStartIndex)
//            {
//                //if vertex is on the same line as prev and next vertex, leave it out.
//                if(skipLineTest || !aQuad.vertex[aStartIndex].isOnLine(aQuad.vertex[aPrevIndex], bQuad.vertex[bNextIndex]))
//                {
//                    joinedVertex.add(aQuad.vertex[aStartIndex]);
//                    joinedLineID.add(bQuad.lineID[bEndIndex]);
//                }
//
//                // add b vertexes except two bQuad vertexes in common with A
//                for(int bOffset = 1; bOffset < bSize - 1; bOffset++)
//                {
//                    int b = bEndIndex + bOffset;
//                    if(b >= bSize) b -= bSize;
//                    joinedVertex.add(bQuad.vertex[b]);
//                    joinedLineID.add(bQuad.lineID[b]);
//                }
//            }
//            else if(a == aEndIndex)
//            {
//                //if vertex is on the same line as prev and next vertex, leave it out
//                //unless we are joining two tris - when we are joining very small tris
//                //the line test will fail, and not a problem to end up with a quad for
//                //when joining larg
//                if(skipLineTest || !aQuad.vertex[aEndIndex].isOnLine(aQuad.vertex[aNextIndex], bQuad.vertex[bPrevIndex]))
//                {
//                    joinedVertex.add(aQuad.vertex[aEndIndex]);
//                    joinedLineID.add(aQuad.lineID[aEndIndex]);
//                }
//            }
//            else
//            {
//                joinedVertex.add(aQuad.vertex[a]);
//                joinedLineID.add(aQuad.lineID[a]);
//           }
//        }   
//        
//        // actually build the new quad!
//        CSGPolygon joinedQuad = new CSGPolygon(aQuad.original, joinedVertex.size());
//        for(int i = 0; i < joinedVertex.size(); i++)
//        {
//            joinedQuad.vertex[i] = joinedVertex.get(i);
//            joinedQuad.lineID[i] = joinedLineID.getInt(i);
//        }
//
//        // must be convex
////        if(!joinedQuad.isConvex())
////        {
//////            HardScience.log.info("Quad not convex");
////            return null;
////        }
//        
////        if(Math.abs(aQuad.getArea() + bQuad.getArea() - joinedQuad.getArea()) > QuadFactory.EPSILON)
////        {
////            HardScience.log.info("area mismatch");
////        }
//        
//        return joinedQuad;
//        
//    }
  
    
    private Collection<CSGPolygon> recombine(Collection<CSGPolygon> quadsIn)
    {
//        quadInCount.addAndGet(quadsIn.size());   
//        recombineCounter.start();
//        Collection<CSGPolygon> result = this.recombineInner(quadsIn);
//        quadOutputCount.addAndGet(result.size());
//        if(recombineCounter.stop())
//        {
//            int in = quadInCount.get();
//            int out = quadOutputCount.get();
//            
//            ExoticMatter.INSTANCE.info("CSG Poly recombination efficiency = %d percent", ((in - out) * 100) / in );
//        } 
//        return result;
//    }
//    private static MicroTimer recombineCounter = new MicroTimer("recombinePolys", 1000000);
//    private  static AtomicInteger quadInCount = new AtomicInteger();
//    private  static AtomicInteger quadOutputCount = new AtomicInteger();
//    
//    private Collection<CSGPolygon> recombineInner(Collection<CSGPolygon> quadsIn)
//    {
        if(quadsIn.size() <= 1) return quadsIn;
        
        /**
         * Index of all polys by vertex
         */
        SetMultimap<Vertex, CSGPolygon> vertexMap = Multimaps.newSetMultimap(Maps.newIdentityHashMap(), Sets::newIdentityHashSet);
        
        quadsIn.forEach(q -> 
        {
            for(Vertex v : q.vertex)
            {
                vertexMap.put(v, q);
            }
        });
        
        
//        double totalArea = 0;
        
    
        
        /** 
         * Cleared at top of each loop and set to true if and only if 
         * new polys are created due to joins AND the line/quad/vertex map
         * has at least one new value added to it. <p>
         * 
         * The second condition avoids making another pass when all the
         * joined polys have edges that are outside edges (and thus can't
         * be joined) or the edge is no longer being tracked because fewer
         * than two polys reference it.
         */
        
        boolean potentialMatchesRemain = true;
        while(potentialMatchesRemain)
        {
            potentialMatchesRemain = false;
            
            for( java.util.Map.Entry<Vertex, Collection<CSGPolygon>> entry : vertexMap.asMap().entrySet())
            {
                // looking specifically for T junctions
                if(entry.getValue().size() == 2)
                {
                    Vertex v = entry.getKey();
                    Iterator<CSGPolygon> it = entry.getValue().iterator();
                    CSGPolygon first = it.next();
                    CSGPolygon second = it.next();
                    @Nullable CSGPolygon newPoly = joinAtVertex(v, first, second);
                    if(newPoly != null)
                    {
                        potentialMatchesRemain = true;
                        vertexMap.remove(v, first);
                        vertexMap.remove(v, second);
                        vertexMap.put(v, newPoly);
                        break;
                    }
                }
            }
//            ObjectIterator<Entry<Int2IntOpenHashMap>> it = edgeMap.int2ObjectEntrySet().fastIterator();
//            
//            while(it.hasNext())
//            {
//                Entry<Int2IntOpenHashMap> entry = it.next();
//                
//                Int2IntOpenHashMap poly2LineIndexMap = entry.getValue();
//                
//                final int lineCount = poly2LineIndexMap.size();
//                
//                if(lineCount < 2)
//                {
//                    // if one or zero polys reference this edge it cannot be joined
//                    // and thus no need to track it any longer. Subsequent operations
//                    // thus cannot assume all lineIDs will be present in the line/quad/vertex map
//                    it.remove();
//                    continue;
//                }
//                
//                int[] edgeQuadIDs = poly2LineIndexMap.keySet().toArray(new int[lineCount]);
//                
//                for(int i = 0; i < lineCount - 1; i++)
//                {
//                    for(int j = i + 1; j < lineCount; j++)
//                    {
//                        // Examining two polys that share an edge
//                        // to determine if they can be combined.
//
//                        CSGPolygon iPoly = polyMap.get(edgeQuadIDs[i]);
//                        CSGPolygon jPoly = polyMap.get(edgeQuadIDs[j]);
//                        
//                        if(iPoly == null || jPoly == null) continue;
//                        
//                        CSGPolygon joined = joinCsgPolys(iPoly, jPoly, entry.getIntKey());
//                        
//                        if(joined != null)
//                        {    
//                            // remove quads from main map
//                            polyMap.remove(iPoly.quadID);
//                            polyMap.remove(jPoly.quadID);
//                            
//                            // add quad to main map
//                            polyMap.put(joined.quadID, joined);
//
//                            // remove quads from edge map
//                            for(int n = 0; n < iPoly.vertex.length; n++)
//                            {                
//                                final int lineID = iPoly.lineID[n];
//                                // negative line ids represent outside edges - not part of map
//                                if(lineID < 0) continue;
//
//                                Int2IntOpenHashMap removeMap = edgeMap.get(lineID);
//                                // may get null entries if line only exists on one quad
//                                // and is no longer tracked - this is OK
//                                if(removeMap  != null) removeMap.remove(iPoly.quadID);
//                            }
//                            
//                            for(int n = 0; n < jPoly.vertex.length; n++)
//                            {
//                                final int lineID = jPoly.lineID[n];
//                                
//                                // negative line ids represent outside edges - not part of map
//                                if(lineID < 0) continue;
//
//                                Int2IntOpenHashMap removeMap = edgeMap.get(lineID);
//                                // may get null entries if line only exists on one quad
//                                // and is no longer tracked - this is OK
//                                if(removeMap  != null) removeMap.remove(jPoly.quadID);
//                            }                            
//                            
//                            // add quad to edge map
//                            // no new edges are created as part of this process
//                            // so we can safely assume the edge will be found
//                            // or it is no longer being tracked because only one poly uses it
//                            for(int n = 0; n < joined.vertex.length; n++)
//                            {
//                                final int lineID = joined.lineID[n];
//                                
//                                // negative line ids represent outside edges - not part of map
//                                if(lineID < 0) continue;
//
//                                Int2IntOpenHashMap addMap = edgeMap.get(lineID);
//                                if(addMap != null)
//                                {
//                                    potentialMatchesRemain = true;
//                                    addMap.put(joined.quadID, n);
//                                }
//                            }
//                        }
//                    }
//                }
//            }
            
        }
        
//        if(quadMap.size() > 1 && quadList.getFirst().face == EnumFacing.DOWN)
//        {
//            HardScience.log.info("too many");
//        }
        
//        ArrayList<ICSGPolygon> retVal = new ArrayList<>();
//        polyMap.values().forEach((p) -> retVal.addAll(p.toQuadsCSG()));
//        return retVal;
        Set<CSGPolygon> polys = Collections.newSetFromMap( new IdentityHashMap<>());
        polys.addAll(vertexMap.values());
        return polys;
    }

    private @Nullable CSGPolygon joinAtVertex(Vertex key, CSGPolygon first, CSGPolygon second)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Adds poly to this node, establishing the plane for this node
     * if it was not already determined. <p>
     * 
     * ASSUMES the given poly is coplanar with the plane of this node
     * if plane is already set.
     */
    void add(CSGPolygon poly)
    {
        this.quads.add(poly);
    }

    @Override
    public Iterator<CSGPolygon> iterator()
    {
        return new Iterator<CSGPolygon>()
        {
           Iterator<CSGNode> nodes = CSGNode.this.allNodes();
           CSGNode node = nodes.next();
           Iterator<CSGPolygon> polys = node.quads.iterator();
           @Nullable CSGPolygon next = computeNext();
           
            private CSGPolygon computeNext()
            {
                if(polys.hasNext()) return polys.next();
                
                while(nodes.hasNext())
                {
                    node = nodes.next();
                    polys = node.quads.iterator();
                    if(polys.hasNext()) return polys.next();
                }
                
                return null;
            }

            @Override
            public boolean hasNext()
            {
                return this.next != null;
            }

            @Override
            public CSGPolygon next()
            {
                CSGPolygon result = this.next;
                this.next = this.computeNext();
                return result;
            }
        };
    }
}
