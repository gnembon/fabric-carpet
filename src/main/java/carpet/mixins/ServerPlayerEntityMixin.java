package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.ServerPlayerEntityInterface;
import carpet.helpers.EntityPlayerActionPack;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements ServerPlayerEntityInterface
{
    @Unique
    public EntityPlayerActionPack actionPack;
    @Override
    public EntityPlayerActionPack getActionPack()
    {
        return actionPack;
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void onServerPlayerEntityContructor(
            MinecraftServer minecraftServer_1,
            ServerWorld serverWorld_1,
            GameProfile gameProfile_1,
            CallbackInfo ci)
    {
        this.actionPack = new EntityPlayerActionPack((ServerPlayerEntity) (Object) this);
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void onTick(CallbackInfo ci)
    {
        try
        {
            actionPack.onUpdate();
        }
        catch (StackOverflowError soe)
        {
            CarpetSettings.LOG.fatal("Caused stack overflow when performing player action", soe);
        }
        catch (Throwable exc)
        {
            CarpetSettings.LOG.fatal("Error executing player tasks", exc);
        }
    }
}
