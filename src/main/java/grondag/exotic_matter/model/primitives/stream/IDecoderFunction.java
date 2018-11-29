package grondag.exotic_matter.model.primitives.stream;

import grondag.exotic_matter.model.primitives.polygon.IMutablePolygon;

@FunctionalInterface
public interface IDecoderFunction
{
    void decode(IIntStream stream, int myBaseAddress, IMutablePolygon target);
}
