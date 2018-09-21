package grondag.exotic_matter.varia.functions;

@FunctionalInterface
public interface IAreaBoundsIntFunction
{
    /**
     * Max values are inclusive.
     */
    int apply(int xMin, int yMin, int xMax, int yMax);
}