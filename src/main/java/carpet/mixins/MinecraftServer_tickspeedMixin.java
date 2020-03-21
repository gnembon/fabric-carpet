package carpet.mixins;

import carpet.helpers.TickSpeed;
import carpet.utils.CarpetProfiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.util.Util;
import net.minecraft.util.profiler.DisableableProfiler;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServer_tickspeedMixin extends ReentrantThreadExecutor<ServerTask>
{
    @Shadow private volatile boolean running;

    @Shadow private long timeReference;

    @Shadow private long field_4557;

    @Shadow @Final private static Logger LOGGER;

    @Shadow private boolean profilerStartQueued;

    @Shadow @Final private DisableableProfiler profiler;

    public MinecraftServer_tickspeedMixin(String name)
    {
        super(name);
    }

    @Shadow protected abstract void tick(BooleanSupplier booleanSupplier_1);

    @Shadow protected abstract boolean shouldKeepTicking();

    @Shadow private boolean field_19249;

    @Shadow private long field_19248;

    @Shadow protected abstract void method_16208();

    @Shadow private volatile boolean loading;

    CarpetProfiler.ProfilerToken currentSection;

    private float carpetMsptAccum = 0.0f;

    /**
     * To ensure compatibility with other mods we should allow milliseconds
     */

    // Cancel a while statement
    @Redirect(method = "run", at = @At(value = "FIELD", target = "Lnet/minecraft/server/MinecraftServer;running:Z"))
    private boolean cancelRunLoop(MinecraftServer server)
    {
        return false;
    }

    // Replaced the above cancelled while statement with this one
    // could possibly just inject that mspt selection at the beginning of the loop, but then adding all mspt's to
    // replace 50L will be a hassle
    @Inject(method = "run", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/server/MinecraftServer;setFavicon(Lnet/minecraft/server/ServerMetadata;)V"))
    private void modifiedRunLoop(CallbackInfo ci)
    {
        while (this.running)
        {
            //long long_1 = Util.getMeasuringTimeMs() - this.timeReference;
            //CM deciding on tick speed
            if (CarpetProfiler.tick_health_requested != 0L)
            {
                CarpetProfiler.start_tick_profiling();
            }
            long msThisTick = 0L;
            long long_1 = 0L;
            if (TickSpeed.time_warp_start_time != 0 && TickSpeed.continueWarp())
            {
                //making sure server won't flop after the warp or if the warp is interrupted
                this.timeReference = this.field_4557 = Util.getMeasuringTimeMs();
                carpetMsptAccum = TickSpeed.mspt;
            }
            else
            {
                if (Math.abs(carpetMsptAccum - TickSpeed.mspt) > 1.0f)
                {
                	// Tickrate changed. Ensure that we use the correct value.
                	carpetMsptAccum = TickSpeed.mspt;
                }

                msThisTick = (long)carpetMsptAccum; // regular tick
                carpetMsptAccum += TickSpeed.mspt - msThisTick;

                long_1 = Util.getMeasuringTimeMs() - this.timeReference;
            }
            //end tick deciding
            //smoothed out delay to include mcpt component. With 50L gives defaults.
            if (long_1 > /*2000L*/1000L+20*TickSpeed.mspt && this.timeReference - this.field_4557 >= /*15000L*/10000L+100*TickSpeed.mspt)
            {
                long long_2 = (long)(long_1 / TickSpeed.mspt);//50L;
                LOGGER.warn("Can't keep up! Is the server overloaded? Running {}ms or {} ticks behind", long_1, long_2);
                this.timeReference += (long)(long_2 * TickSpeed.mspt);//50L;
                this.field_4557 = this.timeReference;
            }

            this.timeReference += msThisTick;//50L;
            if (this.profilerStartQueued)
            {
                this.profilerStartQueued = false;
                this.profiler.getController().enable();
            }

            this.profiler.startTick();
            this.profiler.push("tick");
            this.tick(TickSpeed.time_warp_start_time != 0 ? ()->true : this::shouldKeepTicking);
            this.profiler.swap("nextTickWait");
            this.field_19249 = true;
            this.field_19248 = Math.max(Util.getMeasuringTimeMs() + /*50L*/ msThisTick, this.timeReference);
            if (TickSpeed.time_warp_start_time != 0)
            {
                runTasks();
                runTasks(() -> !hasRunningTasks() );
            }
            else { this.method_16208(); }
            this.profiler.pop();
            this.profiler.endTick();
            this.loading = true;
        }

    }

    @Inject(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/PlayerManager;saveAllPlayerData()V",
            shift = At.Shift.BEFORE
    ))
    private void startAutosave(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        currentSection = CarpetProfiler.start_section(null, "Autosave", CarpetProfiler.TYPE.GENERAL);
    }

    @Inject(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;save(ZZZ)Z",
            shift = At.Shift.AFTER
    ))
    private void finishAutosave(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        CarpetProfiler.end_current_section(currentSection);
    }

    @Inject(method = "tickWorlds", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;getNetworkIo()Lnet/minecraft/server/ServerNetworkIo;",
            shift = At.Shift.BEFORE
    ))
    private void startNetwork(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        currentSection = CarpetProfiler.start_section(null, "Network", CarpetProfiler.TYPE.GENERAL);
    }

    @Inject(method = "tickWorlds", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/PlayerManager;updatePlayerLatency()V",
            shift = At.Shift.AFTER
    ))
    private void finishNetwork(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        CarpetProfiler.end_current_section(currentSection);
    }

    @Inject(method = "method_16208", at = @At("HEAD"))
    private void startAsync(CallbackInfo ci)
    {
        currentSection = CarpetProfiler.start_section(null, "Async Tasks", CarpetProfiler.TYPE.GENERAL);
    }
    @Inject(method = "method_16208", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;runTasks(Ljava/util/function/BooleanSupplier;)V",
            shift = At.Shift.BEFORE
    ))
    private void stopAsync(CallbackInfo ci)
    {
        if (CarpetProfiler.tick_health_requested != 0L)
        {
            CarpetProfiler.end_tick_profiling((MinecraftServer) (Object)this, currentSection);
        }
    }


}
