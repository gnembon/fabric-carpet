package carpet.mixins;

import carpet.CarpetSettings;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import net.minecraft.entity.EntityType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.BastionRemnantFeature;
import net.minecraft.world.gen.feature.BastionRemnantFeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;

import org.spongepowered.asm.mixin.Mixin;

import java.util.Collections;
import java.util.List;

@Mixin(BastionRemnantFeature.class)
public abstract class BastionRemnantFeatureMixin extends StructureFeature<BastionRemnantFeatureConfig> {

    private static final List<Biome.SpawnEntry> spawnList = Lists.newArrayList(new Biome.SpawnEntry(EntityType.PIGLIN_BRUTE, 5, 1, 2), new Biome.SpawnEntry(EntityType.PIGLIN, 10, 2, 4));

    public BastionRemnantFeatureMixin(Codec<BastionRemnantFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public List<Biome.SpawnEntry> getMonsterSpawns()
    {
        if (CarpetSettings.bruteSpawningInBastionRemnants)
            return spawnList;
        return Collections.emptyList();
    }
}
