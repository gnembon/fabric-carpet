package carpet.mixins;

import carpet.helpers.TickSpeed;
import carpet.utils.CarpetProfiler;
import net.minecraft.core.Holder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;

@Mixin(ServerLevel.class)
public abstract class ServerLevel_tickMixin extends Level
{


    protected ServerLevel_tickMixin(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, Holder<DimensionType> holder, Supplier<ProfilerFiller> supplier, boolean bl, boolean bl2, long l, int i)
    {
        super(writableLevelData, resourceKey, holder, supplier, bl, bl2, l, i);
    }

    @Shadow protected abstract void runBlockEvents();

    @Shadow protected abstract void tickTime();

    private CarpetProfiler.ProfilerToken currentSection;

    @Inject(method = "tick", at = @At(
            value = "CONSTANT",
            args = "stringValue=weather"
    ))
    private void startWeatherSection(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        currentSection = CarpetProfiler.start_section((Level)(Object)this, "Environment", CarpetProfiler.TYPE.GENERAL);
    }
    @Inject(method = "tick", at = @At(
            value = "CONSTANT",
            args = "stringValue=tickPending"
    ))
    private void stopWeatherStartTileTicks(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        if (currentSection != null)
        {
            CarpetProfiler.end_current_section(currentSection);
            currentSection = CarpetProfiler.start_section((Level) (Object) this, "Schedule Ticks", CarpetProfiler.TYPE.GENERAL);
        }
    }
    @Inject(method = "tick", at = @At(
            value = "CONSTANT",
            args = "stringValue=raid"
    ))
    private void stopTileTicksStartRaid(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        if (currentSection != null)
        {
            CarpetProfiler.end_current_section(currentSection);
            currentSection = CarpetProfiler.start_section((Level) (Object) this, "Raid", CarpetProfiler.TYPE.GENERAL);
        }
    }

    @Inject(method = "tick", at = @At(
            value = "CONSTANT",
            args = "stringValue=chunkSource"
    ))
    private void stopRaid(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        if (currentSection != null)
        {
            CarpetProfiler.end_current_section(currentSection);
        }
    }
    @Inject(method = "tick", at = @At(
            value = "CONSTANT",
            args = "stringValue=blockEvents"
    ))
    private void startBlockEvents(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        currentSection = CarpetProfiler.start_section((Level) (Object) this, "Block Events", CarpetProfiler.TYPE.GENERAL);
    }

    @Inject(method = "tick", at = @At(
            value = "CONSTANT",
            args = "stringValue=entities"
    ))
    private void stopBlockEventsStartEntitySection(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        if (currentSection != null)
        {
            CarpetProfiler.end_current_section(currentSection);
            currentSection = CarpetProfiler.start_section((Level) (Object) this, "Entities", CarpetProfiler.TYPE.GENERAL);
        }
    }

    @Inject(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;tickBlockEntities()V",
            shift = At.Shift.BEFORE
    ))
    private void endEntitySection(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        CarpetProfiler.end_current_section(currentSection);
        currentSection = null;
    }

    // Chunk

    @Inject(method = "tickChunk", at = @At("HEAD"))
    private void startThunderSpawningSection(CallbackInfo ci) {
        // Counting it in spawning because it's spawning skeleton horses
        currentSection = CarpetProfiler.start_section((Level) (Object) this, "Spawning", CarpetProfiler.TYPE.GENERAL);
    }

    @Inject(method = "tickChunk", at = @At(
            value = "CONSTANT",
            args = "stringValue=iceandsnow"
    ))
    private void endThunderSpawningAndStartIceSnowRandomTicks(CallbackInfo ci) {
        if (currentSection != null) {
            CarpetProfiler.end_current_section(currentSection);
            currentSection = CarpetProfiler.start_section((Level) (Object) this, "Environment", CarpetProfiler.TYPE.GENERAL);
        }
    }

    @Inject(method = "tickChunk", at = @At(
            value = "CONSTANT",
            args = "stringValue=tickBlocks"
    ))
    private void endIceAndSnowAndStartRandomTicks(CallbackInfo ci) {
        if (currentSection != null) {
            CarpetProfiler.end_current_section(currentSection);
            currentSection = CarpetProfiler.start_section((Level) (Object) this, "Random Ticks", CarpetProfiler.TYPE.GENERAL);
        }
    }

    @Inject(method = "tickChunk", at = @At("RETURN"))
    private void endRandomTicks(CallbackInfo ci) {
        if (currentSection != null) {
            CarpetProfiler.end_current_section(currentSection);
            currentSection = null;
        }
    }

    //// freeze

    @Redirect(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/border/WorldBorder;tick()V"
    ))
    private void tickWorldBorder(WorldBorder worldBorder)
    {
        if (TickSpeed.process_entities) worldBorder.tick();
    }

    @Inject(method = "advanceWeatherCycle", cancellable = true, at = @At("HEAD"))
    private void tickWeather(CallbackInfo ci)
    {
        if (!TickSpeed.process_entities) ci.cancel();
    }

    @Redirect(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;tickTime()V"
    ))
    private void tickTimeConditionally(ServerLevel serverWorld)
    {
        if (TickSpeed.process_entities) tickTime();
    }

    @Redirect(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;isDebug()Z"
    ))
    private boolean tickPendingBlocks(ServerLevel serverWorld)
    {
        if (!TickSpeed.process_entities) return true;
        return serverWorld.isDebug(); // isDebug()
    }

    @Redirect(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/raid/Raids;tick()V"
    ))
    private void tickConditionally(Raids raidManager)
    {
        if (TickSpeed.process_entities) raidManager.tick();
    }

    @Redirect(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;runBlockEvents()V"
    ))
    private void tickConditionally(ServerLevel serverWorld)
    {
        if (TickSpeed.process_entities) runBlockEvents();
    }
}
