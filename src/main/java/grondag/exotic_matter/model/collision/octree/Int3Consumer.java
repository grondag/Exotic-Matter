package grondag.exotic_matter.model.collision.octree;

@FunctionalInterface
public interface Int3Consumer
{
    public void accept(int x, int y, int z);
}
