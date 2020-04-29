package carpet.mixins;

import carpet.fakes.ServerChunkManagerInterface;
import carpet.utils.SpawnReporter;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.class_5217;
import net.minecraft.entity.EntityCategory;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.SpawnHelper;
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
public abstract class ServerChunkManagerMixin implements ServerChunkManagerInterface
{
    @Shadow @Final private ServerWorld world;

    @Shadow @Final private ChunkTicketManager ticketManager;

    @Override // shared between scarpet and spawnChunks setting
    public ChunkTicketManager getCMTicketManager()
    {
        return ticketManager;
    }

    @Redirect(method = "tickChunks", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ChunkTicketManager;getLevelCount()I"
    ))
    //this runs once per world spawning cycle. Allows to grab mob counts and count spawn ticks
    private int setupTracking(ChunkTicketManager chunkTicketManager)
    {
        int j = chunkTicketManager.getLevelCount();
        DimensionType dim = this.world.dimension.getType();
        //((WorldInterface)world).getPrecookedMobs().clear(); not needed because mobs are compared with predefined BBs
        SpawnReporter.chunkCounts.put(dim, j);

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
        return j;
    }


    @Inject(method = "tickChunks", at = @At("RETURN"))
    private void onFinishSpawnWorldCycle(CallbackInfo ci)
    {
        class_5217 levelProperties_1 = this.world.getLevelProperties(); // levelProperies class
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
