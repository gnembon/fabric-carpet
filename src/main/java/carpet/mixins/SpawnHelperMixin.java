package carpet.mixins;

import carpet.CarpetSettings;
import carpet.utils.SpawnReporter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SpawnHelper.class)
public abstract class SpawnHelperMixin
{
    @Redirect(method = "spawnEntitiesInChunk", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"
    ))
    private static boolean catchEntitySpawn(World world, Entity entity_1)
    {
        if (SpawnReporter.track_spawns > 0L && SpawnReporter.local_spawns != null)
        {
            SpawnReporter.registerSpawn(
                    world.dimension.getType(),
                    (MobEntity) entity_1,
                    entity_1.getType().getCategory(),
                    entity_1.getBlockPos());
        }
        if (!SpawnReporter.mock_spawns)
            return world.spawnEntity(entity_1);
        return false;
    }


}
