package grondag.exotic_matter.model.primitives.stream;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import grondag.exotic_matter.model.primitives.polygon.IPolygon;
import grondag.exotic_matter.model.primitives.polygon.IStreamReaderPolygon;
import grondag.exotic_matter.varia.intstream.IIntStream;

public abstract class AbstractPolyStream implements IPolyStream
{
    protected IIntStream stream;
    
    /**
     * Address in stream where poly info starts.
     * Some streams with additional metadata may
     * start at something other than zero.
     */
    protected int originAddress;
    
    /**
     * Address where next append will occur.
     * Equivalently, shows how many ints have
     * been written after {@link #originAddress}.
     */
    protected int writeAddress;
    
    protected final StreamBackedPolygon reader = new StreamBackedPolygon();
    protected final StreamBackedPolygon polyA = new StreamBackedPolygon();
    protected final StreamBackedPolygon polyB = new StreamBackedPolygon();
    protected final StreamBackedMutablePolygon internal = new StreamBackedMutablePolygon();
    
    /**
     * Value used to initialize origin and writer for new streams and on reset.
     * Override if stream needs to pack metadatq at front.
     */
    protected int newOrigin()
    {
        return 0;
    }
    
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
    public boolean origin()
    {
        if(isEmpty())
        {
            reader.invalidate();
            return false;
        }
        else
        {
            reader.moveTo(originAddress);
            if(reader.isDeleted())
                next();
            return hasValue();
        }
    }

    protected boolean moveReaderToNext(StreamBackedPolygon targetReader)
    {
        int currentAddress = targetReader.baseAddress;
        if(currentAddress >= writeAddress || currentAddress == EncoderFunctions.BAD_ADDRESS)
            return false;
        
        int nextAddress = currentAddress + targetReader.stride();
        if(nextAddress >= writeAddress)
            return false;
        
        targetReader.moveTo(nextAddress);
        
        while(targetReader.isDeleted() && currentAddress < writeAddress)
        {
            nextAddress = currentAddress + targetReader.stride();
            targetReader.moveTo(nextAddress);
            currentAddress = nextAddress;
        }
        
        return currentAddress < writeAddress;
    }
    
    @Override
    public boolean next()
    {
        return moveReaderToNext(this.reader);
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

    @Override
    public IPolygon reader(int address)
    {
        moveTo(address);
        return reader;
    }
    
    void prepare(IIntStream stream)
    {
        didRelease.set(false);
        this.stream = stream;
        originAddress = newOrigin();
        reader.stream = stream;
        polyA.stream = stream;
        polyB.stream = stream;
        internal.stream =stream;
        writeAddress = originAddress;
        
        // force error on read
        reader.invalidate();
        polyA.invalidate();
        polyB.invalidate();
        internal.invalidate();
    }
    
    @Override
    public final void release()
    {
        if(didRelease.compareAndSet(false, true) && readerCount.get() == 0)
        {
            doRelease();
            returnToPool();
        }
    }
    
    /**
     * Called after {@link #doRelease()} to return this instance to allocation pool.
     * Not part of {@link #doRelease()} to allow call of super.doRelease.
     */
    protected abstract void returnToPool();
    
    /**
     * Will be called after release is called and no more concurrent readers are active.
     */
    protected void doRelease()
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
    public void movePolyA(int address)
    {
        validateAddress(address);
        polyA.moveTo(address);
    }
    
    @Override
    public IPolygon polyA(int address)
    {
        movePolyA(address);
        return polyA;
    }

    @Override
    public IPolygon polyB()
    {
        return polyB;
    }

    @Override
    public void movePolyB(int address)
    {
        validateAddress(address);
        polyB.moveTo(address);
    }
    
    @Override
    public IPolygon polyB(int address)
    {
        movePolyB(address);
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
    public int getLink(int address)
    {
        validateAddress(address);
        internal.moveTo(address);
        return internal.getLink();
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

    protected boolean moveReaderToNextLink(StreamBackedPolygon targetReader)
    {
        int currentAddress = targetReader.baseAddress;
        if(currentAddress >= writeAddress || currentAddress == EncoderFunctions.BAD_ADDRESS)
            return false;
        
        int nextAddress = targetReader.getLink();
        if(nextAddress == IPolygon.NO_LINK_OR_TAG || nextAddress >= writeAddress)
            return false;
        
        targetReader.moveTo(nextAddress);
        currentAddress = nextAddress;
        
        while(currentAddress < writeAddress && targetReader.isDeleted())
        {
            nextAddress = targetReader.getLink();
            if(nextAddress == IPolygon.NO_LINK_OR_TAG || nextAddress >= writeAddress)
                return false;
            
            targetReader.moveTo(nextAddress);
            currentAddress = nextAddress;
        }
        
        return currentAddress < writeAddress && !targetReader.isDeleted();
    }
    
    @Override
    public boolean nextLink()
    {
        return moveReaderToNextLink(this.reader);
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
    
    private static class ThreadSafeReader extends StreamBackedPolygon implements IStreamReaderPolygon
    {
        AbstractPolyStream polyStream;

        @Override
        public final void release()
        {
            super.release();
            if(polyStream.readerCount.decrementAndGet() == 0 && polyStream.didRelease.get() == true)
                polyStream.doRelease();
            stream = null;
            polyStream = null;
            safeReaders.offer(this);
        }

        @Override
        public final void retain()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public final void releaseLast()
        {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public void moveTo(int address)
        {
            super.moveTo(address);
        }

        @Override
        public boolean hasValue()
        {
            return polyStream.isValidAddress(baseAddress);
        }

        @Override
        public boolean next()
        {
            return polyStream.moveReaderToNext(this);
        }

        @Override
        public boolean nextLink()
        {
            return polyStream.moveReaderToNextLink(this);
        }
    }
    
    private static final ArrayBlockingQueue<ThreadSafeReader> safeReaders = new ArrayBlockingQueue<>(256);
    
    /**
     * True once our release method has been called. Reset on prepare.
     */
    protected final AtomicBoolean didRelease = new AtomicBoolean();
    protected final AtomicInteger readerCount = new AtomicInteger();
    
    
    /**
     * Should only be exposed for streams that are immutable.
     */
    protected IStreamReaderPolygon claimThreadSafeReaderImpl()
    {
        readerCount.incrementAndGet();
        
        if(this.didRelease.get())
        {
            readerCount.decrementAndGet();
            throw new UnsupportedOperationException("Cannot claim threadsafe reader on released stream.");
        }
        
        ThreadSafeReader reader = safeReaders.poll();
        if(reader == null)
            reader = new ThreadSafeReader();
        
        reader.polyStream = this;
        reader.stream = this.stream;
        reader.moveTo(this.originAddress);
        return reader;
    }
}
