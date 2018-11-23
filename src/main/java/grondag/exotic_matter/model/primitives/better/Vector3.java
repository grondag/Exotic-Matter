package grondag.exotic_matter.model.primitives.better;

class Vector3<T> extends AbstractVector<T>
{
    protected final T v0;
    protected final T v1;
    protected final T v2;
    
    Vector3(T v0, T v1, T v2)
    {
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
    }
    
    @Override
    T get(int index)
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
    int size()
    {
        return 3;
    }
}