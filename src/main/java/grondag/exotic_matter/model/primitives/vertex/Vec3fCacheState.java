package grondag.exotic_matter.model.primitives.vertex;

import java.util.concurrent.atomic.AtomicInteger;


class Vec3fCacheState
{
    protected AtomicInteger size = new AtomicInteger(0);
    protected final Vec3f[] values;
    
    public Vec3fCacheState(int capacityIn)
    {
        this.values = new Vec3f[capacityIn];
    }
}