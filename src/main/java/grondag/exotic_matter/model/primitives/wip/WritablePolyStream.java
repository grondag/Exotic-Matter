package grondag.exotic_matter.model.primitives.wip;

import grondag.exotic_matter.model.primitives.polygon.IMutablePolygon;
import grondag.exotic_matter.model.primitives.polygon.IPolygon;
import grondag.exotic_matter.model.primitives.stream.IReadOnlyPolyStream;
import grondag.exotic_matter.model.primitives.stream.IWritablePolyStream;
import grondag.exotic_matter.varia.intstream.FixedIntStream;
import grondag.exotic_matter.varia.intstream.IntStreams;

public class WritablePolyStream extends AbstractPolyStream implements IWritablePolyStream
{
    private static  final int MAX_STRIDE;
    
    static
    {
        final int maxFormat = PolyStreamFormat.MUTABLE_FLAG | PolyStreamFormat.HAS_LINK_FLAG
            | PolyStreamFormat.HAS_TAG_FLAG | PolyStreamFormat.HAS_BOUNDS_FLAG;
        
        MAX_STRIDE = 1 + StaticEncoder.INTEGER_WIDTH + PolyEncoder.get(maxFormat).stride();
    }

    protected final StreamBackedMutablePolygon writer;
    protected final FixedIntStream writerStream;
    protected final FixedIntStream defaultStream;
    protected int formatFlags = 0;
    
    public WritablePolyStream()
    {
        writer = new StreamBackedMutablePolygon();
        
        // PERF: consider using regular streams here once fast copy in place
        defaultStream = new FixedIntStream(MAX_STRIDE);
        writerStream = new FixedIntStream(8192);
        writer.stream = writerStream;
        writer.baseAddress = 0;
    }
    
    void prepare(int formatFlags)
    {
        super.prepare(IntStreams.claim());
        this.formatFlags = formatFlags | PolyStreamFormat.MUTABLE_FLAG;
        clearDefaults();
        loadDefaults();
    }

    @Override
    public void release()
    {
        super.release();
    }

    @Override
    public IMutablePolygon writer()
    {
        return writer;
    }
    
    @Override
    public int writerAddress()
    {
        return writeAddress;
    }

    @Override
    public void append()
    {
        super.appendCopy(writer, formatFlags);
    }

    @Override
    public void saveDefaults()
    {
        defaultStream.clear();
        System.arraycopy(writerStream.data, 0, defaultStream.data, 0, PolyStreamFormat.polyStride(writer.format(), false));
    }

    @Override
    public void clearDefaults()
    {
        defaultStream.clear();
        defaultStream.set(0, PolyStreamFormat.setVertexCount(formatFlags, 4));
        writer.stream = defaultStream;
        writer.loadFormat();
        
        writer.setMaxU(0, 1f);
        writer.setMaxU(1, 1f);
        writer.setMaxU(2, 1f);
        
        writer.setMaxV(0, 1f);
        writer.setMaxV(1, 1f);
        writer.setMaxV(2, 1f);
        
        writer.stream = writerStream;
        writer.loadFormat();
    }

    @Override
    public void loadDefaults()
    {
        writerStream.clear();
        System.arraycopy(defaultStream.data, 0, writerStream.data, 0, MAX_STRIDE);
        writer.loadFormat();
    }

    @Override
    public void copyFromAddress(int address)
    {
        validateAddress(address);
        internal.moveTo(address);
        super.appendCopy(internal, formatFlags);
    }

    @Override
    public IReadOnlyPolyStream releaseAndConvertToReader(int formatFlags)
    {
        return new ReadOnlyPolyStream(this, formatFlags);
    }

    @Override
    public void setVertexCount(int vertexCount)
    {
        writer.setFormat(PolyStreamFormat.setVertexCount(writer.format(), vertexCount));
    }

    @Override
    public void setLayerCount(int layerCount)
    {
        writer.setLayerCount(layerCount);
    }

    @Override
    public void appendCopy(IPolygon poly)
    {
        super.appendCopy(poly, PolyStreamFormat.minimalFixedFormat(poly, formatFlags));
    }
}
