package carpet.mixins;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.utils.CarpetProfiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.progress.ChunkProgressListener;
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
            method = "tickServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;tickChildren(Ljava/util/function/BooleanSupplier;)V",
                    shift = At.Shift.BEFORE,
                    ordinal = 0
            )
    )
    private void onTick(BooleanSupplier booleanSupplier_1, CallbackInfo ci) {
        CarpetProfiler.ProfilerToken token = CarpetProfiler.start_section(null, "Carpet", CarpetProfiler.TYPE.GENERAL);
        CarpetServer.tick((MinecraftServer) (Object) this);
        CarpetProfiler.end_current_section(token);
    }

    @Inject(method = "loadLevel", at = @At("HEAD"))
    private void serverLoaded(CallbackInfo ci)
    {
        CarpetServer.onServerLoaded((MinecraftServer) (Object) this);
    }

    @Inject(method = "loadLevel", at = @At("RETURN"))
    private void serverLoadedWorlds(CallbackInfo ci)
    {
        CarpetServer.onServerLoadedWorlds((MinecraftServer) (Object) this);
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void serverClosed(CallbackInfo ci)
    {
        CarpetServer.onServerClosed((MinecraftServer) (Object) this);
    }

    @Inject(method = "stopServer", at = @At("TAIL"))
    private void serverDoneClosed(CallbackInfo ci)
    {
        CarpetServer.onServerDoneClosing((MinecraftServer) (Object) this);
    }

    @Inject(method = "prepareLevels", at = @At("RETURN"))
    private void afterSpawnCreated(ChunkProgressListener worldGenerationProgressListener, CallbackInfo ci)
    {
        if (CarpetSettings.spawnChunksSize != 11)
            CarpetSettings.ChangeSpawnChunksValidator.changeSpawnSize(CarpetSettings.spawnChunksSize);
        
        CarpetSettings.LightBatchValidator.applyLightBatchSizes(CarpetSettings.lightEngineMaxBatchSize);
    }
}
