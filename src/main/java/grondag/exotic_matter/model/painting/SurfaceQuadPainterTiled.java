package grondag.exotic_matter.model.painting;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import grondag.exotic_matter.model.primitives.IMutablePolygon;
import grondag.exotic_matter.model.primitives.IPolygon;
import grondag.exotic_matter.model.primitives.Poly;
import grondag.exotic_matter.model.primitives.QuadHelper;
import grondag.exotic_matter.model.primitives.Vertex;
import grondag.exotic_matter.model.state.ISuperModelState;
import grondag.exotic_matter.model.texture.TextureRotationType;
import grondag.exotic_matter.varia.Useful;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;

/**
 * 
 * See {@link SurfaceTopology#TILED}
 */
public class SurfaceQuadPainterTiled extends QuadPainter
{
    public SurfaceQuadPainterTiled(ISuperModelState modelState, Surface surface, PaintLayer paintLayer)
    {
        super(modelState, surface, paintLayer);
    }

    @Override
    public void paintQuad(IMutablePolygon quad, Consumer<IPolygon> target, boolean isItem)
    {
        assert !quad.isLockUV() : "Tiled surface quad painter received quad with lockUV semantics.  Not expected";
        
        final boolean uFlipped = quad.getMaxU() < quad.getMinU();
        final boolean vFlipped = quad.getMaxV() < quad.getMinV();
        
        final Surface surfIn = quad.getSurfaceInstance();
        final EnumFacing face = quad.getNominalFace();
        final int salt = (surfIn.textureSalt << 3) + (face == null ? 0 : face.ordinal());
        
        /**
         * The smallest UV distance that can be tiled with single texture.
         * Equivalently, the size of each tile in the tiling grid.
         * If no wrap, is simply the texture size.  If we wrap, then
         * pick the distance that gives the texture scaling closest to 1:1.
         * 
         */
        final float tilingDistance = tilingDistance(surfIn.uvWrapDistance, this.texture.textureScale().sliceCount);
        
        /**
         * See if we will need to split the quad on a UV boundary.<p>
         * 
         * Test on span > tile distance isn't sufficient because uv span might fit within a single
         * texture tile distance but actually start or end mid-texture.<p>
         */
        
        final int uMinIndex = uFlipped 
                ? MathHelper.ceil(quad.getMinU() / tilingDistance)
                : MathHelper.floor(quad.getMinU() / tilingDistance);
        
        final int uMaxIndex = uFlipped 
                ? MathHelper.floor(quad.getMaxU() / tilingDistance)
                : MathHelper.ceil(quad.getMaxU() / tilingDistance);

        final int vMinIndex = vFlipped 
                ? MathHelper.ceil(quad.getMinV() / tilingDistance)
                : MathHelper.floor(quad.getMinV() / tilingDistance);
        
        final int vMaxIndex = vFlipped 
                ? MathHelper.floor(quad.getMaxV() / tilingDistance)
                : MathHelper.ceil(quad.getMaxV() / tilingDistance);
        
        final int uStep = uFlipped ? -1 : 1;
        final int vStep = vFlipped ? -1 : 1;
        
        final float uSpan = uStep * tilingDistance;
        final float vSpan = vStep * tilingDistance;
            
        IMutablePolygon uRemainder = quad;
        
        // do the splits
        for(int uIndex = uMinIndex; uIndex != uMaxIndex;  uIndex += uStep)
        {
            final int uIndexFinal = uIndex;
            final float uSplitLow = uIndexFinal * tilingDistance;
            
            uRemainder = splitU(uRemainder, uSplitLow, uSpan, new Consumer<IMutablePolygon>()
            {
                @SuppressWarnings("null")
                @Override
                public void accept(IMutablePolygon t)
                {
                    IMutablePolygon vRemainder = t;
                    for(int vIndex = vMinIndex; vIndex != vMaxIndex;  vIndex += vStep)
                    {
                        final int vIndexFinal = vIndex;
                        final float vSplitLow = vIndexFinal * tilingDistance;
                        
                        vRemainder = splitV(vRemainder, vSplitLow, vSpan, new Consumer<IMutablePolygon>()
                        {
                            @Override
                            public void accept(IMutablePolygon t)
                            {
                                // send for final painting and output
                                paintBoundedQuad(t, target, isItem, salt(salt, uIndexFinal, vIndexFinal));
                            }
                        });
                        
                        if(vRemainder == null) break;
                    }
                }
            });
            
            if(uRemainder == null) break;
        }
    }
    
