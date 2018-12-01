package grondag.exotic_matter.model.primitives.stream;

import grondag.exotic_matter.model.primitives.polygon.IPolygonExt.ICSGMutablePolygon;
import grondag.exotic_matter.model.primitives.polygon.IPolygonExt.ICSGPolygon;

public interface ICSGPolyStream extends IWritablePolyStream, ILinkedPolyStream, IMarkedPolyStream, IDeletablePolyStream, ITaggedPolyStream
{

    @Override
    ICSGPolygon reader();

    @Override
    ICSGPolygon polyA();

    @Override
    ICSGPolygon polyB();

    @Override
    ICSGMutablePolygon writer();
}
