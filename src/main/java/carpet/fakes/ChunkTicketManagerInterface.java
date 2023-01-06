package carpet.fakes;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.Ticket;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.level.ChunkPos;

public interface ChunkTicketManagerInterface
{
    void changeSpawnChunks(ChunkPos pos, int distance);

    Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> getTicketsByPosition();

    void replaceHolder(ChunkHolder oldHolder, ChunkHolder newHolder);
}
