package grondag.exotic_matter.model.primitives.stream;

import java.util.ArrayList;
import java.util.BitSet;

import javax.annotation.Nullable;

import grondag.exotic_matter.model.primitives.polygon.ForwardingPolygon;
import grondag.exotic_matter.model.primitives.polygon.IPolygon;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class SimpleAbstractPolyStream implements IPolyStream
{
    protected final ArrayList<IPolygon> polys;
    protected final IntArrayList links;
    protected final BitSet marks;
    protected final BitSet deletes;
    
    protected final ForwardingPolygon reader = new ForwardingPolygon();
    protected ForwardingPolygon polyA = new ForwardingPolygon();
    protected ForwardingPolygon polyB = new ForwardingPolygon();
    
    protected int readIndex = 0;
    protected int polyAIndex = -1;
    protected int polyBIndex = -1;
    
    protected int deleteCount = 0;
    
    protected SimpleAbstractPolyStream()
    {
        polys = new ArrayList<>();
        links = new IntArrayList();
        marks = new BitSet();
        deletes = new BitSet();
    }
    
    protected SimpleAbstractPolyStream(ArrayList<IPolygon> polys, IntArrayList links, BitSet marks, BitSet deletes)
    {
        this.polys = polys;
        this.links = links;
        this.marks = marks;
        this.deletes = deletes;
    }
    
    @Override
    public int size()
    {
        return polys.size();
    }

    @Override
    public @Nullable IPolygon reader()
    {
        return reader;
    }

    private void checkDeleted()
    {
        while(isDeleted(readIndex))
        {
            if(++readIndex < polys.size())
                reader.wrapped = polys.get(readIndex);
            else
                reader.wrapped = null;
        }
    }
    
    @Override
    public IPolyStream origin()
    {
        readIndex = 0;
        if(polys.size() > 0)
            reader.wrapped = polys.get(0);
        else
            reader.wrapped = null;
        checkDeleted();
        return this;
    }

    @Override
    public boolean next()
    {
        final int size = polys.size();
        if(readIndex >= size)
            return false;
        
        readIndex++;
        
        if(readIndex < size)
        {
            reader.wrapped = polys.get(readIndex);
            checkDeleted();
            return hasValue();
        }
        else
            return false;
    }

    @Override
    public boolean hasValue()
    {
        return readIndex < polys.size() && !isDeleted(readIndex);
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
        reader.wrapped = polys.get(readIndex);
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
        setLink(readIndex, linkAddress);
    }
    
    @Override
    public void setLink(int targetAddress, int linkAddress)
    {
        validateIndex(linkAddress);
        validateIndex(targetAddress);
        links.set(targetAddress, linkAddress + 1);
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
        if(link == IPolygon.NO_LINK)
        {
            readIndex = size;
            reader.wrapped = null;
            return false;
        }
        else
        {
            moveTo(link);
            if(isDeleted())
                return nextLink();
            else
                return true;
        }
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
        polyA.wrapped = polys.get(address);
        polyAIndex = address;
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
        polyB.wrapped = polys.get(address);
        polyBIndex = address;
        return polyB;
    }
   
    protected void append(IPolygon poly)
    {
        if(readIndex == polys.size())
            reader.wrapped = poly;
        polys.add(poly);
        links.add(0);
    }
    
    /**
     * For clone ops
     */
    protected final void append(IPolygon poly, int linkAddress, boolean mark)
    {
        final int size = polys.size();
        
        if(readIndex == size)
            reader.wrapped = poly;
        
        marks.set(size, mark);
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
    public boolean isDeleted()
    {
        return isDeleted(readIndex);
    }

    @Override
    public void setDeleted()
    {
        setDeleted(readIndex);
    }

    @Override
    public boolean isDeleted(int address)
    {
        return deletes.get(address);
    }

    @Override
    public void setDeleted(int address)
    {
        validateIndex(address);
        deletes.set(address);
    }
}
