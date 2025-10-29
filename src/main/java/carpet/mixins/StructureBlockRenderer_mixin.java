package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityWithBoundingBoxRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityWithBoundingBoxRenderState;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityWithBoundingBoxRenderer.class)
public abstract class StructureBlockRenderer_mixin implements BlockEntityRenderer<StructureBlockEntity, BlockEntityWithBoundingBoxRenderState>
{
    @Inject(method = "getViewDistance", at = @At("HEAD"), cancellable = true)
    void newLimit(CallbackInfoReturnable<Integer> cir)
    {
        if (CarpetSettings.structureBlockOutlineDistance != 96)
            cir.setReturnValue(CarpetSettings.structureBlockOutlineDistance);
    }
}
