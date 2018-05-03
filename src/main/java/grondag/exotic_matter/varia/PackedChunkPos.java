package grondag.exotic_matter.varia;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

public class PackedChunkPos
{
    private static final int CHUNK_BOUNDARY = PackedBlockPos.WORLD_BOUNDARY >> 4;

    public static long getPackedChunkPos(BlockPos pos)
    {
            return PackedChunkPos.getPackedChunkPosFromBlockXZ(pos.getX(), pos.getZ());
    }

    public static long getPackedChunkPos(long packedBlockPos)
    {
           return PackedChunkPos.getPackedChunkPosFromBlockXZ(PackedBlockPos.getX(packedBlockPos), PackedBlockPos.getZ(packedBlockPos));
    }

    public static long getPackedChunkPosFromBlockXZ(int blockX, int blockZ)
    {
            return ((long)blockX >> 4) + PackedChunkPos.CHUNK_BOUNDARY | (((long)blockZ >> 4) + PackedChunkPos.CHUNK_BOUNDARY) << 32;
    }

    public static long getPackedChunkPos(ChunkPos chunkPos)
    {
        return getPackedChunkPosFromChunkXZ(chunkPos.x, chunkPos.z);
    }

    public static long getPackedChunkPos(Chunk chunk)
    {
            return getPackedChunkPosFromChunkXZ(chunk.x, chunk.z);
    }

    public static long getPackedChunkPosFromChunkXZ(int chunkX, int chunkZ)
    {
            return (chunkX) + PackedChunkPos.CHUNK_BOUNDARY | ((chunkZ) + PackedChunkPos.CHUNK_BOUNDARY) << 32;
    }

    public static ChunkPos unpackChunkPos(long packedChunkPos)
    {
        return new ChunkPos(PackedChunkPos.getChunkXPos(packedChunkPos), PackedChunkPos.getChunkZPos(packedChunkPos));
    }

    /** analog of Chunk.chunkXPos */
    public static int getChunkXPos(long packedChunkPos)
    {
        return (int)((packedChunkPos & 0xFFFFFFFF) - PackedChunkPos.CHUNK_BOUNDARY);
    }

    /** analog of Chunk.chunkZPos */
    public static int getChunkZPos(long packedChunkPos)
    {
        return (int)(((packedChunkPos >> 32) & 0xFFFFFFFF) - PackedChunkPos.CHUNK_BOUNDARY);
    }

    /** analog of Chunk.getXStart() */
    public static int getChunkXStart(long packedChunkPos)
    {
        return getChunkXPos(packedChunkPos) << 4;
    }

    /** analog of Chunk.getZStart() */
    public static int getChunkZStart(long packedChunkPos)
    {
        return getChunkZPos(packedChunkPos) << 4;
    }
}
