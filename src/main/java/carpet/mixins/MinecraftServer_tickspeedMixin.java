package carpet.mixins;

import carpet.helpers.TickSpeed;
import carpet.patches.CopyProfilerResult;
import carpet.utils.CarpetProfiler;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.EmptyProfileResults;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServer_tickspeedMixin extends ReentrantBlockableEventLoop<TickTask>
{
    @Shadow private volatile boolean running;

    @Shadow private long nextTickTime;

    @Shadow @Final private static Logger LOGGER;

    @Shadow private ProfilerFiller profiler;

    public MinecraftServer_tickspeedMixin(String name)
    {
        super(name);
    }

    @Shadow protected abstract void tickServer(BooleanSupplier booleanSupplier_1);

    @Shadow protected abstract boolean haveTime();

    @Shadow private long delayedTasksMaxNextTickTime;

    @Shadow private volatile boolean isReady;

    @Shadow private long lastOverloadWarning;

    @Shadow private boolean mayHaveDelayedTasks;

    @Shadow public abstract Iterable<ServerLevel> getAllLevels();

    @Shadow private int tickCount;

    @Shadow protected abstract void waitUntilNextTick();

    @Shadow protected abstract void startMetricsRecordingTick();

    @Shadow protected abstract void endMetricsRecordingTick();

    @Shadow private boolean debugCommandProfilerDelayStart;
    CarpetProfiler.ProfilerToken currentSection;

    private float carpetMsptAccum = 0.0f;

    /**
     * To ensure compatibility with other mods we should allow milliseconds
     */

    // Cancel a while statement
    @Redirect(method = "runServer", at = @At(value = "FIELD", target = "Lnet/minecraft/server/MinecraftServer;running:Z"))
    private boolean cancelRunLoop(MinecraftServer server)
    {
        return false;
    } // target run()

    // Replaced the above cancelled while statement with this one
    // could possibly just inject that mspt selection at the beginning of the loop, but then adding all mspt's to
    // replace 50L will be a hassle
    @Inject(method = "runServer", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/server/MinecraftServer;updateStatusIcon(Lnet/minecraft/network/protocol/status/ServerStatus;)V"))
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
                this.nextTickTime = this.lastOverloadWarning = Util.getMillis();
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

                long_1 = Util.getMillis() - this.nextTickTime;
            }
            //end tick deciding
            //smoothed out delay to include mcpt component. With 50L gives defaults.
            if (long_1 > /*2000L*/1000L+20*TickSpeed.mspt && this.nextTickTime - this.lastOverloadWarning >= /*15000L*/10000L+100*TickSpeed.mspt)
            {
                long long_2 = (long)(long_1 / TickSpeed.mspt);//50L;
                LOGGER.warn("Can't keep up! Is the server overloaded? Running {}ms or {} ticks behind", long_1, long_2);
                this.nextTickTime += (long)(long_2 * TickSpeed.mspt);//50L;
                this.lastOverloadWarning = this.nextTickTime;
            }

            if (this.debugCommandProfilerDelayStart) {
                this.debugCommandProfilerDelayStart = false;
                this.profilerTimings = Pair.of(Util.getNanos(), tickCount);
                //this.field_33978 = new MinecraftServer.class_6414(Util.getMeasuringTimeNano(), this.ticks);
            }
            this.nextTickTime += msThisTick;//50L;
            //TickDurationMonitor tickDurationMonitor = TickDurationMonitor.create("Server");
            //this.startMonitor(tickDurationMonitor);
            this.startMetricsRecordingTick();
            this.profiler.push("tick");
            this.tickServer(TickSpeed.time_warp_start_time != 0 ? ()->true : this::haveTime);
            this.profiler.popPush("nextTickWait");
            if (TickSpeed.time_warp_start_time != 0) // clearing all hanging tasks no matter what when warping
            {
                while(this.runEveryTask()) {Thread.yield();}
            }
            this.mayHaveDelayedTasks = true;
            this.delayedTasksMaxNextTickTime = Math.max(Util.getMillis() + /*50L*/ msThisTick, this.nextTickTime);
            // run all tasks (this will not do a lot when warping), but that's fine since we already run them
            this.waitUntilNextTick();
            this.profiler.pop();
            this.endMetricsRecordingTick();
            this.isReady = true;
        }

    }

    // just because profilerTimings class is public
    Pair<Long,Integer> profilerTimings = null;
    /// overworld around profiler timings
    @Inject(method = "isTimeProfilerRunning", at = @At("HEAD"), cancellable = true)
    public void isCMDebugRunning(CallbackInfoReturnable<Boolean> cir)
    {
        cir.setReturnValue(debugCommandProfilerDelayStart || profilerTimings != null);
    }
    @Inject(method = "stopTimeProfiler", at = @At("HEAD"), cancellable = true)
    public void stopCMDebug(CallbackInfoReturnable<ProfileResults> cir)
    {
        if (this.profilerTimings == null) {
            cir.setReturnValue(EmptyProfileResults.EMPTY);
        } else {
            ProfileResults profileResult = new CopyProfilerResult(
                    profilerTimings.getRight(), profilerTimings.getLeft(),
                    this.tickCount, Util.getNanos()
            );
            this.profilerTimings = null;
            cir.setReturnValue(profileResult);
        }
    }


    private boolean runEveryTask() {
        if (super.pollTask()) {
            return true;
        } else {
            if (true) { // unconditionally this time
                for(ServerLevel serverlevel : getAllLevels()) {
                    if (serverlevel.getChunkSource().pollTask()) {
                        return true;
                    }
                }
            }

            return false;
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
