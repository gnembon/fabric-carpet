package carpet.mixins;

import carpet.utils.SpawnReporter;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;

@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheMixin
{
    @Shadow @Final private ServerLevel level;

    @Shadow @Final private DistanceManager distanceManager;

    @Redirect(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;JLjava/util/List;)V", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/DistanceManager;getNaturalSpawnChunkCount()I"
    ))
    //this runs once per world spawning cycle. Allows to grab mob counts and count spawn ticks
    private int setupTracking(DistanceManager chunkTicketManager)
    {
        int j = chunkTicketManager.getNaturalSpawnChunkCount();
        ResourceKey<Level> dim = this.level.dimension(); // getDimensionType;
        //((WorldInterface)world).getPrecookedMobs().clear(); not needed because mobs are compared with predefined BBs
        SpawnReporter.chunkCounts.put(dim, j);

        if (SpawnReporter.trackingSpawns())
        {
            //local spawns now need to be tracked globally cause each calll is just for chunk
            SpawnReporter.local_spawns = new Object2LongOpenHashMap<>();
            SpawnReporter.first_chunk_marker = new HashSet<>();
            for (MobCategory cat : SpawnReporter.cachedMobCategories())
            {
                Pair<ResourceKey<Level>, MobCategory> key = Pair.of(dim, cat);
                SpawnReporter.overall_spawn_ticks.addTo(key, SpawnReporter.spawn_tries.get(cat));
            }
        }
        return j;
    }


    @Inject(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;JLjava/util/List;)V", at = @At("RETURN"))
    private void onFinishSpawnWorldCycle(CallbackInfo ci)
    {
        LevelData levelData = this.level.getLevelData(); // levelProperies class
        boolean boolean_3 = levelData.getGameTime() % 400L == 0L;
        if (SpawnReporter.trackingSpawns() && SpawnReporter.local_spawns != null)
        {
            for (MobCategory cat: SpawnReporter.cachedMobCategories())
            {
                ResourceKey<Level> dim = level.dimension(); // getDimensionType;
                Pair<ResourceKey<Level>, MobCategory> key = Pair.of(dim, cat);
                int spawnTries = SpawnReporter.spawn_tries.get(cat);
                if (!SpawnReporter.local_spawns.containsKey(cat))
                {
                    if (!cat.isPersistent() || boolean_3) // isAnimal
                    {
                        // fill mobcaps for that category so spawn got cancelled
                        SpawnReporter.spawn_ticks_full.addTo(key, spawnTries);
                    }

                }
                else if (SpawnReporter.local_spawns.getLong(cat) > 0)
                {
                    // tick spawned mobs for that type
                    SpawnReporter.spawn_ticks_succ.addTo(key, spawnTries);
                    SpawnReporter.spawn_ticks_spawns.addTo(key, SpawnReporter.local_spawns.getLong(cat));
                        // this will be off comparing to 1.13 as that would succeed if
                        // ANY tries in that round were successful.
                        // there will be much more difficult to mix in
                        // considering spawn tries to remove, as with warp
                        // there is little need for them anyways.
                }
                else // spawn no mobs despite trying
                {
                    //tick didn's spawn mobs of that type
                    SpawnReporter.spawn_ticks_fail.addTo(key, spawnTries);
                }
            }
        }
        SpawnReporter.local_spawns = null;
    }



}
