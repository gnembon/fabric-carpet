package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.world.level.block.PoweredRailBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(PoweredRailBlock.class)
public class PoweredRailBlock_powerLimitMixin
{
    @ModifyConstant(method = "findPoweredRailSignal(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;ZI)Z",
            constant = @Constant(intValue = 8))
    private int powerLimit(int original)
    {
        return CarpetSettings.railPowerLimit-1;
    }
}
