package grondag.exotic_matter.model.primitives.stream;

import grondag.exotic_matter.model.primitives.polygon.IMutablePolygon;
import grondag.exotic_matter.model.primitives.polygon.IPolygon;
import grondag.exotic_matter.varia.intstream.IIntStream;
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
    protected final StreamBackedPolygon copyFrom = new StreamBackedPolygon();
    
    protected IIntStream writerStream;
    protected IIntStream defaultStream;
    protected int formatFlags = 0;
    
    public WritablePolyStream()
    {
        writer = new StreamBackedMutablePolygon();
        writer.baseAddress = 0;
    }
    
    void prepare(int formatFlags)
    {
        super.prepare(IntStreams.claim());
        copyFrom.stream = stream;
        defaultStream = IntStreams.claim();
        writerStream = IntStreams.claim();
        writer.stream = writerStream;
        this.formatFlags = formatFlags | PolyStreamFormat.MUTABLE_FLAG;
        clearDefaults();
        loadDefaults();
    }

    @Override
    protected void doRelease()
    {
        super.doRelease();
        copyFrom.stream = null;
        defaultStream.release();
        writerStream.release();
        defaultStream = null;
        writerStream = null;
        PolyStreams.release(this);
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
        writer.clearFaceNormal();
        defaultStream.clear();
        defaultStream.copyFrom(0, writerStream, 0, PolyStreamFormat.polyStride(writer.format(), false));
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
        
        writer.clearFaceNormal();
        
        writer.stream = writerStream;
        writer.loadFormat();
    }

    @Override
    public void loadDefaults()
    {
        writerStream.clear();
        writerStream.copyFrom(0, defaultStream, 0, MAX_STRIDE);
        writer.loadFormat();
    }

    @Override
    public IReadOnlyPolyStream releaseAndConvertToReader(int formatFlags)
    {
        IReadOnlyPolyStream result = PolyStreams.claimReadOnly(this, formatFlags);
        release();
        return result;
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
        super.appendCopy(poly, formatFlags);
    }
}
