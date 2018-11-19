package grondag.exotic_matter.model.painting;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import grondag.exotic_matter.model.primitives.IMutablePolygon;
import grondag.exotic_matter.model.primitives.IPaintableQuad;
import grondag.exotic_matter.model.primitives.IPaintableVertex;
import grondag.exotic_matter.model.primitives.QuadHelper;
import grondag.exotic_matter.model.primitives.Vertex;
import grondag.exotic_matter.world.FaceCorner;

/**
 * Helper class to splits UV-locked quads into four quadrants at the u,v = 0.5, 0.5
 * point (if needed) and to test if quads are already within a single quadrant.
 */
public class QuadrantSplitter
{
    @Nullable
    public final static FaceCorner uvQuadrant(IPaintableQuad quad)
    {
        final int vCount = quad.vertexCount();
        
        IPaintableVertex v = quad.getPaintableVertex(0);
        
        float uMin = v.u();
        float uMax = v.u();
        float vMin = v.v();
        float vMax = v.v();
        
        for(int i = 1; i < vCount; i++)
        {
            v = quad.getPaintableVertex(i);
            if(v.u() < uMin) 
                uMin = v.u();
            else if(v.u() > uMax)
                uMax = v.u();
            
            if(v.v() < vMin) 
                vMin = v.v();
            else if(v.v() > vMax)
                vMax = v.v();
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
    
    public static final void splitAndPaint(IMutablePolygon quad, Consumer<IMutablePolygon> target)
    {
        int lowCount = 0;
        int highCount = 0;
        final int vCount = quad.vertexCount();
        
        for(int i = 0; i < vCount; i++)
        {
            final int t = vertexType(quad.getVertex(i).u());
            if(t == HIGH)
                highCount++;
            else if(t == LOW)
                lowCount++;
        }
        
        if(lowCount == 0)
            // all on on high side
            splitVAndPaint(quad, target, true);
        else if(highCount == 0)
            // all on low side
            splitVAndPaint(quad, target, false);
        else
        {
            // spanning
            IMutablePolygon high = quad.mutableCopy(highCount + 2);
            int iHighVertex = 0;
            
            IMutablePolygon low = quad.mutableCopy(lowCount + 2);
            int iLowVertex = 0;
            
            Vertex thisVertex = quad.getVertex(vCount -1);
            int thisType = vertexType(thisVertex.u());
                    
            for(int iNext = 0; iNext < vCount; iNext++)
            {
                final Vertex nextVertex = quad.getVertex(iNext);
                final int nextType = vertexType(nextVertex.u());
                
                if(thisType == EDGE)
                {
                    high.setVertex(iHighVertex++, thisVertex);
                    low.setVertex(iLowVertex++, thisVertex);
                }
                else if(thisType == HIGH)
                {
                    high.setVertex(iHighVertex++, thisVertex);
                    if(nextType == LOW)
                    {
                        final float dist = (0.5f - thisVertex.u()) / (nextVertex.u() - thisVertex.u());
                        Vertex vNew = thisVertex.interpolate(nextVertex, dist);
                        low.setVertex(iLowVertex++, vNew);
                        high.setVertex(iHighVertex++, vNew);
                    }
                }
                else
                {
                    low.setVertex(iLowVertex++, thisVertex);
                    if(nextType == HIGH)
                    {
                        final float dist = (0.5f - thisVertex.u()) / (nextVertex.u() - thisVertex.u());
                        Vertex vNew = thisVertex.interpolate(nextVertex, dist);
                        low.setVertex(iLowVertex++, vNew);
                        high.setVertex(iHighVertex++, vNew);
                    }
                }
                thisVertex = nextVertex;
                thisType = nextType;
            }
            
            splitVAndPaint(high, target, true);
            splitVAndPaint(low, target, false);
        }
    }
    
    private static final void splitVAndPaint(IMutablePolygon quad, Consumer<IMutablePolygon> target, boolean isHighU)
    {
        int lowCount = 0;
        int highCount = 0;
        final int vCount = quad.vertexCount();
        
        for(int i = 0; i < vCount; i++)
        {
            final int t = vertexType(quad.getVertex(i).v());
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
            IMutablePolygon high = quad.mutableCopy(highCount + 2);
            int iHighVertex = 0;
            
            IMutablePolygon low = quad.mutableCopy(lowCount + 2);
            int iLowVertex = 0;
            
            Vertex thisVertex = quad.getVertex(vCount -1);
            int thisType = vertexType(thisVertex.v());
                    
            for(int iNext = 0; iNext < vCount; iNext++)
            {
                final Vertex nextVertex = quad.getVertex(iNext);
                final int nextType = vertexType(nextVertex.v());
                
                if(thisType == EDGE)
                {
                    high.setVertex(iHighVertex++, thisVertex);
                    low.setVertex(iLowVertex++, thisVertex);
                }
                else if(thisType == HIGH)
                {
                    high.setVertex(iHighVertex++, thisVertex);
                    if(nextType == LOW)
                    {
                        final float dist = (0.5f - thisVertex.v()) / (nextVertex.v() - thisVertex.v());
                        Vertex vNew = thisVertex.interpolate(nextVertex, dist);
                        low.setVertex(iLowVertex++, vNew);
                        high.setVertex(iHighVertex++, vNew);
                    }
                }
                else
                {
                    low.setVertex(iLowVertex++, thisVertex);
                    if(nextType == HIGH)
                    {
                        final float dist = (0.5f - thisVertex.v()) / (nextVertex.v() - thisVertex.v());
                        Vertex vNew = thisVertex.interpolate(nextVertex, dist);
                        low.setVertex(iLowVertex++, vNew);
                        high.setVertex(iHighVertex++, vNew);
                    }
                }
                thisVertex = nextVertex;
                thisType = nextType;
            }
            
            target.accept(high);
            target.accept(low);
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
