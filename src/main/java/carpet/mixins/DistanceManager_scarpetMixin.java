package carpet.mixins;

import carpet.fakes.TicketsFetcherInterface;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.Ticket;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(DistanceManager.class)
public abstract class DistanceManager_scarpetMixin implements TicketsFetcherInterface
{
    @Shadow @Final private TicketStorage ticketStorage;

    @Override
    public Long2ObjectOpenHashMap<List<Ticket>>  getTicketsByPosition()
    {
        return ((TicketsFetcherInterface)ticketStorage).getTicketsByPosition();
    }

}
