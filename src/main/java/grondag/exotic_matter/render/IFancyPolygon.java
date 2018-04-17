package grondag.exotic_matter.render;

public interface IFancyPolygon extends IPolygon
{
    @Override
    IFancyVertex getVertex(int index);

}
