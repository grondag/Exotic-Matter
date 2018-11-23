package grondag.exotic_matter.model.painting;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import grondag.exotic_matter.model.primitives.QuadHelper;
import grondag.exotic_matter.model.primitives.better.IPaintablePoly;
import grondag.exotic_matter.world.FaceCorner;

/**
 * Helper class to splits UV-locked quads into four quadrants at the u,v = 0.5, 0.5
 * point (if needed) and to test if quads are already within a single quadrant.
 */
public class QuadrantSplitter
{
    @Nullable
    public final static FaceCorner uvQuadrant(IPaintablePoly quad, int layerIndex)
    {
        final int vCount = quad.vertexCount();
        
        float uMin = quad.u(layerIndex, 0);
        float uMax = uMin;
        float vMin = quad.v(layerIndex, 0);
        float vMax = vMin;
        
        for(int i = 1; i < vCount; i++)
        {
            float u = quad.u(layerIndex, i);
            float v = quad.v(layerIndex, i);
            if(u < uMin) 
                uMin = u;
            else if(u > uMax)
                uMax = u;
            
            if(v < vMin) 
                vMin = v;
            else if(v > vMax)
                vMax = v;
        }
        
        // note that v is inverted from FaceCorner semantics.
        // (high v = bottom, low v = top)
        if(vertexType(uMin) == LOW)
        {
            // u is left 
            if(vertexType(uMax) == HIGH)
                // spanning
                return null;
            
            else if(vertexType(vMin) == LOW)
            {
                // v is low
                
                if(vertexType(vMax) == HIGH)
                    // spanning
                    return null;
                else
                    return FaceCorner.TOP_LEFT;
            }
            else
                // v is high
                return FaceCorner.BOTTOM_LEFT;
        }
        else
        {
            // u is right 
            if(vertexType(vMin) == LOW)
            {
                // v is low
                
                if(vertexType(vMax) == HIGH)
                    // spanning
                    return null;
                else
                    return FaceCorner.TOP_RIGHT;
            }
            else
                // v is high
                return FaceCorner.BOTTOM_RIGHT;
        }
    }
    
    public static final void splitAndPaint(IPaintablePoly quad, Consumer<IPaintablePoly> target, int layerIndex)
    {
        int lowCount = 0;
        int highCount = 0;
        final int vCount = quad.vertexCount();
        
        for(int i = 0; i < vCount; i++)
        {
            final int t = vertexType(quad.u(layerIndex, i));
            if(t == HIGH)
                highCount++;
            else if(t == LOW)
                lowCount++;
        }
        
        if(lowCount == 0)
            // all on on high side
            splitVAndPaint(quad, target, true, layerIndex);
        else if(highCount == 0)
            // all on low side
            splitVAndPaint(quad, target, false, layerIndex);
        else
        {
            // spanning
            IPaintablePoly high = quad.claimCopy(highCount + 2);
            int iHighVertex = 0;
            
            IPaintablePoly low = quad.claimCopy(lowCount + 2);
            int iLowVertex = 0;
            
            int iThis = vCount - 1;
            float uThis = quad.u(layerIndex, iThis);
            int thisType = vertexType(uThis);
                    
            for(int iNext = 0; iNext < vCount; iNext++)
            {
                final float uNext = quad.u(layerIndex, iNext);
                final int nextType = vertexType(uNext);
                
                if(thisType == EDGE)
                {
                    high.copyVertexFrom(iHighVertex++, quad, iThis);
                    low.copyVertexFrom(iLowVertex++, quad, iThis);
                }
                else if(thisType == HIGH)
                {
                    high.copyVertexFrom(iHighVertex++, quad, iThis);
                    if(nextType == LOW)
                    {
                        final float dist = (0.5f - uThis) / (uNext - uThis);
                        low.copyInterpolatedVertexFrom(iLowVertex, quad, iThis, quad, iNext, dist);
                        high.copyVertexFrom(iHighVertex, low, iLowVertex);
                        iLowVertex++;
                        iHighVertex++;
                    }
                }
                else
                {
                    low.copyVertexFrom(iLowVertex++, quad, iThis);
                    if(nextType == HIGH)
                    {
                        final float dist = (0.5f - uThis) / (uNext - uThis);
                        low.copyInterpolatedVertexFrom(iLowVertex, quad, iThis, quad, iNext, dist);
                        high.copyVertexFrom(iHighVertex, low, iLowVertex);
                        iLowVertex++;
                        iHighVertex++;
                    }
                }
                iThis = iNext;
                uThis = uNext;
                thisType = nextType;
            }
            
            splitVAndPaint(high, target, true, layerIndex);
            splitVAndPaint(low, target, false, layerIndex);
            
            // quad isn't passed on so release it
            quad.release();
        }
    }
    
