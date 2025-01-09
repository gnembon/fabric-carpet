package carpet.fakes;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.level.Ticket;

import java.util.List;

public interface TicketsFetcherInterface
{
    Long2ObjectOpenHashMap<List<Ticket>> getTicketsByPosition();
}
