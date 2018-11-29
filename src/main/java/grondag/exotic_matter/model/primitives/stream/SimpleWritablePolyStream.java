package grondag.exotic_matter.model.primitives.stream;

import grondag.exotic_matter.model.primitives.polygon.IMutablePolygon;
import grondag.exotic_matter.model.primitives.polygon.IPolygon;
import grondag.exotic_matter.model.primitives.polygon.MutablePolygonNxN;

public class SimpleWritablePolyStream extends SimpleAbstractPolyStream implements IWritablePolyStream
{
    protected final MutablePolygonNxN writer;
    private MutablePolygonNxN defaults;
    
    {
        clearDefaults();
        writer = new MutablePolygonNxN(8);
        loadDefaults();
    }
    
    @Override
    public IMutablePolygon writer()
    {
        return writer;
    }

    @Override
    public int writerAddress()
    {
        return polys.size();
    }

    @Override
    public void append()
    {
        super.append(writer.toPainted());
        loadDefaults();
    }

    @Override
    public void saveDefaults()
    {
        defaults.prepare(writer.layerCount(), writer.vertexCount());
        defaults.load(writer);
    }

    @Override
    public void clearDefaults()
    {
        defaults = new MutablePolygonNxN(8);
        defaults.prepare(1, 4);
    }

    @Override
    public void loadDefaults()
    {
        writer.prepare(defaults.layerCount(), defaults.vertexCount());
        writer.load(defaults);
    }

    @Override
    public void copyFromAddress(int address)
    {
        IPolygon source = polys.get(address);
        writer.prepare(source.layerCount(), source.vertexCount());
        writer.load(source);
    }

    @Override
    public IPolyStream cloneToReader()
    {
        SimpleWritablePolyStream result = new SimpleWritablePolyStream();
        final int limit = this.size();
        for(int i = 0; i < limit; i++)
        {
            result.append(polys.get(i), links.getInt(i), marks.get(i));
        }
        return result;
    }

    @Override
    public IPolyStream convertToReader()
    {
        return (IPolyStream) this;
    }

    @Override
    public void setVertexCount(int vertexCount)
    {
        writer.prepare(writer.layerCount(), vertexCount);
    }

    @Override
    public void setLayerCount(int layerCount)
    {
        writer.prepare(layerCount, writer.vertexCount());
    }
}
