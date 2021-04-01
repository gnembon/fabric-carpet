package carpet.mixins;

import carpet.CarpetSettings;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import net.minecraft.class_6012;
import net.minecraft.entity.EntityType;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.DesertPyramidFeature;
import net.minecraft.world.gen.feature.StructureFeature;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Collections;
import java.util.List;

@Mixin(DesertPyramidFeature.class)
public abstract class DesertPyramidFeatureMixin extends StructureFeature<DefaultFeatureConfig>
{
    private static final class_6012<SpawnSettings.SpawnEntry> MONSTER_SPAWNS;
    
    static
    {
        MONSTER_SPAWNS = class_6012.method_34989(new SpawnSettings.SpawnEntry(EntityType.HUSK, 1, 1, 1));
    }

    public DesertPyramidFeatureMixin(Codec<DefaultFeatureConfig> codec)
    {
        super(codec);
    }


    @Override
    public class_6012<SpawnSettings.SpawnEntry> getMonsterSpawns()
    {
        if (CarpetSettings.huskSpawningInTemples)
        {
            return MONSTER_SPAWNS;
        }
        return  SpawnSettings.field_30982;  // empty spawn list
    }
}

