package grondag.exotic_matter.model.painting;

import java.util.function.Consumer;

import grondag.exotic_matter.model.primitives.IMutablePolygon;
import grondag.exotic_matter.model.primitives.IPolygon;
import grondag.exotic_matter.model.state.ISuperModelState;

public class CubicQuadPainterQuadrants extends CubicQuadPainter
{

    public CubicQuadPainterQuadrants(ISuperModelState modelState, Surface surface, PaintLayer paintLayer)
    {
        super(modelState, surface, paintLayer);
    }

    @Override
    protected void textureQuad(IMutablePolygon inputQuad, Consumer<IPolygon> target, boolean isItem)
    {
        // TODO Auto-generated method stub

    }

}
