package grondag.exotic_matter.simulator;

import javax.annotation.Nullable;

import grondag.exotic_matter.ExoticMatter;
import grondag.exotic_matter.varia.PackedChunkPos;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;

/**
 * Transient chunk loader - forces chunks to remain loaded but does not persist
 */
public class ChunkLoader
{
    private static @Nullable Ticket currentTicket;
    
    private static int chunksUsedThisTicket = 0;
    
    private static final Long2ObjectOpenHashMap<Ticket> retainedChunks = new Long2ObjectOpenHashMap<>();

    private static final Long2IntOpenHashMap retainedCount = new Long2IntOpenHashMap();

    
    private ChunkLoader() {};
    
    public static void clear()
    {
        currentTicket = null;
        chunksUsedThisTicket = 0;
        retainedChunks.clear();
        retainedCount.clear();
    }
    
    public static void retainChunk(World world, int x, int z)
    {
        retainChunk(world, PackedChunkPos.getPackedChunkPosFromChunkXZ(x, z));
    }
    
    public static void retainChunk(World world, long packedChunkPos)
    {
        if(currentTicket == null || (chunksUsedThisTicket >= currentTicket.getChunkListDepth()))
        {
            // Note use of library mod instance instead of volcano mod instance
            // the simulator is the reload listener and is registered under the library mod
            // It will simply throw away all tickets on reload.
            currentTicket = ForgeChunkManager.requestTicket(ExoticMatter.INSTANCE, world, ForgeChunkManager.Type.NORMAL);
            chunksUsedThisTicket = 0;
        }
        
        boolean isNew;
        synchronized(retainedChunks)
        {
            isNew = (retainedCount.addTo(packedChunkPos, 1) == 0);
                    
            if(isNew) retainedChunks.put(packedChunkPos, currentTicket);
        }
        
        if(isNew)
        {
            ForgeChunkManager.forceChunk(currentTicket, PackedChunkPos.unpackChunkPos(packedChunkPos));
            
            //FIXME: remove
            ChunkPos pos = PackedChunkPos.unpackChunkPos(packedChunkPos);
            if(pos.getXStart() > 32000 || pos.getXStart() < -32000)
                ExoticMatter.INSTANCE.info("boop!");
            ExoticMatter.INSTANCE.info("Force loaded chunk @ %d, %d", pos.getXStart(), pos.getZStart());
            
            chunksUsedThisTicket++;
        }
    }

    public static void releaseChunk(World world, int x, int z)
    {
        releaseChunk(world, PackedChunkPos.getPackedChunkPosFromChunkXZ(x, z));
    }
    
    public static void releaseChunk(World world, long packedChunkPos)
    {
        
        Ticket t = null;
        
        synchronized(retainedChunks)
        {
            if(retainedCount.addTo(packedChunkPos, -1) == 1)
            {
                t = retainedChunks.remove(packedChunkPos);
            }
        }
        
        if(t != null)
        {
            ForgeChunkManager.unforceChunk(t, PackedChunkPos.unpackChunkPos(packedChunkPos));
            
            //FIXME: remove
            ChunkPos pos = PackedChunkPos.unpackChunkPos(packedChunkPos);
            ExoticMatter.INSTANCE.info("Released force loaded chunk @ %d, %d", pos.getXStart(), pos.getZStart());
        }
    }
}
