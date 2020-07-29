package carpet.mixins;

import carpet.CarpetSettings;
import carpet.fakes.WorldInterface;
import carpet.utils.SpawnReporter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(SpawnHelper.class)
public class SpawnHelperMixin
{
    @Shadow @Final private static int CHUNK_AREA;

    @Shadow @Final private static SpawnGroup[] SPAWNABLE_GROUPS;

    @Redirect(method = "canSpawn(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/SpawnGroup;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/gen/chunk/ChunkGenerator;Lnet/minecraft/world/biome/Biome$SpawnEntry;Lnet/minecraft/util/math/BlockPos$Mutable;D)Z", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;doesNotCollide(Lnet/minecraft/util/math/Box;)Z"
    ))
    private static boolean doesNotCollide(ServerWorld world, Box bb)
    {
        //.doesNotCollide is VERY expensive. On the other side - most worlds are not made of trapdoors in
        // various configurations, but solid and 'passable' blocks, like air, water grass, etc.
        // checking if in the BB of the entity are only passable blocks is very cheap and covers most cases
        // in case something more complex happens - we default to full block collision check
        if (!CarpetSettings.lagFreeSpawning)
        {
            return world.doesNotCollide(bb);
        }
        int minX = MathHelper.floor(bb.minX);
        int minY = MathHelper.floor(bb.minY);
        int minZ = MathHelper.floor(bb.minZ);
        int maxY = MathHelper.ceil(bb.maxY)-1;
        BlockPos.Mutable blockpos = new BlockPos.Mutable();
        if (bb.getXLength() <= 1) // small mobs
        {
            for (int y=minY; y <= maxY; y++)
            {
                blockpos.set(minX,y,minZ);
                VoxelShape box = world.getBlockState(blockpos).getCollisionShape(world, blockpos);
                if (box != VoxelShapes.empty())
                {
                    if (box == VoxelShapes.fullCube())
                    {
                        return false;
                    }
                    else
                    {
                        return world.doesNotCollide(bb);
                    }
                }
            }
            return true;
        }
        // this code is only applied for mobs larger than 1 block in footprint
        int maxX = MathHelper.ceil(bb.maxX)-1;
        int maxZ = MathHelper.ceil(bb.maxZ)-1;
        for (int y = minY; y <= maxY; y++)
            for (int x = minX; x <= maxX; x++)
                for (int z = minZ; z <= maxZ; z++)
                {
                    blockpos.set(x, y, z);
                    VoxelShape box = world.getBlockState(blockpos).getCollisionShape(world, blockpos);
                    if (box != VoxelShapes.empty())
                    {
                        if (box == VoxelShapes.fullCube())
                        {
                            return false;
                        }
                        else
                        {
                            return world.doesNotCollide(bb);
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
                        block.isIn(BlockTags.FENCES) ||
                        block.isIn(BlockTags.WALLS) ||
                        ((block instanceof FenceGateBlock) && !state.get(FenceGateBlock.OPEN))
                )
                {
                    if (x == minX || x == maxX || z == minZ || z == maxZ) return world.doesNotCollide(bb);
                    return false;
                }
            }
        }
        return true;
    }

    @Redirect(method = "createMob", at = @At(
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

    @Redirect(method = "spawnEntitiesInChunk(Lnet/minecraft/entity/SpawnGroup;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/SpawnHelper$Checker;Lnet/minecraft/world/SpawnHelper$Runner;)V", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;spawnEntityAndPassengers(Lnet/minecraft/entity/Entity;)V"//"Lnet/minecraft/server/world/ServerWorld;spawnEntity(Lnet/minecraft/entity/Entity;)Z"
    ))
    private static void spawnEntity(ServerWorld world, Entity entity_1)
    {
        if (CarpetSettings.lagFreeSpawning)
            // we used the mob - next time we will create a new one when needed
            ((WorldInterface) world).getPrecookedMobs().remove(entity_1.getType());

        if (SpawnReporter.track_spawns > 0L && SpawnReporter.local_spawns != null)
        {
            SpawnReporter.registerSpawn(
                    //world.method_27983(), // getDimensionType //dimension.getType(), // getDimensionType
                    (MobEntity) entity_1,
                    entity_1.getType().getSpawnGroup(),
                    entity_1.getBlockPos());
        }
        if (!SpawnReporter.mock_spawns)
            world.spawnEntityAndPassengers(entity_1);
            //world.spawnEntity(entity_1);
    }

    @Redirect(method = "spawnEntitiesInChunk(Lnet/minecraft/entity/SpawnGroup;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/SpawnHelper$Checker;Lnet/minecraft/world/SpawnHelper$Runner;)V", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/mob/MobEntity;initialize(Lnet/minecraft/world/ServerWorldAccess;Lnet/minecraft/world/LocalDifficulty;Lnet/minecraft/entity/SpawnReason;Lnet/minecraft/entity/EntityData;Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/entity/EntityData;"
    ))
    private static EntityData spawnEntity(MobEntity mobEntity, ServerWorldAccess serverWorldAccess, LocalDifficulty difficulty, SpawnReason spawnReason, EntityData entityData, CompoundTag entityTag)
    {
        if (!SpawnReporter.mock_spawns) // WorldAccess
            return mobEntity.initialize(serverWorldAccess, difficulty, spawnReason, entityData, entityTag);
        return null;
    }

    @Redirect(method = "spawnEntitiesInChunk(Lnet/minecraft/entity/SpawnGroup;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/SpawnHelper$Checker;Lnet/minecraft/world/SpawnHelper$Runner;)V", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;squaredDistanceTo(DDD)D"
    ))
    private static double getSqDistanceTo(PlayerEntity playerEntity, double double_1, double double_2, double double_3,
                                          SpawnGroup entityCategory, ServerWorld serverWorld, Chunk chunk, BlockPos blockPos)
    {
        double distanceTo = playerEntity.squaredDistanceTo(double_1, double_2, double_3);
        if (CarpetSettings.lagFreeSpawning && distanceTo > 16384.0D && entityCategory != SpawnGroup.CREATURE)
            return 0.0;
        return distanceTo;
    }



    ////

    @Redirect(method = "spawn", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/SpawnHelper;spawnEntitiesInChunk(Lnet/minecraft/entity/SpawnGroup;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/world/chunk/WorldChunk;Lnet/minecraft/world/SpawnHelper$Checker;Lnet/minecraft/world/SpawnHelper$Runner;)V"
    ))
    // inject our repeat of spawns if more spawn ticks per tick are chosen.
    private static void spawnMultipleTimes(SpawnGroup category, ServerWorld world, WorldChunk chunk, SpawnHelper.Checker checker, SpawnHelper.Runner runner)
    {
        for (int i = 0; i < SpawnReporter.spawn_tries.get(category); i++)
        {
            SpawnHelper.spawnEntitiesInChunk(category, world, chunk, checker, runner);
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

    @Inject(method = "spawn", at = @At("HEAD"))
    // allows to change mobcaps and captures each category try per dimension before it fails due to full mobcaps.
    private static void checkSpawns(ServerWorld world, WorldChunk chunk, SpawnHelper.Info info,
                                    boolean spawnAnimals, boolean spawnMonsters, boolean shouldSpawnAnimals, CallbackInfo ci)
    {
        if (SpawnReporter.track_spawns > 0L)
        {
            SpawnGroup[] var6 = SPAWNABLE_GROUPS;
            int var7 = var6.length;

            for(int var8 = 0; var8 < var7; ++var8) {
                SpawnGroup entityCategory = var6[var8];
                if ((spawnAnimals || !entityCategory.isPeaceful()) && (spawnMonsters || entityCategory.isPeaceful()) && (shouldSpawnAnimals || !entityCategory.isAnimal()) )
                {
                    RegistryKey<World> dim = world.getRegistryKey(); // getDimensionType;
                    int newCap = (int) ((double)entityCategory.getCapacity()*(Math.pow(2.0,(SpawnReporter.mobcap_exponent/4))));
                    int int_2 = SpawnReporter.chunkCounts.get(dim); // eligible chunks for spawning
                    int int_3 = newCap * int_2 / CHUNK_AREA; //current spawning limits
                    int mobCount = info.getGroupToCount().getInt(entityCategory);

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
            }
        }
    }

}
