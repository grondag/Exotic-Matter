package grondag.exotic_matter.terrain;

import grondag.exotic_matter.model.color.ColorMap.EnumColorMap;
import grondag.exotic_matter.model.painting.IQuadColorizer;
import grondag.exotic_matter.model.painting.PaintLayer;
import grondag.exotic_matter.model.primitives.IMutablePolygon;
import grondag.exotic_matter.model.primitives.Vertex;
import grondag.exotic_matter.model.state.ISuperModelState;
import grondag.exotic_matter.varia.ColorHelper;

public class TerrainColorizer implements IQuadColorizer
{
    public final static TerrainColorizer INSTANCE = new TerrainColorizer() {};
    
    private TerrainColorizer() {};
    
    @Override
    public void recolorQuad(IMutablePolygon result, ISuperModelState modelState, PaintLayer paintLayer)
    {
        final int cold = modelState.getColorMap(paintLayer).getColor(EnumColorMap.BASE);
        
        if(modelState.isFullBrightness(paintLayer))
        {
            final int hot = 0xffff2a24;
            for(int i = 0; i < result.vertexCount(); i++)
            {
                Vertex v = result.getVertex(i);
                if(v != null)
                {
                    int vColor = ColorHelper.interpolate(cold, hot, v.glow / 15f);
                    result.setVertex(i, v.withColor(vColor));
                }
            }
        }
        else 
        {
            for(int i = 0; i < result.vertexCount(); i++)
            {
                Vertex v = result.getVertex(i);
                if(v != null)
                {
                    result.setVertex(i, v.withColorGlow(cold, 0));
                }
            }
        }
    }
}
