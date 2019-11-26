package carpet.mixins;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSortedSet;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.server.world.ChunkTicketManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkTicketManager.class)
public interface ChunkTicketManager_scarpetMixin
{
    @Accessor
    Long2ObjectOpenHashMap<ObjectSortedSet<ChunkTicket<?>>>  getTicketsByPosition();
}
