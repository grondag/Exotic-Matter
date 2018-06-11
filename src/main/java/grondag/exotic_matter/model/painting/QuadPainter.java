package grondag.exotic_matter.model.painting;

import java.util.List;
import java.util.function.Consumer;

import grondag.exotic_matter.model.color.ColorMap.EnumColorMap;
import grondag.exotic_matter.model.primitives.IMutablePolygon;
import grondag.exotic_matter.model.primitives.IPolygon;
import grondag.exotic_matter.model.primitives.Poly;
import grondag.exotic_matter.model.primitives.Vertex;
import grondag.exotic_matter.model.state.ISuperModelState;
import grondag.exotic_matter.model.texture.ITexturePalette;
import grondag.exotic_matter.model.texture.TexturePaletteRegistry;
import grondag.exotic_matter.model.texture.TextureScale;
import grondag.exotic_matter.varia.Color;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3i;

public abstract class QuadPainter
{
    protected final ISuperModelState modelState;
    public final Surface surface;
    public final PaintLayer paintLayer;
    protected final ITexturePalette texture;
    
    /**
     * At this point {@link #isQuadValidForPainting(IPolygon)} has been checked and is true. 
     * Input quad will already be a clone and can be modified directly if expedient.
     * RenderLayer, lighting mode and color will already be set.<p>
     * 
     * Resulting quad(s), if any, must be output using {@link #postPaintProcessQuadAndOutput(IMutablePolygon, List, boolean)}.
     * This is ugly because have to pass through the outputList and isItem parameter - but didn't want to instantiate
     * a new collection for painters that output more than one quad.  Should improve this next time painting is refactored.
     */
    protected abstract void paintQuad(IMutablePolygon inputQuad, Consumer<IPolygon> target, boolean isItem);
    
    public QuadPainter(ISuperModelState modelState, Surface surface, PaintLayer paintLayer)
    {
        this.modelState = modelState;
        this.surface = surface;
        this.paintLayer = paintLayer;

        ITexturePalette tex = modelState.getTexture(paintLayer);
        this.texture = tex == TexturePaletteRegistry.NONE ? modelState.getTexture(PaintLayer.BASE) : tex;
    }
    
    /** for null painter only */
    @SuppressWarnings("null")
    private QuadPainter()
    {
        this.modelState = null;
        this.surface = null;
        this.paintLayer = null;
        this.texture = null;
    }

    protected boolean isQuadValidForPainting(IPolygon inputQuad)
    {
        if(inputQuad.getSurfaceInstance().surface() != this.surface) return false;

        switch(this.paintLayer)
        {
        case BASE:
            if(inputQuad.getSurfaceInstance().disableBase) return false;
            break;

        case MIDDLE:
            if(inputQuad.getSurfaceInstance().disableMiddle) return false;
            break;

        case OUTER:
            if(inputQuad.getSurfaceInstance().disableOuter) return false;
            break;

        case CUT:
        case LAMP:
        default:
            break;

        }

        return true;
    }
    
    
    /**
     * If isItem = true will bump out quads from block center to provide
     * better depth rendering of layers in item rendering.
     */
    public final void addPaintedQuadToList(IPolygon inputQuad, Consumer<IPolygon> target, boolean isItem)
    {
        if(!isQuadValidForPainting(inputQuad)) return;
    
        IMutablePolygon result = Poly.mutableCopyOf(inputQuad);
        result.setRenderPass(modelState.getRenderPass(paintLayer));
        
        modelState.getShape().meshFactory().colorizer(modelState, paintLayer, inputQuad.getSurfaceInstance()).recolorQuad(result, modelState, paintLayer);
     
        // TODO: Vary color slightly with species, as user-selected option
        
        this.paintQuad(result, target, isItem);
    }
    
    /**
     * Call from paint quad in sub classes to return results.
     * Handles UV Lock and item scaling, then adds to the output list.
     */
    protected final void postPaintProcessQuadAndOutput(IMutablePolygon inputQuad, Consumer<IPolygon> target, boolean isItem)
    {
        if(inputQuad.isLockUV())
        {
            // if lockUV is on, derive UV coords by projection
            // of vertex coordinates on the plane of the quad's face
            inputQuad.assignLockedUVCoordinates();;
        }

        if(isItem)
        {
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
        target.accept(inputQuad);
    }

    public static QuadPainter makeNullQuadPainter(ISuperModelState modelState, Surface surface, PaintLayer paintLayer)
    {
        return NullQuadPainter.INSTANCE;
    }
    
    public static class NullQuadPainter extends QuadPainter
    {

        private static final NullQuadPainter INSTANCE = new NullQuadPainter();
        
        private NullQuadPainter()
        {
            super();
        };
        
        @Override
        protected boolean isQuadValidForPainting(IPolygon inputQuad)
        {
            return false;
        }
        
        @Override
        protected void paintQuad(IMutablePolygon inputQuad, Consumer<IPolygon> target, boolean isItem)
        {
        }
        
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
    
    /** 
     * Rotates given surface vector around the center of the texture by the given degree.
     * 
     */
    protected static Vec3i rotateFacePerspective(Vec3i vec, Rotation rotation, TextureScale scale)
    {
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
