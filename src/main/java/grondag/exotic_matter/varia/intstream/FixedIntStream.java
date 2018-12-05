package grondag.exotic_matter.varia.intstream;

import java.util.Arrays;

public class FixedIntStream implements IIntStream
{
    public final int[] data;
    
    public FixedIntStream(int capacity)
    {
        data = new int[capacity];
    }
    
    @Override
    public int get(int address)
    {
        return data[address];
    }

    @Override
    public void set(int address, int value)
    {
        data[address] = value;
    }

    @Override
    public void clear()
    {
        Arrays.fill(data, 0);
    }
    
    public void arrayCopyIn(int[] fromArray, int fromPos, int toPos, int length)
    {
        System.arraycopy(fromArray, fromPos, data, toPos, length);
    }
    
    public void arrayCopyOut(int[] toArray, int fromPos, int toPos, int length)
    {
        System.arraycopy(data, fromPos, toArray, toPos, length);
    }
}
