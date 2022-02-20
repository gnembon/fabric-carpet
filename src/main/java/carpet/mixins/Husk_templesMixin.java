package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.data.worldgen.StructureFeatures;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Husk.class)
public class Husk_templesMixin
{
    @Redirect(method = "checkHuskSpawnRules", at = @At(value = "INVOKE", target="Lnet/minecraft/world/level/ServerLevelAccessor;canSeeSky(Lnet/minecraft/core/BlockPos;)Z"))
    private static boolean isSkylightOrTempleVisible(ServerLevelAccessor serverWorldAccess, BlockPos pos)
    {
        return serverWorldAccess.canSeeSky(pos) ||
                (CarpetSettings.huskSpawningInTemples && (((ServerLevel)serverWorldAccess).structureFeatureManager().getStructureAt(pos, StructureFeatures.DESERT_PYRAMID.value()).isValid()));
    }
}
