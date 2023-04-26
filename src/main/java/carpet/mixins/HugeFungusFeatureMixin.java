package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.HugeFungusConfiguration;
import net.minecraft.world.level.levelgen.feature.HugeFungusFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import static carpet.CarpetSettings.FungusGrowthMode.*;

@Mixin(HugeFungusFeature.class)
public class HugeFungusFeatureMixin {
    @ModifyArgs(method = "place", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/HugeFungusFeature;placeStem(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/levelgen/feature/HugeFungusConfiguration;Lnet/minecraft/core/BlockPos;IZ)V"))
    private void mixin(Args args) {
        boolean natural = !((HugeFungusConfiguration) args.get(2)).planted;
        args.set(5, natural && ((boolean) args.get(5)) ||
            !natural && (CarpetSettings.thickFungusGrowth == ALL ||
            CarpetSettings.thickFungusGrowth == RANDOM && ((RandomSource) args.get(1)).nextFloat() < 0.06F)
        );
    }
}
