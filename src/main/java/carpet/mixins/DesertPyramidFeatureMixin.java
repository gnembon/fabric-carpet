package carpet.mixins;

import carpet.settings.CarpetSettings;
import com.google.common.collect.Lists;
import com.mojang.datafixers.Dynamic;
import net.minecraft.entity.EntityType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.AbstractTempleFeature;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.DesertPyramidFeature;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Mixin(DesertPyramidFeature.class)
public abstract class DesertPyramidFeatureMixin extends AbstractTempleFeature<DefaultFeatureConfig>
{
    private static final List<Biome.SpawnEntry> MONSTER_SPAWNS;
    
    static
    {
        MONSTER_SPAWNS = Lists.newArrayList(new Biome.SpawnEntry[]{new Biome.SpawnEntry(EntityType.HUSK, 1, 1, 1)});
    }
    
    public DesertPyramidFeatureMixin(Function<Dynamic<?>, ? extends DefaultFeatureConfig> function_1)
    {
        super(function_1);
    }
    
    @Override
    public List<Biome.SpawnEntry> getMonsterSpawns()
    {
        if (CarpetSettings.huskSpawningInTemples)
        {
            return MONSTER_SPAWNS;
        }
        return Collections.emptyList();
    }
}

