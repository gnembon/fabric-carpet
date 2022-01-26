package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.PerfCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PerfCommand.class)
public class PerfCommand_permissionMixin
{
    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "method_37340", at = @At("HEAD"), cancellable = true)
    private static void canRun(CommandSourceStack source, CallbackInfoReturnable<Boolean> cir)
    {
        cir.setReturnValue(source.hasPermission(CarpetSettings.perfPermissionLevel));
    }

}
