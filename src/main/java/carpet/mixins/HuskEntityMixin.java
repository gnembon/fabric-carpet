package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.StructureFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HuskEntity.class)
public class HuskEntityMixin
{
    @Redirect(method = "canSpawn", at = @At(value = "INVOKE", target="Lnet/minecraft/world/WorldAccess;isSkyVisible(Lnet/minecraft/util/math/BlockPos;)Z"))
    private static boolean isSkylightOrTempleVisible(WorldAccess WorldAccess, BlockPos blockPos_1)
    {
        return WorldAccess.isSkyVisible(blockPos_1) ||
                (CarpetSettings.huskSpawningInTemples && (((ServerWorld)WorldAccess).getStructureAccessor().method_28388(blockPos_1, false, StructureFeature.DESERT_PYRAMID).hasChildren()));
    }
}
