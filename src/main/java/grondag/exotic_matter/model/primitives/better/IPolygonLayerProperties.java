package grondag.exotic_matter.model.primitives.better;

public interface IPolygonLayerProperties
{
    public String getTextureName();
    public void setTextureName(String textureName);

    public float getMaxU();
    public void setMaxU(float maxU);
    
    public float getMinU();
    public void setMinU(float minU);

    public float getMaxV();
    public void setMaxV(float maxV);

    public float getMinV();
    public void setMinV(float minV);
}
