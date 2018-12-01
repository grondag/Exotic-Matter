package grondag.exotic_matter.model.primitives.stream;

public interface ITaggedPolyStream
{
    /**
     * Mark this poly or clear the mark for the poly at the read cursor.  Meaning of mark is up to user.<p>
     * 
     * This, along with {@link #setLink(int)} are the only 
     * mutating options allowed on a stream reader.<p>
     * 
     * Not meant for concurrent use.
     */
    void setTag(int tag);
    
    /**
     * Absolute address version of {@link #setMark(boolean)}
     */
    void setTag(int address, int tag);

    int getTag();
    
    int getTag(int address);
}
