package grondag.exotic_matter.model.painting;

import java.util.List;
import java.util.function.Consumer;

import grondag.exotic_matter.model.state.ISuperModelState;
import grondag.exotic_matter.model.texture.ITexturePalette;
import grondag.exotic_matter.model.texture.TexturePaletteRegistry;
import grondag.exotic_matter.model.texture.TextureScale;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3i;

public abstract class QuadPainter
{
    protected final ISuperModelState modelState;
    public final Surface surface;
    public final PaintLayer paintLayer;
    protected final ITexturePalette texture;
    
    /**
     * Assigns specific texture and texture rotation based on model state and information
     * in the polygon and assigned surface. Also handles UV mapping for the assigned texture.<p>
     * 
     * Implementations can and should assume locked UV coordinates are assigned before 
     * this is called if UV locking is enabled for the quad<p>
     * 
     * At this point {@link #isQuadValidForPainting(IPaintablePolygon)} has been checked and is true. 
     * Input quad will already be a clone and can be modified directly if expedient.
     * RenderLayer, lighting mode and color will already be set.<p>
     * 
     * Resulting quad(s), if any, must be output using {@link #postPaintProcessQuadAndOutput(IMutablePolygon, List, boolean)}.
     * This is ugly because have to pass through the outputList and isItem parameter - but didn't want to instantiate
     * a new collection for painters that output more than one quad.  Should improve this next time painting is refactored.
     */
    protected abstract void textureQuad(IPaintablePolygon inputQuad, Consumer<IPaintablePolygon> target, boolean isItem);
    
    /**
     * True if the painter requires quads that are split into quadrants split at u,v 0.5, 0.5 in
     * order to apply quandrant-style textures.  If true, quads will be split into quadrants 
     * (if not already generated that way) before being passed to <em>all</em> painters.<p>
     * 
     * Must be done for all painters (not just quadrant painters) to prevent z-fighting during
     * render caused by differences in FP rounding between to similar but not identical surfaces.<p>
     */
    public boolean requiresQuadrants()
    {
        return false;
    }
    
    public QuadPainter(ISuperModelState modelState, Surface surface, PaintLayer paintLayer)
    {
        this.modelState = modelState;
        this.surface = surface;
        this.paintLayer = paintLayer;

        ITexturePalette tex = modelState.getTexture(paintLayer);
        this.texture = tex == TexturePaletteRegistry.NONE ? modelState.getTexture(PaintLayer.BASE) : tex;
    }
    
    protected abstract boolean isQuadValidForPainting(IPaintablePolygon inputQuad);
    
    /**
     * True if painter will render a solid surface. When Acuity API is enabled
     * this signals that overlay textures can be packed into single quad.
     */
    public boolean isSolid()
    {
        switch(this.paintLayer)
        {
        case BASE:
        case CUT:
        case LAMP:
            return !this.modelState.isTranslucent(this.paintLayer);
            
        case MIDDLE:
        case OUTER:
        default:
            return false;
        }
    }
    
    /**
     * If isItem = true will bump out quads from block center to provide
     * better depth rendering of layers in item rendering.
     */
    public final void producePaintedQuad(IPaintablePolygon inputQuad, Consumer<IPaintablePolygon> target, boolean isItem)
    {
        if(!isQuadValidForPainting(inputQuad)) return;
    
        inputQuad.setRenderLayer(modelState.getRenderPass(paintLayer));
     
        inputQuad.setEmissive(modelState.isEmissive(paintLayer));
        
        // TODO: Vary color slightly with species, as user-selected option
        
        this.textureQuad(inputQuad, target, isItem);
    }
    
    /**
     * Call from paint quad in sub classes to return results.
     * Handles item scaling, then adds to the output list.
     */
    protected final void postPaintProcessQuadAndOutput(IPaintablePolygon inputQuad, Consumer<IPaintablePolygon> target, boolean isItem)
    {
        if(isItem)
        {
            //Acuity API doesn't support multi-layer textures for item render yet.
            //This step wouldn't be necessary if it did.
            assert inputQuad.layerCount() == 1;
            
            switch(this.paintLayer)
            {
            case MIDDLE:
                inputQuad.scaleFromBlockCenter(1.01f);
                break;

            case OUTER:
                inputQuad.scaleFromBlockCenter(1.02f);
                break;

            default:
                break;
            }
        }
        modelState.getVertexProcessor(this.paintLayer).process(inputQuad, modelState, paintLayer);

        target.accept(inputQuad);
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
        //PERF: reuse instances?
        
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
}
