package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlowingFluid.class)
public class FlowingFluid_liquidDamageDisabledMixin
{
    @Inject(
            method = "canHoldAnyFluid",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;blocksMotion()Z"
            ),
            cancellable = true
    )
    private static void stopBreakingBlock(final BlockState state, final CallbackInfoReturnable<Boolean> cir)
    {
        if (CarpetSettings.liquidDamageDisabled)
        {
            cir.setReturnValue(state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.LAVA));
        }
    }
}
