package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImpl_interactionUpdatesMixin
{
    @Inject(method = "handleUseItemOn", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;useItemOn(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
            shift = At.Shift.BEFORE
    ))
    private void beforeBlockInteracted(ServerboundUseItemOnPacket playerInteractBlockC2SPacket_1, CallbackInfo ci)
    {
        if (!CarpetSettings.interactionUpdates)
            CarpetSettings.impendingFillSkipUpdates.set(true);
    }

    @Inject(method = "handleUseItemOn", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;useItemOn(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
            shift = At.Shift.AFTER
    ))
    private void afterBlockInteracted(ServerboundUseItemOnPacket playerInteractBlockC2SPacket_1, CallbackInfo ci)
    {
        if (!CarpetSettings.interactionUpdates)
            CarpetSettings.impendingFillSkipUpdates.set(false);
    }

    @Inject(method = "handleUseItem", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;useItem(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            shift = At.Shift.BEFORE
    ))
    private void beforeItemInteracted(ServerboundUseItemPacket packet, CallbackInfo ci)
    {
        if (!CarpetSettings.interactionUpdates)
            CarpetSettings.impendingFillSkipUpdates.set(true);
    }

    @Inject(method = "handleUseItem", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;useItem(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            shift = At.Shift.AFTER
    ))
    private void afterItemInteracted(ServerboundUseItemPacket packet, CallbackInfo ci)
    {
        if (!CarpetSettings.interactionUpdates)
            CarpetSettings.impendingFillSkipUpdates.set(false);
    }
    @Inject(method = "handlePlayerAction", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;handleBlockBreakAction(Lnet/minecraft/core/BlockPos;Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket$Action;Lnet/minecraft/core/Direction;I)V",
            shift = At.Shift.BEFORE
    ))
    private void beforeBlockBroken(ServerboundPlayerActionPacket packet, CallbackInfo ci)
    {
        if (!CarpetSettings.interactionUpdates)
            CarpetSettings.impendingFillSkipUpdates.set(true);
    }

    @Inject(method = "handlePlayerAction", at = @At(
            value = "INVOKE",
            target ="Lnet/minecraft/server/level/ServerPlayerGameMode;handleBlockBreakAction(Lnet/minecraft/core/BlockPos;Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket$Action;Lnet/minecraft/core/Direction;I)V",
            shift = At.Shift.AFTER
    ))
    private void afterBlockBroken(ServerboundPlayerActionPacket packet, CallbackInfo ci)
    {
        if (!CarpetSettings.interactionUpdates)
            CarpetSettings.impendingFillSkipUpdates.set(false);
    }

}
