package carpet.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import carpet.CarpetSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;

@Mixin(BlockStateArgument.class)
public class BlockStateArgument_fillUpdatesMixin
{
    @Redirect(method = "setBlockState", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;postProcessState(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"
    ))
    private BlockState postProcessStateProxy(BlockState state, WorldAccess serverWorld, BlockPos blockPos)
    {
        if (CarpetSettings.impendingFillSkipUpdates)
        {
            return state;
        }
        
        return Block.postProcessState(state, serverWorld, blockPos);
    }
}
