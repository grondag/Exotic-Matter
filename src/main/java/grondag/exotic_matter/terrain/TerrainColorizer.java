package grondag.exotic_matter.terrain;

import grondag.exotic_matter.model.painting.IQuadColorizer;
import grondag.exotic_matter.model.painting.PaintLayer;
import grondag.exotic_matter.model.primitives.IMutablePolygon;
import grondag.exotic_matter.model.primitives.Vertex;
import grondag.exotic_matter.model.state.ISuperModelState;
import grondag.exotic_matter.varia.ColorHelper;

public class TerrainColorizer implements IQuadColorizer
{
    private static final int[] GRADIENT = { 0xffc00000, 0xfff30000, 0xfffa3754, 0xfffb9b39, 0xfffdda0f, 0xfffffba3};
    

    public final static TerrainColorizer INSTANCE = new TerrainColorizer() {};
    
    private TerrainColorizer() {};
    
    private static int glowColor(int glow)
    {
        int lowIndex = glow / 51;
        int highIndex = (glow + 50) / 51;
        if(lowIndex == highIndex) return GRADIENT[lowIndex];
        return ColorHelper.interpolate(GRADIENT[lowIndex], GRADIENT[highIndex], glow - lowIndex * 51);  
    }
    
    @Override
    public void recolorQuad(IMutablePolygon result, ISuperModelState modelState, PaintLayer paintLayer)
    {
        final int cold = modelState.getColorARGB(paintLayer);
        
        if(modelState.hasBrightness(paintLayer))
        {
            for(int i = 0; i < result.vertexCount(); i++)
            {
                Vertex v = result.getVertex(i);
                if(v != null)
                {
                    if(v.glow == 0)
                        result.setVertex(i, v.withColorGlow(cold, 0));
                    else
                        result.setVertex(i, v.withColorGlow(glowColor(v.glow), 128 + v.glow / 2 ));
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
