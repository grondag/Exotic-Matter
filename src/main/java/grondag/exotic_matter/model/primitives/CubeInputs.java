package grondag.exotic_matter.model.primitives;

import javax.annotation.Nullable;

import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.EnumFacing;

public class CubeInputs{
    public float u0;
    public float v0;
    public float u1;
    public float v1;
    public @Nullable String textureName;
    public int color = 0xFFFFFFFF;
    public Rotation textureRotation = Rotation.ROTATE_NONE;
    public boolean rotateBottom = false;
    public boolean isOverlay = false;
    public boolean isItem = false;
    public boolean isFullBrightness = false;
    public Surface surfaceInstance;
    
    public CubeInputs()
    {
        //Minimum needed to prevent NPE
        this.textureRotation = Rotation.ROTATE_NONE;
        this.surfaceInstance = IPolygon.NO_SURFACE;
    }
    
    public CubeInputs(int color, Rotation textureRotation, String textureName, boolean flipU, boolean flipV, boolean isOverlay, boolean isItem)
    {
        this.color = color;
        this.textureRotation = textureRotation;
        this.textureName = textureName;
        this.isOverlay = isOverlay;
        this.isItem = isItem;
        this.u0 = flipU ? 1 : 0;
        this.v0 = flipV ? 1 : 0;
        this.u1 = flipU ? 0 : 1;
        this.v1 = flipV ? 0 : 1;
        this.rotateBottom = true;
        this.surfaceInstance = IPolygon.NO_SURFACE;
    }

    public IPolygon makeRawFace(EnumFacing side){

        IMutablePolygon qi = new PolyImpl(4);
        
        qi.setLockUV(true);
        qi.setRotation((rotateBottom && side == EnumFacing.DOWN) ? this.textureRotation.clockwise().clockwise() : this.textureRotation);
        qi.setTextureName(this.textureName);
        qi.setSurfaceInstance(this.surfaceInstance);

        float minBound = this.isOverlay ? -0.0002f : 0.0f;
        float maxBound = this.isOverlay ? 1.0002f : 1.0f;
        qi.setNominalFace(side);

        switch(side)
        {
        case UP:
            qi.addVertex(0, minBound, maxBound, minBound, u0, v0, this.color);
            qi.addVertex(1, minBound, maxBound, maxBound, u0, v1, this.color);
            qi.addVertex(2, maxBound, maxBound, maxBound, u1, v1, this.color);
            qi.addVertex(3, maxBound, maxBound, minBound, u1, v0, this.color);
            break;

        case DOWN:     
            qi.addVertex(0, maxBound, minBound, maxBound, u0, v1, this.color);
            qi.addVertex(1, minBound, minBound, maxBound, u1, v1, this.color); 
            qi.addVertex(2, minBound, minBound, minBound, u1, v0, this.color); 
            qi.addVertex(3, maxBound, minBound, minBound, u0, v0, this.color);
            break;

        case WEST:
            qi.addVertex(0, minBound, minBound, minBound, u0, v1, this.color);
            qi.addVertex(1, minBound, minBound, maxBound, u1, v1, this.color);
            qi.addVertex(2, minBound, maxBound, maxBound, u1, v0, this.color);
            qi.addVertex(3, minBound, maxBound, minBound, u0, v0, this.color);
            break;

        case EAST:
            qi.addVertex(0, maxBound, minBound, minBound, u1, v1, this.color);
            qi.addVertex(1, maxBound, maxBound, minBound, u1, v0, this.color);
            qi.addVertex(2, maxBound, maxBound, maxBound, u0, v0, this.color);
            qi.addVertex(3, maxBound, minBound, maxBound, u0, v1, this.color);
            break;

        case NORTH:
            qi.addVertex(0, minBound, minBound, minBound, u1, v1, this.color);
            qi.addVertex(1, minBound, maxBound, minBound, u1, v0, this.color);
            qi.addVertex(2, maxBound, maxBound, minBound, u0, v0, this.color);
            qi.addVertex(3, maxBound, minBound, minBound, u0, v1, this.color);
            break;

        case SOUTH:
            qi.addVertex(0, minBound, minBound, maxBound, u0, v1, this.color);
            qi.addVertex(1, maxBound, minBound, maxBound, u1, v1, this.color);
            qi.addVertex(2, maxBound, maxBound, maxBound, u1, v0, this.color);
            qi.addVertex(3, minBound, maxBound, maxBound, u0, v0, this.color);
            break;
        }
        
        return qi;
    }
}