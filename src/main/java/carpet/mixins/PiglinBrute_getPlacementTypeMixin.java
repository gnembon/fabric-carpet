package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnPlacements.class)
public class PiglinBrute_getPlacementTypeMixin {
	@Inject(method = "getPlacementType", at = @At("HEAD"), cancellable = true)
	private static void getPlacementType(final EntityType<?> entityType, final CallbackInfoReturnable<SpawnPlacementType> cir) {
		if (CarpetSettings.piglinsSpawningInBastions && entityType == EntityType.PIGLIN_BRUTE) {
			cir.setReturnValue(SpawnPlacementTypes.ON_GROUND);
		}
	}
}
