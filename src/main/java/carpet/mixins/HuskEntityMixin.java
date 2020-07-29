package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.gen.feature.StructureFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HuskEntity.class)
public class HuskEntityMixin
{
    @Redirect(method = "canSpawn", at = @At(value = "INVOKE", target="Lnet/minecraft/world/ServerWorldAccess;isSkyVisible(Lnet/minecraft/util/math/BlockPos;)Z"))
    private static boolean isSkylightOrTempleVisible(ServerWorldAccess serverWorldAccess, BlockPos pos)
    {
        return serverWorldAccess.isSkyVisible(pos) ||
                (CarpetSettings.huskSpawningInTemples && (((ServerWorld)serverWorldAccess).getStructureAccessor().getStructureAt(pos, false, StructureFeature.DESERT_PYRAMID).hasChildren()));
    }
}
