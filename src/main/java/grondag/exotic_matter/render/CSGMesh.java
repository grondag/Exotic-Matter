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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.util.math.AxisAlignedBB;

/**
 * Collection of polygons with convenient semantics
 * for CSG and other group operations.  Polygons in 
 * the mesh are "stateless" from a CSG perspective - CSG
 * metadata does not live beyond/outside each CSG operation.
 */
public class CSGMesh extends ArrayList<IPolygon>
{
    private static final long serialVersionUID = 796007237565914078L;

    public <T extends IPolygon> CSGMesh(List<T> quads)
    {
        super(quads);
    }
    

    public CSGMesh()
    {
        super();
    }
    
    @Override
    public CSGMesh clone()
    {
        Stream<IPolygon> quadStream;

        if (this.size() > 200) {
            quadStream = this.parallelStream();
        } else {
            quadStream = this.stream();
        }

        return new CSGMesh(quadStream.
                map((IPolygon p) -> Poly.mutableCopyOf(p)).collect(Collectors.toList()));
    }
    
    /**
     * Randomly recolors all the quads as an aid to debugging.
     */
    public void recolor()
    {
        Stream<IPolygon> quadStream;

        if (this.size() > 200) {
            quadStream = this.parallelStream();
        } else {
            quadStream = this.stream();
        }

        quadStream.forEach((IPolygon quad) -> quad.mutableReference().replaceColor((ThreadLocalRandom.current().nextInt(0x1000000) & 0xFFFFFF) | 0xFF000000));
    }
    
    
    public CSGBounds getBounds()
    {
        if (this.isEmpty()) {
            return new CSGBounds(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }

        AxisAlignedBB retVal = null;

        for (IPolygon p : this)
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
     *
     * @param csg other csg
     *
     * @return union of this csg and the specified csg
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
     * Return a new CSG solid representing the difference of this csg and the
     * specified csg.
     *
     * <b>Note:</b> Neither this csg nor the specified csg are weighted.
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
     *
     * @param other other csg
     * @return difference of this csg and the specified csg
     */
    public CSGMesh difference(CSGMesh other) {
        List<IPolygon> inner = new ArrayList<>();
        List<IPolygon> outer = new ArrayList<>();

        CSGBounds bounds = other.getBounds();

        this.stream().forEach((p) -> {
            if (bounds.intersectsWith(p.getAABB())) {
                inner.add(p);
            } else {
                outer.add(p);
            }
        });

        CSGMesh innerCSG = new CSGMesh(inner);

        CSGMesh result = new CSGMesh();
        result.addAll(outer);
        result.addAll(innerCSG.differenceClip(other));

        return result;
    }
    
    private CSGMesh differenceClip(CSGMesh other) {

        CSGNode a = new CSGNode(this);
        CSGNode b = new CSGNode(other);

        a.invert();
        a.clipTo(b);
        b.clipTo(a);
        b.invert();
        b.clipTo(a);
        b.invert();
        a.addAll(b);
        a.invert();

        return new CSGMesh(a.recombinedRawQuads());
    }
    
    /**
     * Return a new CSG solid representing the intersection of this csg and the
     * specified csg.
     *
     * <b>Note:</b> Neither this csg nor the specified csg are weighted.
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
     * }
     * </pre></blockquote>
     *
     * @param csg other csg
     * @return intersection of this csg and the specified csg
     */
    public CSGMesh intersect(CSGMesh other)
    {
        CSGNode a = new CSGNode(this);
        CSGNode b = new CSGNode(other);
        a.invert();
        b.clipTo(a);
        b.invert();
        a.clipTo(b);
        b.clipTo(a);
        a.addAll(b);
        a.invert();
        CSGMesh retVal = new CSGMesh(a.recombinedRawQuads());
        
//        HardScience.log.info("raw count " + a.allRawQuads().size() + "   combined count " + retVal.size());

        return retVal;
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
     *
     * @param csg other csg
     *
     * @return union of this csg and the specified csg
     */
    public CSGMesh union(CSGMesh other) {
    
        
        
        List<IPolygon> inner = new ArrayList<>();
        List<IPolygon> outer = new ArrayList<>();

        CSGBounds bounds = other.getBounds();

        this.stream().forEach((p) -> {
            if (bounds.intersectsWith(p.getAABB())) {
                inner.add(p);
            } else {
                outer.add(p);
            }
        });
        
        CSGMesh result = new CSGMesh();
        
        if (!inner.isEmpty()) {
            CSGMesh innerCSG = new CSGMesh(inner);

            result.addAll(outer);
            result.addAll(innerCSG.unionClip(other));
        } else {
            result.addAll(this);
            result.addAll(other);
        }

        return result;
    }


    private CSGMesh unionClip(CSGMesh other)
    {
        CSGNode a = new CSGNode(this);
        CSGNode b = new CSGNode(other);
        
        a.clipTo(b);
        b.clipTo(a);
        b.invert();
        b.clipTo(a);
        b.invert();
        a.addAll(b);

        return new CSGMesh(a.recombinedRawQuads());
    }
}
