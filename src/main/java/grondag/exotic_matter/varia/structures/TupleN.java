package grondag.exotic_matter.varia.structures;

public class TupleN<T> extends AbstractTuple<T>
{
    final T[] values;
    
    /**
     * RETAINS REFERENCE!
     */
    TupleN(T[] values)
    {
        this.values = values;
    }
    
    @Override
    public T get(int index)
    {
        return values[index];
    }

    @Override
    public int size()
    {
        return values.length;
    }
}
