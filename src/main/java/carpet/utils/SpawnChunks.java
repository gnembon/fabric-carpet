package carpet.utils;

import carpet.fakes.ChunkTicketManagerInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public class SpawnChunks
{
    public static void changeSpawnSize(ServerLevel overworld, int size)
    {
        assert overworld != null : "Overworld must not be null";
        assert overworld == overworld.getServer().overworld() : "Given world isn't overworld";
        ChunkPos centerChunk = new ChunkPos(new BlockPos(
                overworld.getLevelData().getXSpawn(),
                overworld.getLevelData().getYSpawn(),
                overworld.getLevelData().getZSpawn()
        ));
        DistanceManager ticketManager = overworld.getChunkSource().chunkMap.getDistanceManager();
        ((ChunkTicketManagerInterface)ticketManager).changeSpawnChunks(centerChunk, size);
    }
}
