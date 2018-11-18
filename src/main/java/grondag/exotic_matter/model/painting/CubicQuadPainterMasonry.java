package grondag.exotic_matter.model.painting;

import java.util.function.Consumer;

import grondag.exotic_matter.model.primitives.FaceQuadInputs;
import grondag.exotic_matter.model.primitives.IPaintableQuad;
import grondag.exotic_matter.model.state.ISuperModelState;
import grondag.exotic_matter.world.Rotation;
import grondag.exotic_matter.world.SimpleJoin;
import grondag.exotic_matter.world.SimpleJoinFaceState;
import net.minecraft.util.EnumFacing;


public class CubicQuadPainterMasonry extends CubicQuadPainter
{
    protected final static FaceQuadInputs[][] FACE_INPUTS = new FaceQuadInputs[EnumFacing.VALUES.length][SimpleJoinFaceState.values().length];

    protected final SimpleJoin bjs;
    
    public CubicQuadPainterMasonry(ISuperModelState modelState, Surface surface, PaintLayer paintLayer)
    {
        super(modelState, surface, paintLayer);
        this.bjs = modelState.getMasonryJoin();
    }
    
    @Override
    public final void textureQuad(IPaintableQuad quad, Consumer<IPaintableQuad> target, boolean isItem)
    {
        assert quad.isLockUV() : "Masonry cubic quad painter received quad without lockUV semantics.  Not expected";
        
        // face won't be null - checked in validation method
        final EnumFacing face = quad.getNominalFace();
        
        @SuppressWarnings("null")
        SimpleJoinFaceState fjs = SimpleJoinFaceState.find(face, this.bjs);
        
        @SuppressWarnings("null")
        FaceQuadInputs inputs = FACE_INPUTS[face.ordinal()][fjs.ordinal()];
        
        if(inputs == null) return;
            
        quad.setRotation(inputs.rotation);
        quad.setMinU(inputs.flipU ? 1 : 0);
        quad.setMinV(inputs.flipV ? 1 : 0);
        quad.setMaxU(inputs.flipU ? 0 : 1);
        quad.setMaxV(inputs.flipV ? 0 : 1);
        quad.setTextureName(this.texture.getTextureName(this.textureVersionForFace(face), inputs.textureOffset));
        
        this.postPaintProcessQuadAndOutput(quad, target, isItem);
    }
    
    private static enum Textures
    {
        BOTTOM_LEFT_RIGHT,
        BOTTOM_LEFT,
        LEFT_RIGHT,
        BOTTOM,
        ALL;
    }
    
    static
    {
        // mapping is unusual in that a join indicates a border IS present on texture
        for(EnumFacing face: EnumFacing.VALUES){
            FACE_INPUTS[face.ordinal()][SimpleJoinFaceState.NONE.ordinal()] = null; //new ImmutableList.Builder<BakedQuad>().build(); // NO BORDER
            FACE_INPUTS[face.ordinal()][SimpleJoinFaceState.NO_FACE.ordinal()] = null; //new ImmutableList.Builder<BakedQuad>().build(); // NO BORDER
            
            FACE_INPUTS[face.ordinal()][SimpleJoinFaceState.BOTTOM_LEFT_RIGHT.ordinal()] = new FaceQuadInputs( Textures.BOTTOM_LEFT_RIGHT.ordinal(), Rotation.ROTATE_NONE, false, false);
            FACE_INPUTS[face.ordinal()][SimpleJoinFaceState.TOP_BOTTOM_LEFT.ordinal()] = new FaceQuadInputs( Textures.BOTTOM_LEFT_RIGHT.ordinal(), Rotation.ROTATE_90, false, false);
            FACE_INPUTS[face.ordinal()][SimpleJoinFaceState.TOP_LEFT_RIGHT.ordinal()] = new FaceQuadInputs( Textures.BOTTOM_LEFT_RIGHT.ordinal(), Rotation.ROTATE_180, false, false);
            FACE_INPUTS[face.ordinal()][SimpleJoinFaceState.TOP_BOTTOM_RIGHT.ordinal()] = new FaceQuadInputs( Textures.BOTTOM_LEFT_RIGHT.ordinal(), Rotation.ROTATE_270, false, false);

            FACE_INPUTS[face.ordinal()][SimpleJoinFaceState.BOTTOM_LEFT.ordinal()] = new FaceQuadInputs( Textures.BOTTOM_LEFT.ordinal(), Rotation.ROTATE_NONE, false, false);
            FACE_INPUTS[face.ordinal()][SimpleJoinFaceState.TOP_LEFT.ordinal()] = new FaceQuadInputs( Textures.BOTTOM_LEFT.ordinal(), Rotation.ROTATE_90, false, false);
            FACE_INPUTS[face.ordinal()][SimpleJoinFaceState.TOP_RIGHT.ordinal()] = new FaceQuadInputs( Textures.BOTTOM_LEFT.ordinal(), Rotation.ROTATE_180, false, false);
            FACE_INPUTS[face.ordinal()][SimpleJoinFaceState.BOTTOM_RIGHT.ordinal()] = new FaceQuadInputs( Textures.BOTTOM_LEFT.ordinal(), Rotation.ROTATE_270, false, false);

            FACE_INPUTS[face.ordinal()][SimpleJoinFaceState.LEFT_RIGHT.ordinal()] = new FaceQuadInputs( Textures.LEFT_RIGHT.ordinal(), Rotation.ROTATE_NONE, false, false);
            FACE_INPUTS[face.ordinal()][SimpleJoinFaceState.TOP_BOTTOM.ordinal()] = new FaceQuadInputs( Textures.LEFT_RIGHT.ordinal(), Rotation.ROTATE_90, false, false);

            FACE_INPUTS[face.ordinal()][SimpleJoinFaceState.BOTTOM.ordinal()] = new FaceQuadInputs( Textures.BOTTOM.ordinal(), Rotation.ROTATE_NONE, false, false);
            FACE_INPUTS[face.ordinal()][SimpleJoinFaceState.LEFT.ordinal()] = new FaceQuadInputs( Textures.BOTTOM.ordinal(), Rotation.ROTATE_90, false, false);
            FACE_INPUTS[face.ordinal()][SimpleJoinFaceState.TOP.ordinal()] = new FaceQuadInputs( Textures.BOTTOM.ordinal(), Rotation.ROTATE_180, false, false);
            FACE_INPUTS[face.ordinal()][SimpleJoinFaceState.RIGHT.ordinal()] = new FaceQuadInputs( Textures.BOTTOM.ordinal(), Rotation.ROTATE_270, false, false);

            FACE_INPUTS[face.ordinal()][SimpleJoinFaceState.ALL.ordinal()] = new FaceQuadInputs( Textures.ALL.ordinal(), Rotation.ROTATE_NONE, false, false);
        }
    }
}
