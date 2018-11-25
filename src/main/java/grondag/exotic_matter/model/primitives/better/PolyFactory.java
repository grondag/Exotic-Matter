package grondag.exotic_matter.model.primitives.better;

import net.minecraft.util.math.MathHelper;

public class PolyFactory
{
    public static IMutablePolygon newPaintable(int vertexCount, int layerCount)
    {
        // TODO: pool
        return vertexCount == 4 
                ? new MutablePolygon3x4().prepare(layerCount)
                : vertexCount == 3
                    ? new MutablePolygon3x3().prepare(layerCount)
                    : new MutablePolygonNxN(MathHelper.log2DeBruijn(vertexCount)).prepare(layerCount, vertexCount);
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
