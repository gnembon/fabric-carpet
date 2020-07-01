package carpet.fakes;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.math.ChunkPos;

public interface ChunkTicketManagerInterface
{
    void changeSpawnChunks(ChunkPos pos, int distance);

    Long2ObjectOpenHashMap<SortedArraySet<ChunkTicket<?>>> getTicketsByPosition();

    void replaceHolder(ChunkHolder oldHolder, ChunkHolder newHolder);
}
