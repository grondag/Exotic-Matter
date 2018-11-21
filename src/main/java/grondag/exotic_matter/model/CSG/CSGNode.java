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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

import grondag.exotic_matter.model.primitives.better.IMutableGeometricVertex;
import grondag.exotic_matter.model.primitives.better.IMutablePoly;
import grondag.exotic_matter.model.primitives.vertex.Vec3f;
import grondag.exotic_matter.varia.SimpleUnorderedArrayList;
import net.minecraft.util.math.MathHelper;


public class CSGNode
{
    /**
     * RawQuads.
     */
    protected List<CSGPolygon> quads = new ArrayList<>();
    
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

    private static final float CENTER_TO_CORNER_DISTANCE = MathHelper.sqrt(2);
    private static final Vec3f CENTER_TO_CORNER_NORMAL_A = Vec3f.create(CENTER_TO_CORNER_DISTANCE, 0, CENTER_TO_CORNER_DISTANCE);
    private static final Vec3f CENTER_TO_CORNER_NORMAL_B = Vec3f.create(CENTER_TO_CORNER_DISTANCE, 0, -CENTER_TO_CORNER_DISTANCE);
    private static final Vec3f CENTER_TO_SIDE_NORMAL_A = Vec3f.create(0, 0, 1);
    private static final float CENTER_TO_SIDE_DIST_A = 0.5f;
    
    private static final Vec3f CENTER_TO_SIDE_NORMAL_B = Vec3f.create(-1, 0, 0);
    private static final float CENTER_TO_SIDE_DIST_B = -0.5f;
    
    public static class Root extends CSGNode implements Iterable<CSGPolygon>
    {
        final CSGSplitAcceptor.CoFrontBack splitter = new CSGSplitAcceptor.CoFrontBack();
        
        @SuppressWarnings("null")
        private Root(Collection<IMutablePoly> shapeIn, boolean crossSplit)
        {
            super(new CSGPlane(CENTER_TO_CORNER_NORMAL_A, CENTER_TO_CORNER_DISTANCE));
            this.front = new CSGNode(new CSGPlane(CENTER_TO_CORNER_NORMAL_B, 0));
            this.back = new CSGNode(new CSGPlane(CENTER_TO_CORNER_NORMAL_B, 0));
            
            if(crossSplit)
            {
                this.front.front = new CSGNode(new CSGPlane(CENTER_TO_SIDE_NORMAL_B, CENTER_TO_SIDE_DIST_B));
                this.front.back = new CSGNode(new CSGPlane(CENTER_TO_SIDE_NORMAL_A, CENTER_TO_SIDE_DIST_A));
                this.back.front = new CSGNode(new CSGPlane(CENTER_TO_SIDE_NORMAL_A, CENTER_TO_SIDE_DIST_A));
                this.back.back = new CSGNode(new CSGPlane(CENTER_TO_SIDE_NORMAL_B, CENTER_TO_SIDE_DIST_B));
            }

            shapeIn.forEach(p -> splitter.splitPolyStartingWith(new CSGPolygon(p), this));
        }
        
        private Root(Collection<IMutablePoly> shapeIn)
        {
            super(new CSGPolygon(shapeIn.iterator().next()));
            
            Iterator<IMutablePoly> it = shapeIn.iterator();
            it.next();
            while(it.hasNext())
            {
                splitter.splitPolyStartingWith(new CSGPolygon(it.next()), this);
            }
        }
        
        private Root(CSGPlane plane)
        {
            super(plane);
        }
        
        @Override
        public CSGNode.Root clone()
        {
            throw new UnsupportedOperationException();
//            //TODO: if serious about this need to do it without recursion
//            CSGNode.Root result = new CSGNode.Root(this.plane.clone());
//            if(this.front != null) result.front = this.front.clone();
//            if(this.back != null) result.back = this.back.clone();
//            this.quads.forEach(q -> result.quads.add(q.clone()));
//            return result;
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
                   stack.push(Root.this);
                   next = computeNext();
               }
               
                protected @Nullable CSGNode computeNext()
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
        protected void addAll(Iterable<CSGPolygon> polys)
        {
            for(CSGPolygon p : polys)
            {
                splitter.splitPolyStartingWith(p, this);
            }
        }

