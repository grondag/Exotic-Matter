package grondag.exotic_matter.model.primitives.stream;

import java.util.concurrent.ArrayBlockingQueue;

public class PolyStreams
{
    public static final int FORMAT_TAGS = PolyStreamFormat.HAS_TAG_FLAG;
    public static final int FORMAT_LINKS = PolyStreamFormat.HAS_LINK_FLAG;
    public static final int FORMAT_BOUNDS = PolyStreamFormat.CSG_FLAG;
    
    private static final ArrayBlockingQueue<WritablePolyStream> writables = new ArrayBlockingQueue<>(256);
    private static final ArrayBlockingQueue<MutablePolyStream> mutables = new ArrayBlockingQueue<>(128);
    private static final ArrayBlockingQueue<CsgPolyStream> csgStreams = new ArrayBlockingQueue<>(128);
    private static final ArrayBlockingQueue<ReadOnlyPolyStream> readables = new ArrayBlockingQueue<>(256);
    private static final ArrayBlockingQueue<DispatchPolyStream> dispatches = new ArrayBlockingQueue<>(256);
    
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
    
    public static IMutablePolyStream claimMutable(int formatFlags)
    {
        MutablePolyStream result = mutables.poll();
        if(result == null)
            result = new MutablePolyStream();
        result.prepare(formatFlags);
        return result;
    }
    
    static void release(MutablePolyStream freeStream)
    {
        mutables.offer(freeStream);
    }

    public static IReadOnlyPolyStream claimReadOnly(WritablePolyStream writablePolyStream, int formatFlags)
    {
        ReadOnlyPolyStream result = readables.poll();
        if(result == null)
            result = new ReadOnlyPolyStream();
        result.load(writablePolyStream, formatFlags);
        return result;
    }

    static void release(ReadOnlyPolyStream freeStream)
    {
        readables.offer(freeStream);
    }
    
    public static DispatchPolyStream claimDispatch()
    {
        DispatchPolyStream result = dispatches.poll();
        if(result == null)
            result = new DispatchPolyStream();
        result.prepare();
        return result;
    }
    
    static void release(DispatchPolyStream freeStream)
    {
        dispatches.offer(freeStream);
    }
    
    public static CsgPolyStream claimCSG()
    {
        CsgPolyStream result = csgStreams.poll();
        if(result == null)
            result = new CsgPolyStream();
        result.prepare();
        return result;
    }
    
    public static CsgPolyStream claimCSG(IPolyStream stream)
    {
        CsgPolyStream result = claimCSG();
        result.appendAll(stream);
        return result;
    }
    static void release(CsgPolyStream freeStream)
    {
        csgStreams.offer(freeStream);
    }
}
