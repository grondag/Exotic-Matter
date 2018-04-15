package grondag.exotic_matter.model.painter;

import grondag.exotic_matter.model.ISuperModelState;
import grondag.exotic_matter.model.PaintLayer;
import grondag.exotic_matter.model.TextureRotationType;
import grondag.exotic_matter.model.TextureScale;
import grondag.exotic_matter.render.IMutablePolygon;
import grondag.exotic_matter.render.Surface;
import grondag.exotic_matter.varia.Useful;
import net.minecraft.util.EnumFacing;
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
    public IMutablePolygon paintQuad(IMutablePolygon quad)
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
                    ? quad.getSurfaceInstance().textureSalt 
                    : MathHelper.hash(Math.abs(surfaceVec.getZ()) | (this.species << 8) | (quad.getSurfaceInstance().textureSalt << 12));
            
            // rotation 
            quad.setRotation(this.allowTexRotation
                    ? Useful.offsetEnumValue(texture.rotation().rotation, depthAndSpeciesHash & 3)
                    : texture.rotation().rotation);
                    
            
            surfaceVec = rotateFacePerspective(surfaceVec, quad.getRotation(), scale);

            quad.setTextureName(this.texture.getTextureName(0));
            
            int xOffset = (depthAndSpeciesHash >> 2) & scale.sliceCountMask; 
            int yOffset = (depthAndSpeciesHash >> 8) & scale.sliceCountMask; 
            
            int newX = (surfaceVec.getX() + xOffset) & scale.sliceCountMask;
            int newY = (surfaceVec.getY() + yOffset) & scale.sliceCountMask;
            surfaceVec = new Vec3i(newX, newY, surfaceVec.getZ());
            
            boolean flipU = this.allowTexRotation && (depthAndSpeciesHash & 256) == 0;
            boolean flipV = this.allowTexRotation && (depthAndSpeciesHash & 512) == 0;

            float sliceIncrement = scale.sliceIncrement;
            
            int x = flipU ? scale.sliceCount - surfaceVec.getX() : surfaceVec.getX();
            int y = flipV ? scale.sliceCount - surfaceVec.getY() : surfaceVec.getY();
            
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
            int depthHash = quad.getSurfaceInstance().ignoreDepthForRandomization && quad.getSurfaceInstance().textureSalt == 0
                    ? 0 
                    : MathHelper.hash(Math.abs(surfaceVec.getZ()) | (quad.getSurfaceInstance().textureSalt << 8));

            quad.setTextureName(this.texture.getTextureName((this.textureVersionForFace(quad.getNominalFace()) + depthHash) & this.texture.textureVersionMask()));
            
            quad.setRotation(this.allowTexRotation
                    ? Useful.offsetEnumValue(this.textureRotationForFace(quad.getNominalFace()), (depthHash >> 16) & 3)
                    : this.textureRotationForFace(quad.getNominalFace()));
                    
            surfaceVec = rotateFacePerspective(surfaceVec, quad.getRotation(), scale);

            float sliceIncrement = scale.sliceIncrement;
            
            quad.setMinU(surfaceVec.getX() * sliceIncrement);
            quad.setMaxU(quad.getMinU() + sliceIncrement);

            
            quad.setMinV(surfaceVec.getY() * sliceIncrement);
            quad.setMaxV(quad.getMinV() + sliceIncrement);
        }
        return quad;
    }

    /** 
     * Transform input vector so that x & y correspond with u / v on the given face, with u,v origin at upper left
     * and z is depth, where positive values represent distance into the face (away from viewer). <br><br>
     * 
     * Coordinates are start masked to the scale of the texture being used and when we reverse an orthogonalAxis, 
     * we use the texture's sliceMask as the basis so that we remain within the frame of the
     * texture scale we are using.  <br><br>
     * 
     * Note that the x, y components are for determining min/max UV values. 
     * They should NOT be used to set vertex UV coordinates directly.
     * All bigtex models should have lockUV = true, which means that 
     * uv coordinates will be derived at time of quad bake by projecting each
     * vertex onto the plane of the quad's nominal face. 
     * Setting UV coordinates on a quad with lockUV=true has no effect.
     */
    protected static Vec3i getSurfaceVector(Vec3i vec, EnumFacing face, TextureScale scale)
    {
        int sliceCountMask = scale.sliceCountMask;
        int x = vec.getX() & sliceCountMask;
        int y = vec.getY() & sliceCountMask;
        int z = vec.getZ() & sliceCountMask;
        
        switch(face)
        {
        case EAST:
            return new Vec3i(sliceCountMask - z, sliceCountMask - y, -vec.getX());
        
        case WEST:
            return new Vec3i(z, sliceCountMask - y, vec.getX());
        
        case NORTH:
            return new Vec3i(sliceCountMask - x, sliceCountMask - y, vec.getZ());
        
        case SOUTH:
            return new Vec3i(x, sliceCountMask - y, -vec.getZ());
        
        case DOWN:
            return new Vec3i(x, sliceCountMask - z, vec.getY());
    
        case UP:
        default:
            return new Vec3i(x, z, -vec.getY());
        }
    }
}
