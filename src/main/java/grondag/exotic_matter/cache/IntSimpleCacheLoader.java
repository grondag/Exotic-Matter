package grondag.exotic_matter.cache;

public interface IntSimpleCacheLoader<V>
{
    abstract public V load(int key);
    
    // for testing only
    public default IntSimpleCacheLoader<V> createNew()
    { 
        throw new UnsupportedOperationException();
    }
}
