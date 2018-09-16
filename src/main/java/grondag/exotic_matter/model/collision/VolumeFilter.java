package grondag.exotic_matter.model.collision;

import java.util.function.IntConsumer;
//TODO: finish or remove
public class VolumeFilter
{
    final int x;
    final int y;
    final int z;
    final int[] volumeKeys;
    
    public VolumeFilter(int x, int y, int z, int[] volumeKeys)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.volumeKeys = volumeKeys;
    }
    
    public void forEachFilteredVolume(BoxFinder bf, IntConsumer consumer)
    {
        if(bf.isFilled(x, y, z))
            for(int i : volumeKeys)
                consumer.accept(i);
    }
}
