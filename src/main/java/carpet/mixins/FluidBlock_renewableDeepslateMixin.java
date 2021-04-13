package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FluidBlock.class)
public abstract class FluidBlock_renewableDeepslateMixin {

    @Shadow protected abstract void playExtinguishSound(WorldAccess world, BlockPos pos);

    @Inject(method = "receiveNeighborFluids", at = @At(value = "INVOKE",target = "Lnet/minecraft/fluid/FluidState;isStill()Z"), cancellable = true)
    private void receiveFluidToDeepslate(World world, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if(CarpetSettings.renewableDeepslate && !world.getFluidState(pos).isStill() && pos.getY() < 0 && world.) {
            world.setBlockState(pos, Blocks.COBBLED_DEEPSLATE.getDefaultState());
            this.playExtinguishSound(world, pos);
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
