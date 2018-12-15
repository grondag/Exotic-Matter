package grondag.exotic_matter.model.CSG2;
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

import grondag.exotic_matter.model.primitives.polygon.IPolygon;
import grondag.exotic_matter.model.primitives.stream.CsgPolyStream;
import grondag.exotic_matter.model.primitives.stream.IPolyStream;
import grondag.exotic_matter.model.primitives.stream.IWritablePolyStream;
import grondag.exotic_matter.model.primitives.stream.PolyStreams;

/**
 * Access point for CSG operations.  
 */
public abstract class CSG
{
    /**
     * Output a new mesh solid representing the difference of the two input meshes.
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
    public static void difference(IPolyStream a, IPolyStream b, IWritablePolyStream output)
    {
        CsgPolyStream bCSG = PolyStreams.claimCSG(b);
        CsgPolyStream intersect = PolyStreams.claimCSG();

        // add all of A outside of B bounds
        IPolygon p = a.reader();
        if(a.origin()) do
        {
            if (bCSG.intersectsWith(p))
                intersect.appendCopy(p);
            else
                output.appendCopy(p);
        } while(a.next());
        
        // add portions of A within B bounds but not inside B mesh
        intersect.invert();
        intersect.clipTo(bCSG);
        bCSG.clipTo(intersect);
        bCSG.invert();
        bCSG.clipTo(intersect);
        intersect.invert();
        
        bCSG.outputRecombinedQuads(output);
        intersect.outputRecombinedQuads(output);
        
        bCSG.release();
        intersect.release();
    }
    
    /**
     * Output a new mesh representing the intersection of two input meshes.
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
    public static void intersect(IPolyStream a, IPolyStream b, IWritablePolyStream output)
    {
        CsgPolyStream aCSG = PolyStreams.claimCSG(a);
        CsgPolyStream bCSG = PolyStreams.claimCSG(b);
        
        aCSG.invert();
        bCSG.clipTo(aCSG);
        bCSG.invert();
        aCSG.clipTo(bCSG);
        bCSG.clipTo(aCSG);
        
        aCSG.invert();
        bCSG.invert();
        
        aCSG.outputRecombinedQuads(output);
        bCSG.outputRecombinedQuads(output);

        aCSG.release();
        bCSG.release();
    }

    /**
     * Output a new mesh representing the union of the input meshes.
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
    public static void union(IPolyStream a, IPolyStream b, IWritablePolyStream output)
    {
        CsgPolyStream bCSG = PolyStreams.claimCSG(b);
        CsgPolyStream intersect = PolyStreams.claimCSG();
        
        // output all of A outside of B
        IPolygon p = a.reader();
        if(a.origin()) do
        {
            if (bCSG.intersectsWith(p))
                intersect.appendCopy(p);
            else
                output.appendCopy(p);
        } while(a.next());


        if (intersect.isEmpty())
        {
            // A and B bounds don't overlap, so output all of original b
            output.appendAll(b);
        }
        else
        {
            // some potential overlap
            // add union of the overlapping bits, 
            // which will include any parts of B that need to be included
            intersect.clipTo(bCSG);
            bCSG.clipTo(intersect);
            bCSG.invert();
            bCSG.clipTo(intersect);
            bCSG.invert();
            
            intersect.outputRecombinedQuads(output);
            bCSG.outputRecombinedQuads(output);
        } 
        
        bCSG.release();
        intersect.release();
    }
}
