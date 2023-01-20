package carpet.mixins;

import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static carpet.script.CarpetEventServer.Event.PLAYER_CLICKS_BLOCK;
import static carpet.script.CarpetEventServer.Event.PLAYER_DEPLOYS_ELYTRA;
import static carpet.script.CarpetEventServer.Event.PLAYER_DROPS_ITEM;
import static carpet.script.CarpetEventServer.Event.PLAYER_DROPS_STACK;
import static carpet.script.CarpetEventServer.Event.PLAYER_ESCAPES_SLEEP;
import static carpet.script.CarpetEventServer.Event.PLAYER_RELEASED_ITEM;
import static carpet.script.CarpetEventServer.Event.PLAYER_RIDES;
import static carpet.script.CarpetEventServer.Event.PLAYER_JUMPS;
import static carpet.script.CarpetEventServer.Event.PLAYER_RIGHT_CLICKS_BLOCK;
import static carpet.script.CarpetEventServer.Event.PLAYER_CHOOSES_RECIPE;
import static carpet.script.CarpetEventServer.Event.PLAYER_STARTS_SNEAKING;
import static carpet.script.CarpetEventServer.Event.PLAYER_STARTS_SPRINTING;
import static carpet.script.CarpetEventServer.Event.PLAYER_STOPS_SNEAKING;
import static carpet.script.CarpetEventServer.Event.PLAYER_STOPS_SPRINTING;
import static carpet.script.CarpetEventServer.Event.PLAYER_SWAPS_HANDS;
import static carpet.script.CarpetEventServer.Event.PLAYER_SWINGS_HAND;
import static carpet.script.CarpetEventServer.Event.PLAYER_SWITCHES_SLOT;
import static carpet.script.CarpetEventServer.Event.PLAYER_MESSAGE;
import static carpet.script.CarpetEventServer.Event.PLAYER_COMMAND;
import static carpet.script.CarpetEventServer.Event.PLAYER_USES_ITEM;
import static carpet.script.CarpetEventServer.Event.PLAYER_WAKES_UP;

import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;


