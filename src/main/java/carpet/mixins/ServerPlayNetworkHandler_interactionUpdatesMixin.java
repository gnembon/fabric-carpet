package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandler_interactionUpdatesMixin
{
    @Inject(method = "onPlayerInteractBlock", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerInteractionManager;interactBlock(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;",
            shift = At.Shift.BEFORE
    ))
    private void beforeBlockInteracted(PlayerInteractBlockC2SPacket playerInteractBlockC2SPacket_1, CallbackInfo ci)
    {
        if (!CarpetSettings.interactionUpdates)
            CarpetSettings.impendingFillSkipUpdates = true;
    }

    @Inject(method = "onPlayerInteractBlock", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerInteractionManager;interactBlock(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;",
            shift = At.Shift.AFTER
    ))
    private void afterBlockInteracted(PlayerInteractBlockC2SPacket playerInteractBlockC2SPacket_1, CallbackInfo ci)
    {
        if (!CarpetSettings.interactionUpdates)
            CarpetSettings.impendingFillSkipUpdates = false;
    }

    @Inject(method = "onPlayerInteractItem", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerInteractionManager;interactItem(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;",
            shift = At.Shift.BEFORE
    ))
    private void beforeItemInteracted(PlayerInteractItemC2SPacket packet, CallbackInfo ci)
    {
        if (!CarpetSettings.interactionUpdates)
            CarpetSettings.impendingFillSkipUpdates = true;
    }

    @Inject(method = "onPlayerInteractItem", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerInteractionManager;interactItem(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;",
            shift = At.Shift.AFTER
    ))
    private void afterItemInteracted(PlayerInteractItemC2SPacket packet, CallbackInfo ci)
    {
        if (!CarpetSettings.interactionUpdates)
            CarpetSettings.impendingFillSkipUpdates = false;
    }
    @Inject(method = "onPlayerAction", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerInteractionManager;processBlockBreakingAction(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/network/packet/c2s/play/PlayerActionC2SPacket$Action;Lnet/minecraft/util/math/Direction;I)V",
            shift = At.Shift.BEFORE
    ))
    private void beforeBlockBroken(PlayerActionC2SPacket packet, CallbackInfo ci)
    {
        if (!CarpetSettings.interactionUpdates)
            CarpetSettings.impendingFillSkipUpdates = true;
    }

    @Inject(method = "onPlayerAction", at = @At(
            value = "INVOKE",
            target ="Lnet/minecraft/server/network/ServerPlayerInteractionManager;processBlockBreakingAction(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/network/packet/c2s/play/PlayerActionC2SPacket$Action;Lnet/minecraft/util/math/Direction;I)V",
            shift = At.Shift.AFTER
    ))
    private void afterBlockBroken(PlayerActionC2SPacket packet, CallbackInfo ci)
    {
        if (!CarpetSettings.interactionUpdates)
            CarpetSettings.impendingFillSkipUpdates = false;
    }

}
