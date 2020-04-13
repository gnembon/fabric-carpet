package carpet.mixins;

import carpet.fakes.ChunkTicketManagerInterface;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.util.SortedArraySet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkTicketManager.class)
public abstract class ChunkTicketManager_scarpetMixin implements ChunkTicketManagerInterface
{
    @Shadow @Final private Long2ObjectOpenHashMap<SortedArraySet<ChunkTicket<?>>> ticketsByPosition;

    @Override
    public Long2ObjectOpenHashMap<SortedArraySet<ChunkTicket<?>>>  getTicketsByPosition()
    {
        return ticketsByPosition;
    }

}
