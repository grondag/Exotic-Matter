package grondag.exotic_matter.model.painting;

import grondag.exotic_matter.ExoticMatter;
import grondag.exotic_matter.model.primitives.better.IMutablePolygon;
import grondag.exotic_matter.model.state.ISuperModelState;
import grondag.exotic_matter.varia.ColorHelper;

public class VertexProcessorDefault extends VertexProcessor
{
    public final static VertexProcessor INSTANCE = new VertexProcessorDefault();
    
    static
    {
        VertexProcessors.register(VertexProcessorDefault.INSTANCE);
    }
    
    VertexProcessorDefault()
    {
        super("default", 0);
    }
    
    @Override
    public final void process(IMutablePolygon poly, ISuperModelState modelState, PaintLayer paintLayer)
    {
        int color = modelState.getColorARGB(paintLayer);
        int layerIndex = paintLayer.textureLayerIndex;
        
        //TODO: remove?  Was causing problems when acuity is enabled because renderpass will be solid
//        if(modelState.getRenderPass(paintLayer) != BlockRenderLayer.TRANSLUCENT)
//            color =  0xFF000000 | color;
        
        // If surface is a lamp gradient then glow bits are used 
        // to blend the lamp color/brighness with the nominal color/brightness.
        // This does not apply with the lamp paint layer itself (makes no sense).
        // (Generally gradient surfaces should not be painted by lamp color)
        if(paintLayer != PaintLayer.LAMP && poly.getSurface().isLampGradient)
        {
            int lampColor = modelState.getColorARGB(PaintLayer.LAMP);
            int lampBrightness = modelState.isEmissive(PaintLayer.LAMP) ? 255 : 0;
            
            // keep target surface alpha
            int alpha = color & 0xFF000000;
            
            for(int i = 0; i < poly.vertexCount(); i++)
            {
                final float w = poly.getVertexGlow(layerIndex, i) / 255f;
                int b = Math.round(lampBrightness * w);
                int c = ColorHelper.interpolate(color, lampColor, w)  & 0xFFFFFF;
                poly.setVertexColorGlow(layerIndex, i, c | alpha, b);
            }
        }
        else
        {
            // normal shaded surface - tint existing colors, usually WHITE to start with
            for(int i = 0; i < poly.vertexCount(); i++)
            {
                // if acuity is enabled, will use emissive flag directly
                final int brightness = ExoticMatter.proxy.isAcuityEnabled() ? 0 : modelState.isEmissive(paintLayer) ? 255 : 0;
                
                final int c = ColorHelper.multiplyColor(color, poly.getVertexColor(layerIndex, i));
                poly.setVertexColorGlow(layerIndex, i, c, brightness);
            }
        }
    }

}
