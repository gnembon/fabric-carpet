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

    @Inject(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;JLjava/util/List;)V", at = @At("HEAD"))
    private void startSpawningSection(CallbackInfo ci)
    {
        currentSection = CarpetProfiler.start_section(level, "Spawning", CarpetProfiler.TYPE.GENERAL);
    }

    @Inject(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;JLjava/util/List;)V", at = @At("RETURN"))
    private void stopSpawningSection(CallbackInfo ci)
    {
        if (currentSection != null)
        {
            CarpetProfiler.end_current_section(currentSection);
        }
    }
}
