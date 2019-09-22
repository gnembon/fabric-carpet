package carpet.mixins;

import carpet.settings.CarpetSettings;
import carpet.fakes.WorldInterface;
import carpet.utils.SpawnReporter;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.IWorld;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(SpawnHelper.class)
public class SpawnHelperMixin
{
    
    // in World: private static Map<EntityType, Entity> precookedMobs= new HashMap<>();

    @Redirect(method = "spawnEntitiesInChunk", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;doesNotCollide(Lnet/minecraft/util/math/Box;)Z"
    ))
    private static boolean doesNotCollide(World world, Box bb)
    {
        //.doesNotCollide is VERY expensive. On the other side - most worlds are not made of trapdoors in
        // various configurations, but solid and 'passable' blocks, like air, water grass, etc.
        // checking if in the BB of the entity are only passable blocks is very cheap and covers most cases
        // in case something more complex happens - we default to full block collision check
        if (CarpetSettings.lagFreeSpawning)
        {
            BlockPos.Mutable blockpos = new BlockPos.Mutable();
            int minX = MathHelper.floor(bb.minX);
            int maxX = MathHelper.ceil(bb.maxX);
            int minY = MathHelper.floor(bb.minY);
            int maxY = MathHelper.ceil(bb.maxY);
            int minZ = MathHelper.floor(bb.minZ);
            int maxZ = MathHelper.ceil(bb.maxZ);
            for (int y = minY; y < maxY; y++)
                for (int x = minX; x < maxX; x++)
                    for (int z = minZ; z < maxZ; z++)
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
        if (CarpetSettings.lagFreeSpawning)
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
        if (CarpetSettings.lagFreeSpawning)
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

    @Redirect(method = "spawnEntitiesInChunk", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/mob/MobEntity;initialize(Lnet/minecraft/world/IWorld;Lnet/minecraft/world/LocalDifficulty;Lnet/minecraft/entity/SpawnType;Lnet/minecraft/entity/EntityData;Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/entity/EntityData;"
    ))
    private static EntityData spawnEntity(MobEntity mobEntity, IWorld iWorld_1, LocalDifficulty localDifficulty_1, SpawnType spawnType_1, EntityData entityData_1, CompoundTag compoundTag_1)
    {
        if (!SpawnReporter.mock_spawns)
            return mobEntity.initialize(iWorld_1, localDifficulty_1, spawnType_1, entityData_1, compoundTag_1);
        return null;
    }

    @Redirect(method = "spawnEntitiesInChunk", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;squaredDistanceTo(DDD)D"
    ))
    private static double getSqDistanceTo(PlayerEntity playerEntity, double double_1, double double_2, double double_3,
                                          EntityCategory entityCategory_1, World world_1, WorldChunk worldChunk_1, BlockPos blockPos_1)
    {
        double distanceTo = playerEntity.squaredDistanceTo(double_1, double_2, double_3);
        if (CarpetSettings.lagFreeSpawning && distanceTo > 16384.0D && entityCategory_1 != EntityCategory.CREATURE)
            return 0.0;
        return distanceTo;
    }
}
