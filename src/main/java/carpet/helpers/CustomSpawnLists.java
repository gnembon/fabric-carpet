package carpet.helpers;

import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.MobSpawnSettings;

public class CustomSpawnLists
{
    public static final WeightedRandomList<MobSpawnSettings.SpawnerData> PYRAMID_SPAWNS = WeightedRandomList.create(new MobSpawnSettings.SpawnerData(EntityType.HUSK, 1, 1, 1));
    public static final WeightedRandomList<MobSpawnSettings.SpawnerData> SHULKER_SPAWNS = WeightedRandomList.create(new MobSpawnSettings.SpawnerData(EntityType.SHULKER, 10, 4, 4));
    public static final WeightedRandomList<MobSpawnSettings.SpawnerData> BASTION_SPAWNS = WeightedRandomList.create(
            new MobSpawnSettings.SpawnerData(EntityType.PIGLIN_BRUTE, 5, 1, 2),
            new MobSpawnSettings.SpawnerData(EntityType.PIGLIN, 10, 2, 4),
            new MobSpawnSettings.SpawnerData(EntityType.HOGLIN, 2, 1, 2)
    );
}
