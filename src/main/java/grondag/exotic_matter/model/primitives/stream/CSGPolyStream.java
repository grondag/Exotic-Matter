package grondag.exotic_matter.model.primitives.stream;

import grondag.exotic_matter.model.primitives.polygon.IPolygon;
import grondag.exotic_matter.varia.intstream.IntStreams;

public class CSGPolyStream extends MutablePolyStream
{
    private float minX;
    private float minY;
    private float minZ;
    private float maxX;
    private float maxY;
    private float maxZ;
    
    void prepare()
    {
        super.prepare(IntStreams.claim());
        clearStreamBounds();
    }
    
    @Override
    protected void doRelease()
    {
        super.doRelease();
    }
    
    @Override
    protected void returnToPool()
    {
        PolyStreams.release(this);
    }
    
    public float boundsMinX()
    {
        return minX;
    }
    
    public float boundsMinY()
    {
        return minY;
    }
    
    public float boundsMinZ()
    {
        return minZ;
    }
    
    public float boundsMaxX()
    {
        return maxX;
    }
    
    public float boundsMaxY()
    {
        return maxY;
    }
    
    public float boundsMaxZ()
    {
        return maxZ;
    }
    
    @Override
    protected void appendCopy(IPolygon polyIn, int withFormat)
    {
        int boundsAddress = writerAddress();
        super.appendCopy(polyIn, withFormat);
        internal.moveTo(boundsAddress);
        internal.updateBounds();
        expandStreamBounds(internal);
    }
    
    private void clearStreamBounds()
    {
        minX = Float.MAX_VALUE;
        minY = Float.MAX_VALUE;
        minZ = Float.MAX_VALUE;
        maxX = Float.MIN_VALUE;
        maxY = Float.MIN_VALUE;
        maxZ = Float.MIN_VALUE;
    }
    
    private void expandStreamBounds(IPolygon withPoly)
    {
        float f = internal.csgMinX();
        if(f < minX)
            minX = f;
        
        f = internal.csgMinY();
        if(f < minY)
            minY = f;
        
        f = internal.csgMinZ();
        if(f < minZ)
            minZ = f;
        
        f = internal.csgMaxX();
        if(f > maxX)
            maxX = f;
        
        f = internal.csgMaxY();
        if(f > maxY)
            maxY = f;
        
        f = internal.csgMaxZ();
        if(f > maxZ)
            maxZ = f;
    }
    
    /**
     * Call after polys are deleted and the deletion
     * may have significantly reduced overall mesh bounds.
     */
    public void rebuildStreamBounds()
    {
        final int readerPos = reader.baseAddress;
        
        clearStreamBounds();
        if(origin())
        {
            do
                expandStreamBounds(reader);
            while(next());
        }
        
        reader.moveTo(readerPos);
    }
    
    /**
     * For CSG operations we consider a point on the edge to be intersecting.  
     */
    public boolean intersectsWith(double xMin, double yMin, double zMin, double xMax, double yMax, double zMax)
    {

        return this.minX <= xMax && this.maxX >= xMin && this.minY <= yMax && this.maxY >= yMin && this.minZ <= zMax && this.maxZ >= zMin;
    }
    
    /**
     * For CSG operations we consider a point on the edge to be intersecting.  
     */
    public boolean intersectsWith(IPolygon poly)
    {
        if(poly.isCSG())
            return intersectsWith(poly.csgMinX(), poly.csgMinY(), poly.csgMinZ(), poly.csgMaxX(), poly.csgMaxY(), poly.csgMaxZ());
        else
        {
            float minX = poly.getVertexX(0);
            float minY = poly.getVertexY(0);
            float minZ = poly.getVertexZ(0);
            float maxX = minX;
            float maxY = minY;
            float maxZ = minZ;
            
            final int vCount = poly.vertexCount();
            for(int i = 1; i < vCount; i++)
            {
                final float x = poly.getVertexX(i);
                if(x < minX)  
                    minX = x;
                else if(x > maxX)
                    maxX = x;
                
                final float y = poly.getVertexY(i);
                if(y < minY)  
                    minY = y;
                else if(y > maxY)
                    maxY = y;
                
                final float z = poly.getVertexZ(i);
                if(z < minZ)  
                    minZ = z;
                else if(z > maxZ)
                    maxZ = z;
            }
            return intersectsWith(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }
    
    /**
     * For CSG operations we consider a point on the edge to be intersecting.  
     */
    public boolean intersectsWith(CSGPolyStream stream)
    {
        return intersectsWith(stream.boundsMinX(), stream.boundsMinY(), stream.boundsMinZ(), stream.boundsMaxX(), stream.boundsMaxY(), stream.boundsMaxZ());
    }

    public void invert()
    {
        // TODO Auto-generated method stub
        
    }

    public void clipTo(CSGPolyStream b)
    {
        // TODO Auto-generated method stub
        
    }

    public void recombineQuads()
    {
        // TODO Auto-generated method stub
        
    }
}
