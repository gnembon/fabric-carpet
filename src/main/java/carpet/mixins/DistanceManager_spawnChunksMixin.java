package carpet.mixins;

import carpet.fakes.ChunkTicketManagerInterface;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.Unit;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;

@Mixin(DistanceManager.class)
public abstract class DistanceManager_spawnChunksMixin implements ChunkTicketManagerInterface
{
    @Shadow @Final private Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> tickets;

    @Shadow protected abstract void removeTicket(long pos, Ticket<?> ticket);

    @Shadow public abstract <T> void addRegionTicket(TicketType<T> type, ChunkPos pos, int radius, T argument);

    @Override
    public void changeSpawnChunks(ChunkPos chunkPos,  int distance)
    {
        long pos = chunkPos.toLong();
        SortedArraySet<Ticket<?>> set = tickets.get(pos);
        Ticket<?> existingTicket = null;
        if (set != null)
        {
            Iterator<Ticket<?>> iter = set.iterator();
            while(iter.hasNext())
            {
                Ticket<?> ticket = iter.next();
                if (ticket.getType() == TicketType.START)
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
            addRegionTicket(TicketType.START, chunkPos, distance, Unit.INSTANCE);
    }
}
