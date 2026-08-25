package carpet.mixins;

import carpet.fakes.ServerPlayerInterface;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayer_createShadowMixin implements ServerPlayerInterface.ShadowPlayerInterface
{
    @Unique
    private boolean shouldShadow = false;

    @Override
    public void carpet$shadowAfterDisconnect()
    {
        this.shouldShadow = true;
    }

    @Override
    public boolean carpet$shouldShadow()
    {
        return this.shouldShadow;
    }
}
