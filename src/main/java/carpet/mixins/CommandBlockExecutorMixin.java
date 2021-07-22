package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.world.CommandBlockExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CommandBlockExecutor.class)
public class CommandBlockExecutorMixin {

	@Redirect(
			method = "execute",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/CommandBlockExecutor;getSource()Lnet/minecraft/server/command/ServerCommandSource;"
			)
	)
	public ServerCommandSource changeCommandBlockPermissionLevel(CommandBlockExecutor commandBlockExecutor) {
		return commandBlockExecutor.getSource().withLevel(CarpetSettings.commandBlockPermissionLevel);
	}
}
