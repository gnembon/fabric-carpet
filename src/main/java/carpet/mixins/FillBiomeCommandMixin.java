package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FillBiomeCommand.class)
public class FillBiomeCommandMixin
{
	@Redirect(method = "fill", at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/GameRules;getInt(Lnet/minecraft/world/level/GameRules$Key;)I"
	))
	private static int redirectCloneGameRuleInt(GameRules instance, GameRules.Key<GameRules.IntegerValue> key)
	{
		int vanilla = instance.getInt(key);
		return Math.max(vanilla, CarpetSettings.fillLimit);
	}
}
