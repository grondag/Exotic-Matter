package grondag.exotic_matter.model.painting;

import javax.annotation.Nullable;

import grondag.exotic_matter.model.state.ISuperModelState;
import grondag.exotic_matter.model.texture.ITexturePalette;
import grondag.exotic_matter.model.texture.TextureScale;

public class QuadPainterFactory
{
    public static @Nullable QuadPainter getPainterForSurface(ISuperModelState modelState, Surface surface, PaintLayer paintLayer)
    {
        if(surface.isLayerDisabled(paintLayer)) return null;
        
        ITexturePalette texture = modelState.getTexture(paintLayer);
        
        switch(surface.topology)
        {
            
        case TILED:
            switch(texture.textureLayout())
            {
            case SIMPLE:
            case BIGTEX_ANIMATED:
            case SPLIT_X_8:
                return new SurfaceQuadPainterTiled(modelState, surface, paintLayer);
                
            case BORDER_13:
                return null;
                
            case MASONRY_5:
                return null;
                
            default:
                return null;
            }
            
        case CUBIC:
            switch(texture.textureLayout())
            {
            case SIMPLE:
            case BIGTEX_ANIMATED:
            case SPLIT_X_8:
                return(texture.textureScale() == TextureScale.SINGLE)
                        ? new CubicQuadPainterTiles(modelState, surface, paintLayer)
                        : new CubicQuadPainterBigTex(modelState, surface, paintLayer);
                
            case BORDER_13:
                 return surface.allowBorders
                         ? new CubicQuadPainterBorders(modelState, surface, paintLayer)
                         : null;
                
            case MASONRY_5:
                return surface.allowBorders
                        ? new CubicQuadPainterMasonry(modelState, surface, paintLayer)
                        : null;
            
            case QUADRANT_CONNECTED:
                return surface.allowBorders
                        ? new CubicQuadPainterQuadrants(modelState, surface, paintLayer)
                        : null;
                
            default:
                return null;
            }

        default:
            return null;
        }
    }
}
