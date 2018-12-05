package grondag.exotic_matter.model.primitives.wip;

import grondag.exotic_matter.model.primitives.polygon.IPolygon;
import grondag.exotic_matter.model.primitives.stream.IPolyStream;
import grondag.exotic_matter.varia.intstream.IIntStream;

public abstract class AbstractPolyStream implements IPolyStream
{
    protected IIntStream stream;
    
    /**
     * Address in stream where poly info starts.
     * Some streams with additional metadata may
     * start at something other than zero.
     */
    protected int originAddress = 0;
    
    /**
     * Address where next append will occur.
     * Equivalently, shows how many ints have
     * been written after {@link #originAddress}.
     */
    protected int writeAddress = 0;
    
    protected final StreamBackedPolygon reader = new StreamBackedPolygon();
    protected final StreamBackedPolygon polyA = new StreamBackedPolygon();
    protected final StreamBackedPolygon polyB = new StreamBackedPolygon();
    protected final StreamBackedMutablePolygon internal = new StreamBackedMutablePolygon();
    
    protected final boolean isValidAddress(int address)
    {
        return address >= originAddress && address < writeAddress;
    }
    
    protected final void validateAddress(int address)
    {
        if(!isValidAddress(address))
            throw new IndexOutOfBoundsException();
    }
    
    @Override
    public final boolean isEmpty()
    {
        return writeAddress == originAddress;
    }

    @Override
    public final IPolygon reader()
    {
        return reader;
    }

    @Override
    public void origin()
    {
        if(!isEmpty())
            reader.moveTo(originAddress);
    }

    @Override
    public boolean next()
    {
        int currentAddress = reader.baseAddress;
        if(currentAddress >= writeAddress || currentAddress == EncoderFunctions.BAD_ADDRESS)
            return false;
        
        int nextAddress = currentAddress + reader.stride();
        if(nextAddress >= writeAddress)
            return false;
        
        reader.moveTo(nextAddress);
        
        while(reader.isDeleted() && currentAddress < writeAddress)
        {
            nextAddress = currentAddress + reader.stride();
            reader.moveTo(nextAddress);
            currentAddress = nextAddress;
        }
        
        return currentAddress < writeAddress;
    }

    @Override
    public boolean hasValue()
    {
        return isValidAddress(reader.baseAddress) && !reader.isDeleted();
    }

    @Override
    public int getAddress()
    {
        return reader.baseAddress;
    }

    @Override
    public void moveTo(int address)
    {
        validateAddress(address);
        reader.moveTo(address);
    }

    void prepare(IIntStream stream)
    {
        this.stream = stream;
        reader.stream = stream;
        polyA.stream = stream;
        polyB.stream = stream;
        internal.stream =stream;
        originAddress = 0;
        writeAddress = 0;
        
        // force error on read
        reader.invalidate();
        polyA.invalidate();
        polyB.invalidate();
        internal.invalidate();
    }
    
    @Override
    public void release()
    {
        reader.invalidate();
        reader.stream = null;
        polyA.stream = null;
        polyB.stream = null;
        internal.stream = null;
        stream.release();
        stream = null;
    }

    @Override
    public IPolygon polyA()
    {
        return polyA;
    }

    @Override
    public IPolygon movePolyA(int address)
    {
        validateAddress(address);
        polyA.moveTo(address);
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
        validateAddress(address);
        polyA.moveTo(address);
        return polyB;
    }

    @Override
    public void setTag(int tag)
    {
        reader.setTag(tag);
    }

    @Override
    public void setTag(int address, int tag)
    {
        validateAddress(address);
        internal.moveTo(address);
        internal.setTag(tag);
    }

    @Override
    public int getTag()
    {
        return hasValue() ? reader.getTag() : IPolygon.NO_LINK_OR_TAG;
    }

    @Override
    public int getTag(int address)
    {
        validateAddress(address);
        internal.moveTo(address);
        return internal.getTag();
    }

    @Override
    public boolean isDeleted()
    {
        return hasValue() ? reader.isDeleted() : false;
    }

    @Override
    public void setDeleted()
    {
        reader.setDeleted();
    }

    @Override
    public boolean isDeleted(int address)
    {
        validateAddress(address);
        internal.moveTo(address);
        return internal.isDeleted();
    }

    @Override
    public void setDeleted(int address)
    {
        validateAddress(address);
        internal.moveTo(address);
        internal.setDeleted();
    }

    @Override
    public void setLink(int linkAddress)
    {
        reader.setLink(linkAddress);
    }

    @Override
    public void setLink(int targetAddress, int linkAddress)
    {
        validateAddress(targetAddress);
        internal.moveTo(targetAddress);
        internal.setLink(linkAddress);
    }

    @Override
    public boolean hasLink()
    {
        return isValidAddress(reader.baseAddress) && reader.getLink() != IPolygon.NO_LINK_OR_TAG;
    }

    @Override
    public void clearLink()
    {
        reader.setLink(IPolygon.NO_LINK_OR_TAG);
    }

    @Override
    public boolean nextLink()
    {
        int currentAddress = reader.baseAddress;
        if(currentAddress >= writeAddress || currentAddress == EncoderFunctions.BAD_ADDRESS)
            return false;
        
        int nextAddress = reader.getLink();
        if(nextAddress == IPolygon.NO_LINK_OR_TAG || nextAddress >= writeAddress)
            return false;
        
        reader.moveTo(nextAddress);
        currentAddress = nextAddress;
        
        while(currentAddress < writeAddress && reader.isDeleted())
        {
            nextAddress = reader.getLink();
            if(nextAddress == IPolygon.NO_LINK_OR_TAG || nextAddress >= writeAddress)
                return false;
            
            reader.moveTo(nextAddress);
            currentAddress = nextAddress;
        }
        
        return currentAddress < writeAddress && !reader.isDeleted();
    }

    @Override
    public void setMark(boolean isMarked)
    {
        reader.setMark(isMarked);
    }

    @Override
    public void setMark(int address, boolean isMarked)
    {
        validateAddress(address);
        internal.moveTo(address);
        internal.setMark(isMarked);
    }

    @Override
    public boolean isMarked()
    {
        return reader.isMarked();
    }

    @Override
    public boolean isMarked(int address)
    {
        validateAddress(address);
        internal.moveTo(address);
        return internal.isMarked();
    }

    protected void appendCopy(IPolygon polyIn, int withFormat)
    {
        final boolean needReaderLoad = reader.baseAddress == writeAddress;
        final int newFormat = PolyStreamFormat.minimalFixedFormat(polyIn, withFormat);
        stream.set(writeAddress, newFormat);
        internal.moveTo(writeAddress);
        internal.copyFrom(polyIn, true);
        writeAddress += PolyStreamFormat.polyStride(newFormat, true);
        
        if(needReaderLoad)
            reader.loadFormat();
    }
}
