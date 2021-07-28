package carpet.mixins;

import carpet.fakes.WorldInterface;
import carpet.helpers.TickSpeed;
import carpet.utils.CarpetProfiler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(World.class)
public abstract class World_tickMixin implements WorldInterface
{
    @Shadow @Final public boolean isClient;
    CarpetProfiler.ProfilerToken currentSection;
    CarpetProfiler.ProfilerToken entitySection;

    Map<EntityType, Entity> precookedMobs = new HashMap<>();

    public Map<EntityType, Entity> getPrecookedMobs()
    {
        return precookedMobs;
    }

    @Inject(method = "tickBlockEntities", at = @At("HEAD"))
    private void startBlockEntities(CallbackInfo ci) {
        currentSection = CarpetProfiler.start_section((World) (Object) this, "Block Entities", CarpetProfiler.TYPE.GENERAL);
    }

    @Inject(method = "tickBlockEntities", at = @At("TAIL"))
    private void endBlockEntities(CallbackInfo ci) {
        CarpetProfiler.end_current_section(currentSection);
    }
/*
    @Inject(method = "tickBlockEntities", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_5562;method_31704()Z",
            shift = At.Shift.BEFORE,
            ordinal = 0
    ))
    private void startTileEntitySection(CallbackInfo ci, Profiler profiler_1, Iterator i, class_5562 lv)
    {
        entitySection = CarpetProfiler.start_block_entity_section((World)(Object)this, (BlockEntity) lv, CarpetProfiler.TYPE.TILEENTITY);
    }

    @Redirect(method = "tickBlockEntities", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_5562;method_31704()Z",
            ordinal = 0
    ))   // isRemoved()
    private boolean checkProcessTEs(class_5562 class_5562)
    {
        return class_5562.method_31704() || !TickSpeed.process_entities; // blockEntity can be NULL? happened once with fake player
    }

    @Inject(method = "tickBlockEntities", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_5562;method_31703()V",
            shift = At.Shift.AFTER,
            ordinal = 0
    ))
    private void endTileEntitySection(CallbackInfo ci)
    {
         CarpetProfiler.end_current_entity_section(entitySection);
    }
*/
    @Inject(method = "tickEntity", at = @At("HEAD"), cancellable = true)
    private void startEntity(Consumer<Entity> consumer_1, Entity e, CallbackInfo ci)
    {
        if (!(TickSpeed.process_entities || (e instanceof PlayerEntity) || (TickSpeed.is_superHot && isClient && e.getPrimaryPassenger() instanceof PlayerEntity)))
            ci.cancel();
        entitySection =  CarpetProfiler.start_entity_section((World) (Object) this, e, CarpetProfiler.TYPE.ENTITY);
    }

    @Inject(method = "tickEntity", at = @At("TAIL"))
    private void endEntity(Consumer<Entity> call, Entity e, CallbackInfo ci) {
        CarpetProfiler.end_current_entity_section(entitySection);
    }


}