        public void addPolygon(IMutablePoly poly)
        {
            splitter.splitPolyStartingWith(new CSGPolygon(poly), this);
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
        public void clipTo(CSGNode.Root bsp)
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
         * All quads/vertices will be of same (mutable) type as input quads.
         * Will hold no reference to quads in the list.
         */
        @SuppressWarnings("unchecked")
        public List<IMutablePoly> recombinedQuads()
        {
            IdentityHashMap<IMutablePoly, Object> ancestorBuckets = new IdentityHashMap<>();
            
            for(CSGPolygon quad : this) 
            {
                Object bucket =  ancestorBuckets.get(quad.original);
                if(bucket == null)
                {
                    ancestorBuckets.put(quad.original, quad);
                }
                else if(bucket instanceof SimpleUnorderedArrayList)
                {
                    ((SimpleUnorderedArrayList<CSGPolygon>) bucket).add(quad);
                }
                else
                {
                    SimpleUnorderedArrayList<CSGPolygon> list = new SimpleUnorderedArrayList<CSGPolygon>();
                    list.add((CSGPolygon) bucket);
                    list.add(quad);
                    ancestorBuckets.put(quad.original, list);
                }
            }
            
            
            // PERF: use threadlocal
            ArrayList<IMutablePoly> retVal = new ArrayList<>();
            ancestorBuckets.values().forEach((bucket) ->
            {
                if(bucket instanceof SimpleUnorderedArrayList)
                {
                    recombinedAndAddRenderableToList(((SimpleUnorderedArrayList<CSGPolygon>) bucket), retVal);
                }
                else
                {
                    ((CSGPolygon)bucket).addRenderableQuads(retVal);
                }
            });
            
            return retVal;
        }
        
        @Override
        public Iterator<CSGPolygon> iterator()
        {
            return new Iterator<CSGPolygon>()
            {
               Iterator<CSGNode> nodes = Root.this.allNodes();
               CSGNode node = nodes.next();
               Iterator<CSGPolygon> polys = node.quads.iterator();
               @Nullable CSGPolygon next = computeNext();
               
                private @Nullable CSGPolygon computeNext()
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

        public void release()
        {
            // TODO Release claimed mutables
        }
    }
    
    /**
     * Returns root node of BSP tree with given polygons. <p>
     * 
     * Polygons must be coplanar. (not  validated)<br>
     * Input collection must have at least one polygon or will crash.
     * Polygons in the input are copied so that nothing is mutated.<br>
     */
    public static Root create(Collection<IMutablePoly> shapeIn, boolean crossSplit)
    {
        return new Root(shapeIn, crossSplit);
    }
    
    public static Root create(Collection<IMutablePoly> shapeIn)
    {
        return new Root(shapeIn);
    }

    CSGNode(CSGPlane plane)
    { 
        this.plane = plane;
    }
    
    CSGNode(CSGPolygon firstPoly)
    { 
        this(new CSGPlane(firstPoly));
        this.quads.add(firstPoly);        
    }

    @Override
    protected CSGNode clone()
    {
        throw new UnsupportedOperationException();
//        //TODO: if serious about this need to do it without recursion
//        CSGNode result = new CSGNode(this.plane.clone());
//        if(this.front != null) result.front = this.front.clone();
//        if(this.back != null) result.back = this.back.clone();
//        this.quads.forEach(q -> result.quads.add(q.clone()));
//        return result;
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
     * Tries to combine two quads that share the given vertex. To join, all must be true:
     * 1) shared edge id
     * 2) vertexes in opposite order for each quad match each other
     * 3) quads are both inverted or both not inverted
     * 
     * NO LONGER REQUIRED...
     * 4) resulting quad has three or four vertices (Tri or Quad)
     * 5) resulting quad is convex
     * 
     * 
     * Returns null if quads cannot be joined.
     */
    private static @Nullable CSGPolygon joinAtVertex(IMutableGeometricVertex key, CSGPolygon aQuad, CSGPolygon bQuad)
    {
        // quads must be same orientation to be joined
        if(aQuad.isInverted != bQuad.isInverted) return null;

        final int aTargetIndex = aQuad.indexForVertex(key);
        // shouldn't happen, but won't work if does
        if(aTargetIndex == CSGPolygon.VERTEX_NOT_FOUND) 
            return null;
        
        final int bTargetIndex = bQuad.indexForVertex(key);
        // shouldn't happen, but won't work if does
        if(bTargetIndex == CSGPolygon.VERTEX_NOT_FOUND) 
            return null;

        final int aSize = aQuad.vertex.length;
        final int bSize = bQuad.vertex.length;
        final int aMaxIndex = aSize - 1;
        final int bMaxIndex = bSize - 1;

        final int aAfterTargetIndex = aTargetIndex == aMaxIndex ? 0 : aTargetIndex + 1;
        final int aBeforeTargetIndex = aTargetIndex == 0 ? aMaxIndex : aTargetIndex - 1;

        final int bAfterTargetIndex = bTargetIndex == bMaxIndex ? 0 : bTargetIndex + 1;
        final int bBeforeTargetIndex = bTargetIndex == 0 ? bMaxIndex : bTargetIndex - 1;
        
        /** Shared vertex that comes first on A polygon, is second shared vertex on B */
        int aFirstSharedIndex;
        
        /** Shared vertex that comes first on B polygon, is second shared vertex on A */
        int bFirstSharedIndex;
       
        /** Shared vertex that comes second on A polygon, is first shared vertex on B. */
        int aSecondSharedIndex;
        
        /** Shared vertex that comes second on B polygon, is first shared vertex on A. */
        int bSecondSharedIndex;
        
        /** Vertex on A polygon before the first shared A vertex */
        int aBeforeSharedIndex;
        
        /** Vertex on B polygon before the first shared B vertex */
        int bBeforeSharedIndex;
        
        /** Vertex on A polygon after the second shared A vertex */
        int aAfterSharedIndex;
        
        /** Vertex on B polygon after the second shared B vertex */
        int bAfterSharedIndex;

        // look for a second matching vertex on either side of known shared vertex
        if(aQuad.vertex[aAfterTargetIndex] == bQuad.vertex[bBeforeTargetIndex])
        {
            aFirstSharedIndex = aTargetIndex;
            aSecondSharedIndex = aAfterTargetIndex;
            bFirstSharedIndex = bBeforeTargetIndex;
            bSecondSharedIndex = bTargetIndex;
            aBeforeSharedIndex = aBeforeTargetIndex;
            bBeforeSharedIndex = bFirstSharedIndex == 0 ? bMaxIndex : bFirstSharedIndex - 1;
            aAfterSharedIndex = aSecondSharedIndex == aMaxIndex ? 0 : aSecondSharedIndex + 1;
            bAfterSharedIndex = bAfterTargetIndex;
        }
        else if(aQuad.vertex[aBeforeTargetIndex] == bQuad.vertex[bAfterTargetIndex])
        {
            aFirstSharedIndex = aBeforeTargetIndex;
            aSecondSharedIndex = aTargetIndex;
            bFirstSharedIndex =  bTargetIndex;
            bSecondSharedIndex = bAfterTargetIndex;
            aBeforeSharedIndex = aFirstSharedIndex == 0 ? aMaxIndex : aFirstSharedIndex - 1;
            bBeforeSharedIndex = bBeforeTargetIndex;
            aAfterSharedIndex = aAfterTargetIndex;
            bAfterSharedIndex = bSecondSharedIndex == bMaxIndex ? 0 : bSecondSharedIndex + 1;
        }
        else
        {
            return null;
        }
        
        IMutableGeometricVertex[] joinedVertex = new IMutableGeometricVertex[aSize + bSize - 2];
        int joinedSize = 0;
        
        for(int a = 0; a < aSize; a++)
        {
            
            if(a == aFirstSharedIndex)
            {
                //if vertex is on the same line as prev and next vertex, leave it out.
                if(!aQuad.vertex[aFirstSharedIndex].isOnLine(aQuad.vertex[aBeforeSharedIndex], bQuad.vertex[bAfterSharedIndex]))
                {
                    joinedVertex[joinedSize++] = aQuad.vertex[a];
                }

                // add b vertexes except two bQuad vertexes in common with A
                for(int b = 0; b < bSize - 2; b++)
                {
                    int bIndex = bAfterSharedIndex + b;
                    if(bIndex > bMaxIndex) bIndex -= bSize;
                    joinedVertex[joinedSize++] = bQuad.vertex[bIndex];
                }
            }
            else if(a == aSecondSharedIndex)
            {
                //if vertex is on the same line as prev and next vertex, leave it out
                if(!aQuad.vertex[aSecondSharedIndex].isOnLine(aQuad.vertex[aAfterSharedIndex], bQuad.vertex[bBeforeSharedIndex]))
                {
                    joinedVertex[joinedSize++] = aQuad.vertex[a];
                }
            }
            else
            {
                joinedVertex[joinedSize++]  = aQuad.vertex[a];
           }
        }   
        
        if(joinedSize < 3)
        {
            assert false : "Bad polygon formation during CSG recombine.";
            return null;
        }
        
        // actually build the new quad!
        CSGPolygon joinedQuad = new CSGPolygon(aQuad, joinedSize);
        System.arraycopy(joinedVertex, 0, joinedQuad.vertex, 0, joinedSize);
        
        return joinedQuad;
        
    }
    
    @SuppressWarnings("null")
    private static void recombinedAndAddRenderableToList(Collection<CSGPolygon> quadsIn, List<IMutablePoly> output)
    {
        if(quadsIn.size() == 2) 
        {
            recombinedAndAddRenderableToListPairwise(quadsIn, output);
            return;
        }

        /**
         * Index of all polys by vertex
         */
        IdentityHashMap<IMutableGeometricVertex, SimpleUnorderedArrayList<CSGPolygon>> vertexMap = new IdentityHashMap<>();
        
        quadsIn.forEach(q ->  addPolyToVertexMap(vertexMap, q));
        
        Set<CSGPolygon> polys = Collections.newSetFromMap(new IdentityHashMap<>());
        polys.addAll(quadsIn);
        
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
            
            Iterator<Entry<IMutableGeometricVertex, SimpleUnorderedArrayList<CSGPolygon>>> it = vertexMap.entrySet().iterator();
            while(it.hasNext())
            {
                Entry<IMutableGeometricVertex, SimpleUnorderedArrayList<CSGPolygon>> entry = it.next();
                SimpleUnorderedArrayList<CSGPolygon> bucket = entry.getValue();
                if(bucket.size() < 2)
                {
                    // nothing to simplify here
                    it.remove();
                }
                else if(bucket.size() == 2)
                {
                    // eliminate T junctions
                    IMutableGeometricVertex v = entry.getKey();
                    CSGPolygon first = bucket.get(0);
                    CSGPolygon second = bucket.get(1);
                    @Nullable CSGPolygon newPoly = joinAtVertex(v, first, second);
                    if(newPoly != null)
                    {
                        potentialMatchesRemain = true;
                        // we won't see a CME because not removing any vertices at this point except via the iterator
                        it.remove();
                        
                        polys.remove(first);
                        removePolyFromVertexMap(vertexMap, first, v);
                        
                        polys.remove(second);
                        removePolyFromVertexMap(vertexMap, second, v);
                        
                        polys.add(newPoly);
                        addPolyToVertexMapGently(vertexMap, newPoly);
                    }
                }
            }
        }
        polys.forEach(p -> p.addRenderableQuads(output));
    }

    /**
     * Handles special case when there are only 2 polygons to join - avoids building a map.
     * For volcano terrain, this is about 15% of the cases involving a potential join (more than 1 poly)
     */
    private static void recombinedAndAddRenderableToListPairwise(Collection<CSGPolygon> quadsIn, List<IMutablePoly> output)
    {
//        pairCount.incrementAndGet();
        Iterator<CSGPolygon> it = quadsIn.iterator();
        CSGPolygon polyA = it.next();
        CSGPolygon polyB = it.next();
        
        for(IMutableGeometricVertex vA : polyA.vertex)
        {
            for(IMutableGeometricVertex vB : polyB.vertex)
            {
                if(vA == vB)
                {
                    @Nullable CSGPolygon newPoly = joinAtVertex(vA, polyA, polyB);
                    if(newPoly == null)
                        break;
                    else
                    {
                        newPoly.addRenderableQuads(output);
                        return;
                    }
                }
            }
        }
        polyA.addRenderableQuads(output);
        polyB.addRenderableQuads(output);
    }
            
        
    private static void removePolyFromVertexMap(IdentityHashMap<IMutableGeometricVertex, SimpleUnorderedArrayList<CSGPolygon>> vertexMap, CSGPolygon poly, IMutableGeometricVertex excludingVertex )
    {
        for(IMutableGeometricVertex v : poly.vertex)
        {
            if(v == excludingVertex) continue;
            SimpleUnorderedArrayList<CSGPolygon> bucket = vertexMap.get(v);
            if(bucket == null) continue;
            bucket.removeIfPresent(poly);
        }
    }
    
    private static void addPolyToVertexMap(IdentityHashMap<IMutableGeometricVertex, SimpleUnorderedArrayList<CSGPolygon>> vertexMap, CSGPolygon poly )
    {
        for(IMutableGeometricVertex v : poly.vertex)
        {
            SimpleUnorderedArrayList<CSGPolygon> bucket = vertexMap.get(v);
            if(bucket == null)
            {
                bucket = new SimpleUnorderedArrayList<>();
                vertexMap.put(v, bucket);
            }
            bucket.add(poly);
        }
    }
    
    /**
     * For use during second phase of combined - will not create buckets that are not found.
     * Assumes these have been deleted because only had a single poly in them.
     */
    private static void addPolyToVertexMapGently(IdentityHashMap<IMutableGeometricVertex, SimpleUnorderedArrayList<CSGPolygon>> vertexMap, CSGPolygon poly )
    {
        for(IMutableGeometricVertex v : poly.vertex)
        {
            SimpleUnorderedArrayList<CSGPolygon> bucket = vertexMap.get(v);
            if(bucket != null) bucket.add(poly);
        }
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

}
