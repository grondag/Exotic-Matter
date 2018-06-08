package grondag.exotic_matter.model.painting;

import grondag.exotic_matter.model.color.ColorMap.EnumColorMap;
import grondag.exotic_matter.model.primitives.IMutablePolygon;
import grondag.exotic_matter.model.primitives.Vertex;
import grondag.exotic_matter.model.state.ISuperModelState;
import grondag.exotic_matter.varia.Color;
import net.minecraft.util.BlockRenderLayer;

public interface IQuadColorizer
{
    public final static IQuadColorizer DEFAULT = new IQuadColorizer() {};
    
    public default void recolorQuad(IMutablePolygon result, ISuperModelState modelState, PaintLayer paintLayer)
    {
        final boolean fullBright = modelState.isFullBrightness(paintLayer);
        int color = modelState.getColorMap(paintLayer).getColor(fullBright ? EnumColorMap.LAMP : EnumColorMap.BASE);
        
        if(modelState.getRenderPass(paintLayer) == BlockRenderLayer.TRANSLUCENT)
        {
            color = (modelState.isTranslucent(paintLayer) ? modelState.getTranslucency().alphaARGB : 0xFF000000) | (color & 0x00FFFFFF);
        }
        
        if(fullBright)
        {
            // If the surface has a lamp gradient or is otherwise pre-shaded 
            // we don't want to see a gradient when rendering at full brightness
            // so make all vertices same color.
            result.replaceColor(color);
        }
        else if(result.getSurfaceInstance().isLampGradient)
        {
            // If surface has a lamp gradient and rendered with shading, 
            // replace white with the lamp color and black with the normal color.
            // Shading will be handled at bake via per-vertex glow.
            int lampColor = modelState.getColorMap(PaintLayer.LAMP).getColor(EnumColorMap.LAMP);
            for(int i = 0; i < result.vertexCount(); i++)
            {
                Vertex v = result.getVertex(i);
                if(v != null)
                {
                    int vColor = v.color == Color.WHITE ? lampColor : color;
                    result.setVertexColor(i, vColor);
                }
            }
        }
        else
        {
            // normal shaded surface - tint existing colors, usually WHITE to start with
            result.multiplyColor(color);
        }
    }
}
