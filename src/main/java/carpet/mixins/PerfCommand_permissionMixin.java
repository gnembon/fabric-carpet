package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.commands.Commands;
import net.minecraft.server.commands.PerfCommand;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionProviderCheck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PerfCommand.class)
public class PerfCommand_permissionMixin
{
    @Redirect(method = "register", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/commands/Commands;hasPermission(Lnet/minecraft/server/permissions/PermissionCheck;)Lnet/minecraft/server/permissions/PermissionProviderCheck;"
    ))
    private static PermissionProviderCheck canRun(PermissionCheck permissionCheck)
    {
        return Commands.hasPermission(CarpetSettings.perfPermissionCheck);
    }

}
