package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.gen.feature.Feature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HuskEntity.class)
public class HuskEntityMixin
{
    @Redirect(method = "method_20677", at = @At(value = "INVOKE", target="Lnet/minecraft/world/IWorld;isSkyVisible(Lnet/minecraft/util/math/BlockPos;)Z"))
    private static boolean isSkylightOrTempleVisible(IWorld iWorld, BlockPos blockPos_1)
    {
        return iWorld.isSkyVisible(blockPos_1) ||
                (CarpetSettings.huskSpawningInTemples && Feature.DESERT_PYRAMID.isApproximatelyInsideStructure(iWorld, blockPos_1));
    }
}
