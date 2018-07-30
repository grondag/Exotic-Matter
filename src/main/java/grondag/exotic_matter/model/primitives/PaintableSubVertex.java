package grondag.exotic_matter.model.primitives;

import grondag.exotic_matter.varia.ColorHelper;

public abstract class PaintableSubVertex<T extends Vertex> implements IPaintableVertex 
{
    protected final float u;
    protected final float v;
    protected final int color;
    protected final short glow;
    
    protected T parent;
    
    @SuppressWarnings("null")
    protected PaintableSubVertex(float u, float v, int color, int glow)
    {
        this.u = u;
        this.v = v;
        this.color = color;
        this.glow = (short) glow;
    }
    
    protected abstract PaintableSubVertex<T> factory(float u, float v, int color, int glow);
    
    protected PaintableSubVertex<T> copy()
    {
        return factory(this.u, this.v, this.color, this.glow);
    }
    
    protected void setParent(T parent)
    {
        this.parent = parent;
    }
    
    protected PaintableSubVertex<T> interpolate(PaintableSubVertex<T> to, float toWeight)
    {
        final int newGlow = (int) (this.glow + (to.glow - this.glow) * toWeight);
        final int newColor = ColorHelper.interpolate(this.color, to.color, toWeight);
        final float newU = this.u + (to.u - this.u) * toWeight;
        final float newV = this.v + (to.v - this.v) * toWeight;
        
        return factory(newU, newV, newColor, newGlow);
    }
    
    @Override
    public IPaintableVertex forTextureLayer(int layer)
    {
        return parent.forTextureLayer(layer);
    }
    
    @Override
    public short glow()
    {
        return this.glow;
    }
    @Override
    public int color()
    {
        return this.color;
    }
    @Override
    public float u()
    {
        return this.u;
    }
    @Override
    public float v()
    {
        return this.v;
    }
   
    @Override
    public float x()
    {
        return parent.x();
    }
    
    @Override
    public float y()
    {
        return parent.y();
    }

    @Override
    public float z()
    {
        return parent.z();
    }
    
    @Override
    public IPaintableVertex interpolate(IPaintableVertex nextVertex, float dist)
    {
        return parent.interpolate(nextVertex, dist);
    }
}
