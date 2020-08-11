package carpet.mixins;

import carpet.CarpetSettings;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;;
import net.minecraft.entity.EntityType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.gen.feature.BastionRemnantFeature;

import net.minecraft.world.gen.feature.JigsawFeature;
import net.minecraft.world.gen.feature.StructurePoolFeatureConfig;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Collections;
import java.util.List;

@Mixin(BastionRemnantFeature.class)
public abstract class BastionRemnantFeatureMixin extends JigsawFeature
{
    private static final List<SpawnSettings.SpawnEntry> spawnList = Lists.newArrayList(
            new SpawnSettings.SpawnEntry(EntityType.PIGLIN_BRUTE, 5, 1, 2),
            new SpawnSettings.SpawnEntry(EntityType.PIGLIN, 10, 2, 4),
            new SpawnSettings.SpawnEntry(EntityType.HOGLIN, 2, 1, 2)
    );

    public BastionRemnantFeatureMixin(Codec<StructurePoolFeatureConfig> codec, int i, boolean bl, boolean bl2)
    {
        super(codec, i, bl, bl2);
    }

    @Override
    public List<SpawnSettings.SpawnEntry> getMonsterSpawns()
    {
        if (CarpetSettings.piglinsSpawningInBastions)
            return spawnList;
        return Collections.emptyList();
    }
}
