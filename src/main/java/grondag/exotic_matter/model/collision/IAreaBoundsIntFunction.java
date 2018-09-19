package grondag.exotic_matter.model.collision;

@FunctionalInterface
interface IAreaBoundsIntFunction
{
    /**
     * Max values are inclusive.
     */
    int apply(int xMin, int yMin, int xMax, int yMax);
}