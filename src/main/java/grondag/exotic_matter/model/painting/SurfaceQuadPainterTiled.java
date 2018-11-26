package grondag.exotic_matter.model.painting;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import grondag.exotic_matter.model.primitives.QuadHelper;
import grondag.exotic_matter.model.primitives.polygon.IMutablePolygon;
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
    public void textureQuad(IMutablePolygon quad, Consumer<IMutablePolygon> target, boolean isItem)
    {
        assert !quad.isLockUV(layerIndex) : "Tiled surface quad painter received quad with lockUV semantics.  Not expected";
        
        final boolean uFlipped = quad.getMaxU(layerIndex) < quad.getMinU(layerIndex);
        final boolean vFlipped = quad.getMaxV(layerIndex) < quad.getMinV(layerIndex);
        
        final Surface surfIn = quad.getSurface();
        final EnumFacing face = quad.getNominalFace();
        final int salt = (quad.getTextureSalt() << 3) + (face == null ? 0 : face.ordinal());
        
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
                ? MathHelper.ceil(quad.getMinU(layerIndex) / tilingDistance)
                : MathHelper.floor(quad.getMinU(layerIndex) / tilingDistance);
        
        final int uMaxIndex = uFlipped 
                ? MathHelper.floor(quad.getMaxU(layerIndex) / tilingDistance)
                : MathHelper.ceil(quad.getMaxU(layerIndex) / tilingDistance);

        final int vMinIndex = vFlipped 
                ? MathHelper.ceil(quad.getMinV(layerIndex) / tilingDistance)
                : MathHelper.floor(quad.getMinV(layerIndex) / tilingDistance);
        
        final int vMaxIndex = vFlipped 
                ? MathHelper.floor(quad.getMaxV(layerIndex) / tilingDistance)
                : MathHelper.ceil(quad.getMaxV(layerIndex) / tilingDistance);
        
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
        final float uMin = input.getMinU(layerIndex);
        final float uMax = input.getMaxU(layerIndex);
        final int vCountIn = input.vertexCount();
        
        // PERF: make this threadlocal
        final float[] vertexU = new float[vCountIn];
        final boolean flipped = uSpan < 0;
        
        /** points fully within the slice */
        int sliceCount = 0;
        /** points fully within the remainder */
        int remainderCount = 0;
        
        for(int i = 0; i < vCountIn; i++)
        {
            // vertex u where 0 corresponds to low split and 1 to high split
            final float u = (uMin + input.getVertexU(layerIndex, i) * (uMax - uMin) - uSplitLow) / uSpan;
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
            
            input.setMinU(layerIndex, flipped ? 1 : 0);
            input.setMaxU(layerIndex, flipped ? 0 : 1);
            
            for(int i = 0; i < vCountIn; i++)
            {
                input.setVertexU(layerIndex, i, vertexU[i]);
            }
            output.accept(input);
            return null;
        }
        
        // if we get to here, take slice and return remainder
        
        /** point on 0-1 on input vertex scale that separates slice and remainder */
        final float vertexSplitU = (uSplitLow + uSpan) / (uMax - uMin);
        
        IMutablePolygon slice = input.claimCopy(sliceCount + 2);
        slice.setMinU(layerIndex, flipped ? 1 : 0);
        slice.setMaxU(layerIndex, flipped ? 0 : 1);
        int iSliceVertex = 0;
        
        IMutablePolygon remainder = input.claimCopy(remainderCount + 2);
        int iRemainderVertex = 0;
        
        float uThis = vertexU[vCountIn - 1];
        
        int iThis = vCountIn - 1;
        int thisType = vertexType(uThis);
                
        for(int iNext = 0; iNext < vCountIn; iNext++)
        {
            final float uNext = vertexU[iNext];
            final int nextType = vertexType(uNext);
            
            if(thisType == EDGE)
            {
                slice.copyVertexFrom(iSliceVertex, input, iThis).setVertexU(layerIndex, iSliceVertex, uThis);
                iSliceVertex++;
                remainder.copyVertexFrom(iRemainderVertex, input, iThis);
                iRemainderVertex++;
            }
            else if(thisType == SLICE)
            {
                slice.copyVertexFrom(iSliceVertex, input, iThis).setVertexU(layerIndex, iSliceVertex, uThis);
                iSliceVertex++;
                if(nextType == REMAINDER)
                {
                    final float dist = (vertexSplitU - uThis) / (uNext - uThis);
                    remainder.copyInterpolatedVertexFrom(iRemainderVertex, input, iThis, input, iNext, dist);
                    
                    final float uNew = (uMin + remainder.getVertexU(layerIndex, iRemainderVertex) * (uMax - uMin) - uSplitLow) / uSpan;
                    slice.setVertexUV(layerIndex, iSliceVertex, uNew, remainder.getVertexV(layerIndex, iRemainderVertex));
                    
                    iRemainderVertex++;
                    iSliceVertex++;
                }
            }
            else
            {
                remainder.copyVertexFrom(iRemainderVertex, input, iThis);
                iRemainderVertex++;
                if(nextType == SLICE)
                {
                    final float dist = (vertexSplitU - uThis) / (uNext - uThis);
                    remainder.copyInterpolatedVertexFrom(iRemainderVertex, input, iThis, input, iNext, dist);
                    
                    final float uNew = (uMin + remainder.getVertexU(layerIndex, iRemainderVertex) * (uMax - uMin) - uSplitLow) / uSpan;
                    slice.setVertexUV(layerIndex, iSliceVertex, uNew, remainder.getVertexV(layerIndex, iRemainderVertex));
                    
                    iRemainderVertex++;
                    iSliceVertex++;
                }
            }
            
            uThis = uNext;
            iThis = iNext;
            thisType = nextType;
        }
        
        // input no longer needed so release storage
        input.release();
        
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
        final float vMin = input.getMinV(layerIndex);
        final float vMax = input.getMaxV(layerIndex);
        final int vCountIn = input.vertexCount();
        // PERF: make this threadlocal
        final float[] vertexV = new float[vCountIn];
        final boolean flipped = vSpan < 0;
        
        /** points fully within the slice */
        int sliceCount = 0;
        /** points fully within the remainder */
        int remainderCount = 0;
        
        for(int i = 0; i < vCountIn; i++)
        {
            // vertex u where 0 corresponds to low split and 1 to high split
            final float v = (vMin + input.getVertexV(layerIndex, i) * (vMax - vMin) - vSplitLow) / vSpan;
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
            
            input.setMinV(layerIndex, flipped ? 1 : 0);
            input.setMaxV(layerIndex, flipped ? 0 : 1);
            
            for(int i = 0; i < vCountIn; i++)
            {
                input.setVertexV(layerIndex, i, vertexV[i]);
            }
            output.accept(input);
            return null;
        }
        
        // if we get to here, take slice and return remainder
        
        /** point on 0-1 on input vertex scale that separates slice and remainder */
        final float vertexSplitV = (vSplitLow + vSpan) / (vMax - vMin);
        
        IMutablePolygon slice = input.claimCopy(sliceCount + 2);
        slice.setMinV(layerIndex, flipped ? 1 : 0);
        slice.setMaxV(layerIndex, flipped ? 0 : 1);
        int iSliceVertex = 0;
        
        IMutablePolygon remainder = input.claimCopy(remainderCount + 2);
        int iRemainderVertex = 0;
        
        float vThis = vertexV[vCountIn - 1];
        
        int iThis = vCountIn -1;
        int thisType = vertexType(vThis);
                
        for(int iNext = 0; iNext < vCountIn; iNext++)
        {
            final float vNext = vertexV[iNext];
            final int nextType = vertexType(vNext);
            
            if(thisType == EDGE)
            {
                slice.copyVertexFrom(iSliceVertex, input, iThis).setVertexV(layerIndex, iSliceVertex, vThis);
                iSliceVertex++;
                remainder.copyVertexFrom(iRemainderVertex, input, iThis);
                iRemainderVertex++;
            }
            else if(thisType == SLICE)
            {
                slice.copyVertexFrom(iSliceVertex, input, iThis).setVertexV(layerIndex, iSliceVertex, vThis);
                iSliceVertex++;
                if(nextType == REMAINDER)
                {
                    final float dist = (vertexSplitV - vThis) / (vNext - vThis);
                    remainder.copyInterpolatedVertexFrom(iRemainderVertex, input, iThis, input, iNext, dist);
                    
                    final float vNew = (vMin + remainder.getVertexV(layerIndex, iRemainderVertex) * (vMax - vMin) - vSplitLow) / vSpan;
                    slice.setVertexUV(layerIndex, iSliceVertex, remainder.getVertexU(layerIndex, iRemainderVertex), vNew);
                    
                    iRemainderVertex++;
                    iSliceVertex++;
                }
            }
            else
            {
                remainder.copyVertexFrom(iRemainderVertex, input, iThis);
                iRemainderVertex++;
                if(nextType == SLICE)
                {
                    final float dist = (vertexSplitV - vThis) / (vNext - vThis);
                    remainder.copyInterpolatedVertexFrom(iRemainderVertex, input, iThis, input, iNext, dist);
                    
                    final float vNew = (vMin + remainder.getVertexV(layerIndex, iRemainderVertex) * (vMax - vMin) - vSplitLow) / vSpan;
                    slice.setVertexUV(layerIndex,  iSliceVertex, remainder.getVertexU(layerIndex, iRemainderVertex), vNew);
                    
                    iRemainderVertex++;
                    iSliceVertex++;
                }
            }
            
            vThis = vNext;
            iThis = iNext;
            thisType = nextType;
        }
        
        // input no longer needed so release storage
        input.release();
        
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
            Consumer<IMutablePolygon> target, 
            boolean isItem,
            int salt)
    {
        // Note that we could do some randomization via texture offset, but that would
        // be more complicated and might force us to do additional splits on some models (would it?)
        // For now, always use uv 0,0 as tiling origin.
        
        int textureVersion = this.texture.textureVersionMask() & (salt >> 4);
        quad.setTextureName(layerIndex, this.texture.getTextureName(textureVersion));
        
        quad.setRotation(layerIndex, this.texture.rotation().rotationType() == TextureRotationType.RANDOM
          ? Useful.offsetEnumValue(this.texture.rotation().rotation, (salt >> 16) & 3)
          : this.texture.rotation().rotation);
        
        // confirm is still a convex quad or tri
        // because earlier iterations may have left us with something else
        if(quad.vertexCount() > 4 || !quad.isConvex())
        {
            boolean didSplit = quad.toPaintableQuads(p -> postPaintProcessQuadAndOutput(p, target, isItem));
            assert didSplit;
            // can release original polys that have been split - splits are new instances 
            // and this current instance is not passed to the consumer nor used in any other way.
            if(didSplit)
                quad.release();
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

    @Override
    protected boolean isQuadValidForPainting(IMutablePolygon inputQuad)
    {
        return true;
    }
   
}
