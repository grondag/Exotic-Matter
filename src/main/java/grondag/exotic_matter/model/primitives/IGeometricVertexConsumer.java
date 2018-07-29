package grondag.exotic_matter.model.primitives;

@FunctionalInterface
public interface IGeometricVertexConsumer
{
    public void acceptVertex(float x, float y, float z);
}
