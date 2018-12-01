package grondag.exotic_matter.model.primitives.wip;

import net.minecraft.util.math.MathHelper;

public class CrudeIntStream implements IIntStream
{
    private int[] data = new int[256];
    
    @Override
    public int get(int address)
    {
        return data[address];
    }

    @Override
    public void set(int address, int value)
    {
        if(address >= data.length)
        {
            int[] newData = new int[MathHelper.smallestEncompassingPowerOfTwo(address)];
            System.arraycopy(data, 0, newData, 0, data.length);
            data = newData;
        }
        data[address] = value;
    }
}
