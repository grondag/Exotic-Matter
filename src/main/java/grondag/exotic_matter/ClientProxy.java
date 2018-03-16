package grondag.exotic_matter;


import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.model.animation.Animation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy
{
    /**
     * Nulled out at start of each render and then initialized if needed.
     * Allows reuse whereever needed
     */
    @Nullable
    private static ICamera camera;
    private static double cameraX;
    private static double cameraY;
    private static double cameraZ;

    private static void refreshCamera()
    {
        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
        if(entity == null) return;

        float partialTicks = Animation.getPartialTickTime();
        
        ICamera newCam = new Frustum();
        double d0 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double d1 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double d2 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        newCam.setPosition(d0, d1, d2);
        cameraX = d0;
        cameraY = d1;
        cameraZ = d2;
        camera = newCam;
    }

    public static void updateCamera()
    {
        camera = null;
    }

    @Nullable
    public static ICamera camera()
    {
        if(camera == null) refreshCamera();
        return camera;
    }

    public static double cameraX()
    {
        if(camera == null) refreshCamera();
        return cameraX;
    }
    
    public static double cameraY()
    {
        if(camera == null) refreshCamera();
        return cameraY;
    }
    
    public static double cameraZ()
    {
        if(camera == null) refreshCamera();
        return cameraZ;
    }
}
