package grondag.exotic_matter.model.primitives.better;

public interface IVertexCollection<T extends IGeometricVertex>
{
    public int vertexCount();
    
    public T getVertex(int index);
    
    /**
     * Wraps around if index out of range.
     */
    public default T getVertexModulo(int index)
    {
        return getVertex(index % vertexCount());
    }
}
