package grondag.exotic_matter.model.painter;

import grondag.exotic_matter.model.ISuperModelState;
import grondag.exotic_matter.model.PaintLayer;
import grondag.exotic_matter.render.IMutablePolygon;
import grondag.exotic_matter.render.Surface;
import grondag.exotic_matter.varia.Useful;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.math.MathHelper;

public class CubicQuadPainterTiles extends CubicQuadPainter
{
    public CubicQuadPainterTiles(ISuperModelState modelState, Surface surface, PaintLayer paintLayer)
    {
        super(modelState, surface, paintLayer);
    }

    @Override
    public IMutablePolygon paintQuad(IMutablePolygon quad)
    {
        assert quad.isLockUV() : "Tiled cubic quad painter received quad without lockUV semantics.  Not expected";
        
        Rotation rotation = this.textureRotationForFace(quad.getNominalFace());
        int textureVersion = this.textureVersionForFace(quad.getNominalFace());
        
        if(quad.getSurfaceInstance().textureSalt != 0)
        {
            int saltHash = MathHelper.hash(quad.getSurfaceInstance().textureSalt);
            rotation = Useful.offsetEnumValue(rotation, saltHash & 3);
            textureVersion = (textureVersion + (saltHash >> 2)) & this.texture.textureVersionMask();
        }
        
        quad.setRotation(rotation);
        quad.setTextureName(this.texture.getTextureName(textureVersion));
        return quad;
    }
}
