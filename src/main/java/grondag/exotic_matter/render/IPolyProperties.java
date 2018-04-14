package grondag.exotic_matter.render;

import grondag.exotic_matter.render.Surface.SurfaceInstance;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;

public interface IPolyProperties
{
    SurfaceInstance NO_SURFACE = new Surface(SurfaceType.MAIN, SurfaceTopology.CUBIC).unitInstance;

    /** special edge ID signifying is an original edge, not a split */
    int IS_AN_ANCESTOR = -1;
    
    /** special edge ID signifying no ID has been set */
    int NO_ID = 0;

    public String getTextureName();

    public EnumFacing getNominalFace();

    public Rotation getRotation();

    public int getColor();

    public boolean isFullBrightness();

    public boolean isLockUV();

    public int getAncestorQuadID();

    public boolean isInverted();

    public Vec3d getFaceNormal();

    public boolean shouldContractUVs();

    public float getMinU();

    public float getMaxU();

    public float getMinV();

    public float getMaxV();

    public RenderPass getRenderPass();

    public SurfaceInstance getSurfaceInstance();

    public int vertexCount();
    
    public int quadID();
    
    public int getLineID(int index);
    
    public Vertex getVertex(int index);
}
