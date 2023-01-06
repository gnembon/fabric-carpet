package carpet.mixins;

import carpet.CarpetSettings;
import carpet.helpers.FertilizableCoral;
import net.minecraft.world.level.block.CoralFanBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CoralFanBlock.class)
public class CoralFanBlock_renewableCoralMixin implements FertilizableCoral
{
    @Override
    public boolean isEnabled() {
        return CarpetSettings.renewableCoral == CarpetSettings.RenewableCoralMode.EXPANDED;
    }
    // Logic in FertilizableCoral
}
