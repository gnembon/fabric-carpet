package carpet.fakes;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.Ticket;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.level.ChunkPos;

public interface DistanceManagerInterface
{
    default void carpet$changeSpawnChunks(ChunkPos pos, int distance) { throw new UnsupportedOperationException(); }

    default Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> carpet$getTicketsByPosition() { throw new UnsupportedOperationException(); }

    default void carpet$replaceHolder(ChunkHolder oldHolder, ChunkHolder newHolder) { throw new UnsupportedOperationException(); }
}
