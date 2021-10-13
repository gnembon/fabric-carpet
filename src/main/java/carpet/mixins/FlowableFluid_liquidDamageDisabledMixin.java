package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlowableFluid.class)
public class FlowableFluid_liquidDamageDisabledMixin
{
    @Inject(
            method = "canFill",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/Material;blocksMovement()Z"
            ),
            cancellable = true
    )
    private void stopBreakingBlock(BlockView world, BlockPos pos, BlockState state, Fluid fluid, CallbackInfoReturnable<Boolean> cir)
    {
        if (CarpetSettings.liquidDamageDisabled)
        {
            Material material = state.getMaterial();
            cir.setReturnValue(material == Material.AIR || material.isLiquid());
        }
    }
}
