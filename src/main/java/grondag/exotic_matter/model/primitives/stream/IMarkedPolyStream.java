package grondag.exotic_matter.model.primitives.stream;

public interface IMarkedPolyStream extends IPolyStream
{
    /**
     * Mark this poly or clear the mark for the poly at the read cursor.  Meaning of mark is up to user.<p>
     * 
     * This, along with {@link #setLink(int)} are the only 
     * mutating options allowed on a stream reader.<p>
     * 
     * Not meant for concurrent use.
     */
    void setMark(boolean isMarked);
    
    /**
     * Absolute address version of {@link #setMark(boolean)}
     */
    void setMark(int address, boolean isMarked);

    boolean isMarked();
    
    boolean isMarked(int address);
    
    default void flipMark()
    {
        setMark(!isMarked());
    }
    
    default void flipMark(int address)
    {
        setMark(!isMarked(address));
    }
}