    /**
     * The smallest UV distance that can be tiled with single texture.
     * Equivalently, the size of each tile in the tiling grid.
     * If no wrap, is simply the texture size.  If we wrap, then
     * pick the distance that gives the texture scaling closest to 1:1.
     * 
     */
    private float tilingDistance(float uvWrapDistance, int textureScale)
    {
        // if wrap disabled use texture scale and paint at 1:1
        if(uvWrapDistance <= QuadHelper.EPSILON) return textureScale;
        
        //if texture is larger than wrap distance, must scale down to the wrap distance
        if(textureScale > uvWrapDistance) return uvWrapDistance;
        
        /*
         * Examples
         * Wrap = 6, texScale = 2, divisor = 3, -> 2
         * Wrap = 7, texScale = 2, divisor = 3, -> 7/3
         * Wrap = 7, texScale = 4, divisor = 2, -> 7/2
         */
        
        // subtract epsilon because want to favor rounding down at midpoint - fewer polygons
        return uvWrapDistance / Math.round((uvWrapDistance / textureScale) - QuadHelper.EPSILON);
    }
    
    /**
     * Slices off a quad between low bound + span, with the assumption that 
     * no vertices are below the lower bound and sends the output to the consumer.<p>  
     * 
     * Output quads will have uMin/uMax of 0,1 correpsondint to the given split bounds
     * and all vertices will be scaled to that range. This will be true even for quads 
     * that covered a multiple or fraction of a 0-1 range, or which were offset. <p>
     * 
     * Returns a new quad without the sliced-off portion. (IOW, with new interpolated vertexes.)
     * This remainder quad will have the same uMin/uMax as the input quad, and all
     * vertices will remain scaled to that range.<p>
     * 
     * If all input vertices are within the given split bounds, will output a single quad
     * offset and scaled as if it had been sliced, and return null.
     */
    private @Nullable IMutablePolygon splitU(IMutablePolygon input, final float uSplitLow, final float uSpan, Consumer<IMutablePolygon> output)
    {
        final float uMin = input.getMinU();
        final float uMax = input.getMaxU();
        final int vCountIn = input.vertexCount();        
        final float[] vertexU = new float[vCountIn];
        final boolean flipped = uSpan < 0;
        
        /** points fully within the slice */
        int sliceCount = 0;
        /** points fully within the remainder */
        int remainderCount = 0;
        
        for(int i = 0; i < vCountIn; i++)
        {
            // vertex u where 0 corresponds to low split and 1 to high split
            final float u = (uMin + input.getVertex(i).u * (uMax - uMin) - uSplitLow) / uSpan;
            vertexU[i] = u;
            
            final int t = vertexType(u);
            if(t == REMAINDER)
                remainderCount++;
            else if(t == SLICE)
                sliceCount++;
        }
        
        // if nothing to slice return unmodified; no output to consumer
        if(sliceCount == 0)
            return input;
        
        // if this is the last slice,  bring into 0-1 min/max and send to output
        if(remainderCount == 0)
        {
            assert sliceCount > 0 : "degenerate u split - no remainder and no quads in slice";
            
            IMutablePolygon slice = Poly.mutableCopyOf(input);
            slice.setMinU(flipped ? 1 : 0);
            slice.setMaxU(flipped ? 0 : 1);
            
            for(int i = 0; i < vCountIn; i++)
            {
                Vertex v = slice.getVertex(i);
                slice.setVertex(i, v.withUV(vertexU[i], v.v));
            }
            output.accept(slice);
            return null;
        }
        
        // if we get to here, take slice and return remainder
        
        /** point on 0-1 on input vertex scale that separates slice and remainder */
        final float vertexSplitU = (uSplitLow + uSpan) / (uMax - uMin);
        
        IMutablePolygon slice = Poly.mutable(input, sliceCount + 2);
        slice.setMinU(flipped ? 1 : 0);
        slice.setMaxU(flipped ? 0 : 1);
        int iSliceVertex = 0;
        
        IMutablePolygon remainder = Poly.mutable(input, remainderCount + 2);
        int iRemainderVertex = 0;
        
        float uThis = vertexU[vCountIn - 1];
        Vertex thisVertex = input.getVertex(vCountIn -1);
        int thisType = vertexType(uThis);
                
        for(int iNext = 0; iNext < vCountIn; iNext++)
        {
            final Vertex nextVertex = input.getVertex(iNext);
            final float uNext = vertexU[iNext];
            final int nextType = vertexType(uNext);
            
            if(thisType == EDGE)
            {
                slice.setVertex(iSliceVertex++, thisVertex.withUV(uThis, thisVertex.v));
                remainder.setVertex(iRemainderVertex++, thisVertex);
            }
            else if(thisType == SLICE)
            {
                slice.setVertex(iSliceVertex++, thisVertex.withUV(uThis, thisVertex.v));
                if(nextType == REMAINDER)
                {
                    final float dist = (vertexSplitU - thisVertex.u) / (nextVertex.u - thisVertex.u);
                    Vertex vNew = thisVertex.interpolate(nextVertex, dist);
                    remainder.setVertex(iRemainderVertex++, vNew);
                    
                    final float uNew = (uMin + vNew.u * (uMax - uMin) - uSplitLow) / uSpan;
                    slice.setVertex(iSliceVertex++, vNew.withUV(uNew, vNew.v));
                }
            }
            else
            {
                remainder.setVertex(iRemainderVertex++, thisVertex);
                if(nextType == SLICE)
                {
                    final float dist = (vertexSplitU - thisVertex.u) / (nextVertex.u - thisVertex.u);
                    Vertex vNew = thisVertex.interpolate(nextVertex, dist);
                    remainder.setVertex(iRemainderVertex++, vNew);
                    
                    final float uNew = (uMin + vNew.u * (uMax - uMin) - uSplitLow) / uSpan;
                    slice.setVertex(iSliceVertex++, vNew.withUV(uNew, vNew.v));
                }
            }
            
            uThis = uNext;
            thisVertex = nextVertex;
            thisType = nextType;
        }
        
        output.accept(slice);
        return remainder;
        
    }
    
