package grondag.exotic_matter.varia.intstream;

import net.minecraft.util.math.MathHelper;
import scala.actors.threadpool.Arrays;

public class CrudeIntStream implements IIntStream
{
    private int[] data = new int[256];
    
    @Override
    public int get(int address)
    {
        return address >= data.length ? 0 : data[address];
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

    @Override
    public void clear()
    {
        Arrays.fill(data, 0);
    }
}
