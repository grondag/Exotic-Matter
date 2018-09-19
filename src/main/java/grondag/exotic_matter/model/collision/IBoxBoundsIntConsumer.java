package grondag.exotic_matter.model.collision;

@FunctionalInterface
public interface IBoxBoundsIntConsumer
{
    void accept(int minX, int minY, int minZ, int maxX, int maxY, int maxZ);
}