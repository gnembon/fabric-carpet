package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.block.TntBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TntBlock.class)
public abstract class TntBlockMixin
{
    // Add carpet rule check for tntDoNotUpdate to an if statement.
    @Redirect(method = "onBlockAdded", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;isReceivingRedstonePower(Lnet/minecraft/util/math/BlockPos;)Z"))
    private boolean isTNTDoNotUpdate(World world, BlockPos blockPos)
    {
        return !CarpetSettings.tntDoNotUpdate && world.isReceivingRedstonePower(blockPos);
    }
}
