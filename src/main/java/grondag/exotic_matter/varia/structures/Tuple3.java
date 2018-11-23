package grondag.exotic_matter.varia.structures;

public class Tuple3<T> extends Tuple2<T>
{
    protected final T v2;
    
    Tuple3(T v0, T v1, T v2)
    {
        super(v0, v1);
        this.v2 = v2;
    }
    
    @Override
    public T get(int index)
    {
        switch(index)
        {
        case 0:
            return v0;
        case 1:
            return v1;
        case 2:
            return v2;
        default:
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public int size()
    {
        return 3;
    }
}