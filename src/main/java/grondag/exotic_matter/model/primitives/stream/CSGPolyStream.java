package grondag.exotic_matter.model.primitives.stream;

import grondag.exotic_matter.model.primitives.polygon.CSGMutablePolygonNxN;
import grondag.exotic_matter.model.primitives.polygon.IPolygon;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class CSGPolyStream extends SimpleWritablePolyStream implements ICSGPolyStream
{
    protected final IntArrayList tags = new IntArrayList();
    
    public CSGPolyStream()
    {
        super(new CSGMutablePolygonNxN());
        
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
        boolean mark = writer.isMarked();
        int tag = writer.getTag();
        int link = writer.getLink();
        int address = this.writerAddress();
        
        super.append();
        tags.add(tag);
        setMark(address, mark);
        setLink(address, link);
        
        writer.setMark(false);
        writer.setTag(IPolygon.NO_LINK_OR_TAG);
        writer.setLink(IPolygon.NO_LINK_OR_TAG);
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
    public void copyFromAddress(int address)
    {
        super.copyFromAddress(address);
        writer.setMark(this.isMarked(address));
        writer.setTag(this.getTag(address));
    }


    @Override
    public void appendCopy(IPolygon poly)
    {
        int appendIndex = this.writerAddress();
        super.appendCopy(poly);
        this.setMark(appendIndex, poly.isMarked());
    }
}
