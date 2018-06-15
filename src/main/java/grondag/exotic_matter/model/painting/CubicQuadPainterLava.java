package grondag.exotic_matter.model.painting;

import grondag.exotic_matter.model.state.ISuperModelState;
import grondag.exotic_matter.world.CornerJoinFaceState;
import net.minecraft.util.EnumFacing;

public class CubicQuadPainterLava extends CubicQuadPainterQuadrants
{

    public CubicQuadPainterLava(ISuperModelState modelState, Surface surface, PaintLayer paintLayer)
    {
        super(modelState, surface, paintLayer);
    }

    @Override
    protected CornerJoinFaceState faceState(EnumFacing face)
    {
        //TODO
        return this.modelState.getCornerJoin().getFaceJoinState(face);
    }
}
