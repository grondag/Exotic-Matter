package grondag.exotic_matter.model.primitives.stream;

import java.util.ArrayList;
import java.util.BitSet;

import grondag.exotic_matter.model.primitives.polygon.IPolygon;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class SimpleReadablePolyStream extends SimpleAbstractPolyStream
{
    public SimpleReadablePolyStream()
    {
        super();
    }
    
    protected SimpleReadablePolyStream(ArrayList<IPolygon> polys, IntArrayList links, BitSet marks)
    {
        super(polys, links, marks);
    }
}
