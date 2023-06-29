package carpet.mixins;

import carpet.CarpetSettings;
import carpet.helpers.FertilizableCoral;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.level.block.CoralPlantBlock;

@Mixin(CoralPlantBlock.class)
public class CoralPlantBlock_renewableCoralMixin implements FertilizableCoral
{
    @Override
    public boolean isEnabled() {
        return CarpetSettings.renewableCoral == CarpetSettings.RenewableCoralMode.EXPANDED
                || CarpetSettings.renewableCoral == CarpetSettings.RenewableCoralMode.TRUE;
    }
    // Logic in FertilizableCoral
}
