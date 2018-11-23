package grondag.exotic_matter.model.primitives.better;

public class PolyFactory
{
    public static IPaintablePoly newPaintable(int vertexCount, int layerCount)
    {
        return  null;
    }
    
    /**
     * Concise syntax for single layer
     */
    public static IPaintablePoly newPaintable(int vertexCount)
    {
        return newPaintable(vertexCount, 1);
    }

    public static IMutableGeometricVertex claimMutableVertex(float x, float y, float z)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
