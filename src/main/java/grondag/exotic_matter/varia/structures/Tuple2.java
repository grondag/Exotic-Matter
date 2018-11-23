package grondag.exotic_matter.varia.structures;

public class Tuple2<T> extends Tuple1<T>
{
    protected final T v1;
    
    Tuple2(T v0, T v1)
    {
        super(v0);
        this.v1 = v1;
    }
    
    @Override
    public T get(int index)
    {
        if(index == 0)
            return v0;
        else
        {
            assert index == 1;
            return v1;
        }
    }

    @Override
    public int size()
    {
        return 2;
    }
}