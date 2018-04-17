package grondag.exotic_matter.render;

public abstract class Poly 
{
    public static IMutablePolygon mutable(IPolygon template)
    {
        return new PolyImpl(template);
    }
    
    public static IMutablePolygon mutable(IPolygon template, int vertexCount)
    {
        return new PolyImpl(template, vertexCount);
    }
    
    public static IMutablePolygon mutable(int vertexCount)
    {
        return new PolyImpl(vertexCount);
    }
    
    public static IMutablePolygon mutableCopyOf(IPolygon template)
    {
        PolyImpl result = new PolyImpl(template);
        result.copyVertices(template);
        return result;
    }
    
    public static IMutableCSGPolygon mutableCSG(IPolygon template)
    {
        return new PolyImpl(template);
    }
    
    public static IMutableCSGPolygon mutableCSG(IPolygon template, int vertexCount)
    {
        return new PolyImpl(template, vertexCount);
    }
    
    public static IMutableCSGPolygon mutableCSG(int vertexCount)
    {
        return new PolyImpl(vertexCount);
    }

    public static IFancyMutablePolygon fancyMutableCopyOf(IPolygon template)
    {
        PolyImpl result = new PolyImpl(template);
        result.copyVertices(template);
        return result;
    }
    
    public static IFancyMutablePolygon fancyMutable(IPolygon template)
    {
        return new PolyImpl(template);
    }
    
    public static IFancyMutablePolygon fancyMutable(IPolygon template, int vertexCount)
    {
        return new PolyImpl(template, vertexCount);
    }
    
    public static IFancyMutablePolygon fancyMutable(int vertexCount)
    {
        return new PolyImpl(vertexCount);
    }
    
}