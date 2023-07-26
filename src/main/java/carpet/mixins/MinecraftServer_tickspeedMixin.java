package carpet.mixins;

import carpet.fakes.MinecraftServerInterface;
import carpet.helpers.ServerTickRateManager;
import carpet.utils.CarpetProfiler;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Debug(export = true)
@Mixin(value = MinecraftServer.class, priority = Integer.MAX_VALUE - 10)
public abstract class MinecraftServer_tickspeedMixin extends ReentrantBlockableEventLoop<TickTask> implements MinecraftServerInterface
{
    @Shadow private long nextTickTime;

    public MinecraftServer_tickspeedMixin(String name)
    {
        super(name);
    }

    @Shadow private long lastOverloadWarning;

    @Shadow public abstract Iterable<ServerLevel> getAllLevels();

    CarpetProfiler.ProfilerToken currentSection;

    private float carpetMsptAccum = 0.0f;

    private ServerTickRateManager serverTickRateManager;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci)
    {
        serverTickRateManager = new ServerTickRateManager((MinecraftServer)(Object)this);
    }

    @Override
    public ServerTickRateManager getTickRateManager()
    {
        return serverTickRateManager;
    }

    /**
     * To ensure compatibility with other mods we should allow milliseconds
     */
    
    // smoothed out delay to include mspt component. With 50L gives defaults.
    
    @ModifyConstant(method = "runServer", constant = @Constant(longValue = 2000L), allow = 1)
    private long modifyCantKeepUpLengthCheck(long original) {
    	return (long)(1000L + 20 * serverTickRateManager.mspt());
    }

    @ModifyConstant(method = "runServer", constant = @Constant(longValue = 15000L), allow = 1)
    private long modifyCantKeepUpRecentCheck(long original) {
    	return (long)(10000L + 100 * serverTickRateManager.mspt());
    }
    
    @ModifyConstant(method = "runServer", constant = @Constant(longValue = 50L),
    		slice = @Slice(to = @At(value = "NEW", target = "net/minecraft/server/MinecraftServer$TimeProfiler")))
    private long modifyMsptConstantWithMsptDirectly(long original) {
    	return (long)serverTickRateManager.mspt();
    }
    
    @ModifyConstant(method = "runServer", constant = @Constant(longValue = 50L),
    		slice = @Slice(from = @At(value = "NEW", target = "net/minecraft/server/MinecraftServer$TimeProfiler")))
    private long modifyMsptConstantWithMsThisTick(long original) {
    	return msThisTick; // TODO figure out why this and the previous one are different
    }
    
    private long msThisTick; // only ever accessed by same thread, ideally would've been local
    
    @Inject(method = "runServer", at = @At(value = "INVOKE", target = "net/minecraft/Util.getMillis()J", ordinal = 1))
    private void preTick(CallbackInfo ci) {
    	// CM deciding on tick speed
        if (CarpetProfiler.tick_health_requested != 0L)
        {
            CarpetProfiler.start_tick_profiling();
        }
        msThisTick = 0L;
        long l = 0L; // TODO this is to be used for cantKeepUp checks, but currently dies here
        float mspt = serverTickRateManager.mspt();
        if (serverTickRateManager.isInWarpSpeed() && serverTickRateManager.continueWarp())
        {
            // making sure server won't flop after the warp or if the warp is interrupted
            this.nextTickTime = this.lastOverloadWarning = Util.getMillis();
            carpetMsptAccum = mspt;
        }
        else
        {
            if (Math.abs(carpetMsptAccum - mspt) > 1.0f)
            {
            	// Tickrate changed. Ensure that we use the correct value.
            	carpetMsptAccum = mspt;
            }

            msThisTick = (long)carpetMsptAccum; // regular tick
            carpetMsptAccum += mspt - msThisTick;

            l = Util.getMillis() - this.nextTickTime;
        }
        // end tick deciding
    }
    
    @ModifyArg(method = "runServer", at = @At(value = "INVOKE",
    		target = "net/minecraft/server/MinecraftServer.tickServer(Ljava/util/function/BooleanSupplier;)V"))
    private BooleanSupplier alwaysRunTasksIfWarping(BooleanSupplier original) {
    	return serverTickRateManager.isInWarpSpeed() ? () -> true : original;
    }
    
    @Inject(method = "runServer", at = @At(value = "FIELD", target = "net/minecraft/server/MinecraftServer.mayHaveDelayedTasks"))
    private void runPendingTasksWhenWarping(CallbackInfo ci) {
    	// clearing all hanging tasks no matter what when warping
    	if (serverTickRateManager.isInWarpSpeed())
        {
            while (this.runEveryTask()) {
            	Thread.yield(); // TODO check
            }
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
