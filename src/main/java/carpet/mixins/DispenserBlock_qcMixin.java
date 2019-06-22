package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.block.DispenserBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DispenserBlock.class)
public class DispenserBlock_qcMixin
{
    @Redirect(method = "neighborUpdate", at = @At(
            value = "INVOKE",
            target =  "Lnet/minecraft/world/World;isReceivingRedstonePower(Lnet/minecraft/util/math/BlockPos;)Z",
            ordinal = 1
    ))
    private boolean checkUpPower(World world, BlockPos blockPos_1)
    {
        if (!CarpetSettings.quasiConnectivity)
            return false;
        return world.isReceivingRedstonePower(blockPos_1);
    }
}
