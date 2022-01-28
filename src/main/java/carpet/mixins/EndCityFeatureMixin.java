package carpet.mixins;

import carpet.CarpetSettings;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Collections;
import java.util.List;
import net.minecraft.world.level.levelgen.feature.EndCityFeature;

@Mixin(EndCityFeature.class)
public abstract class EndCityFeatureMixin //extends StructureFeature<DefaultFeatureConfig>
{
    /*
    private static final Pool<SpawnSettings.SpawnEntry> spawnList = Pool.of(new SpawnSettings.SpawnEntry(EntityType.SHULKER, 10, 4, 4));

    public EndCityFeatureMixin(Codec<DefaultFeatureConfig> codec)
    {
        super(codec);
    }

    @Override
    public Pool<SpawnSettings.SpawnEntry> getMonsterSpawns()
    {
        if (CarpetSettings.shulkerSpawningInEndCities)
            return spawnList;
        return  SpawnSettings.EMPTY_ENTRY_POOL;
    }
     */
}
