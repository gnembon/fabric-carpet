package carpet.mixins;

import carpet.CarpetSettings;
import carpet.helpers.CustomSpawnLists;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.chunk.StructuresConfig;
//import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.StructureFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(NoiseChunkGenerator.class)
// todo rename mixin after 1.15 is gone
public abstract class NoiseChunkGenerator_husksSpawnMixin extends ChunkGenerator
{
    public NoiseChunkGenerator_husksSpawnMixin(BiomeSource biomeSource, StructuresConfig structuresConfig)
    {
        super(biomeSource, structuresConfig);
    }

    @Inject(method = "getEntitySpawnList", at = @At("HEAD"), cancellable = true)
    private void isInsidePyramid(Biome biome, StructureAccessor accessor, SpawnGroup group, BlockPos pos, CallbackInfoReturnable<Pool<SpawnSettings.SpawnEntry>> cir)
    {
        if (group == SpawnGroup.MONSTER)
        {
            if (CarpetSettings.huskSpawningInTemples)
            {
                if (accessor.getStructureAt(pos, StructureFeature.DESERT_PYRAMID).hasChildren())
                {
                    cir.setReturnValue(CustomSpawnLists.PYRAMID_SPAWNS);
                    return;
                }
            }
            if (CarpetSettings.shulkerSpawningInEndCities)
            {
                if (accessor.getStructureAt(pos, StructureFeature.END_CITY).hasChildren())
                {
                    cir.setReturnValue(CustomSpawnLists.SHULKER_SPAWNS);
                    return;
                }
            }
            if (CarpetSettings.piglinsSpawningInBastions)
            {
                if (accessor.getStructureAt(pos, StructureFeature.BASTION_REMNANT).hasChildren())
                {
                    cir.setReturnValue(CustomSpawnLists.BASTION_SPAWNS);
                }
            }
        }
    }
}
