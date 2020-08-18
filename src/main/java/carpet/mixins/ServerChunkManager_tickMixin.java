package carpet.mixins;

import carpet.helpers.TickSpeed;
import carpet.utils.CarpetProfiler;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerChunkManager.class)
public abstract class ServerChunkManager_tickMixin
{

    @Shadow @Final private ServerWorld world;

    CarpetProfiler.ProfilerToken currentSection;

    @Inject(method = "tickChunks", at = @At("HEAD"))
    private void startSpawningSection(CallbackInfo ci)
    {
        currentSection = CarpetProfiler.start_section(world, "Spawning and Random Ticks", CarpetProfiler.TYPE.GENERAL);
    }

    @Inject(method = "tickChunks", at = @At("RETURN"))
    private void stopSpawningSection(CallbackInfo ci)
    {
        if (currentSection != null)
        {
            CarpetProfiler.end_current_section(currentSection);
        }
    }

    //// Tick freeze
    @Redirect(method = "tickChunks", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;isDebugWorld()Z"
    ))
    private boolean skipChunkTicking(ServerWorld serverWorld)
    {
        if (!TickSpeed.process_entities) return true;
        return serverWorld.isDebugWorld();
    }

}
