package grondag.exotic_matter.render;

import java.util.ArrayList;
import java.util.Iterator;

public class PolygonList implements IPolygonList
{
    public static IPolygonList mutableList()
    {
        return new PolygonList();
    }
    
    private final ArrayList<IPolygon> wrapped = new ArrayList<>();
    
    private PolygonList() {};
    
    @Override
    public IMutablePolygon mutable(IPolygon template)
    {
        IMutablePolygon result = new PolyImpl(template);
        wrapped.add(result);
        return result;
    }
    
    @Override
    public IMutablePolygon mutable(IPolygon template, int vertexCount)
    {
        IMutablePolygon result = new PolyImpl(template, vertexCount);
        wrapped.add(result);
        return result;
    }
    
    @Override
    public IMutablePolygon mutable(int vertexCount)
    {
        IMutablePolygon result = new PolyImpl(vertexCount);
        wrapped.add(result);
        return result;
    }
    
    @Override
    public IMutablePolygon mutableCopyOf(IPolygon template)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int size()
    {
        return wrapped.size();
    }

    @Override
    public boolean isEmpty()
    {
        return wrapped.isEmpty();
    }

    @Override
    public Iterator<IPolygon> iterator()
    {
        return wrapped.iterator();
    }

}
