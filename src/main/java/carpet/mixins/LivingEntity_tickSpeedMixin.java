package carpet.mixins;

import carpet.fakes.MinecraftServerInterface;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntity_tickSpeedMixin
{
	@Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
	private void onPushEntities(CallbackInfo ci)
	{
		if ((LivingEntity)(Object)this instanceof ServerPlayer) {
			var server = (MinecraftServerInterface)((LivingEntity)(Object)this).level().getServer();
			if(server.getTickRateManager().gameIsPaused()) {
				ci.cancel();
			}
		}
	}
}

