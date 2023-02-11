package carpet.utils;

import carpet.fakes.ChunkTicketManagerInterface;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.ChunkPos;

public class SpawnChunks
{
    public static void changeSpawnChunks(ServerChunkCache chunkManager, ChunkPos pos, int size)
    {
        DistanceManager ticketManager = chunkManager.chunkMap.getDistanceManager();
        ((ChunkTicketManagerInterface)ticketManager).changeSpawnChunks(pos, size);
    }
}
