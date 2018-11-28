package grondag.exotic_matter.model.primitives.stream;

public interface IPolyStream
{
    int size();
    
    default boolean isEmpty()
    {
        return size() == 0;
    }
    
    IPolyStreamReader  claimReader();
    
    void release();
}
