package grondag.exotic_matter.model.painting;

import grondag.exotic_matter.model.primitives.IMutablePolygon;
import grondag.exotic_matter.model.primitives.Vertex;
import grondag.exotic_matter.model.state.ISuperModelState;
import grondag.exotic_matter.varia.ColorHelper;
import net.minecraft.util.BlockRenderLayer;

public class VertexProcessorDefault extends VertexProcessor
{
    public final static VertexProcessor INSTANCE = new VertexProcessorDefault();
    
    VertexProcessorDefault()
    {
        super("default");
    }
    
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

}
