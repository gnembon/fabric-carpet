package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.entity.StructureBlockBlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.StructureBlockBlockEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StructureBlockBlockEntityRenderer.class)
public abstract class StructureBlockBlockEntityRendererMixin implements BlockEntityRenderer<StructureBlockBlockEntity>
{
    @Inject(method = "getRenderDistance", at = @At("HEAD"), cancellable = true)
    void newLimit(CallbackInfoReturnable<Integer> cir)
    {
        if (CarpetSettings.structureBlockOutlineDistance != 96)
            cir.setReturnValue(CarpetSettings.structureBlockOutlineDistance);
    }
}
