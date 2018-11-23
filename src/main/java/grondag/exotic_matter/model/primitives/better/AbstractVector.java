package grondag.exotic_matter.model.primitives.better;

abstract class AbstractVector<T>
{
    abstract T get(int index);
    
    abstract int size();
}
