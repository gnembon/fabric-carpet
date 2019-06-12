package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.block.PoweredRailBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(PoweredRailBlock.class)
public class PoweredRailBlock_powerLimitMixin
{
    @ModifyConstant(method = "isPoweredByOtherRails(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;ZI)Z",
            constant = @Constant(intValue = 8))
    private int powerLimit(int original)
    {
        return CarpetSettings.railPowerLimit-1;
    }
}
