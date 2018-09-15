package grondag.exotic_matter.model.collision;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.util.EnumFacing;

/**
 * Accumulates immutable, low-garbage (via cache) lists of collision boxes 
 * and automatically combines boxes that share a surface. <p>
 */
public class JoiningBoxListBuilder implements ICollisionBoxListBuilder
{
    private static final int NOT_FOUND = -1;
    
    private final Int2IntOpenHashMap faceToBoxMap = new Int2IntOpenHashMap();
    private final IntOpenHashSet boxSet = new IntOpenHashSet();
    
    public JoiningBoxListBuilder()
    {
        faceToBoxMap.defaultReturnValue(NOT_FOUND);
    }
    
    @Override
    public void clear()
    {
        faceToBoxMap.clear();
        boxSet.clear();
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
    
    @Override
    public void add(int boxKey)
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

    @Override
    public IntCollection boxes()
    {
        return this.boxSet;
    }
}