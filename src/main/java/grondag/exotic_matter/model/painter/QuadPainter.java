package grondag.exotic_matter.model.painter;

import java.util.List;

import grondag.exotic_matter.model.ColorMap;
import grondag.exotic_matter.model.ColorMap.EnumColorMap;
import grondag.exotic_matter.model.ISuperModelState;
import grondag.exotic_matter.model.ITexturePalette;
import grondag.exotic_matter.model.PaintLayer;
import grondag.exotic_matter.model.TexturePaletteRegistry;
import grondag.exotic_matter.render.IMutablePolygon;
import grondag.exotic_matter.render.IPolygon;
import grondag.exotic_matter.render.Poly;
import grondag.exotic_matter.render.QuadHelper;
import grondag.exotic_matter.render.RenderPass;
import grondag.exotic_matter.render.Surface;
import grondag.exotic_matter.render.Vertex;
import grondag.exotic_matter.varia.Color;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.client.model.pipeline.LightUtil;

public abstract class QuadPainter
{
    /** color map for this surface */
    protected final ColorMap myColorMap;
    protected final RenderPass renderPass;
    
    /** 
     * Color map for lamp surface - used to render lamp gradients
     * Only populated for BOTTOM/CUT surfaces
     */
    protected final ColorMap lampColorMap;

    /**
    * Render layer for lamp surface - used to render lamp gradients
    * Only populated for BOTTOM/CUT surfaces
    */
    protected final RenderPass lampRenderPass;
    
    /**
     * True if paint layer is supposed to be rendered at full brightness.
     */
    protected final boolean isFullBrightnessIntended;
    
    protected final ITexturePalette texture;
    public final Surface surface;
    public final PaintLayer paintLayer;
    /** Do bitwise OR with color value to get correct alpha for rendering */
    protected final int translucencyArgb;
    
    /**
     * Provided quad is already a clone, and should be
     * modified directly and returned.
     * Return null to exclude quad from output.
     * RenderLayer, lighting mode and color will already be set.
     * @return 
     */
    protected abstract IMutablePolygon paintQuad(IMutablePolygon quad);
    
    public QuadPainter(ISuperModelState modelState, Surface surface, PaintLayer paintLayer)
    {
        this.surface = surface;
        this.paintLayer = paintLayer;
        this.myColorMap = modelState.getColorMap(paintLayer);
        
        this.renderPass = modelState.getRenderPass(paintLayer);
        this.isFullBrightnessIntended = modelState.isFullBrightness(paintLayer);

        if(paintLayer == PaintLayer.BASE || paintLayer == PaintLayer.CUT)
        {
            this.lampColorMap = modelState.getColorMap(PaintLayer.LAMP);
            this.lampRenderPass = modelState.getRenderPass(PaintLayer.LAMP);
        }
        else
        {
            this.lampColorMap = null;
            this.lampRenderPass = null;
        }
        
        ITexturePalette tex = modelState.getTexture(paintLayer);
        this.texture = tex == TexturePaletteRegistry.NONE ? modelState.getTexture(PaintLayer.BASE) : tex;
        this.translucencyArgb = modelState.isTranslucent(paintLayer) ? modelState.getTranslucency().alphaARGB : 0xFF000000;
    }
    
    /** for null painter only */
    private QuadPainter()
    {
        this.myColorMap = null;
        this.lampColorMap = null;
        this.lampRenderPass = null;
        this.renderPass = null;
        this.isFullBrightnessIntended = false;
        this.surface = null;
        this.paintLayer = null;
        this.texture = null;
        this.translucencyArgb = 0;
    }

    /**
     * If isItem = true will bump out quads from block center to provide
     * better depth rendering of layers in tiem rendering.
     */
    public void addPaintedQuadToList(IPolygon inputQuad, List<IPolygon> outputList, boolean isItem)
    {
        if(inputQuad.getSurfaceInstance().surface() != this.surface) return;
        
        switch(this.paintLayer)
        {
        case BASE:
            if(inputQuad.getSurfaceInstance().disableBase) return;
            break;
            
        case MIDDLE:
            if(inputQuad.getSurfaceInstance().disableMiddle) return;
            break;
            
        case OUTER:
            if(inputQuad.getSurfaceInstance().disableOuter) return;
            break;
            
        case CUT:
        case LAMP:
        default:
            break;
        
        }
    
        IMutablePolygon result = Poly.mutableCopyOf(inputQuad);
        result.setRenderPass(this.renderPass);
        result.setFullBrightness(this.isFullBrightnessIntended);

        recolorQuad(result);
     
        // TODO: Vary color slightly with species, as user-selected option
        
        result = this.paintQuad(result);
        
        if(result != null) 
        {
            if(result.isLockUV())
            {
                // if lockUV is on, derive UV coords by projection
                // of vertex coordinates on the plane of the quad's face
                result.assignLockedUVCoordinates();;
            }
   
            if(isItem)
            {
                switch(this.paintLayer)
                {
                case MIDDLE:
                    result.scaleFromBlockCenter(1.01f);
                    break;
                    
                case OUTER:
                    result.scaleFromBlockCenter(1.02f);
                    break;
                    
                default:
                    break;
                }
            }
            outputList.add(result);
        }
    }
    
    
    private void recolorQuad(IMutablePolygon result)
    {
        int color = this.myColorMap.getColor(this.isFullBrightnessIntended ? EnumColorMap.LAMP : EnumColorMap.BASE);
        
        if(this.renderPass.blockRenderLayer == BlockRenderLayer.TRANSLUCENT)
        {
            color = this.translucencyArgb | (color & 0x00FFFFFF);
        }
        
        if(this.isFullBrightnessIntended)
        {
            // If the surface has a lamp gradient or is otherwise pre-shaded 
            // we don't want to see a gradient when rendering at full brightness
            // so make all vertices white before we recolor.
            result.replaceColor(color);
        }
        
        else if(result.getSurfaceInstance().isLampGradient && this.lampColorMap != null)
        {
            // if surface has a lamp gradient and rendered with shading, need
            // to replace the colors to form the gradient.
            int shadedColor = QuadHelper.shadeColor(color, (LightUtil.diffuseLight(result.getNormalFace()) + 2) / 3, false);
            int lampColor = this.lampColorMap.getColor(EnumColorMap.LAMP);
            for(int i = 0; i < result.vertexCount(); i++)
            {
                Vertex v = result.getVertex(i);
                if(v != null)
                {
                    int vColor = v.color == Color.WHITE ? lampColor : shadedColor;
                    result.setVertexColor(i, vColor);
                }
            }
            
            // if needed, change render pass of gradient surface to flat so that it doesn't get darkened by AO
            if(!this.lampRenderPass.isShaded && this.renderPass.isShaded)
            {
                result.setRenderPass(this.renderPass.flipShading());
            }
        }
        else
        {
            // normal shaded surface - tint existing colors, usually WHITE to start with
            result.multiplyColor(color);
        }
    }

    public static QuadPainter makeNullQuadPainter(ISuperModelState modelState, Surface surface, PaintLayer paintLayer)
    {
        return NullQuadPainter.INSTANCE;
    }
    
    public static class NullQuadPainter extends QuadPainter
    {

        private static final NullQuadPainter INSTANCE = new NullQuadPainter();
        
        private NullQuadPainter()
        {
            super();
        };
        
        @Override
        public void addPaintedQuadToList(IPolygon inputQuad, List<IPolygon> outputList, boolean isItem)
        {
            // NOOP
        }


        @Override
        protected IMutablePolygon paintQuad(IMutablePolygon quad)
        {
            return null;
        }
        
    }
}
