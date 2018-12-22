package grondag.exotic_matter.model.primitives.stream;

public class PolyVertexMap
{

    public void addPoly(int polyAddress)
    {
        // TODO Auto-generated method stub
        
    }

    public void clear()
    {
        // TODO Auto-generated method stub
        
    }

    public boolean isEmpty()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public int size()
    {
        // TODO Auto-generated method stub
        return 0;
    }


    public class Cursor
    {

        public int polyAddress()
        {
            // TODO Auto-generated method stub
            return 0;
        }

        public void origin()
        {
            // TODO Auto-generated method stub
            
        }
        
        public void next()
        {
            // TODO Auto-generated method stub
            
        }
    }
    
    final Cursor cursor = new Cursor();
    
    public Cursor cursor()
    {
        return cursor;
    }

    
}
