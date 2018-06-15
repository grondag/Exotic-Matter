package grondag.exotic_matter.model.painting;

import grondag.exotic_matter.model.primitives.IMutablePolygon;
import grondag.exotic_matter.model.state.ISuperModelState;

/**
 * Logic to apply color, brightness, glow and other attributes that depend
 * on quad, surface, or model state to each vertex in the quad. 
 * Applied after UV coordinates have been assigned. <p>
 * 
 * While intended to assign color values, could also be used to transform
 * UV, normal or other vertex attributes.
 */
public abstract class VertexProcessor
{
    private static int nextOrdinal = 0;
    
    public final String registryName;
    public final int ordinal;
    
    protected VertexProcessor(String registryName)
    {
        this.ordinal = nextOrdinal++;
        this.registryName = registryName;
    }
    
    public abstract void process(IMutablePolygon result, ISuperModelState modelState, PaintLayer paintLayer);
}
