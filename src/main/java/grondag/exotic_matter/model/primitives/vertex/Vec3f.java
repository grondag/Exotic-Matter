package grondag.exotic_matter.model.primitives.vertex;

import net.minecraft.util.math.MathHelper;

public class Vec3f
{
//    private static AtomicInteger createCount = new AtomicInteger();
//    private static AtomicInteger initCount = new AtomicInteger();
    
    public static final Vec3f ZERO = new Vec3f(0, 0, 0);
    
    public static Vec3f create(float x, float y, float z)
    {
//        if((createCount.incrementAndGet() & 0xFFFFF) == 0xFFFFF)
//        {
//            int c = createCount.get();
//            int i = initCount.get();
//            System.out.println("Instance count = " + i);
//            System.out.println("Create count = " + c);
//            System.out.println("Miss rate = " + i * 100 / c);
//            System.out.println("");
//        }
        return Vec3fSimpleLoadingCache.INSTANCE.get(x, y, z);
    }
    
    protected float x;
    protected float y;
    protected float z;
    
    Vec3f(float x, float y, float z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
//        initCount.incrementAndGet();
    }
    
    public final float dotProduct(float xIn, float yIn, float zIn)
    {
        return Vec3Function.dotProduct(this.x, this.y, this.z, xIn, yIn, zIn);
    }
    
    public final float dotProduct(Vec3f vec)
    {
        return dotProduct(vec.x, vec.y, vec.z);
    }

    /**
     * Returns a new vector with the result of this vector x the specified vector.
     */
    public final Vec3f crossProduct(Vec3f vec)
    {
        return create(this.y * vec.z - this.z * vec.y, this.z * vec.x - this.x * vec.z, this.x * vec.y - this.y * vec.x);
    }
    
    public final float length()
    {
        return MathHelper.sqrt(lengthSquared());
    }

    public final float lengthSquared()
    {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }
    
    public boolean isMutable()
    {
        return false;
    }
    
    // PERF - avoid this
    public final float[] toArray()
    {
        float[] retVal = new float[3];
        retVal[0] = this.x();
        retVal[1] = this.y();
        retVal[2] = this.z();
        return retVal;
    }

    public final float x()
    {
        return x;
    }

    public final float y()
    {
        return y;
    }

    public final float z()
    {
        return z;
    }
    
    public Mutable mutableCopy()
    {
        return new Mutable(x, y, z);
    }
    
    public static class Mutable extends Vec3f
    {
//        private static final ArrayBlockingQueue<Mutable> POOL = new ArrayBlockingQueue<>(1024);
//        
//        public static Mutable claim()
//        {
//            Mutable result = POOL.poll();
//            
//            if(result == null)
//                result = new Mutable(0, 0, 0);
//            
//            return result;
//        }
//        
//        public static void release(Mutable vec)
//        {
//            POOL.offer(vec);
//        }
        
        public Mutable(float x, float y, float z)
        {
            super(x, y, z);
        }
        
        public Mutable load(float x, float y, float z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }
        
        public final Mutable load(Vec3f fromVec)
        {
            this.x = fromVec.x;
            this.y = fromVec.y;
            this.z = fromVec.z;
            return this;
        }
        
        @Override
        public boolean isMutable()
        {
            return true;
        }
        
        public final Vec3f toImmutable()
        {
            return create(x, y, z);
        }
        
        public final Mutable subtract(Vec3f vec)
        {
            return this.subtract(vec.x, vec.y, vec.z);
        }

        public final Mutable subtract(float x, float y, float z)
        {
            return this.addVector(-x, -y, -z);
        }

        public final Mutable add(Vec3f vec)
        {
            return this.addVector(vec.x, vec.y, vec.z);
        }
        
        public final Mutable addVector(float x, float y, float z)
        {
            this.x += x;
            this.y += y;
            this.z += z;
            return this;
        }
        
        public final Mutable scale(float factor)
        {
            this.x *= factor;
            this.y *= factor;
            this.z *= factor;
            return this;
        }
        
        public final Mutable invert()
        {
            this.x = -this.x;
            this.y = -this.y;
            this.z = -this.z;
            return this;
        }
        
        public final Mutable normalize()
        {
            final float mag = length();
            if(mag < 1.0E-4F)
            {
                x = 0;
                y = 0;
                z = 0;
            }
            else
            {
                this.x /= mag;
                this.y /= mag;
                this.z /= mag;
            }
            return this;
        }
    }
}
