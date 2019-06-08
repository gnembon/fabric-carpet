package carpet.mixins;

import carpet.helpers.BlockRotator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemUsageContext.class)
public class ItemUsageContext_cactusMixin
{
    @Redirect(method = "getPlayerFacing", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;getHorizontalFacing()Lnet/minecraft/util/math/Direction;"
    ))
    private Direction getPlayerFacing(PlayerEntity playerEntity)
    {
        Direction dir = playerEntity.getHorizontalFacing();
        if (BlockRotator.flippinEligibility(playerEntity))
        {
            dir = dir.getOpposite();
        }
        return dir;
    }
}
