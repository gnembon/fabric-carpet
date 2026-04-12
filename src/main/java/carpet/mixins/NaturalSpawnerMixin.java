package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.LevelInterface;
import carpet.utils.SpawnReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

@Mixin(NaturalSpawner.class)
public class NaturalSpawnerMixin
{
    @Shadow @Final private static int MAGIC_NUMBER;

    @Shadow @Final private static MobCategory[] SPAWNING_CATEGORIES;

    @Redirect(method = "isValidSpawnPostitionForType",
            at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;noCollision(Lnet/minecraft/world/phys/AABB;)Z"
    ))
    private static boolean doesNotCollide(ServerLevel world, AABB bb)
    {
        //.doesNotCollide is VERY expensive. On the other side - most worlds are not made of trapdoors in
        // various configurations, but solid and 'passable' blocks, like air, water grass, etc.
        // checking if in the BB of the entity are only passable blocks is very cheap and covers most cases
        // in case something more complex happens - we default to full block collision check
        if (!CarpetSettings.lagFreeSpawning)
        {
            return world.noCollision(bb);
        }
        int minX = Mth.floor(bb.minX);
        int minY = Mth.floor(bb.minY);
        int minZ = Mth.floor(bb.minZ);
        int maxY = Mth.ceil(bb.maxY)-1;
        BlockPos.MutableBlockPos blockpos = new BlockPos.MutableBlockPos();
        if (bb.getXsize() <= 1) // small mobs
        {
            for (int y=minY; y <= maxY; y++)
            {
                blockpos.set(minX,y,minZ);
                VoxelShape box = world.getBlockState(blockpos).getCollisionShape(world, blockpos);
                if (box != Shapes.empty())
                {
                    if (box == Shapes.block())
                    {
                        return false;
                    }
                    else
                    {
                        return world.noCollision(bb);
                    }
                }
            }
            return true;
        }
        // this code is only applied for mobs larger than 1 block in footprint
        int maxX = Mth.ceil(bb.maxX)-1;
        int maxZ = Mth.ceil(bb.maxZ)-1;
        for (int y = minY; y <= maxY; y++)
            for (int x = minX; x <= maxX; x++)
                for (int z = minZ; z <= maxZ; z++)
                {
                    blockpos.set(x, y, z);
                    VoxelShape box = world.getBlockState(blockpos).getCollisionShape(world, blockpos);
                    if (box != Shapes.empty())
                    {
                        if (box == Shapes.block())
                        {
                            return false;
                        }
                        else
                        {
                            return world.noCollision(bb);
                        }
                    }
                }
        int min_below = minY - 1;
        // we need to check blocks below for extended hitbox and in that case call
        // only applies to 'large mobs', slimes, spiders, magmacubes, ghasts, etc.
        for (int x = minX; x <= maxX; x++)
        {
            for (int z = minZ; z <= maxZ; z++)
            {
                blockpos.set(x, min_below, z);
                BlockState state = world.getBlockState(blockpos);
                Block block = state.getBlock();
                if (
                        state.is(BlockTags.FENCES) ||
                        state.is(BlockTags.WALLS) ||
                        ((block instanceof FenceGateBlock) && !state.getValue(FenceGateBlock.OPEN))
                )
                {
                    if (x == minX || x == maxX || z == minZ || z == maxZ) return world.noCollision(bb);
                    return false;
                }
            }
        }
        return true;
    }

    @Redirect(method = "getMobForSpawn", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnReason;)Lnet/minecraft/world/entity/Entity;"
    ))
    private static Entity create(final EntityType entityType, final Level world_1, final EntitySpawnReason entitySpawnReason)
    {
        if (CarpetSettings.lagFreeSpawning)
        {
            Map<EntityType<?>, Entity> precookedMobs = ((LevelInterface)world_1).getPrecookedMobs();
            if (precookedMobs.containsKey(entityType))
                //this mob has been <init>'s but not used yet
                return precookedMobs.get(entityType);
            Entity e = entityType.create(world_1, entitySpawnReason);
            precookedMobs.put(entityType, e);
            return e;
        }
        return entityType.create(world_1, entitySpawnReason);
    }

    @Redirect(method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"
    ))
    private static void spawnEntity(ServerLevel world, Entity entity_1,
                                    MobCategory group, ServerLevel world2, ChunkAccess chunk, BlockPos pos, NaturalSpawner.SpawnPredicate checker, NaturalSpawner.AfterSpawnCallback runner)
    {
        if (CarpetSettings.lagFreeSpawning)
            // we used the mob - next time we will create a new one when needed
            ((LevelInterface) world).getPrecookedMobs().remove(entity_1.getType());

        if (SpawnReporter.trackingSpawns() && SpawnReporter.local_spawns != null)
        {
            SpawnReporter.registerSpawn(
                    //world.method_27983(), // getDimensionType //dimension.getType(), // getDimensionType
                    (Mob) entity_1,
                    group, //entity_1.getType().getSpawnGroup(),
                    entity_1.blockPosition());
        }
        if (!SpawnReporter.mockSpawns)
            world.addFreshEntityWithPassengers(entity_1);
            //world.spawnEntity(entity_1);
    }

    @Redirect(method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;finalizeSpawn(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/SpawnGroupData;)Lnet/minecraft/world/entity/SpawnGroupData;"
    ))
    private static SpawnGroupData spawnEntity(Mob mobEntity, ServerLevelAccessor serverWorldAccess, DifficultyInstance difficulty, EntitySpawnReason spawnReason, SpawnGroupData entityData)
    {
        if (!SpawnReporter.mockSpawns) // WorldAccess
            return mobEntity.finalizeSpawn(serverWorldAccess, difficulty, spawnReason, entityData);
        return null;
    }

    @Redirect(method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;distanceToSqr(DDD)D"
    ))
    private static double getSqDistanceTo(Player playerEntity, double double_1, double double_2, double double_3,
                                          MobCategory entityCategory, ServerLevel serverWorld, ChunkAccess chunk, BlockPos blockPos)
    {
        double distanceTo = playerEntity.distanceToSqr(double_1, double_2, double_3);
        if (CarpetSettings.lagFreeSpawning && distanceTo > 16384.0D && entityCategory != MobCategory.CREATURE)
            return 0.0;
        return distanceTo;
    }



    ////

    @Redirect(method = "spawnForChunk", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/NaturalSpawner;spawnCategoryForChunk(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V"
    ))
    // inject our repeat of spawns if more spawn ticks per tick are chosen.
    private static void spawnMultipleTimes(MobCategory category, ServerLevel world, LevelChunk chunk, NaturalSpawner.SpawnPredicate checker, NaturalSpawner.AfterSpawnCallback runner)
    {
        for (int i = 0; i < SpawnReporter.spawn_tries.get(category); i++)
        {
            NaturalSpawner.spawnCategoryForChunk(category, world, chunk, checker, runner);
        }
    }

    // shrug - why no inject, no idea. need to inject twice more. Will check with the names next week
