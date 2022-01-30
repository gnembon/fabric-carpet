package carpet.mixins;

import carpet.helpers.TickSpeed;
import carpet.utils.CarpetProfiler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public class BoundTickingBlockEntity_tickMixin<T extends BlockEntity>
{
    @Shadow @Final private T blockEntity;
    CarpetProfiler.ProfilerToken entitySection;


    @Inject(method = "tick()V", at = @At("HEAD"))
    private void startTileEntitySection(CallbackInfo ci)
    {
        entitySection = CarpetProfiler.start_block_entity_section(blockEntity.getLevel(), blockEntity, CarpetProfiler.TYPE.TILEENTITY);
    }

    @Redirect(method = "tick()V", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/BlockEntityTicker;tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"
    ))
    private void checkProcessTEs(BlockEntityTicker blockEntityTicker, Level world, BlockPos pos, BlockState state, T blockEntity)
    {
        if (TickSpeed.process_entities) blockEntityTicker.tick(world, pos, state, blockEntity);
    }

    @Inject(method = "tick()V", at = @At("RETURN"))
    private void endTileEntitySection(CallbackInfo ci)
    {
        CarpetProfiler.end_current_entity_section(entitySection);
    }

}
