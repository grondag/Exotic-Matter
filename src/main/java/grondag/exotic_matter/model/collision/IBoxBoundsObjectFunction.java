package grondag.exotic_matter.model.collision;

@FunctionalInterface
public interface IBoxBoundsObjectFunction<V>
{
    V accept(int minX, int minY, int minZ, int maxX, int maxY, int maxZ);
}