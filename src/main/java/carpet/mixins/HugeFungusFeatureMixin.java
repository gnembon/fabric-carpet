package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.world.level.levelgen.feature.HugeFungusFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(HugeFungusFeature.class)
public class HugeFungusFeatureMixin {
    @ModifyArg(method = "place", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/HugeFungusFeature;placeStem(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/levelgen/feature/HugeFungusConfiguration;Lnet/minecraft/core/BlockPos;IZ)V"), index = 5)
    private boolean mixin(boolean b1) {
        return b1 || CarpetSettings.canGrowThickFungus;
    }
}
