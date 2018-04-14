package grondag.exotic_matter.model;

import grondag.exotic_matter.render.IPolyProperties;
import grondag.exotic_matter.render.Poly;
import grondag.exotic_matter.render.Vertex;
import grondag.exotic_matter.render.Surface.SurfaceInstance;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.EnumFacing;

public class CubeInputs{
    public float u0;
    public float v0;
    public float u1;
    public float v1;
    public String textureName;
    public int color = 0xFFFFFFFF;
    public Rotation textureRotation = Rotation.ROTATE_NONE;
    public boolean rotateBottom = false;
    public boolean isOverlay = false;
    public boolean isItem = false;
    public boolean isFullBrightness = false;
    public SurfaceInstance surfaceInstance;
    
    public CubeInputs()
    {
        //Minimum needed to prevent NPE
        this.textureRotation = Rotation.ROTATE_NONE;
        this.surfaceInstance = IPolyProperties.NO_SURFACE;
    }
    public CubeInputs(int color, Rotation textureRotation, String textureName, boolean flipU, boolean flipV, boolean isOverlay, boolean isItem)
    {
        this.color = color;
        this.textureRotation = textureRotation;
        this.textureName = textureName;
        this.isOverlay = isOverlay;
        this.isItem = isItem;
        this.u0 = flipU ? 16 : 0;
        this.v0 = flipV ? 16 : 0;
        this.u1 = flipU ? 0 : 16;
        this.v1 = flipV ? 0 : 16;
        this.rotateBottom = true;
        this.surfaceInstance = IPolyProperties.NO_SURFACE;
    }

    public Poly makeRawFace(EnumFacing side){

        Poly qi = new Poly();
        qi.setColor(this.color);
        
        qi.setLockUV(true);
        qi.setFullBrightness(this.isFullBrightness);
        qi.setRotation((rotateBottom && side == EnumFacing.DOWN) ? this.textureRotation.clockwise().clockwise() : this.textureRotation);
        qi.setTextureName(this.textureName);
        qi.setSurfaceInstance(this.surfaceInstance);

        float minBound = this.isOverlay ? -0.0002f : 0.0f;
        float maxBound = this.isOverlay ? 1.0002f : 1.0f;
        qi.setNominalFace(side);

        switch(side)
        {
        case UP:
            qi.addVertex(0, new Vertex(minBound, maxBound, minBound, u0, v0, this.color));
            qi.addVertex(1, new Vertex(minBound, maxBound, maxBound, u0, v1, this.color));
            qi.addVertex(2, new Vertex(maxBound, maxBound, maxBound, u1, v1, this.color));
            qi.addVertex(3, new Vertex(maxBound, maxBound, minBound, u1, v0, this.color));
            break;

        case DOWN:     
            qi.addVertex(0, new Vertex(maxBound, minBound, maxBound, u0, v1, this.color));
            qi.addVertex(1, new Vertex(minBound, minBound, maxBound, u1, v1, this.color)); 
            qi.addVertex(2, new Vertex(minBound, minBound, minBound, u1, v0, this.color)); 
            qi.addVertex(3, new Vertex(maxBound, minBound, minBound, u0, v0, this.color));
            break;

        case WEST:
            qi.addVertex(0, new Vertex(minBound, minBound, minBound, u0, v1, this.color));
            qi.addVertex(1, new Vertex(minBound, minBound, maxBound, u1, v1, this.color));
            qi.addVertex(2, new Vertex(minBound, maxBound, maxBound, u1, v0, this.color));
            qi.addVertex(3, new Vertex(minBound, maxBound, minBound, u0, v0, this.color));
            break;

        case EAST:
            qi.addVertex(0, new Vertex(maxBound, minBound, minBound, u1, v1, this.color));
            qi.addVertex(1, new Vertex(maxBound, maxBound, minBound, u1, v0, this.color));
            qi.addVertex(2, new Vertex(maxBound, maxBound, maxBound, u0, v0, this.color));
            qi.addVertex(3, new Vertex(maxBound, minBound, maxBound, u0, v1, this.color));
            break;

        case NORTH:
            qi.addVertex(0, new Vertex(minBound, minBound, minBound, u1, v1, this.color));
            qi.addVertex(1, new Vertex(minBound, maxBound, minBound, u1, v0, this.color));
            qi.addVertex(2, new Vertex(maxBound, maxBound, minBound, u0, v0, this.color));
            qi.addVertex(3, new Vertex(maxBound, minBound, minBound, u0, v1, this.color));
            break;

        case SOUTH:
            qi.addVertex(0, new Vertex(minBound, minBound, maxBound, u0, v1, this.color));
            qi.addVertex(1, new Vertex(maxBound, minBound, maxBound, u1, v1, this.color));
            qi.addVertex(2, new Vertex(maxBound, maxBound, maxBound, u1, v0, this.color));
            qi.addVertex(3, new Vertex(minBound, maxBound, maxBound, u0, v0, this.color));
            break;
        }
        
        return qi;
    }
}