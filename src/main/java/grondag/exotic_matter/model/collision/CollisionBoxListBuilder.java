package grondag.exotic_matter.model.collision;

import java.util.List;

import com.google.common.collect.ImmutableList;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Accumulates immutable, low-garbage (via cache) lists of collision boxes 
 * and automatically combines boxes that share a surface. <p>
 */
public class CollisionBoxListBuilder
{
    private static final int NOT_FOUND = -1;
    
    private final Int2IntOpenHashMap faceToBoxMap = new Int2IntOpenHashMap();
    private final IntOpenHashSet boxSet = new IntOpenHashSet();
    
    public CollisionBoxListBuilder()
    {
        faceToBoxMap.defaultReturnValue(NOT_FOUND);
    }
    
    public void clear()
    {
        faceToBoxMap.clear();
        boxSet.clear();
    }
    
    public List<AxisAlignedBB> build()
    {
        if(boxSet.isEmpty())
            return ImmutableList.of();
        else
        {
            ImmutableList.Builder<AxisAlignedBB> builder = ImmutableList.builder();
            
            IntIterator it = boxSet.iterator();
            while(it.hasNext())
            {
                builder.add(CollisionBoxStore.getBox(it.nextInt()));
            }
            
            return builder.build();
        }
    }
    
    /**
     * Adds an AABB within a unit cube sliced into eighths on each axis.
     * Values must be 0-8.  Values do not need to be sorted but cannot be equal.
     */
    public void add(int x0, int y0, int z0, int x1, int y1, int z1)
    {
        add(CollisionBoxEncoder.boxKey(x0, y0, z0, x1, y1, z1));
    }
    
    /**
     * Adds an AABB within a unit cube sliced into eighths on each axis.
     * Values must be 0-8.  This version requires that min & max be pre-sorted on each axis.
     * If you don't have pre-sorted values, use {@link #add(int, int, int, int, int, int)}.
     */
    public void addSorted(int minX, int minY, int minZ, int maxX, int maxY, int maxZ)
    {
        add(CollisionBoxEncoder.boxKeySorted(minX, minY, minZ, maxX, maxY, maxZ));
    }
    
    /**
     * Removes all faces keys in addition to box itself.
     */
    private void removeBox(int boxKey)
    {
        faceToBoxMap.remove(CollisionBoxEncoder.faceKey(EnumFacing.UP, boxKey));
        faceToBoxMap.remove(CollisionBoxEncoder.faceKey(EnumFacing.DOWN, boxKey));
        faceToBoxMap.remove(CollisionBoxEncoder.faceKey(EnumFacing.EAST, boxKey));
        faceToBoxMap.remove(CollisionBoxEncoder.faceKey(EnumFacing.WEST, boxKey));
        faceToBoxMap.remove(CollisionBoxEncoder.faceKey(EnumFacing.NORTH, boxKey));
        faceToBoxMap.remove(CollisionBoxEncoder.faceKey(EnumFacing.SOUTH, boxKey));
        boxSet.rem(boxKey);
    }
    
    private void addBox(int boxKey)
    {
        boxSet.add(boxKey);
        faceToBoxMap.put(CollisionBoxEncoder.faceKey(EnumFacing.UP, boxKey), boxKey);
        faceToBoxMap.put(CollisionBoxEncoder.faceKey(EnumFacing.DOWN, boxKey), boxKey);
        faceToBoxMap.put(CollisionBoxEncoder.faceKey(EnumFacing.EAST, boxKey), boxKey);
        faceToBoxMap.put(CollisionBoxEncoder.faceKey(EnumFacing.WEST, boxKey), boxKey);
        faceToBoxMap.put(CollisionBoxEncoder.faceKey(EnumFacing.NORTH, boxKey), boxKey);
        faceToBoxMap.put(CollisionBoxEncoder.faceKey(EnumFacing.SOUTH, boxKey), boxKey);
    }
    
    private void add(int boxKey)
    {
        int bestKey = NOT_FOUND;
        int bestVolume = NOT_FOUND;
        
        for(EnumFacing face : EnumFacing.VALUES)
        {
            int testKey = faceToBoxMap.get(CollisionBoxEncoder.faceKey(face, boxKey));
            if(testKey != NOT_FOUND )
            {
                int v = CollisionBoxEncoder.boxVolume(testKey);
                if(v > bestVolume)
                {
                    bestKey = testKey;
                    bestVolume = v;
                }
            }
        }
        
        if(bestKey == NOT_FOUND)
            addBox(boxKey);
        else
        {
            removeBox(bestKey);
            add(CollisionBoxEncoder.combineBoxes(boxKey, bestKey));
        }
    }

    public int size()
    {
        return this.boxSet.size();
    }
}