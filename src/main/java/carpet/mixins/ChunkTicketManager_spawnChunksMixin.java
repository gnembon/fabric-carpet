package carpet.mixins;

import carpet.fakes.ChunkTicketManagerInterface;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.Unit;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;

@Mixin(ChunkTicketManager.class)
public abstract class ChunkTicketManager_spawnChunksMixin implements ChunkTicketManagerInterface
{

    @Shadow protected abstract SortedArraySet<ChunkTicket<?>> getTicketSet(long position);

    @Shadow @Final private Long2ObjectOpenHashMap<SortedArraySet<ChunkTicket<?>>> ticketsByPosition;

    @Shadow public abstract <T> void removeTicket(ChunkTicketType<T> type, ChunkPos pos, int radius, T argument);

    @Shadow protected abstract void removeTicket(long pos, ChunkTicket<?> ticket);

    @Shadow public abstract <T> void addTicket(ChunkTicketType<T> type, ChunkPos pos, int radius, T argument);

    @Override
    public void changeSpawnChunks(ChunkPos chunkPos,  int distance)
    {
        long pos = chunkPos.toLong();
        SortedArraySet<ChunkTicket<?>> set = ticketsByPosition.get(pos);
        ChunkTicket existingTicket = null;
        if (set != null)
        {
            Iterator<ChunkTicket<?>> iter = set.iterator();
            while(iter.hasNext())
            {
                ChunkTicket ticket = iter.next();
                if (ticket.getType() == ChunkTicketType.START)
                {
                    existingTicket = ticket;
                    iter.remove();
                }
            }
            set.add(existingTicket);
        }
        // the reason we are removing the ticket this way is that there are sideeffects of removal
        if (existingTicket != null)
        {
            removeTicket(pos, existingTicket);
        }
        // set optionally new spawn ticket
        if (distance > 0)
            addTicket(ChunkTicketType.START, chunkPos, distance, Unit.INSTANCE);
    }
}
