package carpet.mixins;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import carpet.patches.NetHandlerPlayServerFake;
import carpet.patches.EntityPlayerMPFake;

@Mixin(PlayerList.class)
public abstract class PlayerList_fakePlayersMixin
{
    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "load", at = @At(value = "RETURN", shift = At.Shift.BEFORE))
    private void fixStartingPos(ServerPlayer serverPlayerEntity_1, CallbackInfoReturnable<CompoundTag> cir)
    {
        if (serverPlayerEntity_1 instanceof EntityPlayerMPFake)
        {
            ((EntityPlayerMPFake) serverPlayerEntity_1).fixStartingPosition.run();
        }
    }

    @Redirect(method = "placeNewPlayer", at = @At(value = "NEW", target = "net/minecraft/server/network/ServerGamePacketListenerImpl"))
    private ServerGamePacketListenerImpl replaceNetworkHandler(MinecraftServer server, Connection clientConnection, ServerPlayer playerIn)
    {
        if (playerIn instanceof EntityPlayerMPFake fake)
        {
            return new NetHandlerPlayServerFake(this.server, clientConnection, fake);
        }
        else
        {
            return new ServerGamePacketListenerImpl(this.server, clientConnection, playerIn);
        }
    }
}