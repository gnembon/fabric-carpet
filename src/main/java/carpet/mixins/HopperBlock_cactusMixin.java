package carpet.mixins;

import carpet.helpers.BlockRotator;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.HopperBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HopperBlock.class)
public class HopperBlock_cactusMixin
{
    @Redirect(method = "getStateForPlacement", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/context/BlockPlaceContext;getClickedFace()Lnet/minecraft/core/Direction;"
    ))
    private Direction getOppositeOpposite(BlockPlaceContext context)
    {
        if (BlockRotator.flippinEligibility(context.getPlayer()))
        {
            return context.getClickedFace().getOpposite();
        }
        return context.getClickedFace();
    }
}
