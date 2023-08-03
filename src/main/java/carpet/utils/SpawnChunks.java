package carpet.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public class SpawnChunks
{
    public static void changeSpawnSize(ServerLevel overworld, int size)
    {
        ChunkPos centerChunk = new ChunkPos(new BlockPos(
                overworld.getLevelData().getXSpawn(),
                overworld.getLevelData().getYSpawn(),
                overworld.getLevelData().getZSpawn()
        ));
        ServerChunkCache chunkCache = overworld.getChunkSource();
        DistanceManager distanceManager = chunkCache.chunkMap.getDistanceManager();
        distanceManager.carpet$changeSpawnChunks(centerChunk, size);
    }
}
