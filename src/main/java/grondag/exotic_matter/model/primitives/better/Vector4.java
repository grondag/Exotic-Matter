package grondag.exotic_matter.model.primitives.better;

final class Vector4<T> extends Vector3<T>
{
    protected final T v3;
    
    Vector4(T v0, T v1, T v2, T v3)
    {
        super(v0, v1, v2);
        this.v3 = v3;
    }
    
    @Override
    final T get(int index)
    {
        switch(index)
        {
        case 0:
            return v0;
        case 1:
            return v1;
        case 2:
            return v2;
        case 3:
            return v3;
        default:
            throw new IndexOutOfBoundsException();
        }
    }
    
    @Override
    final int size()
    {
        return 4;
    }
}