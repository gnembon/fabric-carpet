package carpet.mixins;

import carpet.helpers.TickSpeed;
import carpet.utils.CarpetProfiler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.class_5558;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.chunk.WorldChunk$class_5563")
public class  WorldChunkBEList_tickMixin<T extends BlockEntity>
{
    @Shadow @Final private T field_27224;
    CarpetProfiler.ProfilerToken entitySection;


    @Inject(method = "method_31703", at = @At("HEAD"))
    private void startTileEntitySection(CallbackInfo ci)
    {
        entitySection = CarpetProfiler.start_block_entity_section(field_27224.getWorld(), field_27224, CarpetProfiler.TYPE.TILEENTITY);
    }

    @Redirect(method = "method_31703", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_5558;tick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/BlockEntity;)V"
    ))
    private void checkProcessTEs(class_5558 class_5558, World world, BlockPos blockPos, BlockState blockState, T blockEntity)
    {
        //return class_5562.method_31704() || !TickSpeed.process_entities; // blockEntity can be NULL? happened once with fake player
        if (TickSpeed.process_entities) class_5558.tick(world, blockPos, blockState, blockEntity);
    }

    @Inject(method = "method_31703", at = @At("RETURN"))
    private void endTileEntitySection(CallbackInfo ci)
    {
        CarpetProfiler.end_current_entity_section(entitySection);
    }

}
