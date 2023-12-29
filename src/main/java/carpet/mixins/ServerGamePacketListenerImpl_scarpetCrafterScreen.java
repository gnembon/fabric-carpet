package carpet.mixins;

import net.minecraft.network.protocol.game.ServerboundContainerSlotStateChangedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import carpet.script.value.ScreenValue.ScarpetCrafterMenu;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImpl_scarpetCrafterScreen
{

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleContainerSlotStateChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/CrafterMenu;getContainer()Lnet/minecraft/world/Container;"))
    private void injected(ServerboundContainerSlotStateChangedPacket serverboundContainerSlotStateChangedPacket,CallbackInfo ci) {
        if(player.containerMenu instanceof ScarpetCrafterMenu cm && !(cm.getContainer() instanceof CrafterBlockEntity)){
            cm.setSlotState(serverboundContainerSlotStateChangedPacket.slotId(), serverboundContainerSlotStateChangedPacket.newState());
        };
    }
}
