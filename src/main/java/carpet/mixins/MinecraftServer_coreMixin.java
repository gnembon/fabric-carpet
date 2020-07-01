package carpet.mixins;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import com.google.gson.JsonElement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.world.level.LevelGeneratorType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServer_coreMixin
{
    //to inject right before
    // this.tickWorlds(booleanSupplier_1);
    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;tickWorlds(Ljava/util/function/BooleanSupplier;)V",
                    shift = At.Shift.BEFORE,
                    ordinal = 0
            )
    )
    private void onTick(BooleanSupplier booleanSupplier_1, CallbackInfo ci) {
        CarpetServer.tick((MinecraftServer) (Object) this);
    }

    @Inject(method = "loadWorld", at = @At("HEAD"))
    private void serverLoaded(String string_1, String string_2, long long_1, LevelGeneratorType levelGeneratorType_1, JsonElement jsonElement_1, CallbackInfo ci)
    {
        CarpetServer.onServerLoaded((MinecraftServer) (Object) this);
    }

    @Inject(method = "loadWorld", at = @At("RETURN"))
    private void serverLoadedWorlds(String string_1, String string_2, long long_1, LevelGeneratorType levelGeneratorType_1, JsonElement jsonElement_1, CallbackInfo ci)
    {
        CarpetServer.onServerLoadedWorlds((MinecraftServer) (Object) this);
    }

    @Inject(method = "shutdown", at = @At("HEAD"))
    private void serverClosed(CallbackInfo ci)
    {
        CarpetServer.onServerClosed((MinecraftServer) (Object) this);
    }

    @Inject(method = "prepareStartRegion", at = @At("RETURN"))
    private void afterSpawnCreated(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci)
    {
        if (CarpetSettings.spawnChunksSize != 11)
            CarpetSettings.ChangeSpawnChunksValidator.changeSpawnSize(CarpetSettings.spawnChunksSize);
    }
}