    private static final int EDGE = 0;
    private static final int REMAINDER = 1;
    private static final int SLICE = 2;
    
    private final int vertexType(float uvCoord)
    {
        if(uvCoord >= 1 - QuadHelper.EPSILON)
        {
            if(uvCoord <= 1 + QuadHelper.EPSILON)
                return EDGE;
            else
                return REMAINDER;
        }
        else
        {
            // < 1-QuadHelper.EPSILON
            return SLICE;
        }
    }
    
    /**
     * Just like {@link #splitU(IMutablePolygon, float, float, Consumer)} but for the v dimension.
     */
    private @Nullable IMutablePolygon splitV(IMutablePolygon input, float vSplitLow, float vSpan, Consumer<IMutablePolygon> output)
    {
        final float vMin = input.getMinV();
        final float vMax = input.getMaxV();
        final int vCountIn = input.vertexCount();        
        final float[] vertexV = new float[vCountIn];
        final boolean flipped = vSpan < 0;
        
        /** points fully within the slice */
        int sliceCount = 0;
        /** points fully within the remainder */
        int remainderCount = 0;
        
        for(int i = 0; i < vCountIn; i++)
        {
            // vertex u where 0 corresponds to low split and 1 to high split
            final float v = (vMin + input.getVertex(i).v * (vMax - vMin) - vSplitLow) / vSpan;
            vertexV[i] = v;
            
            final int t = vertexType(v);
            if(t == REMAINDER)
                remainderCount++;
            else if(t == SLICE)
                sliceCount++;
        }
        
        // if nothing to slice return unmodified; no output to consumer
        if(sliceCount == 0)
            return input;
        
        // if this is the last slice,  bring into 0-1 min/max and send to output
        if(remainderCount == 0)
        {
            assert sliceCount > 0 : "degenerate u split - no remainder and no quads in slice";
            
            IMutablePolygon slice = Poly.mutableCopyOf(input);
            slice.setMinV(flipped ? 1 : 0);
            slice.setMaxV(flipped ? 0 : 1);
            
            for(int i = 0; i < vCountIn; i++)
            {
                Vertex v = slice.getVertex(i);
                slice.setVertex(i, v.withUV(v.u, vertexV[i]));
            }
            output.accept(slice);
            return null;
        }
        
        // if we get to here, take slice and return remainder
        
        /** point on 0-1 on input vertex scale that separates slice and remainder */
        final float vertexSplitV = (vSplitLow + vSpan) / (vMax - vMin);
        
        IMutablePolygon slice = Poly.mutable(input, sliceCount + 2);
        slice.setMinV(flipped ? 1 : 0);
        slice.setMaxV(flipped ? 0 : 1);
        int iSliceVertex = 0;
        
        IMutablePolygon remainder = Poly.mutable(input, remainderCount + 2);
        int iRemainderVertex = 0;
        
        float vThis = vertexV[vCountIn - 1];
        Vertex thisVertex = input.getVertex(vCountIn -1);
        int thisType = vertexType(vThis);
                
        for(int iNext = 0; iNext < vCountIn; iNext++)
        {
            final Vertex nextVertex = input.getVertex(iNext);
            final float vNext = vertexV[iNext];
            final int nextType = vertexType(vNext);
            
            if(thisType == EDGE)
            {
                slice.setVertex(iSliceVertex++, thisVertex.withUV(thisVertex.u, vThis));
                remainder.setVertex(iRemainderVertex++, thisVertex);
            }
            else if(thisType == SLICE)
            {
                slice.setVertex(iSliceVertex++, thisVertex.withUV(thisVertex.u, vThis));
                if(nextType == REMAINDER)
                {
                    final float dist = (vertexSplitV - thisVertex.v) / (nextVertex.v - thisVertex.v);
                    Vertex newVertex = thisVertex.interpolate(nextVertex, dist);
                    remainder.setVertex(iRemainderVertex++, newVertex);
                    
                    final float vNew = (vMin + newVertex.v * (vMax - vMin) - vSplitLow) / vSpan;
                    slice.setVertex(iSliceVertex++, newVertex.withUV(newVertex.u, vNew));
                }
            }
            else
            {
                remainder.setVertex(iRemainderVertex++, thisVertex);
                if(nextType == SLICE)
                {
                    final float dist = (vertexSplitV - thisVertex.v) / (nextVertex.v - thisVertex.v);
                    Vertex newVertex = thisVertex.interpolate(nextVertex, dist);
                    remainder.setVertex(iRemainderVertex++, newVertex);
                    
                    final float vNew = (vMin + newVertex.v * (vMax - vMin) - vSplitLow) / vSpan;
                    slice.setVertex(iSliceVertex++, newVertex.withUV(newVertex.u, vNew));
                }
            }
            
            vThis = vNext;
            thisVertex = nextVertex;
            thisType = nextType;
        }
        
        output.accept(slice);
        return remainder;
    }
            
    
    /**
     * Completes painting after quads are split along tile boundaries (if needed) or it has been
     * determined that no split is needed. The input quad will be mutated.<p>
     * 
     * The uv min/max on the quad should be 0-1, where 1 represents a single
     * texture tile distance.  The provided salt will be used for texture randomization.
     */
    private void paintBoundedQuad(
            IMutablePolygon quad, 
            Consumer<IPolygon> target, 
            boolean isItem,
            int salt)
    {
        // note that we could do some randomization via texture offset, but that would
        // be more complicated and might force us to do additional splits on some models (would it?)
        // For now, always use uv 0,0 as tiling origin.
        
        int textureVersion = this.texture.textureVersionMask() & (salt >> 4);
        quad.setTextureName(this.texture.getTextureName(textureVersion));
        
        quad.setRotation(this.texture.rotation().rotationType() == TextureRotationType.RANDOM
          ? Useful.offsetEnumValue(this.texture.rotation().rotation, (salt >> 16) & 3)
          : this.texture.rotation().rotation);
        
        // confirm is still a convex quad or tri
        // because earlier iterations may have left us with something else
        if(quad.vertexCount() > 4 || !quad.isConvex())
        {
            quad.toQuads(p -> postPaintProcessQuadAndOutput((IMutablePolygon)p, target, isItem), true);
        }
        else 
        {
            this.postPaintProcessQuadAndOutput(quad, target, isItem);
        }
    }
    
    private int salt(int textureSalt, int uIndex, int vIndex)
    {
        return MathHelper.hash((textureSalt << 16) | (uIndex << 8) | vIndex);
    }
    
   
}
