package grondag.exotic_matter.simulator;

import javax.annotation.Nullable;

import grondag.exotic_matter.ExoticMatter;
import grondag.exotic_matter.world.PackedChunkPos;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
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
        WorldTaskManager.runOrEnqueueImmediate(() -> 
        {
            currentTicket = null;
            chunksUsedThisTicket = 0;
            retainedChunks.clear();
            retainedCount.clear();
        }); 
    }
    
    public static void retainChunk(World world, int x, int z)
    {
        retainChunk(world, PackedChunkPos.getPackedChunkPosFromChunkXZ(x, z));
    }
    
    public static void retainChunk(World world, long packedChunkPos)
    {
        WorldTaskManager.runOrEnqueueImmediate(() -> 
        {
            Ticket t = currentTicket;
            
            if(t == null || (chunksUsedThisTicket >= t.getChunkListDepth()))
            {
                // Note use of library mod instance instead of volcano mod instance
                // the simulator is the reload listener and is registered under the library mod
                // It will simply throw away all tickets on reload.
                t = ForgeChunkManager.requestTicket(ExoticMatter.INSTANCE, world, ForgeChunkManager.Type.NORMAL);
                currentTicket = t;
                chunksUsedThisTicket = 0;
            }
            
            if(retainedCount.addTo(packedChunkPos, 1) == 0)
            {
                retainedChunks.put(packedChunkPos, t);
                ForgeChunkManager.forceChunk(t, PackedChunkPos.unpackChunkPos(packedChunkPos));
                chunksUsedThisTicket++;
            }
        });
        
    }

    public static void releaseChunk(World world, int x, int z)
    {
        releaseChunk(world, PackedChunkPos.getPackedChunkPosFromChunkXZ(x, z));
    }
    
    public static void releaseChunk(World world, long packedChunkPos)
    {
        WorldTaskManager.runOrEnqueueImmediate(() -> 
        {
            if(retainedCount.addTo(packedChunkPos, -1) == 1)
            {
                Ticket t = retainedChunks.remove(packedChunkPos);
                
                if(t != null)
                    ForgeChunkManager.unforceChunk(t, PackedChunkPos.unpackChunkPos(packedChunkPos));
            }
        });
    }
}
