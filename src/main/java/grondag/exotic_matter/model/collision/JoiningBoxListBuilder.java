package grondag.exotic_matter.model.collision;

import static grondag.exotic_matter.model.collision.CollisionBoxEncoder.X_AXIS;
import static grondag.exotic_matter.model.collision.CollisionBoxEncoder.Y_AXIS;
import static grondag.exotic_matter.model.collision.CollisionBoxEncoder.Z_AXIS;
import static grondag.exotic_matter.model.collision.CollisionBoxEncoder.faceKey;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

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
        CollisionBoxEncoder.forEachFaceKey(boxKey, k -> faceToBoxMap.remove(k));
        boxSet.rem(boxKey);
    }
    
    private void addBox(int boxKey)
    {
        boxSet.add(boxKey);
        CollisionBoxEncoder.forEachFaceKey(boxKey, k -> faceToBoxMap.put(k, boxKey));
    }
    
    //TODO: remove
//    private class FaceAccumulator implements IntConsumer
//    {
//        int bestKey = NOT_FOUND;
//        int bestVolume = NOT_FOUND;
//        
//        void prepare()
//        {
//            bestKey = NOT_FOUND;
//            bestVolume = NOT_FOUND;
//        }
//
//        @Override
//        public void accept(int k)
//        {
//            int testKey = faceToBoxMap.get(k);
//            if(testKey != NOT_FOUND )
//            {
//                int v = CollisionBoxEncoder.boxVolume(testKey);
//                if(v > bestVolume)
//                {
//                    bestKey = testKey;
//                    bestVolume = v;
//                }
//            }            
//        }
//    }
//    
//    private final FaceAccumulator faceAccumulator = new FaceAccumulator();
    
    @Override
    public void add(final int boxKey)
    {
        CollisionBoxEncoder.forBounds(boxKey, (minX, minY, minZ, maxX, maxY, maxZ) ->
        {
            if(minY > 0 && tryCombine(boxKey, faceKey(Y_AXIS, maxY, minX, minZ, maxX, maxZ)))
                return 0;
            
            if(minY < 8 && tryCombine(boxKey, faceKey(Y_AXIS, minY, minX, minZ, maxX, maxZ)))
                return 0;
            
            if(minX > 0 && tryCombine(boxKey, faceKey(X_AXIS, maxX, minY, minZ, maxY, maxZ)))
                return 0;
            
            if(minX < 8 && tryCombine(boxKey, faceKey(X_AXIS, minX, minY, minZ, maxY, maxZ)))
                return 0;
            
            if(minZ > 0 && tryCombine(boxKey, faceKey(Z_AXIS, maxZ, minX, minY, maxX, maxY)))
                return 0;
            
            if(minZ < 8 && tryCombine(boxKey, faceKey(Z_AXIS, minZ, minX, minY, maxX, maxY)))
                return 0;
            
            addBox(boxKey);
            return 0;
        });
    }

    boolean tryCombine(int boxKey, int faceKey)
    {
        int k = faceToBoxMap.get(faceKey);
        if(k == NOT_FOUND )
            return false;
        
        removeBox(k);
        add(CollisionBoxEncoder.combineBoxes(boxKey, k));
        return true;
    }
    
    @Override
    public IntCollection boxes()
    {
        return this.boxSet;
    }
}