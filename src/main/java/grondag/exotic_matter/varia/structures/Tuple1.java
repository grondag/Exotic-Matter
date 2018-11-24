package grondag.exotic_matter.varia.structures;

public class Tuple1<T> implements ITuple<T>
{
    protected final T v0;
    
    Tuple1(T v0)
    {
        this.v0 = v0;
    }
    
    @Override
    public T get(int index)
    {
        assert index == 0;
        return v0;
    }

    @Override
    public int size()
    {
        return 1;
    }
}