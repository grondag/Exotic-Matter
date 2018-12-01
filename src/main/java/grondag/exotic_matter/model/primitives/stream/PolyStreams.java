package grondag.exotic_matter.model.primitives.stream;

public class PolyStreams
{
    public static IWritablePolyStream claimWriter()
    {
        return new SimpleWritablePolyStream();
    }
    
    public static ICSGPolyStream claimCSG()
    {
        return new CSGPolyStream();
    }
}
