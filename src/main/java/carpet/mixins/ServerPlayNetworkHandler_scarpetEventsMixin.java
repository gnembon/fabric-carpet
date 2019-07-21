package carpet.mixins;

import carpet.CarpetServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.packet.ClientCommandC2SPacket;
import net.minecraft.server.network.packet.PlayerActionC2SPacket;
import net.minecraft.server.network.packet.PlayerInputC2SPacket;
import net.minecraft.server.network.packet.PlayerInteractBlockC2SPacket;
import net.minecraft.server.network.packet.PlayerInteractEntityC2SPacket;
import net.minecraft.server.network.packet.PlayerInteractItemC2SPacket;
import net.minecraft.server.network.packet.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandler_scarpetEventsMixin
{
    @Shadow public ServerPlayerEntity player;

    @Inject(method = "onPlayerInput", at = @At(value = "RETURN"))
    private void checkMoves(PlayerInputC2SPacket p, CallbackInfo ci)
    {
        if (p.getSideways() != 0.0F || p.getForward() != 0.0F || p.isJumping() || p.isSneaking())
        {
            CarpetServer.scriptServer.events.onMountControls(player, p.getSideways(), p.getForward(), p.isJumping(), p.isSneaking());
        }
    }

    @Inject(method = "onPlayerMove", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;jump()V"
    ))
    private void onJump(PlayerMoveC2SPacket playerMoveC2SPacket_1, CallbackInfo ci)
    {
        CarpetServer.scriptServer.events.onPlayerJumped(player);
    }

    @Inject(method = "onPlayerAction", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerInteractionManager;method_14263(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/server/network/packet/PlayerActionC2SPacket$Action;Lnet/minecraft/util/math/Direction;I)V"
    ))
    private void onClicked(PlayerActionC2SPacket packet, CallbackInfo ci)
    {
        if (packet.getAction() == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK)
            CarpetServer.scriptServer.events.onBlockClicked(player, packet.getPos(), packet.getDirection());
    }

    @Inject(method = "onPlayerInteractBlock", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerInteractionManager;interactBlock(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;"
    ))
    private void onBlockInteracted(PlayerInteractBlockC2SPacket playerInteractBlockC2SPacket_1, CallbackInfo ci)
    {
        Hand hand = playerInteractBlockC2SPacket_1.getHand();
        BlockHitResult hitRes = playerInteractBlockC2SPacket_1.getHitY();
        CarpetServer.scriptServer.events.onRightClickBlock(player, hand, hitRes );
    }

    @Inject(method = "onPlayerInteractItem", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;updateLastActionTime()V"
    ))
    private void onItemClicked(PlayerInteractItemC2SPacket playerInteractItemC2SPacket_1, CallbackInfo ci)
    {
        CarpetServer.scriptServer.events.onRightClick(player, playerInteractItemC2SPacket_1.getHand());
    }

    @Inject(method = "onClientCommand", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;setSneaking(Z)V",
            ordinal = 0
    ))
    private void onStartSneaking(ClientCommandC2SPacket clientCommandC2SPacket_1, CallbackInfo ci)
    {
        CarpetServer.scriptServer.events.onStartSneaking(player);
    }

    @Inject(method = "onClientCommand", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;setSneaking(Z)V",
            ordinal = 1
    ))
    private void onStopSneaking(ClientCommandC2SPacket clientCommandC2SPacket_1, CallbackInfo ci)
    {
        CarpetServer.scriptServer.events.onStopSneaking(player);
    }

    @Inject(method = "onClientCommand", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;setSprinting(Z)V",
            ordinal = 0
    ))
    private void onStartSprinting(ClientCommandC2SPacket clientCommandC2SPacket_1, CallbackInfo ci)
    {
        CarpetServer.scriptServer.events.onStartSprinting(player);
    }

    @Inject(method = "onClientCommand", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;setSprinting(Z)V",
            ordinal = 1
    ))
    private void onStopSprinting(ClientCommandC2SPacket clientCommandC2SPacket_1, CallbackInfo ci)
    {
        CarpetServer.scriptServer.events.onStopSprinting(player);
    }

    @Inject(method = "onClientCommand", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;isSleeping()Z",
            shift = At.Shift.BEFORE
    ))
    private void onWakeUp(ClientCommandC2SPacket clientCommandC2SPacket_1, CallbackInfo ci)
    {
        //weird one - doesn't seem to work, maybe MP
        CarpetServer.scriptServer.events.onOutOfBed(player);
    }

    @Inject(method = "onClientCommand", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;isFallFlying()Z",
            shift = At.Shift.BEFORE
    ))
    private void onElytraEngage(ClientCommandC2SPacket clientCommandC2SPacket_1, CallbackInfo ci)
    {
        CarpetServer.scriptServer.events.onElytraDeploy(player);
    }

    @Inject(method = "onPlayerInteractEntity", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;interact(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;"
    ))
    private void onEntityInteract(PlayerInteractEntityC2SPacket playerInteractEntityC2SPacket_1, CallbackInfo ci)
    {
        CarpetServer.scriptServer.events.onEntityInteracted(player, playerInteractEntityC2SPacket_1.getEntity(player.getServerWorld()), playerInteractEntityC2SPacket_1.getHand());
    }

    @Inject(method = "onPlayerInteractEntity", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;attack(Lnet/minecraft/entity/Entity;)V"
    ))
    private void onEntityAttack(PlayerInteractEntityC2SPacket playerInteractEntityC2SPacket_1, CallbackInfo ci)
    {
        //todo add hit and hand in the future
        CarpetServer.scriptServer.events.onEntityAttacked(player, playerInteractEntityC2SPacket_1.getEntity(player.getServerWorld()));
    }
}
