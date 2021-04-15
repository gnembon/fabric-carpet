package carpet.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import carpet.CarpetSettings;
import net.minecraft.server.MinecraftServer;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServer_pingPlayerSampleLimit
{

	@ModifyConstant(method = "tick", constant = @Constant(intValue = 12), require = 0, allow = 1)
	private int modifyPlayerSampleLimit(int value)
	{
		return CarpetSettings.pingPlayerListLimit;
	}
}
