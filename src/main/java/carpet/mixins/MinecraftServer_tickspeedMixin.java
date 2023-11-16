package carpet.mixins;

import carpet.fakes.MinecraftServerInterface;
import carpet.utils.CarpetProfiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(value = MinecraftServer.class, priority = Integer.MAX_VALUE - 10)
public abstract class MinecraftServer_tickspeedMixin extends ReentrantBlockableEventLoop<TickTask> implements MinecraftServerInterface
{
    public MinecraftServer_tickspeedMixin(String name)
    {
        super(name);
    }

    CarpetProfiler.ProfilerToken currentSection;

    // Replaced the above cancelled while statement with this one
    // could possibly just inject that mspt selection at the beginning of the loop, but then adding all mspt's to
    // replace 50L will be a hassle
    @Inject(method = "runServer", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/server/MinecraftServer;startMetricsRecordingTick()V"))
    private void modifiedRunLoop(CallbackInfo ci)
    {
        if (CarpetProfiler.tick_health_requested != 0L)
        {
            CarpetProfiler.start_tick_profiling();
        }
    }


    @Inject(method = "tickServer", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;saveEverything(ZZZ)Z", // save
            shift = At.Shift.BEFORE
    ))
    private void startAutosave(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        currentSection = CarpetProfiler.start_section(null, "Autosave", CarpetProfiler.TYPE.GENERAL);
    }

    @Inject(method = "tickServer", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;saveEverything(ZZZ)Z",
            shift = At.Shift.AFTER
    ))
    private void finishAutosave(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        CarpetProfiler.end_current_section(currentSection);
    }

    @Inject(method = "tickChildren", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;getConnection()Lnet/minecraft/server/network/ServerConnectionListener;",
            shift = At.Shift.BEFORE
    ))
    private void startNetwork(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        currentSection = CarpetProfiler.start_section(null, "Network", CarpetProfiler.TYPE.GENERAL);
    }

    @Inject(method = "tickChildren", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;tick()V",
            shift = At.Shift.AFTER
    ))
    private void finishNetwork(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        CarpetProfiler.end_current_section(currentSection);
    }

    @Inject(method = "waitUntilNextTick", at = @At("HEAD"))
    private void startAsync(CallbackInfo ci)
    {
        currentSection = CarpetProfiler.start_section(null, "Async Tasks", CarpetProfiler.TYPE.GENERAL);
    }
    @Inject(method = "waitUntilNextTick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;managedBlock(Ljava/util/function/BooleanSupplier;)V",
            shift = At.Shift.BEFORE
    ))
    private void stopAsync(CallbackInfo ci)
    {
        if (CarpetProfiler.tick_health_requested != 0L)
        {
            CarpetProfiler.end_current_section(currentSection);
            CarpetProfiler.end_tick_profiling((MinecraftServer) (Object)this);
        }
    }


}
