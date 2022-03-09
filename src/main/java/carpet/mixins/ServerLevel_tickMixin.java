package carpet.mixins;

import carpet.helpers.TickSpeed;
import carpet.utils.CarpetProfiler;
import net.minecraft.core.Holder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.WrapWithCondition;

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
    protected ServerLevel_tickMixin(WritableLevelData properties, ResourceKey<Level> registryKey, Holder<DimensionType> dimensionType, Supplier<ProfilerFiller> supplier, boolean bl, boolean bl2, long l)
    {
        super(properties, registryKey, dimensionType, supplier, bl, bl2, l);
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
            args = "stringValue=chunkSource"
    ))
    private void stopWeatherStartChunkSection(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        if (currentSection != null)
        {
            CarpetProfiler.end_current_section(currentSection);
            // we go deeper here
        }
    }
    @Inject(method = "tick", at = @At(
            value = "CONSTANT",
            args = "stringValue=tickPending"
    ))
    private void stopChunkStartBlockSection(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        if (currentSection != null)
        {
            // out of chunk
            currentSection = CarpetProfiler.start_section((Level) (Object) this, "Blocks", CarpetProfiler.TYPE.GENERAL);
        }
    }

    @Inject(method = "tick", at = @At(
            value = "CONSTANT",
            args = "stringValue=raid"
    ))
    private void stopBlockStartVillageSection(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        if (currentSection != null)
        {
            CarpetProfiler.end_current_section(currentSection);
            currentSection = CarpetProfiler.start_section((Level) (Object) this, "Village", CarpetProfiler.TYPE.GENERAL);
        }
    }
    @Inject(method = "tick", at = @At(
            value = "CONSTANT",
            args = "stringValue=chunkSource"
    ))
    private void stopVillageSection(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        if (currentSection != null)
        {
            CarpetProfiler.end_current_section(currentSection);
            currentSection = null;
        }
    }

    @Inject(method = "tick", at = @At(
            value = "CONSTANT",
            args = "stringValue=blockEvents"
    ))
    private void startBlockAgainSection(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        currentSection = CarpetProfiler.start_section((Level) (Object) this, "Blocks", CarpetProfiler.TYPE.GENERAL);
    }

    @Inject(method = "tick", at = @At(
            value = "CONSTANT",
            args = "stringValue=entities"
    ))
    private void stopBlockAgainStartEntitySection(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
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
    }

    //// freeze

    @WrapWithCondition(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/border/WorldBorder;tick()V"
    ))
    private boolean shouldTickWorldBorder(WorldBorder worldBorder)
    {
        return TickSpeed.process_entities;
    }

    @Inject(method = "advanceWeatherCycle", cancellable = true, at = @At("HEAD"))
    private void tickWeather(CallbackInfo ci)
    {
        if (!TickSpeed.process_entities) ci.cancel();
    }

    @WrapWithCondition(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;tickTime()V"
    ))
    private boolean shouldTickTime(ServerLevel level)
    {
        return TickSpeed.process_entities;
    }

    @ModifyExpressionValue(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;isDebug()Z"
    ))
    private boolean shouldNOTtickPendingBlocks(boolean original)
    {
        return original || !TickSpeed.process_entities;
    }

    @WrapWithCondition(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/raid/Raids;tick()V"
    ))
    private boolean shouldTickRaids(Raids raidManager)
    {
        return TickSpeed.process_entities;
    }

    @WrapWithCondition(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;runBlockEvents()V"
    ))
    private boolean shouldTickBlockEvents(ServerLevel serverWorld)
    {
        return TickSpeed.process_entities;
    }
}
