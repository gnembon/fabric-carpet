package carpet.mixins;

import carpet.utils.CarpetProfiler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;

@Mixin(World.class)
public abstract class World_tickMixin
{
    CarpetProfiler.ProfilerToken beToken;

    @Inject(method = "tickBlockEntities", at = @At("HEAD"))
    private void startBlockEntities(CallbackInfo ci) {
        CarpetProfiler.start_section((World) (Object) this, "Tile Entities");
    }

    @Inject(method = "tickBlockEntities", at = @At("TAIL"))
    private void endBlockEntities(CallbackInfo ci) {
        CarpetProfiler.end_current_section();
    }

    @Inject(method = "tickBlockEntities", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/entity/BlockEntity;isInvalid()Z",
            shift = At.Shift.BEFORE,
            ordinal = 0
    ))
    private void startTileEntitySection(CallbackInfo ci, Profiler profiler_1, int int_1, BlockEntity blockEntity_2)
    {
        beToken = CarpetProfiler.start_tileentity_section((World)(Object)this, blockEntity_2);
    }

    @Inject(method = "tickBlockEntities", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/entity/BlockEntity;isInvalid()Z",
            shift = At.Shift.BEFORE,
            ordinal = 1
    ))
    private void endTileEntitySection(CallbackInfo ci)
    {
         CarpetProfiler.end_current_entity_section(beToken);
         beToken = null;
    }


}
