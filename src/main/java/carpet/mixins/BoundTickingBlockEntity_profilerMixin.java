package carpet.mixins;

import carpet.utils.CarpetProfiler;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public class BoundTickingBlockEntity_profilerMixin<T extends BlockEntity>
{
    @Shadow @Final private T blockEntity;
    CarpetProfiler.ProfilerToken entitySection;

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void startTileEntitySection(CallbackInfo ci)
    {
        entitySection = CarpetProfiler.start_block_entity_section(blockEntity.getLevel(), blockEntity, CarpetProfiler.TYPE.TILEENTITY);
    }

    @Inject(method = "tick()V", at = @At("RETURN"))
    private void endTileEntitySection(CallbackInfo ci)
    {
        CarpetProfiler.end_current_entity_section(entitySection);
    }
}
