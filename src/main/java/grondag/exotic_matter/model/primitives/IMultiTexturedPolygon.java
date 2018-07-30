package grondag.exotic_matter.model.primitives;

public interface IMultiTexturedPolygon extends IPaintableQuad
{
    public int layerCount();
    
    public IPaintableQuad getSubQuad(int layerIndex);
}
