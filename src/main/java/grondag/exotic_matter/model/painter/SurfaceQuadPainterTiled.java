package grondag.exotic_matter.model.painter;

import java.util.List;

import grondag.exotic_matter.model.ISuperModelState;
import grondag.exotic_matter.model.PaintLayer;
import grondag.exotic_matter.model.TextureRotationType;
import grondag.exotic_matter.render.IMutablePolygon;
import grondag.exotic_matter.render.IPolygon;
import grondag.exotic_matter.render.Surface;
import grondag.exotic_matter.render.Surface.SurfaceInstance;
import grondag.exotic_matter.render.SurfaceTopology;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.math.MathHelper;

/**
 * 
 * See {@link SurfaceTopology#TILED}
 */
public class SurfaceQuadPainterTiled extends QuadPainter
{
    public SurfaceQuadPainterTiled(ISuperModelState modelState, Surface surface, PaintLayer paintLayer)
    {
        super(modelState, surface, paintLayer);
    }

    @Override
    public void paintQuad(IMutablePolygon quad, List<IPolygon> outputList, boolean isItem)
    {
        assert !quad.isLockUV() : "Tiled surface quad painter received quad with lockUV semantics.  Not expected";
        
        //TODO: handle splitting when quads cross a wrapping boundary in either dimension
        final SurfaceInstance surfIn = quad.getSurfaceInstance();
        
        // determine if we are tiling the texture at all based on UV scale
        
        int modelScale = surfIn.uvScale;
        int textureScale = this.texture.textureScale().sliceCount;
        
        /** 
         * Max number of textured tiles needed in either direction to cover entire surface
         * should be 1 or a power of 2, because both modelScale and textureScale are such.
         */
        int textureSlices = modelScale > textureScale ? 1 : modelScale / textureScale;
       
        /**  Max quad U value may be outside the 0-1 range. */
        final float maxU = Math.max(quad.getMaxU(), quad.getMinU());
        
        /** Capture randomization hints from u values.  */
        final int uSalt = Math.round(maxU);
        
        /** u tile position */
        final int uOrdinal = Math.round(maxU * textureSlices) - 1;
        
        /**  Max quad V value, may be outside the 0-1 range. */
        final float maxV = Math.max(quad.getMaxV(), quad.getMinV());
        
        /** Capture randomization hints from u values.  */
        final int vSalt = Math.round(maxU);
        
        /** v tile position */
        final  int vOrdinal = Math.round(maxV * textureSlices) - 1;
        
        // temporary until splits working - bring unit uv coordinates within the 0-1 range
        int shiftU = (int)maxU - 1;
        if(shiftU > 0)
        {
            quad.setMaxU(quad.getMaxU() - shiftU);
            quad.setMinU(quad.getMinU() - shiftU);
        }
        
        int shiftV = (int)maxV - 1;
        if(shiftV > 0)
        {
            quad.setMaxV(quad.getMaxV() - shiftV);
            quad.setMinV(quad.getMinV() - shiftV);
        }
//        
//        // uv coordinates should now be in range 0 to textureSlices
//        // so just need to scale so that max values are 1.0
//        
//        float uvScale = 1f / textureSlices;
//        quad.scaleQuadUV(uvScale, uvScale);
       
       int hash = MathHelper.hash(uOrdinal | (vOrdinal << 8) | (uSalt << 16) | (vSalt << 24));
        
        int textureVersion = this.texture.textureVersionMask() & (hash >> 4);
        quad.setTextureName(this.texture.getTextureName(textureVersion));
                
        //TODO: would this still work?
//        int rotationOrdinal = this.texture.rotation().rotation.ordinal();
//        if(this.texture.rotation().rotationType() == TextureRotationType.RANDOM)
//        {
//            rotationOrdinal = (rotationOrdinal + hash) & 3;
//        }
//        
//        quad.setRotation(Rotation.values()[rotationOrdinal]);
//        
//        if(rotationOrdinal > 0)
//        {
//            for(int i = 0; i < rotationOrdinal; i++)
//            {
//                float oldMinU = quad.getMinU();
//                float oldMaxU = quad.getMaxU();
//                quad.setMinU(quad.getMinV());
//                quad.setMaxU(quad.getMaxV());
//                quad.setMinV(16 - oldMaxU);
//                quad.setMaxV(16 - oldMinU);
//            }
//        }
        
        this.postPaintProcessQuadAndAddToList(quad, outputList, isItem);
    }
}
