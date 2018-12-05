package grondag.exotic_matter.model.primitives.wip;

import grondag.exotic_matter.model.primitives.polygon.IPolygon;
import grondag.exotic_matter.model.primitives.stream.IReadOnlyPolyStream;
import grondag.exotic_matter.varia.intstream.FixedIntStream;

public class ReadOnlyPolyStream extends AbstractPolyStream implements IReadOnlyPolyStream
{
    public ReadOnlyPolyStream(WritablePolyStream streamIn, int formatFlags)
    {
        super();
        int capacity = 0;
        
        if(!streamIn.isEmpty())
        {
            streamIn.origin();
            IPolygon reader = streamIn.reader();
            do
                capacity += PolyStreamFormat.minimalFixedSize(reader, formatFlags);
            while(streamIn.next());
        }
        
        prepare(new FixedIntStream(capacity));
        
        if(!streamIn.isEmpty())
        {
            streamIn.origin();
            IPolygon reader = streamIn.reader();
            do
                this.appendCopy(reader, formatFlags);
            while(streamIn.next());
        }
    }
}
