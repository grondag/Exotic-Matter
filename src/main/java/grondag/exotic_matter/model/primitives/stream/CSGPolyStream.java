package grondag.exotic_matter.model.primitives.stream;

import grondag.exotic_matter.model.primitives.polygon.CSGMutablePolygonNxN;
import grondag.exotic_matter.model.primitives.polygon.IPolygon;
import grondag.exotic_matter.model.primitives.polygon.IPolygonExt.ICSGMutablePolygon;
import grondag.exotic_matter.model.primitives.polygon.IPolygonExt.ICSGPolygon;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class CSGPolyStream extends SimpleWritablePolyStream implements ICSGPolyStream
{
    protected final IntArrayList tags = new IntArrayList();
    
    protected final CSGMutablePolygonNxN writerCSG;
    
    public CSGPolyStream()
    {
        super(new CSGMutablePolygonNxN());
        writerCSG = (CSGMutablePolygonNxN)this.writer;
        
        reader.markSetter = (b) -> setMark(b);
        reader.markGetter = () -> isMarked();
        reader.deletedSetter = (b) -> setDeleted();
        reader.deletedGetter = () -> isDeleted();
        reader.linkSetter = (i) -> setLink(i);
        reader.linkGetter = () -> links.getInt(readIndex);
        reader.tagSetter = (i) -> setTag(i);
        reader.tagGetter = () -> getTag();
        
        polyA.markSetter = (b) -> setMark(polyAIndex, b);
        polyA.markGetter = () -> isMarked(polyAIndex);
        polyA.deletedSetter = (b) -> setDeleted(polyAIndex);
        polyA.deletedGetter = () -> isDeleted(polyAIndex);
        polyA.linkSetter = (i) -> setLink(polyAIndex, i);
        polyA.linkGetter = () -> links.getInt(polyAIndex);
        polyA.tagSetter = (i) -> setTag(polyAIndex, i);
        polyA.tagGetter = () -> getTag(polyAIndex);
        
        polyB.markSetter = (b) -> setMark(polyBIndex, b);
        polyB.markGetter = () -> isMarked(polyBIndex);
        polyB.deletedSetter = (b) -> setDeleted(polyBIndex);
        polyB.deletedGetter = () -> isDeleted(polyBIndex);
        polyB.linkSetter = (i) -> setLink(polyBIndex, i);
        polyB.linkGetter = () -> links.getInt(polyBIndex);
        polyB.tagSetter = (i) -> setTag(polyBIndex, i);
        polyB.tagGetter = () -> getTag(polyBIndex);
    }
    
    @Override
    public void append()
    {
        boolean mark = writerCSG.isMarked();
        int tag = writerCSG.getTag();
        int link = writerCSG.getLink();
        int address = this.writerAddress();
        
        super.append();
        tags.add(tag);
        setMark(address, mark);
        setLink(address, link);
        
        writerCSG.setMark(false);
        writerCSG.setTag(IPolygon.NO_TAG);
        writerCSG.setLink(IPolygon.NO_LINK);
    }


    @Override
    public void setTag(int tag)
    {
        tags.set(readIndex, tag);
    }

    @Override
    public void setTag(int address, int tag)
    {
        tags.set(address, tag);
    }

    @Override
    public int getTag()
    {
        return tags.getInt(readIndex);
    }

    @Override
    public int getTag(int address)
    {
        return tags.getInt(address);
    }


    @Override
    public ICSGMutablePolygon writer()
    {
        return writerCSG;
    }

    @Override
    public void copyFromAddress(int address)
    {
        super.copyFromAddress(address);
        writerCSG.setMark(this.isMarked(address));
        writerCSG.setTag(this.getTag(address));
    }


    @Override
    public void appendCopy(IPolygon poly)
    {
        int appendIndex = this.writerAddress();
        super.appendCopy(poly);
        this.setMark(appendIndex, poly.isMarked());
    }


    @Override
    public ICSGPolygon reader()
    {
        return reader;
    }

    @Override
    public ICSGPolygon polyA()
    {
        return polyA;
    }

    @Override
    public ICSGPolygon movePolyA(int address)
    {
        super.movePolyA(address);
        return polyA;
    }


    @Override
    public ICSGPolygon polyB()
    {
        return polyB;
    }


    @Override
    public ICSGPolygon movePolyB(int address)
    {
        super.movePolyB(address);
        return polyB;
    }
    
    @Override
    public boolean isDeleted()
    {
        return super.isDeleted();
    }

    @Override
    public void setDeleted()
    {
        super.setDeleted();
    }

    @Override
    public boolean isDeleted(int address)
    {
        return super.isDeleted(address);
    }

    @Override
    public void setDeleted(int address)
    {
       super.setDeleted(address);
    }

    @Override
    public void setMark(boolean isMarked)
    {
        super.setMark(isMarked);
    }

    @Override
    public void setMark(int address, boolean isMarked)
    {
        super.setMark(address, isMarked);
    }

    @Override
    public boolean isMarked()
    {
        return super.isMarked();
    }

    @Override
    public boolean isMarked(int address)
    {
        return super.isMarked(address);
    }

    @Override
    public void setLink(int linkAddress)
    {
        super.setLink(linkAddress);
    }

    @Override
    public void setLink(int targetAddress, int linkAddress)
    {
        super.setLink(targetAddress, linkAddress);
    }

    @Override
    public boolean hasLink()
    {
        return super.hasLink();
    }

    @Override
    public void clearLink()
    {
        super.clearLink();
    }

    @Override
    public boolean nextLink()
    {
        return super.nextLink();
    }
}
