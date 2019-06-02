package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.WorldInterface;
import carpet.utils.SpawnReporter;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BoundingBox;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(SpawnHelper.class)
public class SpawnHelperMixin
{
    // in World: private static Map<EntityType, Entity> precookedMobs= new HashMap<>();

    @Redirect(method = "spawnEntitiesInChunk", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;doesNotCollide(Lnet/minecraft/util/math/BoundingBox;)Z"
    ))
    private static boolean doesNotCollide(World world, BoundingBox bb)
    {
        //.doesNotCollide is VERY expensive. On the other side - most worlds are not made of trapdoors in
        // various configurations, but solid and 'passable' blocks, like air, water grass, etc.
        // checking if in the BB of the entity are only passable blocks is very cheap and covers most cases
        // in case something more complex happens - we default to full block collision check
        if (CarpetSettings.b_lagFreeSpawning)
        {
            BlockPos.Mutable blockpos = new BlockPos.Mutable();
            for (int y = (int)bb.minY, maxy = (int)Math.ceil(bb.maxY); y < maxy; y++)
                for (int x = (int)bb.minX,  maxx = (int)Math.ceil(bb.maxX); x < maxx; x++)
                    for (int z = (int)bb.minZ, maxz = (int)Math.ceil(bb.maxZ); z < maxz; z++)
                    {
                        blockpos.set(x, y, z);
                        VoxelShape box = world.getBlockState(blockpos).getCollisionShape(world, blockpos);
                        if ( box == VoxelShapes.empty())
                            continue;
                        if (Block.isShapeFullCube(box))
                            return false;
                        return world.doesNotCollide(bb);
                    }
            return true;
        }
        return world.doesNotCollide(bb);
    }

    @Redirect(method = "spawnEntitiesInChunk", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/EntityType;create(Lnet/minecraft/world/World;)Lnet/minecraft/entity/Entity;"
    ))
    private static Entity create(EntityType<?> entityType, World world_1)
    {
        if (CarpetSettings.b_lagFreeSpawning)
        {
            Map<EntityType, Entity> precookedMobs = ((WorldInterface)world_1).getPrecookedMobs();
            if (precookedMobs.containsKey(entityType))
                //this mob has been <init>'s but not used yet
                return precookedMobs.get(entityType);
            Entity e = entityType.create(world_1);
            precookedMobs.put(entityType, e);
            return e;
        }
        return entityType.create(world_1);
    }

    @Redirect(method = "spawnEntitiesInChunk", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"
    ))
    private static boolean spawnEntity(World world, Entity entity_1)
    {
        if (CarpetSettings.b_lagFreeSpawning)
            // we used the mob - next time we will create a new one when needed
            ((WorldInterface) world).getPrecookedMobs().remove(entity_1.getType());

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
