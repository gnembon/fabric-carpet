package carpet.mixins;

import carpet.utils.CarpetProfiler;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerChunkManager.class)
public abstract class ServerChunkManager_tickMixin
{
    @Shadow @Final private ServerWorld world;
    CarpetProfiler.ProfilerToken currentSection;

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At(
            value = "CONSTANT",
            args = "stringValue=purge"
    ))
    private void startTicketSection(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        currentSection = CarpetProfiler.start_section(world, "Ticket Manager", CarpetProfiler.TYPE.GENERAL);
    }

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At(
            value = "CONSTANT",
            args = "stringValue=chunks"
    ))
    private void stopTicketStartSpawningSection(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        if (currentSection != null)
        {
            CarpetProfiler.end_current_section(currentSection);
            currentSection = CarpetProfiler.start_section(world, "Spawning and Random Ticks", CarpetProfiler.TYPE.GENERAL);
        }
    }

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At(
            value = "CONSTANT",
            args = "stringValue=unload"
    ))
    private void stopSpawningStartUnloadSection(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        if (currentSection != null)
        {
            CarpetProfiler.end_current_section(currentSection);
            currentSection = CarpetProfiler.start_section(world, "Unloading", CarpetProfiler.TYPE.GENERAL);
        }
    }

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At("TAIL"))
    private void stopUnloadSection(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        if (currentSection != null)
        {
            CarpetProfiler.end_current_section(currentSection);
        }
    }
}
