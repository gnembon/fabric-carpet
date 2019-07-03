package carpet.mixins;

import carpet.helpers.TickSpeed;
import carpet.utils.CarpetProfiler;
import net.minecraft.entity.raid.RaidManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.village.ZombieSiegeManager;
import net.minecraft.world.WanderingTraderManager;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelGeneratorType;
import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;

@Mixin(ServerWorld.class)
public abstract class ServerWorld_tickMixin extends World
{
    @Shadow protected abstract void sendBlockActions();

    CarpetProfiler.ProfilerToken currentSection;

    protected ServerWorld_tickMixin(LevelProperties levelProperties_1, DimensionType dimensionType_1, BiFunction<World, Dimension, ChunkManager> biFunction_1, Profiler profiler_1, boolean boolean_1)
    {
        super(levelProperties_1, dimensionType_1, biFunction_1, profiler_1, boolean_1);
    }

    @Inject(method = "tick", at = @At(
            value = "CONSTANT",
            args = "stringValue=world border"
    ))
    private void startWeatherSection(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        currentSection = CarpetProfiler.start_section((World)(Object)this, "Environment", CarpetProfiler.TYPE.GENERAL);
    }
    @Inject(method = "tick", at = @At(
            value = "CONSTANT",
            args = "stringValue=world border"
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
            currentSection = CarpetProfiler.start_section((World) (Object) this, "Blocks", CarpetProfiler.TYPE.GENERAL);
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
            currentSection = CarpetProfiler.start_section((World) (Object) this, "Village", CarpetProfiler.TYPE.GENERAL);
        }
    }

    @Inject(method = "tick", at = @At(
            value = "CONSTANT",
            args = "stringValue=tickPending"
    ))
    private void stopVillageStartBlockAgainSection(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        if (currentSection != null)
        {
            CarpetProfiler.end_current_section(currentSection);
            currentSection = CarpetProfiler.start_section((World) (Object) this, "Blocks", CarpetProfiler.TYPE.GENERAL);
        }
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
            currentSection = CarpetProfiler.start_section((World) (Object) this, "Entities", CarpetProfiler.TYPE.GENERAL);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void endEntitySection(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        CarpetProfiler.end_current_section(currentSection);
    }

    //// freeze

    @Redirect(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/border/WorldBorder;tick()V"
    ))
    private void tickWorldBorder(WorldBorder worldBorder)
    {
        if (TickSpeed.process_entities) worldBorder.tick();
    }

    @Redirect(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/dimension/Dimension;hasSkyLight()Z"
    ))
    private boolean tickWorldBorder(Dimension dimension)
    {
        return TickSpeed.process_entities && dimension.hasSkyLight();
    }

    @Redirect(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;tickTime()V"
    ))
    private void tickTimeConditionally(ServerWorld serverWorld)
    {
        if (TickSpeed.process_entities) this.tickTime();
    }

    @Redirect(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/LevelProperties;getGeneratorType()Lnet/minecraft/world/level/LevelGeneratorType;"
    ))
    private LevelGeneratorType tickPendingBlocks(LevelProperties levelProperties)
    {
        if (TickSpeed.process_entities) return levelProperties.getGeneratorType();
        return LevelGeneratorType.DEBUG_ALL_BLOCK_STATES;
    }

    @Redirect(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/raid/RaidManager;tick()V"
    ))
    private void tickConditionally(RaidManager raidManager)
    {
        if (TickSpeed.process_entities) raidManager.tick();
    }

    @Redirect(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/WanderingTraderManager;tick()V"
    ))
    private void tickConditionally(WanderingTraderManager wanderingTraderManager)
    {
        if (TickSpeed.process_entities) wanderingTraderManager.tick();
    }

    @Redirect(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;sendBlockActions()V"
    ))
    private void tickConditionally(ServerWorld serverWorld)
    {
        if (TickSpeed.process_entities) sendBlockActions();
    }


}