/*
    @Redirect(method = "spawn", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/SpawnHelper$Info;isBelowCap(Lnet/minecraft/entity/SpawnGroup;)Z"
    ))
    // allows to change mobcaps and captures each category try per dimension before it fails due to full mobcaps.
    private static boolean changeMobcaps(
            SpawnHelper.Info info, SpawnGroup entityCategory,
            ServerWorld serverWorld, WorldChunk chunk, SpawnHelper.Info info_outer, boolean spawnAnimals, boolean spawnMonsters, boolean shouldSpawnAnimals
    )
    {
        DimensionType dim = serverWorld.dimension.getType();
        int newCap = (int) ((double)entityCategory.getSpawnCap()*(Math.pow(2.0,(SpawnReporter.mobcap_exponent/4))));
        if (SpawnReporter.track_spawns > 0L)
        {
            int int_2 = SpawnReporter.chunkCounts.get(dim); // eligible chunks for spawning
            int int_3 = newCap * int_2 / CHUNK_AREA; //current spawning limits
            int mobCount = info.getCategoryToCount().getInt(entityCategory);

            if (SpawnReporter.track_spawns > 0L && !SpawnReporter.first_chunk_marker.contains(entityCategory))
            {
                SpawnReporter.first_chunk_marker.add(entityCategory);
                //first chunk with spawn eligibility for that category
                Pair key = Pair.of(dim, entityCategory);


                int spawnTries = SpawnReporter.spawn_tries.get(entityCategory);

                SpawnReporter.spawn_attempts.put(key,
                        SpawnReporter.spawn_attempts.get(key) + spawnTries);

                SpawnReporter.spawn_cap_count.put(key,
                        SpawnReporter.spawn_cap_count.get(key) + mobCount);
            }

            if (mobCount <= int_3 || SpawnReporter.mock_spawns)
            {
                //place 0 to indicate there were spawn attempts for a category
                //if (entityCategory != EntityCategory.CREATURE || world.getServer().getTicks() % 400 == 0)
                // this will only be called once every 400 ticks anyways
                SpawnReporter.local_spawns.putIfAbsent(entityCategory, 0L);

                //else
                //full mobcaps - and key in local_spawns will be missing
            }
        }
        return SpawnReporter.mock_spawns || info.getCategoryToCount().getInt(entityCategory) < newCap;
    }

*/
    //temporary mixin until naming gets fixed

    @Inject(method = "spawnForChunk", at = @At("HEAD"))
    // allows to change mobcaps and captures each category try per dimension before it fails due to full mobcaps.
    private static void checkSpawns(ServerLevel world, LevelChunk chunk, NaturalSpawner.SpawnState info,
                                    List<MobCategory> list, CallbackInfo ci)
    {
        if (SpawnReporter.trackingSpawns())
        {
            for (MobCategory entityCategory: list)
            {

                    ResourceKey<Level> dim = world.dimension(); // getDimensionType;
                    int newCap = entityCategory.getMaxInstancesPerChunk();  //(int) ((double)entityCategory.getCapacity()*(Math.pow(2.0,(SpawnReporter.mobcap_exponent/4))));
                    int int_2 = SpawnReporter.chunkCounts.get(dim); // eligible chunks for spawning
                    int int_3 = newCap * int_2 / MAGIC_NUMBER; //current spawning limits
                    int mobCount = info.getMobCategoryCounts().getInt(entityCategory);

                    if (SpawnReporter.trackingSpawns() && !SpawnReporter.first_chunk_marker.contains(entityCategory))
                    {
                        SpawnReporter.first_chunk_marker.add(entityCategory);
                        //first chunk with spawn eligibility for that category
                        Pair<ResourceKey<Level>, MobCategory> key = Pair.of(dim, entityCategory);

                        int spawnTries = SpawnReporter.spawn_tries.get(entityCategory);

                        SpawnReporter.spawn_attempts.addTo(key, spawnTries);

                        SpawnReporter.spawn_cap_count.addTo(key, mobCount);
                    }

                    if (mobCount <= int_3 || SpawnReporter.mockSpawns) //TODO this will not float with player based mobcaps
                    {
                        //place 0 to indicate there were spawn attempts for a category
                        //if (entityCategory != EntityCategory.CREATURE || world.getServer().getTicks() % 400 == 0)
                        // this will only be called once every 400 ticks anyways
                        SpawnReporter.local_spawns.putIfAbsent(entityCategory, 0L);

                        //else
                        //full mobcaps - and key in local_spawns will be missing
                    }
            }
        }
    }

}
