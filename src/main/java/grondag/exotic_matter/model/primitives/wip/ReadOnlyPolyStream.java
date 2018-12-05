package grondag.exotic_matter.model.primitives.wip;

import grondag.exotic_matter.model.primitives.polygon.IPolygon;
import grondag.exotic_matter.model.primitives.stream.IReadOnlyPolyStream;
import grondag.exotic_matter.varia.intstream.IntStreams;

public class ReadOnlyPolyStream extends AbstractPolyStream implements IReadOnlyPolyStream
{
    void load(WritablePolyStream streamIn, int formatFlags)
    {
        int capacity = 0;
        
        if(!streamIn.isEmpty())
        {
            streamIn.origin();
            IPolygon reader = streamIn.reader();
            do
                capacity += PolyStreamFormat.minimalFixedSize(reader, formatFlags);
            while(streamIn.next());
        }
        
        prepare(IntStreams.claim(capacity));
        
        if(!streamIn.isEmpty())
        {
            streamIn.origin();
            IPolygon reader = streamIn.reader();
            do
                this.appendCopy(reader, formatFlags);
            while(streamIn.next());
        }
        
        this.stream.compact();
    }
    
    @Override
    public void release()
    {
        super.release();
        PolyStreams2.release(this);
    }
}
