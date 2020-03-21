package carpet.mixins;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static carpet.script.CarpetEventServer.Event.PLAYER_ATTACKS_ENTITY;
import static carpet.script.CarpetEventServer.Event.PLAYER_CLICKS_BLOCK;
import static carpet.script.CarpetEventServer.Event.PLAYER_DEPLOYS_ELYTRA;
import static carpet.script.CarpetEventServer.Event.PLAYER_DROPS_ITEM;
import static carpet.script.CarpetEventServer.Event.PLAYER_DROPS_STACK;
import static carpet.script.CarpetEventServer.Event.PLAYER_INTERACTS_WITH_ENTITY;
import static carpet.script.CarpetEventServer.Event.PLAYER_RELEASED_ITEM;
import static carpet.script.CarpetEventServer.Event.PLAYER_RIDES;
import static carpet.script.CarpetEventServer.Event.PLAYER_JUMPS;
import static carpet.script.CarpetEventServer.Event.PLAYER_RIGHT_CLICKS_BLOCK;
import static carpet.script.CarpetEventServer.Event.PLAYER_STARTS_SNEAKING;
import static carpet.script.CarpetEventServer.Event.PLAYER_STARTS_SPRINTING;
import static carpet.script.CarpetEventServer.Event.PLAYER_STOPS_SNEAKING;
import static carpet.script.CarpetEventServer.Event.PLAYER_STOPS_SPRINTING;
import static carpet.script.CarpetEventServer.Event.PLAYER_USES_ITEM;
import static carpet.script.CarpetEventServer.Event.PLAYER_WAKES_UP;


