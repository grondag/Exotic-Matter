package grondag.exotic_matter.model.primitives.wip;

import static grondag.exotic_matter.model.primitives.wip.PolyStreamFormat.*;

public class SimpleMutablePolygon extends StreamBackedMutablePolygon
{
    public SimpleMutablePolygon()
    {
        this(1, 4);
    }
    
    public SimpleMutablePolygon(int layerCount, int vertexCount)
    {
        stream = new CrudeIntStream();
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
    }
}
