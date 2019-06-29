package carpet.mixins;

import carpet.utils.SpawnReporter;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.entity.EntityCategory;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelProperties;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.HashMap;
import java.util.HashSet;

@Mixin(ServerChunkManager.class)
public abstract class ServerChunkManagerMixin
{
    @Shadow @Final private ServerWorld world;

    @Shadow @Final private static int CHUNKS_ELIGIBLE_FOR_SPAWNING;

    @Inject(
            method = "tickChunks",
            locals = LocalCapture.CAPTURE_FAILHARD,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;entryIterator()Ljava/lang/Iterable;",
                    shift = At.Shift.AFTER,
                    ordinal = 0

    ))
    //this runs once per world spawning cycle. Allows to grab mob counts and count spawn ticks
    private void grabMobcaps(CallbackInfo ci,
                             long long_1,
                             long long_2,
                             LevelProperties levelProperties_1,
                             boolean boolean_1,
                             boolean boolean_2,
                             int int_1,
                             BlockPos blockPos_1,
                             boolean boolean_3,
                             int int_2,
                             EntityCategory[] entityCategorys_1,
                             Object2IntMap object2IntMap_1)
    {
        DimensionType dim = this.world.dimension.getType();
        //((WorldInterface)world).getPrecookedMobs().clear(); not needed because mobs are compared with predefined BBs
        SpawnReporter.mobCounts.put(dim, (Object2IntMap<EntityCategory>)object2IntMap_1);
        SpawnReporter.chunkCounts.put(dim, int_2);

        if (SpawnReporter.track_spawns > 0L)
        {
            //local spawns now need to be tracked globally cause each calll is just for chunk
            SpawnReporter.local_spawns = new HashMap<>();
            SpawnReporter.first_chunk_marker = new HashSet<>();
            for (EntityCategory cat : EntityCategory.values())
            {
                Pair key = Pair.of(dim, cat);
                SpawnReporter.overall_spawn_ticks.put(key,
                        SpawnReporter.overall_spawn_ticks.get(key)+
                        SpawnReporter.spawn_tries.get(cat));
            }
        }
    }

    @Redirect(method = "method_20801", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/SpawnHelper;spawnEntitiesInChunk(Lnet/minecraft/entity/EntityCategory;Lnet/minecraft/world/World;Lnet/minecraft/world/chunk/WorldChunk;Lnet/minecraft/util/math/BlockPos;)V"
    ))
    // inject our repeat of spawns if more spawn ticks per tick are chosen.
    private void spawnMultipleTimes(EntityCategory entityCategory_1, World world_1, WorldChunk worldChunk_1, BlockPos blockPos_1)
    {
        for (int i = 0; i < SpawnReporter.spawn_tries.get(entityCategory_1); i++)
        {
            SpawnHelper.spawnEntitiesInChunk(entityCategory_1, world_1, worldChunk_1, blockPos_1);
        }
    }

    @Redirect(method = "method_20801", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/EntityCategory;getSpawnCap()I"
    ))
    // allows to change mobcaps and captures each category try per dimension before it fails due to full mobcaps.
    private int getNewMobcaps(EntityCategory entityCategory)
    {
        DimensionType dim = this.world.dimension.getType();
        int newCap = (int) ((double)entityCategory.getSpawnCap()*(Math.pow(2.0,(SpawnReporter.mobcap_exponent/4))));
        if (SpawnReporter.track_spawns > 0L)
        {
            int int_2 = SpawnReporter.chunkCounts.get(dim); // eligible chunks for spawning
            int int_3 = newCap * int_2 / CHUNKS_ELIGIBLE_FOR_SPAWNING; //current spawning limits
            int mobCount = SpawnReporter.mobCounts.get(dim).getInt(entityCategory);

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
        return SpawnReporter.mock_spawns?Integer.MAX_VALUE:newCap;
    }

    @Inject(method = "tickChunks", at = @At("RETURN"))
    private void onFinishSpawnWorldCycle(CallbackInfo ci)
    {
        LevelProperties levelProperties_1 = this.world.getLevelProperties();
        boolean boolean_3 = levelProperties_1.getTime() % 400L == 0L;
        if (SpawnReporter.track_spawns > 0L && SpawnReporter.local_spawns != null)
        {
            for (EntityCategory cat: EntityCategory.values())
            {
                DimensionType dim = world.dimension.getType();
                Pair key = Pair.of(world.dimension.getType(), cat);
                int spawnTries = SpawnReporter.spawn_tries.get(cat);
                if (!SpawnReporter.local_spawns.containsKey(cat))
                {
                    if (!cat.isAnimal() || boolean_3)
                    {
                        // fill mobcaps for that category so spawn got cancelled
                        SpawnReporter.spawn_ticks_full.put(key,
                                SpawnReporter.spawn_ticks_full.get(key)+ spawnTries);
                    }

                }
                else if (SpawnReporter.local_spawns.get(cat) > 0)
                {
                    // tick spawned mobs for that type
                    SpawnReporter.spawn_ticks_succ.put(key,
                        SpawnReporter.spawn_ticks_succ.get(key)+spawnTries);
                    SpawnReporter.spawn_ticks_spawns.put(key,
                        SpawnReporter.spawn_ticks_spawns.get(key)+
                        SpawnReporter.local_spawns.get(cat));
                        // this will be off comparing to 1.13 as that would succeed if
                        // ANY tries in that round were successful.
                        // there will be much more difficult to mix in
                        // considering spawn tries to remove, as with warp
                        // there is little need for them anyways.
                }
                else // spawn no mobs despite trying
                {
                    //tick didn's spawn mobs of that type
                    SpawnReporter.spawn_ticks_fail.put(key,
                        SpawnReporter.spawn_ticks_fail.get(key)+spawnTries);
                }
            }
        }
        SpawnReporter.local_spawns = null;
    }



}
