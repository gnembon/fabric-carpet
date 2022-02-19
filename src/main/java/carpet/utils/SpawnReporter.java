package carpet.utils;

import carpet.CarpetSettings;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.NetherFortressFeature;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class SpawnReporter
{
    public static boolean mock_spawns = false;
    
    public static Long track_spawns = 0L;
    public static final HashMap<ResourceKey<Level>, Integer> chunkCounts = new HashMap<>();

    public static final HashMap<Pair<ResourceKey<Level>, MobCategory>, Object2LongMap<EntityType>> spawn_stats = new HashMap<>();
    public static double mobcap_exponent = 0.0D;
    
    public static final HashMap<Pair<ResourceKey<Level>, MobCategory>, Long> spawn_attempts = new HashMap<>();
    public static final HashMap<Pair<ResourceKey<Level>, MobCategory>, Long> overall_spawn_ticks = new HashMap<>();
    public static final HashMap<Pair<ResourceKey<Level>, MobCategory>, Long> spawn_ticks_full = new HashMap<>();
    public static final HashMap<Pair<ResourceKey<Level>, MobCategory>, Long> spawn_ticks_fail = new HashMap<>();
    public static final HashMap<Pair<ResourceKey<Level>, MobCategory>, Long> spawn_ticks_succ = new HashMap<>();
    public static final HashMap<Pair<ResourceKey<Level>, MobCategory>, Long> spawn_ticks_spawns = new HashMap<>();
    public static final HashMap<Pair<ResourceKey<Level>, MobCategory>, Long> spawn_cap_count = new HashMap<>();
    public static final HashMap<Pair<ResourceKey<Level>, MobCategory>, EvictingQueue<Pair<EntityType, BlockPos>>> spawned_mobs = new HashMap<>();
    public static final HashMap<MobCategory, Integer> spawn_tries = new HashMap<>();
    public static BlockPos lower_spawning_limit = null;
    public static BlockPos upper_spawning_limit = null;
    // in case game gets each thread for each world - these need to belong to workd.
    public static HashMap<MobCategory, Long> local_spawns = null; // per world
    public static HashSet<MobCategory> first_chunk_marker = null;

    static {
        reset_spawn_stats(null, true);
    }

    private static BaseComponent getTypeName(MobCategory category)
    {
        return Messenger.tr("carpet.common.mob_category." + category.getName());
    }

    public static void registerSpawn(Mob mob, MobCategory cat, BlockPos pos)
    {
        if (lower_spawning_limit != null)
        {
            if (!( (lower_spawning_limit.getX() <= pos.getX() && pos.getX() <= upper_spawning_limit.getX()) &&
                 (lower_spawning_limit.getY() <= pos.getY() && pos.getY() <= upper_spawning_limit.getY()) && 
                 (lower_spawning_limit.getZ() <= pos.getZ() && pos.getZ() <= upper_spawning_limit.getZ())
               ))
            {
                return;
            }
        }
        Pair<ResourceKey<Level>, MobCategory> key = Pair.of(mob.level.dimension(), cat);
        long count = spawn_stats.get(key).getOrDefault(mob.getType(), 0L);
        spawn_stats.get(key).put(mob.getType(), count + 1);
        spawned_mobs.get(key).put(Pair.of(mob.getType(), pos));
        if (!local_spawns.containsKey(cat))
        {
            CarpetSettings.LOG.error("Rogue spawn detected for category "+cat.getName()+" for mob "+mob.getType().getDescription().getString()+". If you see this message let carpet peeps know about it on github issues.");
            local_spawns.put(cat, 0L);
        }
        local_spawns.put(cat, local_spawns.get(cat)+1);
    }

    public static final int MAGIC_NUMBER = (int)Math.pow(17.0D, 2.0D);
    /*public static double currentMagicNumber()
    {
        return MAGIC_NUMBER / (Math.pow(2.0,(SpawnReporter.mobcap_exponent/4)));
    }*/

    public static List<BaseComponent> printMobcapsForDimension(ServerLevel world, boolean multiline)
    {
        ResourceKey<Level> dim = world.dimension();
        List<BaseComponent> lst = new ArrayList<>();
        if (multiline)
            lst.add(Messenger.tr("carpet.command.spawn.mobcap.header", Messenger.dim(dim)));
        NaturalSpawner.SpawnState lastSpawner = world.getChunkSource().getLastSpawnState();
        Object2IntMap<MobCategory> dimCounts = lastSpawner.getMobCategoryCounts();
        int chunkcount = chunkCounts.getOrDefault(dim, -1);
        if (dimCounts == null || chunkcount < 0)
        {
            lst.add(Messenger.c("g   --", Messenger.tr("carpet.command.spawn.mobcap.unavailable"), "g --"));
            return lst;
        }

        List<String> shortCodes = new ArrayList<>();
        for (MobCategory enumcreaturetype : MobCategory.values())
        {
            int cur = dimCounts.getOrDefault(enumcreaturetype, -1);
            int max = (int)(chunkcount * ((double)enumcreaturetype.getMaxInstancesPerChunk() / MAGIC_NUMBER)); // from ServerChunkManager.CHUNKS_ELIGIBLE_FOR_SPAWNING
            String color = Messenger.heatmap_color(cur, max);
            String mobColor = Messenger.creatureTypeColor(enumcreaturetype);
            if (multiline)
            {
                int rounds = spawn_tries.get(enumcreaturetype);
                lst.add(Messenger.c(
                        "w   ", getTypeName(enumcreaturetype), "w : ",
                        (cur < 0) ? "g -" : (color + " " + cur), "g  / ", mobColor + " " + max,
                        (rounds == 1) ? Messenger.s("") : Messenger.c("gi  (", "gi", Messenger.tr("carpet.command.spawn.mobcap.round_per_tick", spawn_tries.get(enumcreaturetype)), "gi )")
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
            if (shortCodes.size()>0)
            {
                shortCodes.remove(shortCodes.size() - 1);
                lst.add(Messenger.c(shortCodes.toArray(new Object[0])));
            }
            else
            {
                lst.add(Messenger.c("g   --", Messenger.tr("carpet.command.spawn.mobcap.unavailable"), "g --"));
            }

        }
        return lst;
    }
    
    public static List<BaseComponent> recent_spawns(Level world, MobCategory creature_type)
    {
        List<BaseComponent> lst = new ArrayList<>();
        if ((track_spawns == 0L))
        {
            lst.add(Messenger.tr("carpet.command.spawn.recent_spawns.tracking_not_started"));
            return lst;
        }
        
        lst.add(Messenger.tr("carpet.command.spawn.recent_spawns.header", getTypeName(creature_type)));
        for (Pair<EntityType, BlockPos> pair : spawned_mobs.get(Pair.of(world.dimension(), creature_type)).keySet()) // getDImTYpe
        {
            lst.add( Messenger.c(
                    "w  - ",
                    Messenger.tp("wb",pair.getRight()),
                    "w : ",
                    pair.getLeft().getDescription()
                    ));
        }
        
        if (lst.size()==1)
        {
            lst.add(Messenger.c(Messenger.s(" - "), Messenger.tr("carpet.command.spawn.recent_spawns.nothing")));
        }
        return lst;

    }
    
    public static List<BaseComponent> show_mobcaps(BlockPos pos, ServerLevel worldIn)
    {
        DyeColor under = WoolTool.getWoolColorAtPosition(worldIn, pos.below());
        if (under == null)
        {
            if (track_spawns > 0L)
            {
                return tracking_report(worldIn);
            }
            else
            {
                return printMobcapsForDimension(worldIn, true );
            }
        }
        MobCategory creature_type = get_type_code_from_wool_code(under);
        if (creature_type != null)
        {
            if (track_spawns > 0L)
            {
                return recent_spawns(worldIn, creature_type);
            }
            else
            {
                return printEntitiesByType(creature_type, worldIn, true);
                
            }
            
        }
        if (track_spawns > 0L)
        {
            return tracking_report(worldIn);
        }
        else
        {
            return printMobcapsForDimension(worldIn, true );
        }
        
    }
    
    public static MobCategory get_type_code_from_wool_code(DyeColor color)
    {
        switch (color)
        {
            case RED:
                return MobCategory.MONSTER;
            case GREEN:
                return MobCategory.CREATURE;
            case BLUE:
                return MobCategory.WATER_CREATURE;
            case BROWN:
                return MobCategory.AMBIENT;
            case CYAN:
                return MobCategory.WATER_AMBIENT;
            default:
            	return null;
        }
    }
    
    public static List<BaseComponent> printEntitiesByType(MobCategory cat, Level worldIn, boolean all) //Class<?> entityType)
    {
        List<BaseComponent> lst = new ArrayList<>();
        lst.add( Messenger.tr("carpet.command.spawn.print_entities.header", getTypeName(cat)) );
        for (Entity entity : ((ServerLevel)worldIn).getEntities(EntityTypeTest.forClass(Entity.class), (e) -> e.getType().getCategory()==cat))
        {
            boolean persistent = entity instanceof Mob && ( ((Mob) entity).isPersistenceRequired() || ((Mob) entity).requiresCustomPersistence());
            if (!all && persistent)
                continue;

            EntityType type = entity.getType();
            BlockPos pos = entity.blockPosition();
            lst.add( Messenger.c(
                    "w  - ",
                    Messenger.tp(persistent?"gb":"wb",pos),
                    persistent ? "g" : "w", Messenger.c("  : ", type.getDescription())
            ));

        }
        if (lst.size()==1)
        {
            lst.add(Messenger.c(Messenger.s(" - "), Messenger.tr("carpet.command.spawn.print_entities.empty")));
        }
        return lst;
    }
    
    public static void initialize_mocking()
    {
        mock_spawns = true;
        
    }
    public static void stop_mocking()
    {
        mock_spawns = false;
    }
    public static void reset_spawn_stats(MinecraftServer server, boolean full)
    {

        spawn_stats.clear();
        spawned_mobs.clear();
        for (MobCategory enumcreaturetype : MobCategory.values())
        {
            if (full)
            {
                spawn_tries.put(enumcreaturetype, 1);
            }
            if (server != null) for (ResourceKey<Level> dim : server.levelKeys())
            {
                Pair<ResourceKey<Level>, MobCategory> key = Pair.of(dim, enumcreaturetype);
                overall_spawn_ticks.put(key, 0L);
                spawn_attempts.put(key, 0L);
                spawn_ticks_full.put(key, 0L);
                spawn_ticks_fail.put(key, 0L);
                spawn_ticks_succ.put(key, 0L);
                spawn_ticks_spawns.put(key, 0L);
                spawn_cap_count.put(key, 0L);
                spawn_stats.put(key, new Object2LongOpenHashMap<>());
                spawned_mobs.put(key, new EvictingQueue<>());
            }
        }
        track_spawns = 0L;
    }

    private static BaseComponent getWorldCode(ResourceKey<Level> world)
    {
        if (world == Level.OVERWORLD) return Messenger.s("");
        return Messenger.c(" (", Messenger.dim(world), " )");
    }

    public static List<BaseComponent> tracking_report(Level worldIn)
    {

        List<BaseComponent> report = new ArrayList<>();
        if (track_spawns == 0L)
        {
            report.add(Messenger.tr("carpet.command.spawn.tracking.disabled", Messenger.c("wi /spawn tracking start")));
            return report;
        }
        long duration = (long) worldIn.getServer().getTickCount() - track_spawns;
        report.add(Messenger.c("bw --------------------"));
        BaseComponent simulated = mock_spawns? Messenger.c(" [", Messenger.tr("carpet.command.spawn.tracking.simulated"), " ] "): Messenger.s("");
        String location = (lower_spawning_limit != null)?String.format(" [in (%d, %d, %d)x(%d, %d, %d)]",
                lower_spawning_limit.getX(),lower_spawning_limit.getY(),lower_spawning_limit.getZ(),
                upper_spawning_limit.getX(),upper_spawning_limit.getY(),upper_spawning_limit.getZ() ):"";
        report.add(Messenger.tr("carpet.command.spawn.tracking.header", simulated, location, String.format("%.1f", (duration/72000.0)*60)));
        for (MobCategory enumcreaturetype : MobCategory.values())
        {
            //String type_code = String.format("%s", enumcreaturetype);
            boolean there_are_mobs_to_list = false;
            for (ResourceKey<Level> dim : worldIn.getServer().levelKeys()) //String world_code: new String[] {"", " (N)", " (E)"})
            {
                Pair<ResourceKey<Level>, MobCategory> code = Pair.of(dim, enumcreaturetype);
                if (spawn_ticks_spawns.get(code) > 0L)
                {
                    there_are_mobs_to_list = true;
                    double hours = overall_spawn_ticks.get(code)/72000.0;
                    report.add(Messenger.c(
                            Messenger.s(" > "), getTypeName(enumcreaturetype),
                            getWorldCode(dim),
                            Messenger.s(String.format(" (%.1f min), %.1f m/t, %%{%.1fF %.1f- %.1f+}; %.2f s/att",
                                60*hours,
                                (1.0D*spawn_cap_count.get(code))/ spawn_attempts.get(code),
                                (100.0D*spawn_ticks_full.get(code))/ spawn_attempts.get(code),
                                (100.0D*spawn_ticks_fail.get(code))/ spawn_attempts.get(code),
                                (100.0D*spawn_ticks_succ.get(code))/ spawn_attempts.get(code),
                                (1.0D*spawn_ticks_spawns.get(code))/(spawn_ticks_fail.get(code)+spawn_ticks_succ.get(code))
                            ))
                    ));
                    for (EntityType type: spawn_stats.get(code).keySet())
                    {
                        report.add(Messenger.c(
                                Messenger.s("   - "),
                                Messenger.tr("carpet.command.spawn.tracking.entity_stat",
                                        type.getDescription(),
                                        spawn_stats.get(code).getLong(type),
                                        (72000 * spawn_stats.get(code).getLong(type)/duration )
                                )
                        ));
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
    private static List<MobSpawnSettings.SpawnerData> getSpawnEntries(final ServerLevel level, final StructureFeatureManager structureFeatureManager, final ChunkGenerator generator, final MobCategory mobCategory, final BlockPos pos, final Holder<Biome> biome) {
        if (NaturalSpawner.isInNetherFortressBounds (pos, level, mobCategory, structureFeatureManager)) {
            return NetherFortressFeature.FORTRESS_ENEMIES.unwrap();
        }
        return generator.getMobsAt(biome != null ? biome : level.getBiome(pos), structureFeatureManager, mobCategory, pos).unwrap();
    }

    public static List<BaseComponent> report(BlockPos pos, ServerLevel worldIn)
    {
        List<BaseComponent> rep = new ArrayList<>();
        int x = pos.getX(); int y = pos.getY(); int z = pos.getZ();
        ChunkAccess chunk = worldIn.getChunk(pos);
        int lc = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 1;
        BaseComponent where = Messenger.tr((y >= lc) ? "carpet.command.spawn.report.blocks_above" : "carpet.command.spawn.report.blocks_below",  Mth.abs(y-lc));
        if (y == lc) where = Messenger.tr("carpet.command.spawn.report.blocks_right_at");
        rep.add(Messenger.tr("carpet.command.spawn.report.max_y", String.format("%+d", x), String.format("%+d", z), lc, where));
        rep.add(Messenger.tr("carpet.command.spawn.report.spawns_header"));
        for (MobCategory enumcreaturetype : MobCategory.values())
        {
            String type_code = enumcreaturetype.getName().substring(0, 3);
            List<MobSpawnSettings.SpawnerData> lst = getSpawnEntries(worldIn, worldIn.structureFeatureManager(), worldIn.getChunkSource().getGenerator(), enumcreaturetype, pos, worldIn.getBiome(pos) );//  ((ChunkGenerator)worldIn.getChunkManager().getChunkGenerator()).getEntitySpawnList(, worldIn.getStructureAccessor(), enumcreaturetype, pos);
            if (lst != null && !lst.isEmpty())
            {
                for (MobSpawnSettings.SpawnerData spawnEntry : lst)
                {
                    if (SpawnPlacements.getPlacementType(spawnEntry.type)==null)
                        continue; // vanilla bug
                    boolean canspawn = NaturalSpawner.isSpawnPositionOk(SpawnPlacements.getPlacementType(spawnEntry.type), worldIn, pos, spawnEntry.type);
                    int will_spawn = -1;
                    boolean fits;
                    boolean fits1;
                    
                    Mob mob;
                    try
                    {
                        mob = (Mob) spawnEntry.type.create(worldIn);
                    }
                    catch (Exception exception)
                    {
                        CarpetSettings.LOG.warn("Exception while creating mob for spawn reporter", exception);
                        return rep;
                    }
                    
                    boolean fits_true = false;
                    boolean fits_false = false;
                    
                    if (canspawn)
                    {
                        will_spawn = 0;
                        for (int attempt = 0; attempt < 50; ++attempt)
                        {
                            float f = (float)x + 0.5F;
                            float f1 = (float)z + 0.5F;
                            mob.moveTo((double)f, (double)y, (double)f1, worldIn.random.nextFloat() * 360.0F, 0.0F);
                            fits1 = worldIn.noCollision(mob);
                            EntityType etype = mob.getType();

                            for (int i = 0; i < 20; ++i)
                            {
                                if (
                                        SpawnPlacements.checkSpawnRules(etype,worldIn, MobSpawnType.NATURAL, pos, worldIn.random) &&
                                        NaturalSpawner.isSpawnPositionOk(SpawnPlacements.getPlacementType(etype), worldIn, pos, etype) &&
                                        mob.checkSpawnRules(worldIn, MobSpawnType.NATURAL)
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
                                    will_spawn += 1;
                                }
                            }
                            mob.finalizeSpawn(worldIn, worldIn.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.NATURAL, null, null);
                            // the code invokes onInitialSpawn after getCanSpawHere
                            fits = fits1 && worldIn.noCollision(mob);
                            if (fits)
                            {
                                fits_true = true;
                            }
                            else
                            {
                                fits_false = true;
                            }
                            
                            killEntity(mob);
                            
                            try
                            {
                                mob = (Mob) spawnEntry.type.create(worldIn);
                            }
                            catch (Exception exception)
                            {
                                CarpetSettings.LOG.warn("Exception while creating mob for spawn reporter", exception);
                                return rep;
                            }
                        }
                    }
                    
                    String creature_name = mob.getType().getDescription().getString();
                    String pack_size = String.format("%d", mob.getMaxSpawnClusterSize());//String.format("%d-%d", animal.minGroupCount, animal.maxGroupCount);
                    int weight = spawnEntry.getWeight().asInt();
                    if (canspawn)
                    {
                        String c = (fits_true && will_spawn>0)?"e":"gi";
                        rep.add(Messenger.c(
                                String.format("%s %s: %s (%d:%d-%d/%d), can: ",c,type_code,creature_name,weight,spawnEntry.minCount, spawnEntry.maxCount,  mob.getMaxSpawnClusterSize()),
                                "l YES",
                                c+" , fit: ",
                                ((fits_true && fits_false)?"y YES and NO":(fits_true?"l YES":"r NO")),
                                c+" , will: ",
                                ((will_spawn>0)?"l ":"r ")+Math.round((double)will_spawn)/10+"%"
                        ));
                    }
                    else
                    {
                        rep.add(Messenger.c(String.format("gi %s: %s (%d:%d-%d/%d), can: ",type_code,creature_name,weight,spawnEntry.minCount, spawnEntry.maxCount, mob.getMaxSpawnClusterSize()), "n NO"));
                    }
                    killEntity(mob);
                }
            }
        }
        return rep;
    }
}