package carpet.mixins;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PistonBlockEntity.class)
public abstract class __PistonBlockEntity_honeyMixin
{
    @Shadow protected abstract Box extendBox(Box box_1, Direction direction_1, double double_1);

    @Shadow private BlockState pushedBlock;

    @Redirect(method = "pushEntities", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/BlockState;getBlock()Lnet/minecraft/block/Block;"
    ))
    private Block getNotSlime(BlockState blockState)
    {
        return Blocks.STONE;
    }

    @Redirect(method = "pushEntities", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/entity/PistonBlockEntity;extendBox(Lnet/minecraft/util/math/Box;Lnet/minecraft/util/math/Direction;D)Lnet/minecraft/util/math/Box;"
    ))
    private Box extendMore(PistonBlockEntity pistonBlockEntity, Box box_1, Direction direction_1, double double_1)
    {
        Box ret = extendBox(box_1, direction_1, double_1);
        if (pushedBlock.getBlock() == Blocks.SLIME_BLOCK)
            ret = ret.expand(0.5);
        return ret;
    }
}
