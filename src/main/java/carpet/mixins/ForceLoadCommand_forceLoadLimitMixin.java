package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.server.command.ForceLoadCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ForceLoadCommand.class)
public class ForceLoadCommand_forceLoadLimitMixin
{
    @ModifyConstant(method = "executeChange", constant = @Constant(longValue = 256L))
    private static long forceloadLimit(long original)
    {
        return CarpetSettings.forceloadLimit;
    }

    @ModifyConstant(method = "executeChange", constant = @Constant(intValue = 256))
    private static int forceloadLimitError(int original)
    {
        return CarpetSettings.forceloadLimit;
    }
}
