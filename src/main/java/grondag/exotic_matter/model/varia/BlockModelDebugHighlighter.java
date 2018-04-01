package grondag.exotic_matter.model.varia;

import java.util.List;

import javax.annotation.Nonnull;

import org.lwjgl.opengl.GL11;

import grondag.exotic_matter.block.SuperDispatcher;
import grondag.exotic_matter.block.SuperDispatcher.DispatchDelegate;
import grondag.exotic_matter.model.ISuperBlock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;


public class BlockModelDebugHighlighter 
{
	public static void handleDrawBlockHighlightEvent(DrawBlockHighlightEvent event) 
	{
        BlockPos pos = event.getTarget().getBlockPos();
        EntityPlayer player = event.getPlayer();
        if(player != null)
        {
            World world = player.world;
    		IBlockState bs = world.getBlockState(pos);
    		if (bs.getBlock() instanceof ISuperBlock) 
    		{
    		    ISuperBlock block = (ISuperBlock) bs.getBlock();
    		    
    		    DispatchDelegate delegate = SuperDispatcher.INSTANCE.getDelegate(block);
    		    
    		    bs = bs.getBlock().getExtendedState(bs, world, pos);
    		    
    	        Tessellator tessellator = Tessellator.getInstance();
    	        BufferBuilder bufferBuilder = tessellator.getBuffer();
    	        float partialTicks = event.getPartialTicks();
    	        
    	        
    	        double d0 = pos.getX() - (player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks);
    	        double d1 = pos.getY() - (player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks);
    	        double d2 = pos.getZ() - (player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks);
    	        
    	        
    		    renderModel(d0, d1, d2, world, delegate, bs, pos, tessellator, bufferBuilder);
    		    
  
    		}
		}
	}
	
    private static void renderModel(
            double d0,
            double d1,
            double d2,
            @Nonnull IBlockAccess world,
            @Nonnull IBakedModel model, 
            @Nonnull IBlockState state, 
            @Nonnull BlockPos pos, 
            @Nonnull Tessellator tessellator, 
            @Nonnull BufferBuilder buffer)
    {

        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.glLineWidth(1.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        GlStateManager.enablePolygonOffset();
        GlStateManager.doPolygonOffset(-1, -1);
        
        List<BakedQuad> quads = model.getQuads(state, null, 0);
        if(!quads.isEmpty()) quads.forEach(q -> drawQuad(d0, d1, d2, q, tessellator, buffer));
        
        for(EnumFacing side : EnumFacing.values())
        {
            if(state.shouldSideBeRendered(world, pos, side))
            {
                quads = model.getQuads(state, side, 0);
                if(!quads.isEmpty()) quads.forEach(q -> drawQuad(d0, d1, d2, q, tessellator, buffer));
            }
        }
        
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.disablePolygonOffset();
    }
    
    
    private static void drawQuad(
            double d0,
            double d1,
            double d2,
            BakedQuad quad, 
            @Nonnull Tessellator tessellator, 
            @Nonnull BufferBuilder buffer)
    {
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        
        int[] vertexData = quad.getVertexData();
        
        double[] normalData = new double[24];
        
        int dataOffset = 0;
        int normalDataOffset = 0;
        
        final int normalFormatOffset = quad.getFormat().getNormalOffset() / 4;
        
        for(int i = 0; i < 4; i++)
        {
            double  x = d0 + Float.intBitsToFloat(vertexData[dataOffset]);
            double  y = d1 + Float.intBitsToFloat(vertexData[dataOffset + 1]);
            double  z = d2 + Float.intBitsToFloat(vertexData[dataOffset + 2]);
            buffer.pos(x, y, z).color(1f, 1f, 1f, 1f).endVertex();
            
            if(quad.getFormat().hasNormal())
            {
                normalData[normalDataOffset++] = x;
                normalData[normalDataOffset++] = y;
                normalData[normalDataOffset++] = z;
                
                int normal = vertexData[dataOffset + normalFormatOffset];
                
                normalData[normalDataOffset++] = ((float)(byte)normal) / 127f;
                normalData[normalDataOffset++] = ((float)(byte)(normal >>> 8)) / 127f;
                normalData[normalDataOffset++] = ((float)(byte)(normal >>> 16)) / 127f;
            }
 
            dataOffset += quad.getFormat().getIntegerSize();
        }
        tessellator.draw();
        
        if(quad.getFormat().hasNormal())
        {
            int normalDataIndex = 0;
            for(int i = 0; i < 4; i++)
            {
                buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
                
                double  x = normalData[normalDataIndex++];
                double  y = normalData[normalDataIndex++];
                double  z = normalData[normalDataIndex++];
                buffer.pos(x, y, z).color(0.7f, 1f, 1f, 1f).endVertex();
                x += normalData[normalDataIndex++];
                y += normalData[normalDataIndex++];
                z += normalData[normalDataIndex++];
                buffer.pos(x, y, z).color(0.7f, 1f, 1f, 1f).endVertex();
                tessellator.draw();
            }
        }
    }
}
