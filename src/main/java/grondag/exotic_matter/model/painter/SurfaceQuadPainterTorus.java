package grondag.exotic_matter.model.painter;

import grondag.exotic_matter.model.ISuperModelState;
import grondag.exotic_matter.model.PaintLayer;
import grondag.exotic_matter.render.IFancyMutablePolygon;
import grondag.exotic_matter.render.Surface;

public class SurfaceQuadPainterTorus extends SurfaceQuadPainter
{
    
    public SurfaceQuadPainterTorus(ISuperModelState modelState, Surface surface, PaintLayer paintLayer)
    {
        super(modelState, surface, paintLayer);
    }

    @Override
    public IFancyMutablePolygon paintQuad(IFancyMutablePolygon quad)
    {
        assert !quad.isLockUV() : "Toroidal surface quad painter received quad with lockUV semantics.  Not expected";
        
        return null;
    }
}