@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImpl_scarpetEventsMixin
{
    @Shadow public ServerPlayer player;

    @Inject(method = "handlePlayerInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setPlayerInput(FFZZ)V"))
    private void checkMoves(ServerboundPlayerInputPacket p, CallbackInfo ci)
    {
        if (PLAYER_RIDES.isNeeded() && (p.getXxa() != 0.0F || p.getZza() != 0.0F || p.isJumping() || p.isShiftKeyDown()))
        {
            PLAYER_RIDES.onMountControls(player, p.getXxa(), p.getZza(), p.isJumping(), p.isShiftKeyDown());
        }
    }

    @Inject(method = "handlePlayerAction", cancellable = true, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;drop(Z)Z", // dropSelectedItem
            ordinal = 0,
            shift = At.Shift.BEFORE
    ))
    private void onQItem(ServerboundPlayerActionPacket playerActionC2SPacket_1, CallbackInfo ci)
    {
        if(PLAYER_DROPS_ITEM.onPlayerEvent(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "handlePlayerAction", cancellable = true, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;",
            ordinal = 0,
            shift = At.Shift.BEFORE
    ))
    private void onHandSwap(ServerboundPlayerActionPacket playerActionC2SPacket_1, CallbackInfo ci)
    {
        if(PLAYER_SWAPS_HANDS.onPlayerEvent(player)) ci.cancel();
    }

    @Inject(method = "handlePlayerAction", cancellable = true, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;drop(Z)Z", // dropSelectedItem
            ordinal = 1,
            shift = At.Shift.BEFORE
    ))
    private void onCtrlQItem(ServerboundPlayerActionPacket playerActionC2SPacket_1, CallbackInfo ci)
    {
        if(PLAYER_DROPS_STACK.onPlayerEvent(player)) {
            ci.cancel();
        }
    }


    @Inject(method = "handleMovePlayer", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;jumpFromGround()V"
    ))
    private void onJump(ServerboundMovePlayerPacket playerMoveC2SPacket_1, CallbackInfo ci)
    {
        PLAYER_JUMPS.onPlayerEvent(player);
    }

    @Inject(method = "handlePlayerAction", cancellable = true, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;handleBlockBreakAction(Lnet/minecraft/core/BlockPos;Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket$Action;Lnet/minecraft/core/Direction;II)V",
            shift = At.Shift.BEFORE
    ))
    private void onClicked(ServerboundPlayerActionPacket packet, CallbackInfo ci)
    {
        if (packet.getAction() == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK)
            if(PLAYER_CLICKS_BLOCK.onBlockAction(player, packet.getPos(), packet.getDirection())) {
                ci.cancel();
            }
    }

    @Redirect(method = "handlePlayerAction", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;releaseUsingItem()V"
    ))
    private void onStopUsing(ServerPlayer serverPlayerEntity)
    {
        if (PLAYER_RELEASED_ITEM.isNeeded())
        {
            InteractionHand hand = serverPlayerEntity.getUsedItemHand();
            ItemStack stack = serverPlayerEntity.getUseItem().copy();
            serverPlayerEntity.releaseUsingItem();
            PLAYER_RELEASED_ITEM.onItemAction(player, hand, stack);
        }
        else
        {
            serverPlayerEntity.releaseUsingItem();
        }
    }

    @Inject(method = "handleUseItemOn", cancellable = true, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;useItemOn(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"
    ))
    private void onBlockInteracted(ServerboundUseItemOnPacket playerInteractBlockC2SPacket_1, CallbackInfo ci)
    {
        if (PLAYER_RIGHT_CLICKS_BLOCK.isNeeded())
        {
            InteractionHand hand = playerInteractBlockC2SPacket_1.getHand();
            BlockHitResult hitRes = playerInteractBlockC2SPacket_1.getHitResult();
            if(PLAYER_RIGHT_CLICKS_BLOCK.onBlockHit(player, hand, hitRes)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "handleUseItem", cancellable = true, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V"
    ))
    private void onItemClicked(ServerboundUseItemPacket playerInteractItemC2SPacket_1, CallbackInfo ci)
    {
        if (PLAYER_USES_ITEM.isNeeded())
        {
            InteractionHand hand = playerInteractItemC2SPacket_1.getHand();
            if(PLAYER_USES_ITEM.onItemAction(player, hand, player.getItemInHand(hand).copy())) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "handlePlayerCommand", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;setShiftKeyDown(Z)V",
            ordinal = 0
    ))
    private void onStartSneaking(ServerboundPlayerCommandPacket clientCommandC2SPacket_1, CallbackInfo ci)
    {
        PLAYER_STARTS_SNEAKING.onPlayerEvent(player);
    }

    @Inject(method = "handlePlayerCommand", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;setShiftKeyDown(Z)V",
            ordinal = 1
    ))
    private void onStopSneaking(ServerboundPlayerCommandPacket clientCommandC2SPacket_1, CallbackInfo ci)
    {
        PLAYER_STOPS_SNEAKING.onPlayerEvent(player);
    }

    @Inject(method = "handlePlayerCommand", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;setSprinting(Z)V",
            ordinal = 0
    ))
    private void onStartSprinting(ServerboundPlayerCommandPacket clientCommandC2SPacket_1, CallbackInfo ci)
    {
        PLAYER_STARTS_SPRINTING.onPlayerEvent(player);
    }

    @Inject(method = "handlePlayerCommand", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;setSprinting(Z)V",
            ordinal = 1
    ))
    private void onStopSprinting(ServerboundPlayerCommandPacket clientCommandC2SPacket_1, CallbackInfo ci)
    {
        PLAYER_STOPS_SPRINTING.onPlayerEvent(player);
    }

    @Inject(method = "handlePlayerCommand", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;isSleeping()Z",
            shift = At.Shift.BEFORE
    ))
    private void onWakeUp(ServerboundPlayerCommandPacket clientCommandC2SPacket_1, CallbackInfo ci)
    {
        //weird one - doesn't seem to work, maybe MP
        if (player.isSleeping())
            PLAYER_WAKES_UP.onPlayerEvent(player);
        else
            PLAYER_ESCAPES_SLEEP.onPlayerEvent(player);

    }

    @Inject(method = "handlePlayerCommand", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;tryToStartFallFlying()Z",
            shift = At.Shift.BEFORE
    ))
    private void onElytraEngage(ServerboundPlayerCommandPacket clientCommandC2SPacket_1, CallbackInfo ci)
    {
        PLAYER_DEPLOYS_ELYTRA.onPlayerEvent(player);
    }

    /*@Inject(method = "onPlayerInteractEntity", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;interact(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;"
    ))
    private void onEntityInteract(PlayerInteractEntityC2SPacket playerInteractEntityC2SPacket_1, CallbackInfo ci)
    {
        PLAYER_INTERACTS_WITH_ENTITY.onEntityHandAction(player, playerInteractEntityC2SPacket_1.getEntity(player.getWorld()), playerInteractEntityC2SPacket_1.getHand());
    }*/

    /*@Inject(method = "onPlayerInteractEntity", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;attack(Lnet/minecraft/entity/Entity;)V"
    ))
    private void onEntityAttack(PlayerInteractEntityC2SPacket playerInteractEntityC2SPacket_1, CallbackInfo ci)
    {
        //todo add hit and hand in the future
        PLAYER_ATTACKS_ENTITY.onEntityHandAction(player, playerInteractEntityC2SPacket_1.getEntity(player.getWorld()), null);
    }*/

    @Inject(method = "handleContainerButtonClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V"))
    private void onItemBeingPickedFromInventory(ServerboundContainerButtonClickPacket packet, CallbackInfo ci)
    {
        // crafts not int the crafting window
        //CarpetSettings.LOG.error("Player clicks button "+packet.getButtonId());
    }
    @Inject(method = "handlePlaceRecipe", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V"))
    private void onRecipeSelectedInRecipeManager(ServerboundPlaceRecipePacket packet, CallbackInfo ci)
    {
        if (PLAYER_CHOOSES_RECIPE.isNeeded())
        {
            if(PLAYER_CHOOSES_RECIPE.onRecipeSelected(player, packet.getRecipe(), packet.isShiftDown())) ci.cancel();
        }
    }

    @Inject(method = "handleSetCarriedItem", at = @At("HEAD"))
    private void onUpdatedSelectedSLot(ServerboundSetCarriedItemPacket packet, CallbackInfo ci)
    {
        if (PLAYER_SWITCHES_SLOT.isNeeded() && player.getServer() != null && player.getServer().isSameThread())
        {
            PLAYER_SWITCHES_SLOT.onSlotSwitch(player, player.getInventory().selected, packet.getSlot());
        }
    }

    @Inject(method = "handleAnimate", at = @At(
            value = "INVOKE", target =
            "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V",
            shift = At.Shift.BEFORE)
    )
    private void onSwing(ServerboundSwingPacket packet, CallbackInfo ci)
    {
        if (PLAYER_SWINGS_HAND.isNeeded() && !player.swinging)
        {
            PLAYER_SWINGS_HAND.onHandAction(player, packet.getHand());
        }
    }

    @Inject(method = "handleChat(Lnet/minecraft/network/protocol/game/ServerboundChatPacket;)V",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void onChatMessage(ServerboundChatPacket serverboundChatPacket, CallbackInfo ci) {
        if (PLAYER_MESSAGE.isNeeded())
        {
            if(PLAYER_MESSAGE.onPlayerMessage(player, serverboundChatPacket.message())) ci.cancel();
        }
    }

    @Inject(method = "handleChatCommand(Lnet/minecraft/network/protocol/game/ServerboundChatCommandPacket;)V",
            at = @At(value = "HEAD")
    )
    private void onChatCommandMessage(ServerboundChatCommandPacket serverboundChatCommandPacket, CallbackInfo ci) {
        if (PLAYER_COMMAND.isNeeded())
        {
            PLAYER_COMMAND.onPlayerMessage(player, serverboundChatCommandPacket.command());
        }
    }
}
