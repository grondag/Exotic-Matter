package grondag.exotic_matter.model.CSG;

import grondag.exotic_matter.model.primitives.polygon.IPolygon;
import grondag.exotic_matter.model.primitives.stream.IPolyStream;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

public class CSGBounds extends AxisAlignedBB
{
    public static CSGBounds getBounds(IPolyStream forPolygons)
    {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;
        float maxZ = Float.MIN_VALUE;
        
        if(!forPolygons.isEmpty())
        {
            IPolygon reader = forPolygons.reader();
            forPolygons.origin();

            do
            {
                final int vCount = reader.vertexCount();
                for(int i = 0; i < vCount; i++)
                {
                    final float x = reader.getVertexX(i);
                    if(x < minX)  
                        minX = x;
                    else if(x > maxX)
                        maxX = x;
                    
                    final float y = reader.getVertexY(i);
                    if(y < minY)  
                        minY = y;
                    else if(y > maxY)
                        maxY = y;
                    
                    final float z = reader.getVertexZ(i);
                    if(z < minZ)  
                        minZ = z;
                    else if(z > maxZ)
                        maxZ = z;
                }
            } while(forPolygons.next());
        }

        return new CSGBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }
    
    public CSGBounds(double x1, double y1, double z1, double x2, double y2, double z2)
    {
        super(x1, y1, z1, x2, y2, z2);
    }

    public CSGBounds(Vec3d min, Vec3d max)
    {
        super(min.x, min.y, min.z, max.x, max.y, max.z);
    }
    
    /**
     * For CSG operations we consider a point on the edge to be intersecting.
     */
    @Deprecated
    public boolean intersectsWith(AxisAlignedBB other)
    {
        return this.intersects(other.minX, other.minY, other.minZ, other.maxX, other.maxY, other.maxZ);
    }
    
    public boolean intersectsWith(IPolygon poly)
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
        return this.intersects(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * For CSG operations we consider a point on the edge to be intersecting.  
     */
    @Override
    public boolean intersects(double xMin, double yMin, double zMin, double xMax, double yMax, double zMax)
    {

        return this.minX <= xMax && this.maxX >= xMin && this.minY <= yMax && this.maxY >= yMin && this.minZ <= zMax && this.maxZ >= zMin;
    }
}
