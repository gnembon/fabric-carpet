package carpet.mixins;

import carpet.CarpetSettings;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Collections;
import java.util.List;
import net.minecraft.world.level.levelgen.feature.DesertPyramidFeature;

@Mixin(DesertPyramidFeature.class)
public abstract class DesertPyramidFeatureMixin
{
    /*
    private static final Pool<SpawnSettings.SpawnEntry> MONSTER_SPAWNS;
    
    static
    {
        MONSTER_SPAWNS = Pool.of(new SpawnSettings.SpawnEntry(EntityType.HUSK, 1, 1, 1));
    }

    public DesertPyramidFeatureMixin(Codec<DefaultFeatureConfig> codec)
    {
        super(codec);
    }


    @Override
    public Pool<SpawnSettings.SpawnEntry> getMonsterSpawns()
    {
        if (CarpetSettings.huskSpawningInTemples)
        {
            return MONSTER_SPAWNS;
        }
        return  SpawnSettings.EMPTY_ENTRY_POOL;
    }

     */
}

