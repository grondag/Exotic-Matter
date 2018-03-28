package grondag.exotic_matter.world;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableList;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public class ChunkBlockMap<T>
{
    protected final HashMap<BlockPos, T> blocks = new HashMap<BlockPos, T>();
    
    @Nullable
    protected List<Pair<BlockPos, T>> sortedList;
    
    public final AxisAlignedBB chunkAABB;
//    public final int chunkX;
//    public final int chunkZ;
    
    private static final int CHUNK_START_MASK = ~0xF;
    
    
    public ChunkBlockMap(BlockPos pos)
    {
//        this.chunkX = pos.getX() >> 4;
//        this.chunkZ = pos.getZ() >> 4;
        this.chunkAABB = new AxisAlignedBB(
                pos.getX() & CHUNK_START_MASK,
                0,
                pos.getZ() & CHUNK_START_MASK,
                pos.getX() | 0xF,
                255,
                pos.getZ() | 0xF);
    }
    /**
     * ASSUMES POSITION IS IN THIS CHUNK!
     */
    public T get(BlockPos pos)
    {
        return this.get(pos);
    }
  
    /**
     * ASSUMES POSITION IS IN THIS CHUNK!
     */
    public boolean containsValueAt(BlockPos pos)
    {
        return this.blocks.containsKey(pos);
    }
    
    /**
     * ASSUMES POSITION IS IN THIS CHUNK!
     * Returns previous value.
     */
    @Nullable
    public T put(BlockPos pos, T value)
    {
        this.sortedList = null;
        return this.blocks.put(pos, value);
    }
    
   
    /**
     * ASSUMES POSITION IS IN THIS CHUNK!
     * Returns previous value.
     */
    @Nullable
    public T remove(BlockPos pos)
    {
        this.sortedList = null;
        return this.blocks.remove(pos);
    }
  
    public int size()
    {
        return this.blocks.size();
    }
    
    public boolean isEmpty()
    {
        return this.blocks.isEmpty();
    }
    
    public void clear()
    {
        this.blocks.clear();
    }
    
    /**
     * Sorted from bottom to top.
     */
    @SuppressWarnings("null")
    public List<Pair<BlockPos, T>> asSortedList()
    {
        if(this.sortedList == null)
        {
            if(this.blocks.isEmpty())
            {
                this.sortedList = ImmutableList.of();
            }
            else
            {
                this.sortedList = this.blocks.entrySet().stream()
                        .map(new Function<HashMap.Entry<BlockPos, T>, Pair<BlockPos, T>>(){

                            @Override
                            public Pair<BlockPos, T> apply(@Nullable Entry<BlockPos, T> t)
                            {
                                return Pair.of(t.getKey(), t.getValue());
                            }})
                        .sorted(new Comparator<Pair<BlockPos, T>>() {

                            @Override
                            public int compare(@Nullable Pair<BlockPos, T> o1, @Nullable Pair<BlockPos, T> o2)
                            {
                                return Integer.compare(o1.getLeft().getY(), o2.getLeft().getY());
                            }})
                        .collect(ImmutableList.toImmutableList());
            }
        }
        return this.sortedList;
    }
}
