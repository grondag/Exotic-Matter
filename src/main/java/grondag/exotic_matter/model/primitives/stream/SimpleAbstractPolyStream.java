package grondag.exotic_matter.model.primitives.stream;

import java.util.ArrayList;
import java.util.BitSet;

import javax.annotation.Nullable;

import grondag.exotic_matter.model.primitives.polygon.IPolygon;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public abstract class SimpleAbstractPolyStream implements IPolyStream
{
    protected final ArrayList<IPolygon> polys;
    protected final IntArrayList links;
    protected final BitSet marks;
    
    private IPolygon polyA;
    private IPolygon polyB;
    
    protected int readIndex = 0;
    
    protected SimpleAbstractPolyStream()
    {
        polys = new ArrayList<>();
        links = new IntArrayList();
        marks = new BitSet();
    }
    
    protected SimpleAbstractPolyStream(ArrayList<IPolygon> polys, IntArrayList links, BitSet marks)
    {
        this.polys = polys;
        this.links = links;
        this.marks = marks;
    }
    
    @Override
    public int size()
    {
        return polys.size();
    }

    @Override
    public @Nullable IPolygon reader()
    {
        return readIndex < polys.size() ? polys.get(readIndex) : null;
    }

    @Override
    public void origin()
    {
        readIndex = 0;
    }

    @Override
    public boolean next()
    {
        final int size = polys.size();
        return readIndex < size ? ++readIndex < size : false;
    }

    @Override
    public boolean atEnd()
    {
        return readIndex >= polys.size();
    }

    @Override
    public int getAddress()
    {
        validateIndex(readIndex);
        return readIndex;
    }

    protected void validateIndex(int index)
    {
        if(index <= 0 || index >= polys.size())
            throw new IndexOutOfBoundsException();
    }
    
    @Override
    public void moveTo(int address)
    {
        validateIndex(address);
        readIndex = address;
    }
    
    @Override
    public void setMark(boolean isMarked)
    {
        setMark(readIndex, isMarked);
    }
    
    @Override
    public void setMark(int address, boolean isMarked)
    {
        validateIndex(address);
        marks.set(address, isMarked);
    }

    @Override
    public boolean isMarked()
    {
        return isMarked(readIndex);
    }
    
    @Override
    public boolean isMarked(int address)
    {
        validateIndex(address);
        return marks.get(address);
    }
    
    @Override
    public void setLink(int linkAddress)
    {
        validateIndex(linkAddress);
        validateIndex(readIndex);
        links.set(readIndex, linkAddress + 1);
    }

    @Override
    public boolean hasLink()
    {
        validateIndex(readIndex);
        return links.getInt(readIndex) != 0;
    }

    @Override
    public void clearLink()
    {
        validateIndex(readIndex);
        links.set(readIndex, 0);
    }

    @Override
    public boolean nextLink()
    {
        final int size = polys.size();
        if(readIndex >= size)
            return false;
        
        int link = links.getInt(readIndex);
        if(link == 0)
            return false;
        
        moveTo(link - 1);
        return true;
    }

    @Override
    public void release()
    {
        // NOOP
    }

    @Override
    public IPolygon polyA()
    {
        return polyA;
    }

    @Override
    public IPolygon movePolyA(int address)
    {
        validateIndex(address);
        polyA = polys.get(address);
        return polyA;
    }

    @Override
    public IPolygon polyB()
    {
        return polyB;
    }

    @Override
    public IPolygon movePolyB(int address)
    {
        validateIndex(address);
        polyB = polys.get(address);
        return polyB;
    }
   
    protected final void append(IPolygon poly)
    {
        polys.add(poly);
        links.add(0);
    }
    
    /**
     * For clone ops
     */
    protected final void append(IPolygon poly, int linkAddress, boolean mark)
    {
        marks.set(polys.size(), mark);
        polys.add(poly);
        links.add(linkAddress);
    }
    
    @Override
    public IWritablePolyStream cloneToWritable()
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
    public IPolyStream claimReader()
    {
        return new SimpleReadablePolyStream(polys, links, marks);
    }

}
