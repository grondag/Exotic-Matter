package grondag.exotic_matter.terrain;

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
        final int cold = modelState.getColorARGB(paintLayer);
        
        if(modelState.hasBrightness(paintLayer))
        {
            final int hot = 0xffff4A24;
            for(int i = 0; i < result.vertexCount(); i++)
            {
                Vertex v = result.getVertex(i);
                if(v != null)
                {
                    int vColor = ColorHelper.interpolate(cold, hot, v.glow / 255f);
                    result.setVertex(i, v.withColorGlow(vColor, v.glow));
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
                    result.setVertex(i, v.withColorGlow((cold & 0xFFFFFF) | (v.glow << 24), 0));
                }
            }
        }
    }
}
