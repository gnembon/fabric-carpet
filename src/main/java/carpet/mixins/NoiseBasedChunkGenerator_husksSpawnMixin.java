package carpet.mixins;

import carpet.CarpetSettings;
import carpet.helpers.CustomSpawnLists;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.StructureSettings;
import net.minecraft.world.level.levelgen.feature.StructureFeature;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGenerator_husksSpawnMixin extends ChunkGenerator
{
    public NoiseBasedChunkGenerator_husksSpawnMixin(BiomeSource biomeSource, StructureSettings structuresConfig)
    {
        super(biomeSource, structuresConfig);
    }

    @Inject(method = "getMobsAt", at = @At("HEAD"), cancellable = true)
    private void isInsidePyramid(Biome biome, StructureFeatureManager accessor, MobCategory group, BlockPos pos, CallbackInfoReturnable<WeightedRandomList<MobSpawnSettings.SpawnerData>> cir)
    {
        if (group == MobCategory.MONSTER)
        {
            if (CarpetSettings.huskSpawningInTemples)
            {
                if (accessor.getStructureAt(pos, StructureFeature.DESERT_PYRAMID).isValid())
                {
                    cir.setReturnValue(CustomSpawnLists.PYRAMID_SPAWNS);
                    return;
                }
            }
            if (CarpetSettings.shulkerSpawningInEndCities)
            {
                if (accessor.getStructureAt(pos, StructureFeature.END_CITY).isValid())
                {
                    cir.setReturnValue(CustomSpawnLists.SHULKER_SPAWNS);
                    return;
                }
            }
            if (CarpetSettings.piglinsSpawningInBastions)
            {
                if (accessor.getStructureAt(pos, StructureFeature.BASTION_REMNANT).isValid())
                {
                    cir.setReturnValue(CustomSpawnLists.BASTION_SPAWNS);
                }
            }
        }
    }
}
