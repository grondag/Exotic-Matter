package grondag.exotic_matter.varia.functions;

@FunctionalInterface
public interface IBoxBoundsIntConsumer
{
    void accept(int minX, int minY, int minZ, int maxX, int maxY, int maxZ);
}