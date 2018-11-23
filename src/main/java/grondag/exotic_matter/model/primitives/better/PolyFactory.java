package grondag.exotic_matter.model.primitives.better;

public class PolyFactory
{
    public static IMutablePolygon newPaintable(int vertexCount, int layerCount)
    {
        return  null;
    }
    
    /**
     * Concise syntax for single layer
     */
    public static IMutablePolygon newPaintable(int vertexCount)
    {
        return newPaintable(vertexCount, 1);
    }

    public static IMutableVertex claimMutableVertex(float x, float y, float z)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
