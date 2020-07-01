package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.class_5425;
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
    @Redirect(method = "canSpawn", at = @At(value = "INVOKE", target="Lnet/minecraft/class_5425;isSkyVisible(Lnet/minecraft/util/math/BlockPos;)Z"))
    private static boolean isSkylightOrTempleVisible(class_5425 class_5425, BlockPos pos)
    {
        return class_5425.isSkyVisible(pos) ||
                (CarpetSettings.huskSpawningInTemples && (((ServerWorld)class_5425).getStructureAccessor().method_28388(pos, false, StructureFeature.DESERT_PYRAMID).hasChildren()));
    }
}
