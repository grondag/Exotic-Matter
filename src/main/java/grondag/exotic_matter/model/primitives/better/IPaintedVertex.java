package grondag.exotic_matter.model.primitives.better;

public interface IPaintedVertex extends IGeometricVertex
{
    public short glow(int layerIndex);

    public int color(int layerIndex);

    public float u(int layerIndex);

    public float v(int layerIndex);
}
