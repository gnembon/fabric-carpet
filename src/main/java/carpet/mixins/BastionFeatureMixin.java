package carpet.mixins;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.level.levelgen.feature.BastionFeature;

@Mixin(BastionFeature.class)
public abstract class BastionFeatureMixin //extends JigsawFeature
{
    /*
    private static final Pool<SpawnSettings.SpawnEntry> spawnList = Pool.of(
            new SpawnSettings.SpawnEntry(EntityType.PIGLIN_BRUTE, 5, 1, 2),
            new SpawnSettings.SpawnEntry(EntityType.PIGLIN, 10, 2, 4),
            new SpawnSettings.SpawnEntry(EntityType.HOGLIN, 2, 1, 2)
    );

    public BastionRemnantFeatureMixin(Codec<StructurePoolFeatureConfig> codec, int i, boolean bl, boolean bl2)
    {
        super(codec, i, bl, bl2);
    }

    @Override
    public Pool<SpawnSettings.SpawnEntry> getMonsterSpawns()
    {
        if (CarpetSettings.piglinsSpawningInBastions)
            return spawnList;
        return SpawnSettings.EMPTY_ENTRY_POOL;
    }
     */
}
