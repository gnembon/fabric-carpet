package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.commands.Commands;
import net.minecraft.commands.PermissionSource;
import net.minecraft.server.commands.PerfCommand;
import net.minecraft.server.commands.PermissionCheck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PerfCommand.class)
public class PerfCommand_permissionMixin
{
    @Redirect(method = "register", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/commands/Commands;hasPermission(I)Lnet/minecraft/server/commands/PermissionCheck;"
    ))
    private static PermissionCheck<PermissionSource> canRun(int i)
    {
        return Commands.hasPermission(CarpetSettings.perfPermissionLevel);
    }

}
