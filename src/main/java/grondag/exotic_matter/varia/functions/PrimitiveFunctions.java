package grondag.exotic_matter.varia.functions;

public abstract class PrimitiveFunctions
{
    @FunctionalInterface
    public interface IntToIntFunction
    {
        int apply(int i);
    }
}
