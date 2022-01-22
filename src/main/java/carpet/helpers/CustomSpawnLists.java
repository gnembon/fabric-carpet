package carpet.helpers;

import net.minecraft.entity.EntityType;
import net.minecraft.util.collection.Pool;
import net.minecraft.world.biome.SpawnSettings;

public class CustomSpawnLists
{
    public static final Pool<SpawnSettings.SpawnEntry> PYRAMID_SPAWNS = Pool.of(new SpawnSettings.SpawnEntry(EntityType.HUSK, 1, 1, 1));
    public static final Pool<SpawnSettings.SpawnEntry> SHULKER_SPAWNS = Pool.of(new SpawnSettings.SpawnEntry(EntityType.SHULKER, 10, 4, 4));
    public static final Pool<SpawnSettings.SpawnEntry> BASTION_SPAWNS = Pool.of(
            new SpawnSettings.SpawnEntry(EntityType.PIGLIN_BRUTE, 5, 1, 2),
            new SpawnSettings.SpawnEntry(EntityType.PIGLIN, 10, 2, 4),
            new SpawnSettings.SpawnEntry(EntityType.HOGLIN, 2, 1, 2)
    );
}
