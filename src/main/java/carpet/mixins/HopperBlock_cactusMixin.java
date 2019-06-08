package carpet.mixins;

import carpet.helpers.BlockRotator;
import net.minecraft.block.HopperBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HopperBlock.class)
public class HopperBlock_cactusMixin
{
    @Redirect(method = "getPlacementState", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemPlacementContext;getSide()Lnet/minecraft/util/math/Direction;"
    ))
    private Direction getOppositeOpposite(ItemPlacementContext context)
    {
        if (BlockRotator.flippinEligibility(context.getPlayer()))
        {
            return context.getSide().getOpposite();
        }
        return context.getSide();
    }
}
