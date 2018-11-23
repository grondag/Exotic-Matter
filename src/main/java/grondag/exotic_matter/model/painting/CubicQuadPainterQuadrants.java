package grondag.exotic_matter.model.painting;

import java.util.function.Consumer;

import grondag.exotic_matter.model.primitives.better.IPaintablePoly;
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
    
    private static TextureQuadrant textureMap(FaceCorner corner, CornerJoinFaceState faceState)
    {
        if(faceState.isJoined(corner.leftSide))
        {
            if(faceState.isJoined(corner.rightSide))
                return faceState.needsCorner(corner) ? TextureQuadrant.CORNER : TextureQuadrant.FULL;
            else 
                return TextureQuadrant.SIDE_RIGHT;
        }
        else if(faceState.isJoined(corner.rightSide))
            return TextureQuadrant.SIDE_LEFT;
        else 
            return TextureQuadrant.ROUND;
    }
    
    static
    {
        for(FaceCorner corner : FaceCorner.values())
        {
            for(CornerJoinFaceState faceState : CornerJoinFaceState.values())
            {
                TEXTURE_MAP[corner.ordinal()][faceState.ordinal()] = textureMap(corner, faceState);
            }
        }
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
    protected final void textureQuad(IPaintablePoly quad, Consumer<IPaintablePoly> target, boolean isItem)
    {
        assert quad.isLockUV(layerIndex) : "Quadrant cubic quad painter received quad without lockUV semantics.  Not expected";
    
        final FaceCorner quadrant = QuadrantSplitter.uvQuadrant(quad, layerIndex);
        if(quadrant == null) 
            return;
        
        final EnumFacing nominalFace = quad.getNominalFace();
        if(nominalFace == null) return;
        
        final int textureVersion = this.texture.textureVersionMask() 
                & (this.textureHashForFace(nominalFace) >> (quadrant.ordinal() * 4));
        
        quad.setTextureName(layerIndex, this.texture.getTextureName(textureVersion));
        
        final CornerJoinFaceState faceState = this.faceState(nominalFace);
        
        TEXTURE_MAP[quadrant.ordinal()][faceState.ordinal()].applyForQuadrant(quad, layerIndex, quadrant);
        
        this.postPaintProcessQuadAndOutput(quad, target, isItem);
    }
    
    @Override
    public final boolean requiresQuadrants()
    {
        return true;
    }
}
