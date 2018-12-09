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
        resetBounds();
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
        updateBounds(internal);
    }
    
    private void resetBounds()
    {
        minX = Float.MAX_VALUE;
        minY = Float.MAX_VALUE;
        minZ = Float.MAX_VALUE;
        maxX = Float.MIN_VALUE;
        maxY = Float.MIN_VALUE;
        maxZ = Float.MIN_VALUE;
    }
    
    private void updateBounds(IPolygon withPoly)
    {
        float f = internal.boundsMinX();
        if(f < minX)
            minX = f;
        
        f = internal.boundsMinY();
        if(f < minY)
            minY = f;
        
        f = internal.boundsMinZ();
        if(f < minZ)
            minZ = f;
        
        f = internal.boundsMaxX();
        if(f > maxX)
            maxX = f;
        
        f = internal.boundsMaxY();
        if(f > maxY)
            maxY = f;
        
        f = internal.boundsMaxZ();
        if(f > maxZ)
            maxZ = f;
    }
    
    /**
     * Call after polys are deleted and the deletion
     * may have significantly reduced overall mesh bounds.
     */
    public void refreshStreamBounds()
    {
        final int readerPos = reader.baseAddress;
        
        resetBounds();
        if(origin())
        {
            do
                updateBounds(reader);
            while(next());
        }
        
        reader.moveTo(readerPos);
    }
}
