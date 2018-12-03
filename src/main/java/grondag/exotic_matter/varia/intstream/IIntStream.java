package grondag.exotic_matter.varia.intstream;

public interface IIntStream
{
    int get(int address);
    
    void set(int address, int value);
    
    default void copyFrom(int targetAddress, IIntStream source, int sourceAddress, int length)
    {
        for(int i = 0; i < length; i++)
            set(targetAddress + i, source.get(sourceAddress + 1));
    }
    
    default void release() { }

    /**
     * Sets all ints in the stream to zero. Does not deallocate any storage.
     */
    void clear();
}
