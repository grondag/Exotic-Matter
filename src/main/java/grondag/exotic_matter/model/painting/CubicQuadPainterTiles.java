package grondag.exotic_matter.model.painting;

import java.util.function.Consumer;

import grondag.exotic_matter.model.primitives.polygon.IPaintablePolygon;
import grondag.exotic_matter.model.state.ISuperModelState;
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
    public void textureQuad(IPaintablePolygon quad, Consumer<IPaintablePolygon> target, boolean isItem)
    {
        assert quad.isLockUV() : "Tiled cubic quad painter received quad without lockUV semantics.  Not expected";
        
        Rotation rotation = this.textureRotationForFace(quad.getNominalFace());
        int textureVersion = this.textureVersionForFace(quad.getNominalFace());
        
        if(quad.textureSalt() != 0)
        {
            int saltHash = MathHelper.hash(quad.textureSalt());
            rotation = Useful.offsetEnumValue(rotation, saltHash & 3);
            textureVersion = (textureVersion + (saltHash >> 2)) & this.texture.textureVersionMask();
        }
        
        quad.setRotation(rotation);
        quad.setTextureName(this.texture.getTextureName(textureVersion));
        
        this.postPaintProcessQuadAndOutput(quad, target, isItem);
    }
}
