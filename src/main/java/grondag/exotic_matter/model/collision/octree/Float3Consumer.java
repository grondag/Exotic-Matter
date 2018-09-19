package grondag.exotic_matter.model.collision.octree;

@FunctionalInterface
public interface Float3Consumer
{
    public void accept(float x, float y, float z);
}
