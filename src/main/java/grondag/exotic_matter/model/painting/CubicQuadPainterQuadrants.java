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
    
//        FaceQuadInputs inputs = FACE_INPUTS[face.ordinal()][ bjs.getFaceJoinState(face).ordinal()];
//
//        if(inputs == null) return;
//        
//        // don't render the "no border" texture unless this is a tile of some kind
//        if(inputs == NO_BORDER && !this.texture.renderNoBorderAsTile()) return;
//        
//        quad.setRotation(inputs.rotation);
////        cubeInputs.rotateBottom = false;
//        quad.setMinU(inputs.flipU ? 1 : 0);
//        quad.setMinV(inputs.flipV ? 1 : 0);
//        quad.setMaxU(inputs.flipU ? 0 : 1);
//        quad.setMaxV(inputs.flipV ? 0 : 1);
        final FaceCorner quadrant = QuadQuadrantSplitter.uvQuadrant(quad);
        if(quadrant == null) 
            return;
        
        @SuppressWarnings("null")
        final int textureVersion = this.texture.textureVersionMask() 
                & (this.textureHashForFace(quad.getNominalFace()) >> (quadrant.ordinal() * 4));
        
        quad.setTextureName(this.texture.getTextureName(textureVersion));
        
        this.postPaintProcessQuadAndOutput(quad, target, isItem);
    }
    
    @Override
    public final boolean requiresQuadrants()
    {
        return true;
    }
}
