package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TntBlock.class)
public abstract class TntBlock_noUpdateMixin
{
    // Add carpet rule check for tntDoNotUpdate to an if statement.
    @Redirect(method = "onPlace", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;hasNeighborSignal(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean isTNTDoNotUpdate(Level world, BlockPos blockPos)
    {
        return !CarpetSettings.tntDoNotUpdate && world.hasNeighborSignal(blockPos);
    }
}
