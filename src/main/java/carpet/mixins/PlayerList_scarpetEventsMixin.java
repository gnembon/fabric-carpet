package carpet.mixins;

import carpet.fakes.ServerPlayerInterface;
import carpet.script.external.Vanilla;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static carpet.script.CarpetEventServer.Event.PLAYER_MESSAGE;
import static carpet.script.CarpetEventServer.Event.PLAYER_RESPAWNS;

@Mixin(PlayerList.class)
public class PlayerList_scarpetEventsMixin
{
    @Shadow @Final private MinecraftServer server;

    @Inject(method = "respawn", at = @At("HEAD"))
    private void onResp(ServerPlayer serverPlayer, boolean olive, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir)
    {
        PLAYER_RESPAWNS.onPlayerEvent(serverPlayer);
    }

    @Inject(method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void cancellableChatMessageEvent(PlayerChatMessage message, ServerPlayer player, ChatType.Bound params, CallbackInfo ci) {
        // having this earlier breaks signatures
        if (PLAYER_MESSAGE.isNeeded())
        {
            if (PLAYER_MESSAGE.onPlayerMessage(player, message.signedContent())) ci.cancel();
        }
    }

    @Inject(method = "respawn", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;initInventoryMenu()V"
    ))
    private void invalidatePreviousInstance(ServerPlayer player, boolean alive, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir)
    {
        ((ServerPlayerInterface)player).invalidateEntityObjectReference();
    }

    @Inject(method = "reloadResources", at = @At("HEAD"))
    private void reloadCommands(CallbackInfo ci)
    {
        Vanilla.MinecraftServer_getScriptServer(server).reAddCommands();
    }
}
