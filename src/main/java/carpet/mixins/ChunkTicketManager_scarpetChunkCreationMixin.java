package carpet.mixins;

import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import carpet.fakes.ChunkTicketManagerInterface;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTicketManager;

@Mixin(ChunkTicketManager.class)
public abstract class ChunkTicketManager_scarpetChunkCreationMixin implements ChunkTicketManagerInterface
{
    @Shadow
    @Final
    private Set<ChunkHolder> chunkHolders;

    @Override
    public void replaceHolder(final ChunkHolder oldHolder, final ChunkHolder newHolder)
    {
        this.chunkHolders.remove(oldHolder);
        this.chunkHolders.add(newHolder);
    }
}
