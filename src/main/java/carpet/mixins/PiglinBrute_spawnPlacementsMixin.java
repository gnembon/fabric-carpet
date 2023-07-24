package carpet.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpawnPlacements.class)
public class PiglinBrute_spawnPlacementsMixin {
	// register custom spawn in SpawnPlacements.java static
	// Shadow the private method
	@Shadow
	private static <T extends Mob> void register(
		EntityType<T> entityType, SpawnPlacements.Type type, Heightmap.Types types, SpawnPlacements.SpawnPredicate<T> spawnPredicate
	) {
		// NOOP
	}
	@Unique
	private static boolean checkPiglinBruteSpawnRules(
		EntityType<PiglinBrute> entityType, LevelAccessor levelAccessor, MobSpawnType mobSpawnType, BlockPos blockPos, RandomSource randomSource
	) {
		return !levelAccessor.getBlockState(blockPos.below()).is(Blocks.NETHER_WART_BLOCK);
	}

	@Inject(method = "<clinit>", at = @At("RETURN"))
	private static void onRegisterStatic(CallbackInfo ci) {
		SpawnPlacements.SpawnPredicate<PiglinBrute> piglinSpawnPredicate = PiglinBrute_spawnPlacementsMixin::checkPiglinBruteSpawnRules;
		register(EntityType.PIGLIN_BRUTE, SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, piglinSpawnPredicate);
	}

}
