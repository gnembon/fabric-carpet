package carpet.mixins;

import carpet.fakes.TicketsFetcherInterface;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.level.Ticket;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(TicketStorage.class)
public class TicketStorage_scarpetMixin implements TicketsFetcherInterface
{
    @Shadow @Final private Long2ObjectOpenHashMap<List<Ticket>> tickets;

    @Override
    public Long2ObjectOpenHashMap<List<Ticket>> getTicketsByPosition()
    {
        return tickets;
    }
}
