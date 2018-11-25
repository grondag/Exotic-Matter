package grondag.exotic_matter.model.primitives.better;

import javax.annotation.Nullable;

import grondag.exotic_matter.model.primitives.vertex.Vec3f;

public abstract class AbstractPackedVertex<T extends AbstractPackedVertex<T>> extends AbstractVertex<T>
{
    protected Vec3f pos;
    @Nullable protected Vec3f normal;
    
    @Override
    public final Vec3f pos()
    {
        return pos;
    }

    @Override
    public final Vec3f normal()
    {
        return normal;
    }

    @SuppressWarnings("null")
    @Override
    public final IMutableVertex flip()
    {
        if(this.normal != null)
            this.normal = Vec3f.create(-normal.x(), -normal.y(), -normal.z());
        return this;
    }

    @Override
    public final void setNormal(@Nullable Vec3f normal)
    {
        this.normal = normal;
    }

    @Override
    public final void setNormal(float x, float y, float z)
    {
        this.normal = Vec3f.create(x, y, z);
    }

    @Override
    public final void setPos(Vec3f pos)
    {
        this.pos = pos;
    }

    @Override
    public final void setPos(float x, float y, float z)
    {
        this.pos = Vec3f.create(x, y, z);
    }

    @SuppressWarnings("null")
    @Override
    public final float normalX()
    {
        return normal == null ? 0 : normal.x();
    }

    @SuppressWarnings("null")
    @Override
    public final float normalY()
    {
        return normal == null ? 0 : normal.y();
    }

    @SuppressWarnings("null")
    @Override
    public final float normalZ()
    {
        return normal == null ? 0 : normal.z();
    }

    @Override
    public final boolean hasNormal()
    {
        return normal != null;
    }

    @Override
    public final float x()
    {
        return pos.x();
    }

    @Override
    public final float y()
    {
        return pos.y();
    }

    @Override
    public final float z()
    {
        return pos.z();
    }
    
    /**
     * Not supported on packed vertices - they have a fixed number of layers.
     */
    @Override
    public final void setLayerCount(int layerCount)
    {
        throw new UnsupportedOperationException();
    }
}
