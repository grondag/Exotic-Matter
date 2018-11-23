package grondag.exotic_matter.model.primitives.better;

public class VectorN<T> extends AbstractVector<T>
{
    final T[] values;
    
    /**
     * RETAINS REFERENCE!
     */
    VectorN(T[] values)
    {
        this.values = values;
    }
    
    @Override
    T get(int index)
    {
        return values[index];
    }

    @Override
    int size()
    {
        return values.length;
    }
}
