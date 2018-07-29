package grondag.exotic_matter.model.primitives;

@FunctionalInterface
public interface INormalVertexConsumer
{
    public void acceptVertex(float x, float y, float z, float xNormal, float yNormal, float zNormal);
}
