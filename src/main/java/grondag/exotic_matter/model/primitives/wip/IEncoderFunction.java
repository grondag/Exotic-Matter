package grondag.exotic_matter.model.primitives.wip;

import grondag.exotic_matter.model.primitives.polygon.IPolygon;

@FunctionalInterface
public interface IEncoderFunction
{
    void encode(IIntStream stream, int myBaseAddress, IPolygon source);
}