    private static final void splitVAndPaint(IPaintablePoly quad, Consumer<IPaintablePoly> target, boolean isHighU, int layerIndex)
    {
        int lowCount = 0;
        int highCount = 0;
        final int vCount = quad.vertexCount();
        
        for(int i = 0; i < vCount; i++)
        {
            final int t = vertexType(quad.v(layerIndex, i));
            if(t == HIGH)
                highCount++;
            else if(t == LOW)
                lowCount++;
        }
        
        if(lowCount == 0)
            // all on on high side
            target.accept(quad);
        else if(highCount == 0)
            // all on low side
            target.accept(quad);
        else
        {
            // spanning
            IPaintablePoly high = quad.claimCopy(highCount + 2);
            int iHighVertex = 0;
            
            IPaintablePoly low = quad.claimCopy(lowCount + 2);
            int iLowVertex = 0;
            
            int iThis = vCount - 1;
            float vThis = quad.v(layerIndex, iThis);
            int thisType = vertexType(vThis);
                    
            for(int iNext = 0; iNext < vCount; iNext++)
            {
                final float vNext = quad.v(layerIndex, iNext);
                final int nextType = vertexType(vNext);
                
                if(thisType == EDGE)
                {
                    high.copyVertexFrom(iHighVertex++, quad, iThis);
                    low.copyVertexFrom(iLowVertex++, quad, iThis);
                }
                else if(thisType == HIGH)
                {
                    high.copyVertexFrom(iHighVertex++, quad, iThis);
                    if(nextType == LOW)
                    {
                        final float dist = (0.5f - vThis) / (vNext - vThis);
                        low.copyInterpolatedVertexFrom(iLowVertex, quad, iThis, quad, iNext, dist);
                        high.copyVertexFrom(iHighVertex, low, iLowVertex);
                        iLowVertex++;
                        iHighVertex++;
                    }
                }
                else
                {
                    low.copyVertexFrom(iLowVertex++, quad, iThis);
                    if(nextType == HIGH)
                    {
                        final float dist = (0.5f - vThis) / (vNext - vThis);
                        low.copyInterpolatedVertexFrom(iLowVertex, quad, iThis, quad, iNext, dist);
                        high.copyVertexFrom(iHighVertex, low, iLowVertex);
                        iLowVertex++;
                        iHighVertex++;
                    }
                }
                iThis = iNext;
                vThis = vNext;
                thisType = nextType;
            }
            
            target.accept(high);
            target.accept(low);
            
            // quad isn't passed on so release it
            quad.release();
        }
    }
    
    private static final int EDGE = 0;
    private static final int HIGH = 1;
    private static final int LOW = 2;
    
    private final static int vertexType(float uvCoord)
    {
        if(uvCoord >= 0.5f - QuadHelper.EPSILON)
        {
            if(uvCoord <= 0.5f + QuadHelper.EPSILON)
                return EDGE;
            else
                return HIGH;
        }
        else
        {
            // < 0.5f - QuadHelper.EPSILON
            return LOW;
        }
    }
}
