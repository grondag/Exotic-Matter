package grondag.exotic_matter.model.primitives.stream;

import grondag.exotic_matter.model.primitives.polygon.IPolygon;

@FunctionalInterface
public interface IEncoderFunction
{
    void encode(IIntStream stream, int myBaseAddress, IPolygon source);
}
