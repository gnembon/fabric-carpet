package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.LavaFluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LavaFluid.class)
public abstract class LavaFluid_renewableDeepslateMixin {
    @Shadow protected abstract void playExtinguishEvent(WorldAccess world, BlockPos pos);

    @Inject(method = "flow", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getDefaultState()Lnet/minecraft/block/BlockState;"), cancellable = true)
    private void generateDeepslate(WorldAccess world, BlockPos pos, BlockState state, Direction direction, FluidState fluidState, CallbackInfo ci)
    {
        if(CarpetSettings.renewableDeepslate && ((World)world).getRegistryKey() == World.OVERWORLD && pos.getY() < 16)
        {
            world.setBlockState(pos, Blocks.DEEPSLATE.getDefaultState(), 3);
            this.playExtinguishEvent(world, pos);
            ci.cancel();
        }
    }
}
