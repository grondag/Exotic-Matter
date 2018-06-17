package grondag.exotic_matter.model.painting;

import grondag.exotic_matter.model.state.ISuperModelState;
import grondag.exotic_matter.terrain.TerrainState;
import grondag.exotic_matter.world.BlockCorner;
import grondag.exotic_matter.world.CornerJoinFaceState;
import grondag.exotic_matter.world.FarCorner;
import grondag.exotic_matter.world.HorizontalCorner;
import grondag.exotic_matter.world.HorizontalFace;
import grondag.exotic_matter.world.ICornerJoinTestProvider;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;

public class CubicQuadPainterLava extends CubicQuadPainterQuadrants
{

    public CubicQuadPainterLava(ISuperModelState modelState, Surface surface, PaintLayer paintLayer)
    {
        super(modelState, surface, paintLayer);
    }

    @Override
    protected CornerJoinFaceState faceState(EnumFacing face)
    {
        // lava rendering only needed/supported on top face
        if(face != EnumFacing.UP) return CornerJoinFaceState.NO_FACE;
        
        final TerrainState flowState = modelState.getTerrainState();
        
        return CornerJoinFaceState.find(EnumFacing.UP, new ICornerJoinTestProvider()
        {
            @Override
            public boolean result(EnumFacing face)
            {
                if(face.getAxis() == Axis.Y) return false;

                final HorizontalFace hFace = HorizontalFace.find(face);
                return flowState.neighborHotness(hFace) != 0
                        || flowState.getSideHeight(hFace) == TerrainState.NO_BLOCK;
            }
        
            @Override
            public boolean result(BlockCorner corner)
            {
                final HorizontalCorner hCorner = corner.horizonal;
                if(hCorner == null) return false;
                
                return flowState.neighborHotness(hCorner) != 0
                        || flowState.getCornerHeight(hCorner) == TerrainState.NO_BLOCK;
            }
        
            @Override
            public boolean result(FarCorner corner)
            {
                return false;
            }
        });
    }

    
}
