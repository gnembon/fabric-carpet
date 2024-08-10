package carpet.fakes;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.Ticket;
import net.minecraft.util.SortedArraySet;

public interface ChunkTicketManagerInterface
{

    Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> getTicketsByPosition();

    void replaceHolder(ChunkHolder oldHolder, ChunkHolder newHolder);
}
