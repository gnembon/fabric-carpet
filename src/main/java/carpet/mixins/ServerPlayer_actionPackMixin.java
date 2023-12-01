package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import carpet.patches.EntityPlayerMPFake;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayer_actionPackMixin implements ServerPlayerInterface
{
    @Unique
    public EntityPlayerActionPack actionPack;

    @Override
    public EntityPlayerActionPack getActionPack()
    {
        return actionPack;
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void onServerPlayerEntityContructor(MinecraftServer minecraftServer, ServerLevel serverLevel, GameProfile gameProfile, ClientInformation cli, CallbackInfo ci)
    {
        this.actionPack = new EntityPlayerActionPack((ServerPlayer) (Object) this);
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void onTick(CallbackInfo ci)
    {
        if (CarpetSettings.fakePlayerTicksInEU) {
            actionPack.onUpdate();
        }
    }

    @Inject(method = "doTick", at = @At(value = "HEAD"))
    private void tickActionPack(CallbackInfo ci)
    {
        if (!((Object) this instanceof EntityPlayerMPFake)) {
            actionPack.onUpdate();
        }
    }
}
