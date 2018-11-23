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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import grondag.exotic_matter.model.primitives.better.IPaintablePoly;
import grondag.exotic_matter.model.primitives.better.IPaintedPoly;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Access point for CSG operations.  
 */
public abstract class CSGMesh
{
    // TODO: reduce garbagification by releasing or reusing any mutables that can't escape
    // TODO: ensure no references retained to any output
    
    public static CSGBounds getBounds(Collection<? extends IPaintedPoly> forPolygons)
    {
        if (forPolygons.isEmpty()) {
            return new CSGBounds(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }

        AxisAlignedBB retVal = null;

        for (IPaintedPoly p : forPolygons)
        {
            if(retVal == null)
            {
                retVal = p.getAABB();
            }
            else
            {
                retVal = retVal.union(p.getAABB());
            }
        }

        return retVal == null 
                ? new CSGBounds(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
                : new CSGBounds(retVal.minX, retVal.minY, retVal.minZ, retVal.maxX, retVal.maxY, retVal.maxZ);
    }
    
    /**
     * Return a new CSG solid representing the union of this csg and the
     * specified csg.
     *
     * <b>Note:</b> Neither this csg nor the specified csg are weighted.
     *
     * <blockquote><pre>
     *    A.union(B)
     *
     *    +-------+            +-------+
     *    |       |            |       |
     *    |   A   |            |       |
     *    |    +--+----+   =   |       +----+
     *    +----+--+    |       +----+       |
     *         |   B   |            |       |
     *         |       |            |       |
     *         +-------+            +-------+
     * </pre></blockquote>
     *
     */
//    public CSG union(CSG csg) {
//
//        switch (getOptType()) {
//            case CSG_BOUND:
//                return _unionCSGBoundsOpt(csg);
//            case POLYGON_BOUND:
//                return _unionPolygonBoundsOpt(csg);
//            default:
////                return _unionIntersectOpt(csg);
//                return _unionNoOpt(csg);
//        }
//    }
    
    /**
     * Return a new mesh solid representing the difference of the two input meshes.
     *
     * <blockquote><pre>
     * A.difference(B)
     *
     * +-------+            +-------+
     * |       |            |       |
     * |   A   |            |       |
     * |    +--+----+   =   |    +--+
     * +----+--+    |       +----+
     *      |   B   |
     *      |       |
     *      +-------+
     * </pre></blockquote>
     */
    public static Collection<IPaintablePoly> difference(Collection<? extends IPaintedPoly> a, Collection<? extends IPaintedPoly> b)
    {
     // PERF: use threadlocals
        List<IPaintablePoly> inner = new ArrayList<>();
        List<IPaintablePoly> outer = new ArrayList<>();

        CSGBounds bounds = getBounds(b);

        a.stream().forEach((p) ->
        {
            if (bounds.intersectsWith(p.getAABB()))
            {
                inner.add(p.claimCopy());
            } else
            {
                outer.add(p.claimCopy());
            }
        });

        outer.addAll(differenceClip(inner, b));
        return outer;
    }
    
    private static Collection<IPaintablePoly> differenceClip(Collection<? extends IPaintedPoly> aPolys, Collection<? extends IPaintedPoly> bPolys)
    {

        CSGNode.Root a = CSGNode.create(aPolys);
        CSGNode.Root b = CSGNode.create(bPolys);

        a.invert();
        a.clipTo(b);
        b.clipTo(a);
        b.invert();
        b.clipTo(a);
        b.invert();
        a.addAll(b);
        a.invert();

        return a.recombinedQuads();
    }
    
    /**
     * Return a new mesh representing the intersection of two input meshes.
     *
     * <blockquote><pre>
     *     A.intersect(B)
     *
     *     +-------+
     *     |       |
     *     |   A   |
     *     |    +--+----+   =   +--+
     *     +----+--+    |       +--+
     *          |   B   |
     *          |       |
     *          +-------+
     * </pre></blockquote>     
     */
    public static Collection<IPaintablePoly> intersect(Collection<? extends IPaintedPoly> aMesh, Collection<? extends IPaintedPoly> bMesh)
    {
        CSGNode.Root a = CSGNode.create(aMesh);
        CSGNode.Root b = CSGNode.create(bMesh);
        
        Collection<IPaintablePoly> result = intersect(a, b);
        a.release();
        b.release();
        return result;
    }
    
    public static Collection<IPaintablePoly> intersect(CSGNode.Root a, CSGNode.Root b)
    {
        a.invert();
        b.clipTo(a);
        b.invert();
        a.clipTo(b);
        b.clipTo(a);
        a.addAll(b);
        a.invert();
        return a.recombinedQuads();
    }

    /**
     * Return a new mesh representing the union of the input meshes.
     *
     * <blockquote><pre>
     *    A.union(B)
     *
     *    +-------+            +-------+
     *    |       |            |       |
     *    |   A   |            |       |
     *    |    +--+----+   =   |       +----+
     *    +----+--+    |       +----+       |
     *         |   B   |            |       |
     *         |       |            |       |
     *         +-------+            +-------+
     * </pre></blockquote>
     *
     */
    public static Collection<IPaintablePoly> union(Collection<? extends IPaintedPoly> aMesh, Collection<? extends IPaintedPoly> bMesh)
    {
        // PERF: use threadlocals
        List<IPaintablePoly> inner = new ArrayList<>();
        List<IPaintablePoly> outer = new ArrayList<>();

        CSGBounds bounds = getBounds(bMesh);

        aMesh.stream().forEach((p) ->
        {
            if (bounds.intersectsWith(p.getAABB())) 
            {
                inner.add(p.claimCopy());
            } else
            {
                outer.add(p.claimCopy());
            }
        });
        
        List<IPaintablePoly> result = new ArrayList<>();
        
        // always add all of A that is outside B
        result.addAll(outer);

        if (!inner.isEmpty())
        {
            // some potential overlap
            // add union of the overlapping bits, 
            // which will include any parts of B that need to be included
            result.addAll(unionClip(inner, bMesh));
            
            // TODO: release inner?
        } 
        else
        {
            // no overlap, so add all of b
            bMesh.stream().forEach((p) -> result.add(p.claimCopy()));
        }
        
        return result;
    }


    private static Collection<IPaintablePoly> unionClip(Collection<? extends IPaintedPoly> aMesh, Collection<? extends IPaintedPoly> bMesh)
    {
        CSGNode.Root a = CSGNode.create(aMesh);
        CSGNode.Root b = CSGNode.create(bMesh);
        
        a.clipTo(b);
        b.clipTo(a);
        b.invert();
        b.clipTo(a);
        b.invert();
        a.addAll(b);
        
        Collection<IPaintablePoly> result = a.recombinedQuads();
        a.release();
        b.release();
        return result;
    }
}
