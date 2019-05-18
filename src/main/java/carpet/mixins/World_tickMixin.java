package carpet.mixins;

import carpet.utils.CarpetProfiler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.function.Consumer;

@Mixin(World.class)
public abstract class World_tickMixin
{
    CarpetProfiler.ProfilerToken currentSection;
    CarpetProfiler.ProfilerToken entitySection;

    @Inject(method = "tickBlockEntities", at = @At("HEAD"))
    private void startBlockEntities(CallbackInfo ci) {
        currentSection = CarpetProfiler.start_section((World) (Object) this, "Tile Entities", CarpetProfiler.TYPE.GENERAL);
    }

    @Inject(method = "tickBlockEntities", at = @At("TAIL"))
    private void endBlockEntities(CallbackInfo ci) {
        CarpetProfiler.end_current_section(currentSection);
    }

    @Inject(method = "tickBlockEntities", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/entity/BlockEntity;isInvalid()Z",
            shift = At.Shift.BEFORE,
            ordinal = 0
    ))
    private void startTileEntitySection(CallbackInfo ci, Profiler profiler_1, Iterator i, BlockEntity blockEntity_2)
    {
        entitySection = CarpetProfiler.start_entity_section((World)(Object)this, blockEntity_2, CarpetProfiler.TYPE.TILEENTITY);
    }

    @Inject(method = "tickBlockEntities", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/entity/BlockEntity;isInvalid()Z",
            shift = At.Shift.BEFORE,
            ordinal = 1
    ))
    private void endTileEntitySection(CallbackInfo ci)
    {
         CarpetProfiler.end_current_entity_section(entitySection);
    }

    @Inject(method = "tickEntity", at = @At("HEAD"))
    private void startEntity(Consumer<Entity> call, Entity e, CallbackInfo ci)
    {
        entitySection =  CarpetProfiler.start_entity_section((World) (Object) this, e, CarpetProfiler.TYPE.ENTITY);
    }

    @Inject(method = "tickEntity", at = @At("TAIL"))
    private void endEntity(Consumer<Entity> call, Entity e, CallbackInfo ci) {
        CarpetProfiler.end_current_entity_section(entitySection);
    }


}
