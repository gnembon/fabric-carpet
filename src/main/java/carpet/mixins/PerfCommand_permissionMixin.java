package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.PerfCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PerfCommand.class)
public class PerfCommand_permissionMixin
{
    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "method_37340(Lnet/minecraft/server/command/ServerCommandSource;)Z", at = @At("HEAD"), cancellable = true)
    private static void canRun(ServerCommandSource source, CallbackInfoReturnable<Boolean> cir)
    {
        cir.setReturnValue(source.hasPermissionLevel(CarpetSettings.perfPermissionLevel));
    }

}
