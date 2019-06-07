package carpet.mixins;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.ClientConnection;
import net.minecraft.text.TranslatableText;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import carpet.patches.NetHandlerPlayServerFake;
import carpet.patches.EntityPlayerMPFake;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin
{
    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "loadPlayerData", at = @At(value = "RETURN", shift = At.Shift.BEFORE))
    private void fixStartingPos(ServerPlayerEntity serverPlayerEntity_1, CallbackInfoReturnable<CompoundTag> cir)
    {
        if (serverPlayerEntity_1 instanceof EntityPlayerMPFake)
        {
            ((EntityPlayerMPFake) serverPlayerEntity_1).fixStartingPosition.run();
        }
    }

    @Redirect(method = "onPlayerConnect", at = @At(value = "NEW", target = "net/minecraft/server/network/ServerPlayNetworkHandler"))
    private ServerPlayNetworkHandler replaceNetworkHandler(MinecraftServer server, ClientConnection clientConnection, ServerPlayerEntity playerIn)
    {
        boolean isServerPlayerEntity = playerIn instanceof EntityPlayerMPFake;
        if (isServerPlayerEntity)
        {
            return new NetHandlerPlayServerFake(this.server, clientConnection, playerIn);
        }
        else
        {
            return new ServerPlayNetworkHandler(this.server, clientConnection, playerIn);
        }
    }

    @Redirect(method = "createPlayer", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;hasNext()Z"))
    private boolean cancelWhileLoop(Iterator iterator)
    {
        return false;
    }

    @Inject(method = "createPlayer", at = @At(value = "INVOKE", shift = At.Shift.BEFORE,
            target = "Ljava/util/Iterator;hasNext()Z"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void newWhileLoop(GameProfile gameProfile_1, CallbackInfoReturnable<ServerPlayerEntity> cir, UUID uUID_1,
                              List list_1, Iterator var5)
    {
        while (var5.hasNext())
        {
            ServerPlayerEntity serverPlayerEntity_3 = (ServerPlayerEntity) var5.next();
            if(serverPlayerEntity_3 instanceof EntityPlayerMPFake)
            {
                serverPlayerEntity_3.kill();
                continue;
            }
            serverPlayerEntity_3.networkHandler.disconnect(new TranslatableText("multiplayer.disconnect.duplicate_login", new Object[0]));
        }
    }

}