package grondag.exotic_matter.model.primitives;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;

public class Vec3f
{
    public static final Vec3f ZERO = new Vec3f(0, 0, 0);

    public static Vec3f create(float x, float y, float z)
    {
        return new Vec3f(x, y, z);
    }
    
    public static Vec3f create(Vec3i vec)
    {
        return create(vec.getX(), vec.getY(), vec.getZ());
    }
    
    public final float x;
    public final float y;
    public final float z;
    
    private Vec3f(float x, float y, float z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public final Vec3f normalize()
    {
        float mag = MathHelper.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
        return mag < 1.0E-4F ? ZERO : create(this.x / mag, this.y / mag, this.z / mag);
    }

    public final float dotProduct(Vec3f vec)
    {
        return this.x * vec.x + this.y * vec.y + this.z * vec.z;
    }

    /**
     * Returns a new vector with the result of this vector x the specified vector.
     */
    public final Vec3f crossProduct(Vec3f vec)
    {
        return create(this.y * vec.z - this.z * vec.y, this.z * vec.x - this.x * vec.z, this.x * vec.y - this.y * vec.x);
    }

    public final Vec3f subtract(Vec3f vec)
    {
        return this.subtract(vec.x, vec.y, vec.z);
    }

    public final Vec3f subtract(float x, float y, float z)
    {
        return this.addVector(-x, -y, -z);
    }

    public final Vec3f add(Vec3f vec)
    {
        return this.addVector(vec.x, vec.y, vec.z);
    }
    
    public final Vec3f addVector(float x, float y, float z)
    {
        return create(this.x + x, this.y + y, this.z + z);
    }
    
    public final Vec3f scale(float factor)
    {
        return create(this.x * factor, this.y * factor, this.z * factor);
    }
    
    public final Vec3f inverse()
    {
        return create(-this.x, -this.y, -this.z);
    }
    
    public final float lengthVector()
    {
        return MathHelper.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    public final float lengthSquared()
    {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }
    
    // PERF - avoid this
    public final float[] toArray()
    {
        float[] retVal = new float[3];
        retVal[0] = this.x;
        retVal[1] = this.y;
        retVal[2] = this.z;
        return retVal;
    }
}
