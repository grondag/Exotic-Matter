package grondag.exotic_matter.model.painting;

import grondag.exotic_matter.model.primitives.IMutablePolygon;
import grondag.exotic_matter.model.primitives.Vertex;
import grondag.exotic_matter.model.state.ISuperModelState;
import grondag.exotic_matter.varia.ColorHelper;
import net.minecraft.util.BlockRenderLayer;

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
    
    public final static VertexProcessor DEFAULT = new VertexProcessor("default")
    {
        @Override
        public final void process(IMutablePolygon result, ISuperModelState modelState, PaintLayer paintLayer)
        {
            final int brightness = modelState.getBrightness(paintLayer) * 17; // x17 because glow is 0-255
            
            int color = modelState.getColorARGB(paintLayer);
            if(modelState.getRenderPass(paintLayer) != BlockRenderLayer.TRANSLUCENT)
                color =  0xFF000000 | color;
            
            // If surface is a lamp gradient then glow bits are used 
            // to blend the lamp color/brighness with the nominal color/brightness.
            // This does not apply with the lamp paint layer itself (makes no sense).
            // (Generally gradient surfaces should not be painted by lamp color)
            if(paintLayer != PaintLayer.LAMP && result.getSurfaceInstance().isLampGradient)
            {
                int lampColor = modelState.getColorARGB(PaintLayer.LAMP);
                int lampBrightness = modelState.getBrightness(PaintLayer.LAMP)  * 17; // x17 because glow is 0-255
                
                // keep target surface alpha
                int alpha = color & 0xFF000000;
                
                for(int i = 0; i < result.vertexCount(); i++)
                {
                    Vertex v = result.getVertex(i);
                    if(v != null)
                    {
                        final float w = v.glow / 255f;
                        int b = Math.round(lampBrightness * w + brightness * (1 - w));
                        int c = ColorHelper.interpolate(color, lampColor, w)  & 0xFFFFFF;
                        result.setVertex(i, v.withColorGlow(c | alpha, b));
                    }
                }
            }
            else
            {
                // normal shaded surface - tint existing colors, usually WHITE to start with
                for(int i = 0; i < result.vertexCount(); i++)
                {
                    Vertex v = result.getVertex(i);
                    if(v != null)
                    {
                        final int c = ColorHelper.multiplyColor(color, v.color);
                        result.setVertex(i, v.withColorGlow(c, brightness));
                    }
                }
            }
        }
    };
    
    static
    {
        VertexProcessors.register(DEFAULT);
    }
}
