package grondag.exotic_matter.model.primitives.stream;

import javax.annotation.Nullable;

import grondag.exotic_matter.model.primitives.polygon.IMutablePolygon;

public class SimpleMutablePolyStream extends SimpleWritablePolyStream implements IMutablePolyStream
{
    protected int editIndex = 0;
    
    @Override
    public @Nullable IMutablePolygon editor()
    {
        return editIndex < polys.size() ? (IMutablePolygon) polys.get(editIndex) : null;
    }

    @Override
    public void editorOrigin()
    {
        editIndex = 0;
    }

    @Override
    public boolean editorNext()
    {
        final int size = polys.size();
        return editIndex < size ? ++editIndex < size : false;
    }

    @Override
    public boolean editorAtEnd()
    {
        return editIndex >= polys.size();
    }

    @Override
    public int editorGetAddress()
    {
        validateIndex(editIndex);
        return editIndex;
    }

    @Override
    public void editorMoveTo(int address)
    {
        validateIndex(address);
        editIndex = address;
    }

    @Override
    public void append()
    {
        append(writer.claimCopy());
        loadDefaults();
    }
    
    @Override
    public IPolyStream cloneToReader()
    {
        SimpleReadablePolyStream result = new SimpleReadablePolyStream();
        final int limit = this.size();
        for(int i = 0; i < limit; i++)
        {
            result.append(((IMutablePolygon)polys.get(i)).toPainted(), links.getInt(i), marks.get(i));
        }
        return result;
    }

    @Override
    public IPolyStream convertToReader()
    {
        return cloneToReader();
    }
    
}
