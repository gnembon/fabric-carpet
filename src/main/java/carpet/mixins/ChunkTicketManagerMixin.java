package carpet.mixins;

import it.unimi.dsi.fastutil.objects.ObjectSortedSet;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.server.world.ChunkTicketManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkTicketManager.class)
public interface ChunkTicketManagerMixin
{
    @Invoker("getTicketSet")
    ObjectSortedSet<ChunkTicket<?>> invokeGetTicketSet(long pos);

    @Invoker("getChunkHolder")
    ChunkHolder invokeGetChunkHolder(long pos);
}
