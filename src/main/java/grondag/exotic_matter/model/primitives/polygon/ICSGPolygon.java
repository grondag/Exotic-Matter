package grondag.exotic_matter.model.primitives.polygon;

public interface ICSGPolygon
{
    default public float planeDistance()
    {
        throw new UnsupportedOperationException();
    }
    
    default public float boundsMinX()
    {
        throw new UnsupportedOperationException();
    }
    
    default public float boundsMinY()
    {
        throw new UnsupportedOperationException();
    }
    
    default public float boundsMinZ()
    {
        throw new UnsupportedOperationException();
    }
    
    default public float boundsMaxX()
    {
        throw new UnsupportedOperationException();
    }
    
    default public float boundsMaxY()
    {
        throw new UnsupportedOperationException();
    }
    
    default public float boundsMaxZ()
    {
        throw new UnsupportedOperationException();
    }
}
