package grondag.exotic_matter.model.primitives.polygon;

import grondag.exotic_matter.model.primitives.vertex.IMutableVertex;

public interface IPrimitiveFactory
{
    /**
     * Concise syntax for single layer
     */
    IMutablePolygon newPaintable(int vertexCount);

    IMutablePolygon newPaintable(int vertexCount, int layerCount);

    IPolygon toPainted(IMutablePolygon mutable);

    /**
     * Copy will include vertices only if vertex counts match.
     */
    IMutablePolygon claimCopy(IPolygon template, int vertexCount);
    
    /**
     * Copy will include vertices.
     */
    public default IMutablePolygon claimCopy(IPolygon template)
    {
        return claimCopy(template, template.vertexCount());
    }
    
    IMutableVertex claimMutableVertex();
}
