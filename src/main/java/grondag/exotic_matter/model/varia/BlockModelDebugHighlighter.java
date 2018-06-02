package grondag.exotic_matter.model.varia;

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
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.common.property.IExtendedBlockState;


public class BlockModelDebugHighlighter 
{
	public static void handleDrawBlockHighlightEvent(DrawBlockHighlightEvent event) 
	{
        BlockPos pos = event.getTarget().getBlockPos();
        EntityPlayer player = event.getPlayer();
        if(player != null && pos != null)
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
            @Nonnull DispatchDelegate model, 
            @Nonnull IBlockState state, 
            @Nonnull BlockPos pos, 
            @Nonnull Tessellator tessellator, 
            @Nonnull BufferBuilder buffer)
    {

//        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.glLineWidth(2.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        GlStateManager.doPolygonOffset(-1f, -1f);
        GlStateManager.enablePolygonOffset();
        
        model.forAllQuads((IExtendedBlockState) state, q -> drawQuad(d0, d1, d2, q, tessellator, buffer));
        
//        GlStateManager.enableDepth();
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
        
    
        int[] vertexData = quad.getVertexData();
        
        double[] normalData = new double[24];
        
        int dataOffset = 0;
        int normalDataOffset = 0;
        
        final int normalFormatOffset = quad.getFormat().getNormalOffset() / 4;
        
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
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
                
                normalData[normalDataOffset++] = ((float)(byte)(normal & 0xFF)) / 127f;
                normalData[normalDataOffset++] = ((float)(byte)((normal >>> 8) & 0xFF)) / 127f;
                normalData[normalDataOffset++] = ((float)(byte)((normal >>> 16) & 0xFF)) / 127f;
            }
 
            dataOffset += quad.getFormat().getIntegerSize();
        }
        tessellator.draw();
        
        if(quad.getFormat().hasNormal())
        {
            int normalDataIndex = 0;
            for(int i = 0; i < 4; i++)
            {
                
                final double  x = normalData[normalDataIndex++];
                final double  y = normalData[normalDataIndex++];
                final double  z = normalData[normalDataIndex++];
                final double  dx = normalData[normalDataIndex++];
                final double  dy = normalData[normalDataIndex++];
                final double  dz = normalData[normalDataIndex++];
                
                // only draw vertex normals that differ from the standard side normals
                int zeroCount = 0;
                if(Math.abs(dx) < 0.0000001) zeroCount++; 
                if(Math.abs(dy) < 0.0000001) zeroCount++;
                if(Math.abs(dz) < 0.0000001) zeroCount++; 
                if(zeroCount == 2) continue;
                
                buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
                buffer.pos(x, y, z).color(0.7f, 1f, 1f, 1f).endVertex();
                buffer.pos(x + dx, y + dy, z + dz).color(0.7f, 1f, 1f, 1f).endVertex();
                tessellator.draw();
            }
        }
    }
}
