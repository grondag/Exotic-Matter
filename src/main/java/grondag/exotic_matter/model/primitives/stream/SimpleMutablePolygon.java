package grondag.exotic_matter.model.primitives.stream;

import static grondag.exotic_matter.model.primitives.stream.PolyStreamFormat.MUTABLE_FLAG;
import static grondag.exotic_matter.model.primitives.stream.PolyStreamFormat.isMutable;

import grondag.exotic_matter.varia.intstream.IntStreams;

public class SimpleMutablePolygon extends StreamBackedMutablePolygon
{
    public SimpleMutablePolygon()
    {
        this(1, 4);
    }
    
    public SimpleMutablePolygon(int layerCount, int vertexCount)
    {
        stream = IntStreams.claim();
        baseAddress = 0;
        prepare(layerCount, vertexCount);
    }
    
    public void prepare(int layerCount, int vertexCount)
    {
        stream.clear();
        int format = PolyStreamFormat.setLayerCount(MUTABLE_FLAG, layerCount);
        assert isMutable(format);
        format = PolyStreamFormat.setVertexCount(format, vertexCount);
        this.setFormat(format);
        loadStandardDefaults();
    }

    @Override
    public void release()
    {
        super.release();
        stream.release();
        stream = null;
    }
}
