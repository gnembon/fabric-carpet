package carpet.mixins;

import carpet.utils.CarpetProfiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;

@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCache_profilerMixin
{

    @Shadow @Final ServerLevel level;

    CarpetProfiler.ProfilerToken currentSection;

    @Inject(method = "tickChunks", at = @At("HEAD"))
    private void startSpawningSection(CallbackInfo ci)
    {
        currentSection = CarpetProfiler.start_section(level, "Spawning", CarpetProfiler.TYPE.GENERAL);
    }

    @Inject(method = "tickChunks", at = @At(
            value = "FIELD",
            target = "net/minecraft/server/level/ServerChunkCache.level:Lnet/minecraft/server/level/ServerLevel;",
            ordinal = 10
    ))
    private void skipChunkTicking(CallbackInfo ci)
    {
        if (currentSection != null)
        {
            CarpetProfiler.end_current_section(currentSection);
        }
    }

    @Inject(method = "tickChunks", at = @At(
            value = "INVOKE",
            target = "net/minecraft/server/level/ServerLevel.tickChunk(Lnet/minecraft/world/level/chunk/LevelChunk;I)V",
            shift = At.Shift.AFTER
    ))
    private void resumeSpawningSection(CallbackInfo ci)
    {
        currentSection = CarpetProfiler.start_section(level, "Spawning", CarpetProfiler.TYPE.GENERAL);
    }

    @Inject(method = "tickChunks", at = @At("RETURN"))
    private void stopSpawningSection(CallbackInfo ci)
    {
        if (currentSection != null)
        {
            CarpetProfiler.end_current_section(currentSection);
        }
    }

    //@Redirect(method = "tick", at = @At(
    //        value = "INVOKE",
    //        target = "Lnet/minecraft/server/level/DistanceManager;purgeStaleTickets()V"
    //))
    //private void pauseTicketSystem(DistanceManager distanceManager)
    //{
        // pausing expiry of tickets
        // that will prevent also chunks from unloading, so require a deep frozen state
        //ServerTickRateManager trm = ((MinecraftServerInterface) level.getServer()).getTickRateManager();
        //if (!trm.runsNormally() && trm.deeplyFrozen()) return;
        //distanceManager.purgeStaleTickets();
    //}

}
