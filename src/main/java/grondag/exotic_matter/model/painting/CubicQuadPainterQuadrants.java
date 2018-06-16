package grondag.exotic_matter.model.painting;

import java.util.function.Consumer;

import grondag.exotic_matter.model.primitives.IMutablePolygon;
import grondag.exotic_matter.model.primitives.IPolygon;
import grondag.exotic_matter.model.state.ISuperModelState;
import grondag.exotic_matter.world.CornerJoinFaceState;
import grondag.exotic_matter.world.FaceCorner;
import net.minecraft.util.EnumFacing;

/**
 * Applies quadrant-style border textures. 
 * Quads must have a nominal face.
 * Will split quads that span quadrants.
 */
public class CubicQuadPainterQuadrants extends CubicQuadPainter
{
    private static final TextureQuadrant[][] TEXTURE_MAP = new TextureQuadrant[FaceCorner.values().length][CornerJoinFaceState.values().length];
    
    static
    {
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.ALL_BL.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.ALL_BL_BR.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.ALL_BR.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.ALL_NO_CORNERS.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.ALL_TL.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.ALL_TL_BL.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.ALL_TL_BL_BR.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.ALL_TL_BR.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.ALL_TL_TR.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.ALL_TL_TR_BL.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.ALL_TL_TR_BL_BR.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.ALL_TL_TR_BR.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.ALL_TR.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.ALL_TR_BL.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.ALL_TR_BL_BR.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.ALL_TR_BR.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.BOTTOM.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.BOTTOM_LEFT_BL.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.BOTTOM_LEFT_NO_CORNER.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.BOTTOM_LEFT_RIGHT_BL.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.BOTTOM_LEFT_RIGHT_BL_BR.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.BOTTOM_LEFT_RIGHT_BR.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.BOTTOM_LEFT_RIGHT_NO_CORNERS.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.BOTTOM_RIGHT_BR.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.BOTTOM_RIGHT_NO_CORNER.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.LEFT.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.LEFT_RIGHT.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.NONE.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.NO_FACE.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.RIGHT.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.TOP.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.TOP_BOTTOM.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.TOP_BOTTOM_LEFT_BL.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.TOP_BOTTOM_LEFT_NO_CORNERS.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.TOP_BOTTOM_LEFT_TL.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.TOP_BOTTOM_LEFT_TL_BL.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.TOP_BOTTOM_RIGHT_BR.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.TOP_BOTTOM_RIGHT_NO_CORNERS.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.TOP_BOTTOM_RIGHT_TR.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.TOP_BOTTOM_RIGHT_TR_BR.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.TOP_LEFT_NO_CORNER.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.TOP_LEFT_RIGHT_NO_CORNERS.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.TOP_LEFT_RIGHT_TL.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.TOP_LEFT_RIGHT_TL_TR.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.TOP_LEFT_RIGHT_TR.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.TOP_LEFT_TL.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.TOP_RIGHT_NO_CORNER.ordinal()] = TextureQuadrant.FULL;
        TEXTURE_MAP[FaceCorner.TOP_LEFT.ordinal()][CornerJoinFaceState.TOP_RIGHT_TR.ordinal()] = TextureQuadrant.FULL;
    }
    public CubicQuadPainterQuadrants(ISuperModelState modelState, Surface surface, PaintLayer paintLayer)
    {
        super(modelState, surface, paintLayer);
    }
    
    protected CornerJoinFaceState faceState(EnumFacing face)
    {
        return this.modelState.getCornerJoin().getFaceJoinState(face);
    }
    
    @Override
    protected final void textureQuad(IMutablePolygon quad, Consumer<IPolygon> target, boolean isItem)
    {
        assert quad.isLockUV() : "Quadrant cubic quad painter received quad without lockUV semantics.  Not expected";
    
        final FaceCorner quadrant = QuadrantSplitter.uvQuadrant(quad);
        if(quadrant == null) 
            return;
        
        @SuppressWarnings("null")
        final int textureVersion = this.texture.textureVersionMask() 
                & (this.textureHashForFace(quad.getNominalFace()) >> (quadrant.ordinal() * 4));
        
        quad.setTextureName(this.texture.getTextureName(textureVersion));
        
        CornerJoinFaceState faceState = this.faceState(quad.getNominalFace());
        
        TextureQuadrant.SIDE_LEFT.applyForQuadrant(quad, quadrant);
        
        this.postPaintProcessQuadAndOutput(quad, target, isItem);
    }
    
    @Override
    public final boolean requiresQuadrants()
    {
        return true;
    }
}
