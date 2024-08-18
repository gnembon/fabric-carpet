package carpet.utils;

import carpet.CarpetSettings;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressStructure;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.world.entity.MobCategory.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class SpawnReporter
{
    private static final MobCategory[] CACHED_MOBCATEGORY_VALUES = MobCategory.values();
    public static boolean mockSpawns = false;

    public static final HashMap<ResourceKey<Level>, Integer> chunkCounts = new HashMap<>();

    public static final HashMap<Pair<ResourceKey<Level>, MobCategory>, Object2LongOpenHashMap<EntityType<?>>> spawn_stats = new HashMap<>();
    public static double mobcap_exponent = 0.0D;
    
    public static final Object2LongOpenHashMap<Pair<ResourceKey<Level>, MobCategory>> spawn_attempts = new Object2LongOpenHashMap<>();
    public static final Object2LongOpenHashMap<Pair<ResourceKey<Level>, MobCategory>> overall_spawn_ticks = new Object2LongOpenHashMap<>();
    public static final Object2LongOpenHashMap<Pair<ResourceKey<Level>, MobCategory>> spawn_ticks_full = new Object2LongOpenHashMap<>();
    public static final Object2LongOpenHashMap<Pair<ResourceKey<Level>, MobCategory>> spawn_ticks_fail = new Object2LongOpenHashMap<>();
    public static final Object2LongOpenHashMap<Pair<ResourceKey<Level>, MobCategory>> spawn_ticks_succ = new Object2LongOpenHashMap<>();
    public static final Object2LongOpenHashMap<Pair<ResourceKey<Level>, MobCategory>> spawn_ticks_spawns = new Object2LongOpenHashMap<>();
    public static final Object2LongOpenHashMap<Pair<ResourceKey<Level>, MobCategory>> spawn_cap_count = new Object2LongOpenHashMap<>();
    public static final HashMap<Pair<ResourceKey<Level>, MobCategory>, EvictingQueue<Pair<EntityType<?>, BlockPos>>> spawned_mobs = new HashMap<>();
    public static final HashMap<MobCategory, Integer> spawn_tries = new HashMap<>();

    private static int spawnTrackingStartTime = 0;
    private static BoundingBox trackedSpawningArea = null;
    // in case game gets each thread for each world - these need to belong to workd.
    public static Object2LongOpenHashMap<MobCategory> local_spawns = null; // per world
    public static HashSet<MobCategory> first_chunk_marker = null;

    public static void registerSpawn(Mob mob, MobCategory cat, BlockPos pos)
    {
        if (trackedSpawningArea != null && !trackedSpawningArea.isInside(pos))
        {
            return;
        }
        Pair<ResourceKey<Level>, MobCategory> key = Pair.of(mob.level().dimension(), cat);
        spawn_stats.get(key).addTo(mob.getType(), 1);
        spawned_mobs.get(key).put(Pair.of(mob.getType(), pos));
        if (!local_spawns.containsKey(cat))
        {
            CarpetSettings.LOG.error("Rogue spawn detected for category "+cat.getName()+" for mob "+mob.getType().getDescription().getString()+". If you see this message let carpet peeps know about it on github issues.");
            local_spawns.put(cat, 0L);
        }
        local_spawns.addTo(cat, 1);
    }

    public static final int MAGIC_NUMBER = (int)Math.pow(17.0D, 2.0D);
    /*public static double currentMagicNumber()
    {
        return MAGIC_NUMBER / (Math.pow(2.0,(SpawnReporter.mobcap_exponent/4)));
    }*/

    public static List<Component> printMobcapsForDimension(ServerLevel world, boolean multiline)
    {
        ResourceKey<Level> dim = world.dimension();
        String name = dim.location().getPath();
        List<Component> lst = new ArrayList<>();
        if (multiline)
            lst.add(Messenger.s(String.format("Mobcaps for %s:",name)));
        NaturalSpawner.SpawnState lastSpawner = world.getChunkSource().getLastSpawnState();
        Object2IntMap<MobCategory> dimCounts = lastSpawner.getMobCategoryCounts();
        int chunkcount = chunkCounts.getOrDefault(dim, -1);
        if (dimCounts == null || chunkcount < 0)
        {
            lst.add(Messenger.c("g   --UNAVAILABLE--"));
            return lst;
        }

        List<String> shortCodes = new ArrayList<>();
        for (MobCategory category : cachedMobCategories())
        {
            int cur = dimCounts.getOrDefault(category, -1);
            int max = (int)(chunkcount * ((double)category.getMaxInstancesPerChunk() / MAGIC_NUMBER)); // from ServerChunkManager.CHUNKS_ELIGIBLE_FOR_SPAWNING
            String color = Messenger.heatmap_color(cur, max);
            String mobColor = Messenger.creatureTypeColor(category);
            if (multiline)
            {
                int rounds = spawn_tries.get(category);
                lst.add(Messenger.c(String.format("w   %s: ", category.getName()),
                        (cur < 0) ? "g -" : (color + " " + cur), "g  / ", mobColor + " " + max,
                        (rounds == 1) ? "w " : String.format("gi  (%d rounds/tick)", spawn_tries.get(category))
                ));
            }
            else
            {
                shortCodes.add(color+" "+((cur<0)?"-":cur));
                shortCodes.add("g /");
                shortCodes.add(mobColor+" "+max);
                shortCodes.add("g ,");
            }
        }
        if (!multiline)
        {
            if (shortCodes.size() > 0)
            {
                shortCodes.remove(shortCodes.size() - 1);
                lst.add(Messenger.c(shortCodes.toArray(new Object[0])));
            }
            else
            {
                lst.add(Messenger.c("g   --UNAVAILABLE--"));
            }

        }
        return lst;
    }
    
    public static List<Component> getRecentSpawns(Level world, MobCategory category)
    {
        List<Component> lst = new ArrayList<>();
        if (!trackingSpawns())
        {
            lst.add(Messenger.s("Spawn tracking not started"));
            return lst;
        }
        String categoryName = category.getName();
        
        lst.add(Messenger.s(String.format("Recent %s spawns:", categoryName)));
        for (Pair<EntityType<?>, BlockPos> pair : spawned_mobs.get(Pair.of(world.dimension(), category)).keySet())
        {
            lst.add( Messenger.c(
                    "w  - ",
                    Messenger.tp("wb",pair.getRight()),
                    String.format("w : %s", pair.getLeft().getDescription().getString())
                    ));
        }
        
        if (lst.size() == 1)
        {
            lst.add(Messenger.s(" - Nothing spawned yet, sorry."));
        }
        return lst;

    }
    
    public static List<Component> handleWoolAction(BlockPos pos, ServerLevel worldIn)
    {
        DyeColor under = WoolTool.getWoolColorAtPosition(worldIn, pos.below());
        if (under == null)
        {
            if (trackingSpawns())
            {
                return makeTrackingReport(worldIn);
            }
            else
            {
                return printMobcapsForDimension(worldIn, true );
            }
        }
        MobCategory category = getCategoryFromWoolColor(under);
        if (category != null)
        {
            if (trackingSpawns())
            {
                return getRecentSpawns(worldIn, category);
            }
            else
            {
                return printEntitiesByType(category, worldIn, true);
                
            }
            
        }
        if (trackingSpawns())
        {
            return makeTrackingReport(worldIn);
        }
        else
        {
            return printMobcapsForDimension(worldIn, true );
        }
        
    }
    
    public static MobCategory getCategoryFromWoolColor(DyeColor color)
    {
        return switch (color)
        {
            case RED   -> MONSTER;
            case GREEN -> CREATURE;
            case BLUE  -> WATER_CREATURE;
            case BROWN -> AMBIENT;
            case CYAN  -> WATER_AMBIENT;
            default    -> null;
        };
    }
    
    public static List<Component> printEntitiesByType(MobCategory cat, ServerLevel worldIn, boolean all)
    {
        List<Component> lst = new ArrayList<>();
        lst.add( Messenger.s(String.format("Loaded entities for %s category:", cat)));
        for (Entity entity : worldIn.getEntities(EntityTypeTest.forClass(Entity.class), (e) -> e.getType().getCategory() == cat))
        {
            boolean persistent = entity instanceof Mob mob && ( mob.isPersistenceRequired() || mob.requiresCustomPersistence());
            if (!all && persistent)
                continue;

            EntityType<?> type = entity.getType();
            BlockPos pos = entity.blockPosition();
            lst.add( Messenger.c(
                    "w  - ",
                    Messenger.tp(persistent ? "gb" : "wb", pos),
                    String.format(persistent ? "g : %s" : "w : %s", type.getDescription().getString())
            ));

        }
        if (lst.size() == 1)
        {
            lst.add(Messenger.s(" - Empty."));
        }
        return lst;
    }
    
    public static void initializeMocking()
    {
        mockSpawns = true;
    }
    public static void stopMocking()
    {
        mockSpawns = false;
    }

    public static void resetSpawnStats(MinecraftServer server, boolean full)
    {
        if (full)
        {
            for (MobCategory category : cachedMobCategories())
                spawn_tries.put(category, 1);
        }
        overall_spawn_ticks.clear();
        spawn_attempts.clear();
        spawn_ticks_full.clear();
        spawn_ticks_fail.clear();
        spawn_ticks_succ.clear();
        spawn_ticks_spawns.clear();
        spawn_cap_count.clear();

        // can't fast-path to clear given different worlds could have different amount of worlds
        for (MobCategory category : cachedMobCategories()) {
            for (ResourceKey<Level> world : server.levelKeys()) {
                Pair<ResourceKey<Level>, MobCategory> key = Pair.of(world, category);
                spawn_stats.put(key, new Object2LongOpenHashMap<>());
                spawned_mobs.put(key, new EvictingQueue<>());
            }
        }
        spawnTrackingStartTime = 0;
    }

    public static MobCategory[] cachedMobCategories() {
        return CACHED_MOBCATEGORY_VALUES;
    }

    public static boolean trackingSpawns() {
        return spawnTrackingStartTime != 0L;
    }

    public static void startTracking(MinecraftServer server, BoundingBox trackedArea) {
        resetSpawnStats(server, false);
        spawnTrackingStartTime = server.getTickCount();
        trackedSpawningArea = trackedArea;
    }

    public static void stopTracking(MinecraftServer server) {
        resetSpawnStats(server, false);
        SpawnReporter.spawnTrackingStartTime = 0;
        trackedSpawningArea = null;
    }

    private static String getWorldCode(ResourceKey<Level> world)
    {
        if (world == Level.OVERWORLD) return "";
        return "("+Character.toUpperCase(world.location().getPath().charAt("THE_".length()))+")";
    }
    
    public static List<Component> makeTrackingReport(Level worldIn)
    {
        List<Component> report = new ArrayList<>();
        if (!trackingSpawns())
        {
            report.add(Messenger.c(
                    "w Spawn tracking is disabled, type '",
                    "wi /spawn tracking start","/spawn tracking start",
                    "w ' to enable"));
            return report;
        }
        int duration = worldIn.getServer().getTickCount() - spawnTrackingStartTime;
        report.add(Messenger.c("bw --------------------"));
        String simulated = mockSpawns ? "[SIMULATED] " : "";
        String location = (trackedSpawningArea != null) ? String.format("[in (%d, %d, %d)x(%d, %d, %d)]",
                trackedSpawningArea.minX(), trackedSpawningArea.minY(), trackedSpawningArea.minZ(),
                trackedSpawningArea.maxX(), trackedSpawningArea.maxY(), trackedSpawningArea.maxZ() ):"";
        report.add(Messenger.s(String.format("%sSpawn statistics %s: for %.1f min", simulated, location, (duration/72000.0)*60)));

        for (MobCategory category : cachedMobCategories())
        {
            for (ResourceKey<Level> dim : worldIn.getServer().levelKeys())
            {
                Pair<ResourceKey<Level>, MobCategory> key = Pair.of(dim, category);
                if (spawn_ticks_spawns.getLong(key) > 0L)
                {
                    double hours = overall_spawn_ticks.getLong(key)/72000.0;
                    long spawnAttemptsForCategory = spawn_attempts.getLong(key);
                    report.add(Messenger.s(String.format(" > %s%s (%.1f min), %.1f m/t, %%{%.1fF %.1f- %.1f+}; %.2f s/att",
                        category.getName().substring(0,3), getWorldCode(dim),
                        60*hours,
                        (1.0D * spawn_cap_count.getLong(key)) / spawnAttemptsForCategory,
                        (100.0D * spawn_ticks_full.getLong(key)) / spawnAttemptsForCategory,
                        (100.0D * spawn_ticks_fail.getLong(key)) / spawnAttemptsForCategory,
                        (100.0D * spawn_ticks_succ.getLong(key)) / spawnAttemptsForCategory,
                        (1.0D * spawn_ticks_spawns.getLong(key)) / (spawn_ticks_fail.getLong(key) + spawn_ticks_succ.getLong(key))
                    )));
                    for (Object2LongMap.Entry<EntityType<?>> entry: spawn_stats.get(key).object2LongEntrySet())
                    {
                        report.add(Messenger.s(String.format("   - %s: %d spawns, %d per hour",
                                entry.getKey().getDescription().getString(),
                                entry.getLongValue(),
                                (72000 * entry.getLongValue()/duration ))));
                    }
                }
            }
        }
        return report;
    }

    public static void killEntity(LivingEntity entity)
    {
        if (entity.isPassenger())
        {
            entity.getVehicle().discard();
        }
        if (entity.isVehicle())
        {
            for (Entity e: entity.getPassengers())
            {
                e.discard();
            }
        }
        if (entity instanceof Ocelot)
        {
            for (Entity e: entity.getCommandSenderWorld().getEntities(entity, entity.getBoundingBox()))
            {
                e.discard();
            }
        }
        entity.discard();
    }

    // yeeted from NaturalSpawner - temporary access fix
    private static List<MobSpawnSettings.SpawnerData> getSpawnEntries(ServerLevel serverLevel, StructureManager structureManager, ChunkGenerator chunkGenerator, MobCategory mobCategory, BlockPos blockPos, @Nullable Holder<Biome> holder) {
        return NaturalSpawner.isInNetherFortressBounds(blockPos, serverLevel, mobCategory, structureManager) ? NetherFortressStructure.FORTRESS_ENEMIES.unwrap() : chunkGenerator.getMobsAt(holder != null ? holder : serverLevel.getBiome(blockPos), structureManager, mobCategory, blockPos).unwrap();
    }

    public static List<Component> report(BlockPos pos, ServerLevel worldIn)
    {
        List<Component> rep = new ArrayList<>();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        ChunkAccess chunk = worldIn.getChunk(pos);
        int lc = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 1;
        String relativeHeight = (y == lc) ? "right at it." : String.format("%d blocks %s it.", Mth.abs(y - lc), (y >= lc) ? "above" : "below");
        rep.add(Messenger.s(String.format("Maximum spawn Y value for (%+d, %+d) is %d. You are " + relativeHeight, x, z, lc)));
        rep.add(Messenger.s("Spawns:"));
        for (MobCategory category : cachedMobCategories())
        {
            String categoryCode = String.valueOf(category).substring(0, 3);
            List<MobSpawnSettings.SpawnerData> lst = getSpawnEntries(worldIn, worldIn.structureManager(), worldIn.getChunkSource().getGenerator(), category, pos, worldIn.getBiome(pos));
            if (lst != null && !lst.isEmpty())
            {
                for (MobSpawnSettings.SpawnerData spawnEntry : lst)
                {
                    if (SpawnPlacements.getPlacementType(spawnEntry.type) == null)
                        continue; // vanilla bug
                    boolean canSpawn = SpawnPlacements.isSpawnPositionOk(spawnEntry.type, worldIn, pos);
                    int willSpawn = -1;
                    boolean fits = false;
                    
                    Mob mob;
                    try
                    {
                        mob = (Mob) spawnEntry.type.create(worldIn, EntitySpawnReason.NATURAL);
                    }
                    catch (Exception e)
                    {
                        CarpetSettings.LOG.warn("Exception while creating mob for spawn reporter", e);
                        return rep;
                    }
                    
                    if (canSpawn)
                    {
                        willSpawn = 0;
                        for (int attempt = 0; attempt < 50; ++attempt)
                        {
                            float f = x + 0.5F;
                            float f1 = z + 0.5F;
                            mob.moveTo(f, y, f1, worldIn.random.nextFloat() * 360.0F, 0.0F);
                            fits = worldIn.noCollision(mob);
                            EntityType<?> etype = mob.getType();

                            for (int i = 0; i < 20; ++i)
                            {
                                if (
                                        SpawnPlacements.checkSpawnRules(etype,worldIn, EntitySpawnReason.NATURAL, pos, worldIn.random) &&
                                        SpawnPlacements.isSpawnPositionOk(etype, worldIn, pos) &&
                                        mob.checkSpawnRules(worldIn, EntitySpawnReason.NATURAL)
                                    // && mob.canSpawn(worldIn) // entity collisions // mostly - except ocelots
                                )
                                {
                                    if (etype == EntityType.OCELOT)
                                    {
                                        BlockState blockState = worldIn.getBlockState(pos.below());
                                        if ((pos.getY() < worldIn.getSeaLevel()) || !(blockState.is(Blocks.GRASS_BLOCK) || blockState.is(BlockTags.LEAVES))) {
                                           continue;
                                        }
                                    }
                                    willSpawn += 1;
                                }
                            }
                            mob.finalizeSpawn(worldIn, worldIn.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.NATURAL, null);
                            // the code invokes onInitialSpawn after getCanSpawHere
                            fits = fits && worldIn.noCollision(mob);
                            
                            killEntity(mob);
                            
                            try
                            {
                                mob = (Mob) spawnEntry.type.create(worldIn, EntitySpawnReason.NATURAL);
                            }
                            catch (Exception e)
                            {
                                CarpetSettings.LOG.warn("Exception while creating mob for spawn reporter", e);
                                return rep;
                            }
                        }
                    }
                    
                    String mobTypeName = mob.getType().getDescription().getString();
                    //String pack_size = Integer.toString(mob.getMaxSpawnClusterSize());//String.format("%d-%d", animal.minGroupCount, animal.maxGroupCount);
                    int weight = spawnEntry.getWeight().asInt();
                    if (canSpawn)
                    {
                        String color = (fits && willSpawn > 0) ? "e" : "gi";
                        rep.add(Messenger.c(
                                String.format("%s %s: %s (%d:%d-%d/%d), can: ", color, categoryCode, mobTypeName, weight, spawnEntry.minCount, spawnEntry.maxCount,  mob.getMaxSpawnClusterSize()),
                                "l YES",
                                color + " , fit: ",
                                (fits ? "l YES" : "r NO"),
                                color + " , will: ",
                                ((willSpawn > 0)?"l ":"r ") + Math.round((double)willSpawn) / 10 + "%"
                        ));
                    }
                    else
                    {
                        rep.add(Messenger.c(String.format("gi %s: %s (%d:%d-%d/%d), can: ", categoryCode, mobTypeName, weight, spawnEntry.minCount, spawnEntry.maxCount, mob.getMaxSpawnClusterSize()), "n NO"));
                    }
                    killEntity(mob);
                }
            }
        }
        return rep;
    }
}
