package grondag.exotic_matter.cache;

public interface ObjectSimpleCacheLoader<K, V>
{
    abstract public V load(K key);
    
    /** for benchmark testing */
    default ObjectSimpleCacheLoader<K, V> createNew() { return null; };
}
