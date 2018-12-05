package grondag.exotic_matter.model.primitives.stream;

import javax.annotation.Nullable;

import grondag.exotic_matter.model.primitives.polygon.ForwardingMutablePolygon;
import grondag.exotic_matter.model.primitives.polygon.IMutablePolygon;

public class SimpleMutablePolyStream extends SimpleWritablePolyStream implements IMutablePolyStream
{
    private final ForwardingMutablePolygon editor = new ForwardingMutablePolygon();
    protected int editIndex = 0;
    
    @Override
    public @Nullable IMutablePolygon editor()
    {
        return editor;
    }

    private void checkEditorDeleted()
    {
        while(isDeleted(editIndex))
        {
            if(++editIndex < polys.size())
                editor.wrapped = polys.get(editIndex);
            else
                editor.wrapped = null;
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
                return editorHasValue();
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
        return editIndex < polys.size() && !isDeleted(editIndex) && editor.wrapped != null;
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
    }

    @Override
    public void editorSetDeleted()
    {
        setDeleted(editIndex);
    }
    
    @Override
    public void append()
    {
        append(writer.claimCopy());
        loadDefaults();
    }
    
    @Override
    public IReadOnlyPolyStream releaseAndConvertToReader()
    {
        throw new UnsupportedOperationException("Mutable poly streams can't be directly converted.");
    }
}
