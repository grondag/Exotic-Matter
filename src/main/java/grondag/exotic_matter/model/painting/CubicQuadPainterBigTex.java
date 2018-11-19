package grondag.exotic_matter.model.painting;

import java.util.function.Consumer;

import grondag.exotic_matter.model.primitives.polygon.IPaintablePolygon;
import grondag.exotic_matter.model.state.ISuperModelState;
import grondag.exotic_matter.model.texture.TextureRotationType;
import grondag.exotic_matter.model.texture.TextureScale;
import grondag.exotic_matter.varia.Useful;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;

public class CubicQuadPainterBigTex extends CubicQuadPainter
{
    private final boolean allowTexRotation;
    
    public CubicQuadPainterBigTex(ISuperModelState modelState, Surface surface, PaintLayer paintLayer)
    {
        super(modelState, surface, paintLayer);
        
        this.allowTexRotation = this.texture.rotation().rotationType() != TextureRotationType.FIXED;
    }

    @Override
    public void textureQuad(IPaintablePolygon quad, Consumer<IPaintablePolygon> target, boolean isItem)
    {
        // Determine what type of randomizations to apply so that we have a different
        // appearance based on depth and species.
        // If we are applying a single texture, then we alternate by translating, flipping and rotating the texture.
        // In this case, the normal variation logic in superclass does not apply.
        //
        // If the texture has alternates, we simply use the normal alternation/rotation logic in super class.
        // But we won't don't translate because then we'd need a way to know the 
        // texture version of adjacent volumes.  This would be possible if we got a reference to the 
        // alternator array instead of the alternator result, but it would needlessly complex.
        // 
        // If the texture has alternates, we also vary the texture selection and, if supported, 
        // based on depth within the plane, to provide variation between adjacent layers.
        // This depth-based variation can be disabled with a setting in the surface instance.
     
        assert quad.isLockUV() : "BigTex cubic quad painter received quad without lockUV semantics.  Not expected";

        Vec3i surfaceVec = CubicQuadPainterBigTex.getSurfaceVector(this.pos, quad.getNominalFace(), this.texture.textureScale());
        
        TextureScale scale = this.texture.textureScale();
        
        if(this.texture.textureVersionCount() == 1)
        {
            // no alternates, so do uv flip and offset and rotation based on depth & species only
            
            // abs is necessary so that hash input components combine together properly
            // Small random numbers already have most bits set.
            int depthAndSpeciesHash = quad.getSurfaceInstance().ignoreDepthForRandomization
                    ? MathHelper.hash((this.species << 8) | quad.textureSalt())
                    : MathHelper.hash(Math.abs(surfaceVec.getZ()) | (this.species << 8) | (quad.textureSalt() << 12));
            

            
            // rotation 
            quad.setRotation(this.allowTexRotation
                    ? Useful.offsetEnumValue(texture.rotation().rotation, depthAndSpeciesHash & 3)
                    : texture.rotation().rotation);
                    
          
            surfaceVec = rotateFacePerspective(surfaceVec, quad.getRotation(), scale);

            quad.setTextureName(this.texture.getTextureName(0));
            
            final int xOffset = (depthAndSpeciesHash >> 2) & scale.sliceCountMask; 
            final int yOffset = (depthAndSpeciesHash >> 8) & scale.sliceCountMask; 
            
            final int newX = (surfaceVec.getX() + xOffset) & scale.sliceCountMask;
            final int newY = (surfaceVec.getY() + yOffset) & scale.sliceCountMask;
            surfaceVec = new Vec3i(newX, newY, surfaceVec.getZ());
            
            final boolean flipU = this.allowTexRotation && (depthAndSpeciesHash & 256) == 0;
            final boolean flipV = this.allowTexRotation && (depthAndSpeciesHash & 512) == 0;

            final float sliceIncrement = scale.sliceIncrement;
            
            final int x = flipU ? scale.sliceCount - surfaceVec.getX() : surfaceVec.getX();
            final int y = flipV ? scale.sliceCount - surfaceVec.getY() : surfaceVec.getY();
            
            quad.setMinU(x * sliceIncrement);
            quad.setMaxU(quad.getMinU() + (flipU ? -sliceIncrement : sliceIncrement));

            
            quad.setMinV(y * sliceIncrement);
            quad.setMaxV(quad.getMinV() + (flipV ? -sliceIncrement : sliceIncrement));
            
        }
        else
        {
            // multiple texture versions, so do rotation and alternation normally, except add additional variation for depth;
            
         // abs is necessary so that hash input components combine together properly
            // Small random numbers already have most bits set.
            final int depthHash = quad.getSurfaceInstance().ignoreDepthForRandomization && quad.textureSalt() == 0
                    ? 0 
                    : MathHelper.hash(Math.abs(surfaceVec.getZ()) | (quad.textureSalt() << 8));

            quad.setTextureName(this.texture.getTextureName((this.textureVersionForFace(quad.getNominalFace()) + depthHash) & this.texture.textureVersionMask()));
            
            quad.setRotation(this.allowTexRotation
                    ? Useful.offsetEnumValue(this.textureRotationForFace(quad.getNominalFace()), (depthHash >> 16) & 3)
                    : this.textureRotationForFace(quad.getNominalFace()));
                    
            surfaceVec = rotateFacePerspective(surfaceVec, quad.getRotation(), scale);

            final float sliceIncrement = scale.sliceIncrement;
            
            quad.setMinU(surfaceVec.getX() * sliceIncrement);
            quad.setMaxU(quad.getMinU() + sliceIncrement);

            
            quad.setMinV(surfaceVec.getY() * sliceIncrement);
            quad.setMaxV(quad.getMinV() + sliceIncrement);
        }
        this.postPaintProcessQuadAndOutput(quad, target, isItem);
    }
}
