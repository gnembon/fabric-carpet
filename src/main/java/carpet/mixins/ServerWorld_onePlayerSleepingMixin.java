package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Mixin(ServerWorld.class)
public class ServerWorld_onePlayerSleepingMixin
{
    @Shadow @Final private List<ServerPlayerEntity> players;

    @Shadow private boolean allPlayersSleeping;

    @Inject(method = "updatePlayersSleeping", cancellable = true, at = @At("HEAD"))
    private void updateOnePlayerSleeping(CallbackInfo ci)
    {
        if(CarpetSettings.onePlayerSleeping)
        {
            allPlayersSleeping = false;
            for (ServerPlayerEntity p : players)
                if (!p.isSpectator() && p.isSleeping())
                {
                    allPlayersSleeping = true;
                    ci.cancel();
                    return;
                }
            ci.cancel();
        }
    }

    @Redirect(method = "tick", at = @At(
            value = "INVOKE",
            target = "Ljava/util/stream/Stream;noneMatch(Ljava/util/function/Predicate;)Z"
    ))
    private boolean noneMatchSleep(Stream<ServerPlayerEntity> stream, Predicate<ServerPlayerEntity> predicate)
    {
        if (CarpetSettings.onePlayerSleeping)
            return stream.anyMatch((p) -> !p.isSpectator() && p.isSleepingLongEnough());
        return stream.noneMatch(predicate);
    }

}
