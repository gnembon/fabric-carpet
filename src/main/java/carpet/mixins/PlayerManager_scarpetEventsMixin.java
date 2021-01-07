package carpet.mixins;

import carpet.fakes.ServerPlayerEntityInterface;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static carpet.script.CarpetEventServer.Event.PLAYER_RESPAWNS;

@Mixin(PlayerManager.class)
public class PlayerManager_scarpetEventsMixin
{
    @Inject(method = "respawnPlayer", at = @At("HEAD"))
    private void onRespawn(ServerPlayerEntity player, boolean olive, CallbackInfoReturnable<ServerPlayerEntity> cir)
    {
        PLAYER_RESPAWNS.onPlayerEvent(player);
    }

    @Inject(method = "respawnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;onSpawn()V"))
    private void invalidatePreviousInstance(ServerPlayerEntity player, boolean alive, CallbackInfoReturnable<ServerPlayerEntity> cir)
    {
        ((ServerPlayerEntityInterface)player).invalidateEntityObjectReference();
    }

}
