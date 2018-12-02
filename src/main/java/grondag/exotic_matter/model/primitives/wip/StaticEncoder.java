package grondag.exotic_matter.model.primitives.wip;

import grondag.acuity.api.IRenderPipeline;
import grondag.exotic_matter.model.painting.Surface;
import grondag.exotic_matter.world.Rotation;
import net.minecraft.util.BlockRenderLayer;

public class StaticEncoder
{

    public static Surface getSurface(IIntStream stream, int baseAddress)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static void setSurface(IIntStream stream, int baseAddress, Surface surface)
    {
        // TODO Auto-generated method stub
    }
    
    public static boolean shouldContractUVs(IIntStream stream, int baseAddress, int layerIndex)
    {
        // TODO Auto-generated method stub
        return false;
    }

    public static void setContractUVs(IIntStream stream, int baseAddress, int layerIndex, boolean shouldContract)
    {
     // TODO Auto-generated method stub
    }
    
    public static Rotation getRotation(IIntStream stream, int baseAddress, int layerIndex)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static void setRotation(IIntStream stream, int baseAddress, int layerIndex, Rotation rotation)
    {
        // TODO Auto-generated method stub
    }
    
    public static int getTextureSalt(IIntStream stream, int baseAddress)
    {
        // TODO Auto-generated method stub
        return 0;
    }
    
    public static void setTextureSalt(IIntStream stream, int baseAddress, int salt)
    {
        // TODO Auto-generated method stub
    }

    public static boolean isLockUV(IIntStream stream, int baseAddress, int layerIndex)
    {
        // TODO Auto-generated method stub
        return false;
    }
    
    public static void setLockUV(IIntStream stream, int baseAddress, int layerIndex, boolean lockUV)
    {
        // TODO Auto-generated method stub
    }

    public static boolean hasRenderLayer(IIntStream stream, int baseAddress, BlockRenderLayer layer)
    {
        // TODO Auto-generated method stub
        return false;
    }

    public static BlockRenderLayer getRenderLayer(IIntStream stream, int baseAddress, int layerIndex)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static void setRenderLayer(IIntStream stream, int baseAddress, int layerIndex, BlockRenderLayer layer)
    {
        // TODO Auto-generated method stub
    }
    
    public static boolean isEmissive(IIntStream stream, int baseAddress, int layerIndex)
    {
        // TODO Auto-generated method stub
        return false;
    }
    
    public static void setEmissive(IIntStream stream, int baseAddress, int layerIndex, boolean isEmissive)
    {
        // TODO Auto-generated method stub
    }

    public static IRenderPipeline getPipeline(IIntStream stream, int baseAddress)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static void setPipeline(IIntStream stream, int baseAddress, IRenderPipeline pipeline)
    {
        // TODO Auto-generated method stub
    }
}