@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandler_scarpetEventsMixin
{
    @Shadow public ServerPlayerEntity player;

    @Inject(method = "onPlayerInput", at = @At(value = "RETURN"))
    private void checkMoves(PlayerInputC2SPacket p, CallbackInfo ci)
    {
        if (PLAYER_RIDES.isNeeded() && (p.getSideways() != 0.0F || p.getForward() != 0.0F || p.isJumping() || p.isSneaking()))
        {
            PLAYER_RIDES.onMountControls(player, p.getSideways(), p.getForward(), p.isJumping(), p.isSneaking());
        }
    }

    @Inject(method = "onPlayerAction", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;dropSelectedItem(Z)Z",
            ordinal = 0,
            shift = At.Shift.BEFORE
    ))
    private void onQItem(PlayerActionC2SPacket playerActionC2SPacket_1, CallbackInfo ci)
    {
        PLAYER_DROPS_ITEM.onPlayerEvent(player);
    }

    @Inject(method = "onPlayerAction", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;dropSelectedItem(Z)Z",
            ordinal = 1,
            shift = At.Shift.BEFORE
    ))
    private void onCtrlQItem(PlayerActionC2SPacket playerActionC2SPacket_1, CallbackInfo ci)
    {
        PLAYER_DROPS_STACK.onPlayerEvent(player);
    }


    @Inject(method = "onPlayerMove", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;jump()V"
    ))
    private void onJump(PlayerMoveC2SPacket playerMoveC2SPacket_1, CallbackInfo ci)
    {
        PLAYER_JUMPS.onPlayerEvent(player);
    }

    @Inject(method = "onPlayerAction", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerInteractionManager;processBlockBreakingAction(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/network/packet/c2s/play/PlayerActionC2SPacket$Action;Lnet/minecraft/util/math/Direction;I)V",
            shift = At.Shift.BEFORE
    ))
    private void onClicked(PlayerActionC2SPacket packet, CallbackInfo ci)
    {
        if (packet.getAction() == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK)
            PLAYER_CLICKS_BLOCK.onBlockAction(player, packet.getPos(), packet.getDirection());
    }

    @Redirect(method = "onPlayerAction", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;stopUsingItem()V"
    ))
    private void onStopUsing(ServerPlayerEntity serverPlayerEntity)
    {
        if (PLAYER_RELEASED_ITEM.isNeeded())
        {
            Hand hand = serverPlayerEntity.getActiveHand();
            ItemStack stack = serverPlayerEntity.getActiveItem().copy();
            serverPlayerEntity.stopUsingItem();
            PLAYER_RELEASED_ITEM.onItemAction(player, hand, stack);
        }
        else
        {
            serverPlayerEntity.stopUsingItem();
        }
    }

    @Inject(method = "onPlayerInteractBlock", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerInteractionManager;interactBlock(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;"
    ))
    private void onBlockInteracted(PlayerInteractBlockC2SPacket playerInteractBlockC2SPacket_1, CallbackInfo ci)
    {
        if (PLAYER_RIGHT_CLICKS_BLOCK.isNeeded())
        {
            Hand hand = playerInteractBlockC2SPacket_1.getHand();
            BlockHitResult hitRes = playerInteractBlockC2SPacket_1.getHitY();
            PLAYER_RIGHT_CLICKS_BLOCK.onBlockHit(player, hand, hitRes);
        }
    }

    @Inject(method = "onPlayerInteractItem", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;updateLastActionTime()V"
    ))
    private void onItemClicked(PlayerInteractItemC2SPacket playerInteractItemC2SPacket_1, CallbackInfo ci)
    {
        if (PLAYER_USES_ITEM.isNeeded())
        {
            Hand hand = playerInteractItemC2SPacket_1.getHand();
            PLAYER_USES_ITEM.onItemAction(player, hand, player.getStackInHand(hand).copy());
        }
    }

    @Inject(method = "onClientCommand", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;setSneaking(Z)V",
            ordinal = 0
    ))
    private void onStartSneaking(ClientCommandC2SPacket clientCommandC2SPacket_1, CallbackInfo ci)
    {
        PLAYER_STARTS_SNEAKING.onPlayerEvent(player);
    }

    @Inject(method = "onClientCommand", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;setSneaking(Z)V",
            ordinal = 1
    ))
    private void onStopSneaking(ClientCommandC2SPacket clientCommandC2SPacket_1, CallbackInfo ci)
    {
        PLAYER_STOPS_SNEAKING.onPlayerEvent(player);
    }

    @Inject(method = "onClientCommand", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;setSprinting(Z)V",
            ordinal = 0
    ))
    private void onStartSprinting(ClientCommandC2SPacket clientCommandC2SPacket_1, CallbackInfo ci)
    {
        PLAYER_STARTS_SPRINTING.onPlayerEvent(player);
    }

    @Inject(method = "onClientCommand", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;setSprinting(Z)V",
            ordinal = 1
    ))
    private void onStopSprinting(ClientCommandC2SPacket clientCommandC2SPacket_1, CallbackInfo ci)
    {
        PLAYER_STOPS_SPRINTING.onPlayerEvent(player);
    }

    @Inject(method = "onClientCommand", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;isSleeping()Z",
            shift = At.Shift.BEFORE
    ))
    private void onWakeUp(ClientCommandC2SPacket clientCommandC2SPacket_1, CallbackInfo ci)
    {
        //weird one - doesn't seem to work, maybe MP
        PLAYER_WAKES_UP.onPlayerEvent(player);
    }

    @Inject(method = "onClientCommand", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;method_23668()Z",
            shift = At.Shift.BEFORE
    ))
    private void onElytraEngage(ClientCommandC2SPacket clientCommandC2SPacket_1, CallbackInfo ci)
    {
        PLAYER_DEPLOYS_ELYTRA.onPlayerEvent(player);
    }

    @Inject(method = "onPlayerInteractEntity", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;interact(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;"
    ))
    private void onEntityInteract(PlayerInteractEntityC2SPacket playerInteractEntityC2SPacket_1, CallbackInfo ci)
    {
        PLAYER_INTERACTS_WITH_ENTITY.onEntityAction(player, playerInteractEntityC2SPacket_1.getEntity(player.getServerWorld()), playerInteractEntityC2SPacket_1.getHand());
    }

    @Inject(method = "onPlayerInteractEntity", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;attack(Lnet/minecraft/entity/Entity;)V"
    ))
    private void onEntityAttack(PlayerInteractEntityC2SPacket playerInteractEntityC2SPacket_1, CallbackInfo ci)
    {
        //todo add hit and hand in the future
        PLAYER_ATTACKS_ENTITY.onEntityAction(player, playerInteractEntityC2SPacket_1.getEntity(player.getServerWorld()), null);
    }
}
