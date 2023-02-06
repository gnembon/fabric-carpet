package carpet.mixins;

import carpet.CarpetServer;
import carpet.fakes.ServerPlayerInterface;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static carpet.script.CarpetEventServer.Event.PLAYER_RESPAWNS;

@Mixin(PlayerList.class)
public class PlayerList_scarpetEventsMixin
{
    @Inject(method = "respawn", at = @At("HEAD"))
    private void onRespawn(ServerPlayer player, boolean olive, CallbackInfoReturnable<ServerPlayer> cir)
    {
        PLAYER_RESPAWNS.onPlayerEvent(player);
    }

    @Inject(method = "respawn", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;initInventoryMenu()V"
    ))
    private void invalidatePreviousInstance(ServerPlayer player, boolean alive, CallbackInfoReturnable<ServerPlayer> cir)
    {
        ((ServerPlayerInterface)player).invalidateEntityObjectReference();
    }

    @Inject(method = "reloadResources", at = @At("HEAD"))
    private void reloadCommands(CallbackInfo ci)
    {
        CarpetServer.scriptServer.reAddCommands();
    }
}
