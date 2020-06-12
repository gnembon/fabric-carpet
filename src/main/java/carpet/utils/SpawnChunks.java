package carpet.utils;

import carpet.fakes.ChunkTicketManagerInterface;
import carpet.fakes.ServerChunkManagerInterface;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.util.math.ChunkPos;

public class SpawnChunks
{
    public static void changeSpawnChunks(ServerChunkManager chunkManager, ChunkPos pos, int size)
    {
        ChunkTicketManager ticketManager = ((ServerChunkManagerInterface)chunkManager).getCMTicketManager();
        ((ChunkTicketManagerInterface)ticketManager).changeSpawnChunks(pos, size);
    }
}
