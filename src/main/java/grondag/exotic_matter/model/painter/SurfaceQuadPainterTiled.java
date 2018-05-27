package grondag.exotic_matter.model.painter;

import java.util.List;

import grondag.exotic_matter.model.ISuperModelState;
import grondag.exotic_matter.model.PaintLayer;
import grondag.exotic_matter.model.TextureRotationType;
import grondag.exotic_matter.render.IMutablePolygon;
import grondag.exotic_matter.render.IPolygon;
import grondag.exotic_matter.render.Surface;
import grondag.exotic_matter.render.SurfaceTopology;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.math.MathHelper;

/**
 * 
 * See {@link SurfaceTopology#TILED}
 */
public class SurfaceQuadPainterTiled extends SurfaceQuadPainter
{
    public SurfaceQuadPainterTiled(ISuperModelState modelState, Surface surface, PaintLayer paintLayer)
    {
        super(modelState, surface, paintLayer);
    }

    @Override
    public void paintQuad(IMutablePolygon quad, List<IPolygon> outputList, boolean isItem)
    {
        assert !quad.isLockUV() : "Tiled surface quad painter received quad with lockUV semantics.  Not expected";
        
        int sliceCount = this.texture.textureScale().sliceCount;
        
        float maxU = Math.max(quad.getMaxU(), quad.getMinU());
        int uOrdinal = ((Math.round(maxU) - 1) / sliceCount);
        
        float maxV = Math.max(quad.getMaxV(), quad.getMinV());
        int vOrdinal = ((Math.round(maxV) - 1) / sliceCount);
        
        // bring unit uv coordinates within the range of our texture size
        int shiftU = uOrdinal * sliceCount;
        if(shiftU > 0)
        {
            quad.setMaxU(quad.getMaxU() - shiftU);
            quad.setMinU(quad.getMinU() - shiftU);
        }
        
        int shiftV= vOrdinal * sliceCount;
        if(shiftV > 0)
        {
            quad.setMaxV(quad.getMaxV() - shiftV);
            quad.setMinV(quad.getMinV() - shiftV);
        }
        
        // uv coordinates should now be in range 0 to sliceCount
        // so just need to scale so that max values are 16.0
        
        float uvScale = 16f / this.texture.textureScale().sliceCount;
        quad.scaleQuadUV(uvScale, uvScale);
       
       int hash = MathHelper.hash(uOrdinal | (vOrdinal << 8));
        
        int textureVersion = this.texture.textureVersionMask() & (hash >> 4);
        quad.setTextureName(this.texture.getTextureName(textureVersion));
                
        //FIXME - doubt that the rest of this still works at all after
        //lockUV and texture rotation were refactored.

        int rotationOrdinal = this.texture.rotation().rotation.ordinal();
        if(this.texture.rotation().rotationType() == TextureRotationType.RANDOM)
        {
            rotationOrdinal = (rotationOrdinal + hash) & 3;
        }
        
        quad.setRotation(Rotation.values()[rotationOrdinal]);
        
        if(rotationOrdinal > 0)
        {
            for(int i = 0; i < rotationOrdinal; i++)
            {
                float oldMinU = quad.getMinU();
                float oldMaxU = quad.getMaxU();
                quad.setMinU(quad.getMinV());
                quad.setMaxU(quad.getMaxV());
                quad.setMinV(16 - oldMaxU);
                quad.setMaxV(16 - oldMinU);

            }
        }
        this.postPaintProcessQuadAndAddToList(quad, outputList, isItem);
    }
}
