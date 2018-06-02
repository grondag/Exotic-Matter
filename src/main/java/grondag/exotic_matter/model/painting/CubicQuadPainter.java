package grondag.exotic_matter.model.painting;

import grondag.exotic_matter.model.state.ISuperModelState;
import grondag.exotic_matter.varia.Useful;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;

public abstract class CubicQuadPainter extends QuadPainter
{

    /** use for texture version & rotation alternation. Lower 8 bits for version, upper for rotation */
    private final int variationHashX;
    /** use for texture version & rotation alternation. Lower 8 bits for version, upper for rotation */
    private final int variationHashY;
    /** use for texture version & rotation alternation. Lower 8 bits for version, upper for rotation */
    private final int variationHashZ;
    
    protected final int species;
    
    protected final Vec3i pos;

    protected CubicQuadPainter(ISuperModelState modelState, Surface surface, PaintLayer paintLayer)
    {
        super(modelState, surface, paintLayer);
        
        this.species = modelState.hasSpecies() ? modelState.getSpecies() : 0;

        int x = modelState.getPosX();
        int y = modelState.getPosY();
        int z  = modelState.getPosZ();
        this.pos = new Vec3i(x, y, z);
        
        int shift = this.texture.textureScale().power;
        
        int speciesBits = species << 16;
        
        int xBits = (((y >> shift) & 0xFF) << 8) | ((z >> shift) & 0xFF) | speciesBits;
        int yBits = (((x >> shift) & 0xFF) << 8) | ((z >> shift) & 0xFF) | speciesBits;
        int zBits = (((x >> shift) & 0xFF) << 8) | ((y >> shift) & 0xFF) | speciesBits;
        
        this.variationHashX = MathHelper.hash(xBits);
        this.variationHashY = MathHelper.hash(yBits);
        this.variationHashZ = MathHelper.hash(zBits);
    }
    
    
    protected int textureVersionForFace(EnumFacing face)
    {
        if(this.texture.textureVersionCount() == 0) return 0;
        
        switch(face)
        {
        case DOWN:
        case UP:
            return this.variationHashY & this.texture.textureVersionMask();

        case EAST:
        case WEST:
            return this.variationHashX & this.texture.textureVersionMask();

        case NORTH:
        case SOUTH:
            return this.variationHashZ & this.texture.textureVersionMask();
            
        default:
            return 0;
        
        }
    }
    
    /** 
     * Gives randomized (if applicable) texture rotation for the given face.
     * If texture rotation type is FIXED, gives the textures default rotation.
     * If texture rotation type is CONSISTENT, is based on species only. 
     * If texture rotation type is RANDOM, is based on position (chunked by texture size) and species (if applies).
     */
    protected Rotation textureRotationForFace(EnumFacing face)
    {
        switch(this.texture.rotation().rotationType())
        {
        case CONSISTENT:
            return this.species == 0 
                ? this.texture.rotation().rotation
                : Useful.offsetEnumValue(this.texture.rotation().rotation, MathHelper.hash(this.species) & 3);
            
        case FIXED:
        default:
            return this.texture.rotation().rotation;
            
        case RANDOM:
            switch(face)
            {
            case DOWN:
            case UP:
                return Useful.offsetEnumValue(this.texture.rotation().rotation, (this.variationHashY >> 8) & 3);

            case EAST:
            case WEST:
                return Useful.offsetEnumValue(this.texture.rotation().rotation, (this.variationHashX >> 8) & 3);

            case NORTH:
            case SOUTH:
                return Useful.offsetEnumValue(this.texture.rotation().rotation, (this.variationHashZ >> 8) & 3);
                
            default:
                return this.texture.rotation().rotation;
            
            }
        }
    }
}
