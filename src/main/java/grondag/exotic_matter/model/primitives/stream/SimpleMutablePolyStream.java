package grondag.exotic_matter.model.primitives.stream;

import javax.annotation.Nullable;

import grondag.exotic_matter.model.primitives.PolyFactory;
import grondag.exotic_matter.model.primitives.polygon.ForwardingMutablePolygon;
import grondag.exotic_matter.model.primitives.polygon.IMutablePolygon;
import grondag.exotic_matter.model.primitives.polygon.IPolygon;

public class SimpleMutablePolyStream extends SimpleWritablePolyStream implements IMutablePolyStream
{
    private final ForwardingMutablePolygon editor = new ForwardingMutablePolygon();
    protected int editIndex = 0;
    protected int deleteCount = 0;
    
    private static final IPolygon DELETED = PolyFactory.COMMON_POOL.newPaintable(3, 1).toPainted();
    
    @Override
    public @Nullable IMutablePolygon editor()
    {
        return editor;
    }

    private void checkEditorDeleted()
    {
        if(editor.wrapped == DELETED)
        {
            final int size = polys.size();
            while(editor.wrapped == DELETED)
            {
                if(++editIndex < size)
                    editor.wrapped = polys.get(editIndex);
                else
                    editor.wrapped = null;
            }
        }
    }
    
    @Override
    public void editorOrigin()
    {
        editIndex = 0;
        if(polys.isEmpty())
            editor.wrapped = null;
        else
        {
            editor.wrapped = polys.get(0);
            checkEditorDeleted();
        }
    }

    @Override
    public boolean editorNext()
    {
        final int size = polys.size();
        if(editIndex < size)
        {
            if(++editIndex < size)
            {
                editor.wrapped = polys.get(editIndex);
                checkEditorDeleted();
                return editIndex < size;
            }
            else
            {
                editor.wrapped = null;
                return false;
            }
        }
        else
            return false;
    }

    @Override
    public boolean editorHasValue()
    {
        return editIndex < polys.size() && editor.wrapped != DELETED && editor.wrapped != null;
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
        editor.wrapped = polys.get(address);
        checkEditorDeleted();
    }

    @Override
    public void editorDelete()
    {
        delete(editIndex);
    }

    @Override
    public void delete(int address)
    {
        validateIndex(address);
        deleteCount++;
        polys.set(address, DELETED);
        if(address == editIndex)
        {
            editor.wrapped = DELETED;
            checkEditorDeleted();
        }
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
            IPolygon poly = polys.get(i);
            if(poly != DELETED)
                result.append(((IMutablePolygon)polys).toPainted(), links.getInt(i), marks.get(i));
        }
        return result;
    }

    
    
    @Override
    public int size()
    {
        return super.size() - deleteCount;
    }

    private void checkDeleted()
    {
        if(reader.wrapped == DELETED)
        {
            final int size = polys.size();
            while(reader.wrapped == DELETED)
            {
                if(++readIndex < size)
                    reader.wrapped = polys.get(readIndex);
                else
                    reader.wrapped = null;
            }
        }
    }
    
    @Override
    public void origin()
    {
        super.origin();
        checkDeleted();
    }

    @Override
    public boolean next()
    {
        super.next();
        checkDeleted();
        return editIndex < polys.size();
    }

    @Override
    public boolean hasValue()
    {
        return super.hasValue() && reader.wrapped != null && reader.wrapped != DELETED;
    }

    @Override
    public boolean nextLink()
    {
        boolean result = super.nextLink();
        while(result)
        {
            if(reader.wrapped != DELETED)
                break;
            
            result = super.nextLink();
        }
        
        return result;
    }

    @Override
    public IWritablePolyStream cloneToWritable()
    {
        SimpleWritablePolyStream result = new SimpleWritablePolyStream();
        final int limit = this.size();
        for(int i = 0; i < limit; i++)
        {
            IMutablePolygon poly = (IMutablePolygon) polys.get(i);
            if(poly != DELETED)
                result.append(poly.toPainted(), links.getInt(i), marks.get(i));
        }
        return result;
    }

    @Override
    public IPolyStream convertToReader()
    {
        throw new UnsupportedOperationException("Mutable poly streams can't be directly converted.");
    }

    @Override
    public IPolyStream claimReader()
    {
        throw new UnsupportedOperationException("Mutable poly streams don't support extra readers.");
    }
}
