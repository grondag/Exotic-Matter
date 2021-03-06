package grondag.exotic_matter.model.painting;

import grondag.exotic_matter.model.primitives.polygon.IMutablePolygon;
import grondag.exotic_matter.model.primitives.polygon.IPolygon;
import grondag.exotic_matter.model.primitives.stream.IMutablePolyStream;
import grondag.exotic_matter.model.state.ISuperModelState;
import grondag.exotic_matter.model.texture.ITexturePalette;
import grondag.exotic_matter.model.texture.TexturePaletteRegistry;
import grondag.exotic_matter.model.texture.TextureScale;
import grondag.exotic_matter.varia.Useful;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;


public abstract class QuadPainter
{
    @FunctionalInterface
    public static interface IPaintMethod
    {
        /**
         * Assigns specific texture and texture rotation based on model state 
         * and information in the polygon and surface. Also handles texture UV mapping.<p>
         * 
         * Implementations can and should assume locked UV coordinates are assigned before 
         * this is called if UV locking is enabled for the quad<p>
         * 
         * Implementation should claim and use first render layer with a null texture name.<br>
         * (Claim by assigning a non-null texture name.)
         * 
         * Any polys in the input stream that are split should be deleted and new polys appended to the stream.<p>
         * 
         * Implementation may assume stream is non-empty and stream editor is at origin.<p>
         * 
         */
        void paintQuads(IMutablePolyStream stream, ISuperModelState modelState, PaintLayer paintLayer);
    }

    public static int firstAvailableTextureLayer(IPolygon poly)
    {
        return poly.getTextureName(0) == null
                ? 0
                : poly.getTextureName(1) == null
                    ? 1
                    : 2;
    }
    
    protected static ITexturePalette getTexture(ISuperModelState modelState, PaintLayer paintLayer)
    {
        ITexturePalette tex = modelState.getTexture(paintLayer);
        return tex == TexturePaletteRegistry.NONE ? modelState.getTexture(PaintLayer.BASE) : tex;
    }
    
    /**
     * True if painter will render a solid surface. When Acuity API is enabled
     * this signals that overlay textures can be packed into single quad.
     */
    public static boolean isSolid(ISuperModelState modelState, PaintLayer paintLayer)
    {
        switch(paintLayer)
        {
        case BASE:
        case CUT:
        case LAMP:
            return !modelState.isTranslucent(paintLayer);
            
        case MIDDLE:
        case OUTER:
        default:
            return false;
        }
    }
    
    /**
     * Call from paint quad in sub classes to return results.
     * Handles item scaling, then adds to the output list.
     */
    protected static void commonPostPaint(IMutablePolygon editor, int layerIndex, ISuperModelState modelState, PaintLayer paintLayer)
    {
        editor.setRenderLayer(layerIndex, modelState.getRenderPass(paintLayer));
        editor.setEmissive(layerIndex, modelState.isEmissive(paintLayer));
        
        modelState.getVertexProcessor(paintLayer).process(editor, layerIndex, modelState, paintLayer);
        
        // FIXME: not going to work with new primitives w/ shared geometry
        // move this to baking if still needed
//        if(isItem)
//        {
//            switch(this.paintLayer)
//            {
//            case MIDDLE:
//                inputQuad.scaleFromBlockCenter(1.01f);
//                break;
//
//            case OUTER:
//                inputQuad.scaleFromBlockCenter(1.02f);
//                break;
//
//            default:
//                break;
//            }
//        }
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
    protected static Vec3i getSurfaceVector(int blockX, int blockY, int blockZ, EnumFacing face, TextureScale scale)
    {
        //PERF: reuse instances?
        
        int sliceCountMask = scale.sliceCountMask;
        int x = blockX & sliceCountMask;
        int y = blockY & sliceCountMask;
        int z = blockZ & sliceCountMask;
        
        switch(face)
        {
        case EAST:
            return new Vec3i(sliceCountMask - z, sliceCountMask - y, -blockX);
        
        case WEST:
            return new Vec3i(z, sliceCountMask - y, blockX);
        
        case NORTH:
            return new Vec3i(sliceCountMask - x, sliceCountMask - y, blockZ);
        
        case SOUTH:
            return new Vec3i(x, sliceCountMask - y, -blockZ);
        
        case DOWN:
            return new Vec3i(x, sliceCountMask - z, blockY);
    
        case UP:
        default:
            return new Vec3i(x, z, -blockY);
        }
    }
    
    /** 
     * Rotates given surface vector around the center of the texture by the given degree.
     * 
     */
    protected static Vec3i rotateFacePerspective(Vec3i vec, Rotation rotation, TextureScale scale)
    {
        // PERF - reuse instances?
        switch(rotation)
        {
        case ROTATE_90:
            return new Vec3i(vec.getY(), scale.sliceCountMask - vec.getX(), vec.getZ());
    
        case ROTATE_180:
            return new Vec3i(scale.sliceCountMask - vec.getX(), scale.sliceCountMask - vec.getY(), vec.getZ());
            
        case ROTATE_270:
            return new Vec3i(scale.sliceCountMask - vec.getY(), vec.getX(), vec.getZ());
    
        case ROTATE_NONE:
        default:
            return vec;
        
        }
    }
    
    protected static int textureVersionForFace(EnumFacing face, ITexturePalette tex, ISuperModelState modelState)
    {
        if(tex.textureVersionCount() == 0) return 0;
        return textureHashForFace(face, tex, modelState) & tex.textureVersionMask();
    }
    
    protected static int textureHashForFace(EnumFacing face, ITexturePalette tex, ISuperModelState modelState)
    {
        final int species = modelState.hasSpecies() ? modelState.getSpecies() : 0;
        final int speciesBits = species << 16;
        final int shift = tex.textureScale().power;
        
        switch(face)
        {
            case DOWN:
            case UP:
            {
                final int yBits = (((modelState.getPosX() >> shift) & 0xFF) << 8) | ((modelState.getPosZ() >> shift) & 0xFF) | speciesBits;
                return MathHelper.hash(yBits);
            }
            
            case EAST:
            case WEST:
            {
                final int xBits = (((modelState.getPosY() >> shift) & 0xFF) << 8) | ((modelState.getPosZ() >> shift) & 0xFF) | speciesBits;
                return MathHelper.hash(xBits);
            }
            
            case NORTH:
            case SOUTH:
            {
                final int zBits = (((modelState.getPosX() >> shift) & 0xFF) << 8) | ((modelState.getPosY() >> shift) & 0xFF) | speciesBits;
                return MathHelper.hash(zBits);
            }
            
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
    protected static Rotation textureRotationForFace(EnumFacing face, ITexturePalette tex, ISuperModelState modelState)
    {
        final int species = modelState.hasSpecies() ? modelState.getSpecies() : 0;
        switch(tex.rotation().rotationType())
        {
            case CONSISTENT:
                return species == 0 
                    ? tex.rotation().rotation
                    : Useful.offsetEnumValue(tex.rotation().rotation, MathHelper.hash(species) & 3);
                
            case FIXED:
            default:
                return tex.rotation().rotation;
                
            case RANDOM:
                return Useful.offsetEnumValue(tex.rotation().rotation, (textureHashForFace(face, tex, modelState) >> 8) & 3);
        }
    }
}
