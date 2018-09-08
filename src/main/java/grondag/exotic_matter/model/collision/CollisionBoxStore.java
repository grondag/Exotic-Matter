package grondag.exotic_matter.model.collision;

import grondag.exotic_matter.cache.IntSimpleCacheLoader;
import grondag.exotic_matter.cache.IntSimpleLoadingCache;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Caches AABB instances that share the same packed key.  Mods can 
 * use many collision boxes, so this helps reduce memory use and garbage.
 */
public class CollisionBoxStore
{
    private static final IntSimpleLoadingCache<AxisAlignedBB> boxCache = new IntSimpleLoadingCache<AxisAlignedBB>(new BoxLoader(),  0xFFF);

    public static AxisAlignedBB getBox(int boxKey)
    {
        return boxCache.get(boxKey);
    }
    
    private static class BoxLoader implements IntSimpleCacheLoader<AxisAlignedBB>
    {
        @Override
        public AxisAlignedBB load(int boxKey)
        {
            return new AxisAlignedBB(CollisionBoxEncoder.minX(boxKey) / 8f, CollisionBoxEncoder.minY(boxKey) / 8f, CollisionBoxEncoder.minZ(boxKey) / 8f, 
                    CollisionBoxEncoder.maxX(boxKey) / 8f, CollisionBoxEncoder.maxY(boxKey) / 8f, CollisionBoxEncoder.maxZ(boxKey) / 8f);
        }
    }
}
