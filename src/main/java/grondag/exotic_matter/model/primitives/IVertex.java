package grondag.exotic_matter.model.primitives;

import javax.annotation.Nullable;

public interface IVertex
{
    public short glow();

    public int color();

    public float u();

    public float v();
    
    public Vec3f pos();
    
    public default float x()
    {
        return pos().x();
    }

    public default float y()
    {
        return pos().y();
    }
    
    public default float z()
    {
        return pos().z();
    }

    public @Nullable Vec3f normal();
}
