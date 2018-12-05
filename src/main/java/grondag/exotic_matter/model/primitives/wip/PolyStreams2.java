package grondag.exotic_matter.model.primitives.wip;

import java.util.concurrent.ArrayBlockingQueue;

import grondag.exotic_matter.model.primitives.stream.IReadOnlyPolyStream;
import grondag.exotic_matter.model.primitives.stream.IWritablePolyStream;

public class PolyStreams2
{
    public static final int FORMAT_TAGS = PolyStreamFormat.HAS_TAG_FLAG;
    public static final int FORMAT_LINKS = PolyStreamFormat.HAS_LINK_FLAG;
    public static final int FORMAT_BOUNDS = PolyStreamFormat.HAS_BOUNDS_FLAG;
    
    private static final ArrayBlockingQueue<WritablePolyStream> writables = new ArrayBlockingQueue<>(256);
    private static final ArrayBlockingQueue<ReadOnlyPolyStream> readables = new ArrayBlockingQueue<>(256);
    
    public static IWritablePolyStream claimWritable()
    {
        return claimWritable(0);
    }
    
    public static IWritablePolyStream claimWritable(int formatFlags)
    {
        WritablePolyStream result = writables.poll();
        if(result == null)
            result = new WritablePolyStream();
        result.prepare(formatFlags);
        return result;
    }
    
    static void release(WritablePolyStream freeStream)
    {
        writables.offer(freeStream);
    }

    public static IReadOnlyPolyStream claimReadOnly(WritablePolyStream writablePolyStream, int formatFlags)
    {
        ReadOnlyPolyStream result = readables.poll();
        if(result == null)
            result = new ReadOnlyPolyStream();
        result.load(writablePolyStream, formatFlags);
        return result;
    }

    public static void release(ReadOnlyPolyStream freeStream)
    {
        readables.offer(freeStream);
    }
    
    
}
