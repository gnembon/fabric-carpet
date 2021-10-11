package carpet.utils;

import carpet.CarpetSettings;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.OcelotEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tag.BlockTags;
import net.minecraft.text.BaseText;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.Heightmap;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.StructureFeature;
import org.apache.commons.lang3.tuple.Pair;

import carpet.CarpetSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class SpawnReporter
{
    public static boolean mock_spawns = false;
    
    public static Long track_spawns = 0L;
    public static final HashMap<RegistryKey<World>, Integer> chunkCounts = new HashMap<>();

    public static final HashMap<Pair<RegistryKey<World>, SpawnGroup>, Object2LongMap<EntityType>> spawn_stats = new HashMap<>();
    public static double mobcap_exponent = 0.0D;
    
    public static final HashMap<Pair<RegistryKey<World>, SpawnGroup>, Long> spawn_attempts = new HashMap<>();
    public static final HashMap<Pair<RegistryKey<World>, SpawnGroup>, Long> overall_spawn_ticks = new HashMap<>();
    public static final HashMap<Pair<RegistryKey<World>, SpawnGroup>, Long> spawn_ticks_full = new HashMap<>();
    public static final HashMap<Pair<RegistryKey<World>, SpawnGroup>, Long> spawn_ticks_fail = new HashMap<>();
    public static final HashMap<Pair<RegistryKey<World>, SpawnGroup>, Long> spawn_ticks_succ = new HashMap<>();
    public static final HashMap<Pair<RegistryKey<World>, SpawnGroup>, Long> spawn_ticks_spawns = new HashMap<>();
    public static final HashMap<Pair<RegistryKey<World>, SpawnGroup>, Long> spawn_cap_count = new HashMap<>();
    public static final HashMap<Pair<RegistryKey<World>, SpawnGroup>, EvictingQueue<Pair<EntityType, BlockPos>>> spawned_mobs = new HashMap<>();
    public static final HashMap<SpawnGroup, Integer> spawn_tries = new HashMap<>();
    public static BlockPos lower_spawning_limit = null;
    public static BlockPos upper_spawning_limit = null;
    // in case game gets each thread for each world - these need to belong to workd.
    public static HashMap<SpawnGroup, Long> local_spawns = null; // per world
    public static HashSet<SpawnGroup> first_chunk_marker = null;

    static {
        reset_spawn_stats(null, true);
    }

    public static void registerSpawn(MobEntity mob, SpawnGroup cat, BlockPos pos)
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
        Pair<RegistryKey<World>, SpawnGroup> key = Pair.of(mob.world.getRegistryKey(), cat);
        long count = spawn_stats.get(key).getOrDefault(mob.getType(), 0L);
        spawn_stats.get(key).put(mob.getType(), count + 1);
        spawned_mobs.get(key).put(Pair.of(mob.getType(), pos));
        if (!local_spawns.containsKey(cat))
        {
            CarpetSettings.LOG.error("Rogue spawn detected for category "+cat.getName()+" for mob "+mob.getType().getName().getString()+". If you see this message let carpet peeps know about it on github issues.");
            local_spawns.put(cat, 0L);
        }
        local_spawns.put(cat, local_spawns.get(cat)+1);
    }

    public static final int MAGIC_NUMBER = (int)Math.pow(17.0D, 2.0D);
    public static double currentMagicNumber()
    {
        return MAGIC_NUMBER / (Math.pow(2.0,(SpawnReporter.mobcap_exponent/4)));
    }

    public static List<BaseText> printMobcapsForDimension(ServerWorld world, boolean multiline)
    {
        RegistryKey<World> dim = world.getRegistryKey();
        String name = dim.getValue().getPath();
        List<BaseText> lst = new ArrayList<>();
        if (multiline)
            lst.add(Messenger.s(String.format("Mobcaps for %s:",name)));
        SpawnHelper.Info lastSpawner = world.getChunkManager().getSpawnInfo();
        Object2IntMap<SpawnGroup> dimCounts = lastSpawner.getGroupToCount();
        int chunkcount = chunkCounts.getOrDefault(dim, -1);
        if (dimCounts == null || chunkcount < 0)
        {
            lst.add(Messenger.c("g   --UNAVAILABLE--"));
            return lst;
        }

        List<String> shortCodes = new ArrayList<>();
        for (SpawnGroup enumcreaturetype : SpawnGroup.values())
        {
            int cur = dimCounts.getOrDefault(enumcreaturetype, -1);
            int max = (int)(chunkcount * ((double)enumcreaturetype.getCapacity() / currentMagicNumber())); // from ServerChunkManager.CHUNKS_ELIGIBLE_FOR_SPAWNING
            String color = Messenger.heatmap_color(cur, max);
            String mobColor = Messenger.creatureTypeColor(enumcreaturetype);
            if (multiline)
            {
                int rounds = spawn_tries.get(enumcreaturetype);
                lst.add(Messenger.c(String.format("w   %s: ", enumcreaturetype.getName()),
                        (cur < 0) ? "g -" : (color + " " + cur), "g  / ", mobColor + " " + max,
                        (rounds == 1) ? "w " : String.format("gi  (%d rounds/tick)", spawn_tries.get(enumcreaturetype))
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
                lst.add(Messenger.c("g   --UNAVAILABLE--"));
            }

        }
        return lst;
    }
    
    public static List<BaseText> recent_spawns(World world, SpawnGroup creature_type)
    {
        List<BaseText> lst = new ArrayList<>();
        if ((track_spawns == 0L))
        {
            lst.add(Messenger.s("Spawn tracking not started"));
            return lst;
        }
        String type_code = creature_type.getName();
        
        lst.add(Messenger.s(String.format("Recent %s spawns:",type_code)));
        for (Pair<EntityType, BlockPos> pair : spawned_mobs.get(Pair.of(world.getRegistryKey(), creature_type)).keySet()) // getDImTYpe
        {
            lst.add( Messenger.c(
                    "w  - ",
                    Messenger.tp("wb",pair.getRight()),
                    String.format("w : %s", pair.getLeft().getName().getString())
                    ));
        }
        
        if (lst.size()==1)
        {
            lst.add(Messenger.s(" - Nothing spawned yet, sorry."));
        }
        return lst;

    }
    
    public static List<BaseText> show_mobcaps(BlockPos pos, ServerWorld worldIn)
    {
        DyeColor under = WoolTool.getWoolColorAtPosition(worldIn, pos.down());
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
        SpawnGroup creature_type = get_type_code_from_wool_code(under);
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
    
    public static SpawnGroup get_type_code_from_wool_code(DyeColor color)
    {
        switch (color)
        {
            case RED:
                return SpawnGroup.MONSTER;
            case GREEN:
                return SpawnGroup.CREATURE;
            case BLUE:
                return SpawnGroup.WATER_CREATURE;
            case BROWN:
                return SpawnGroup.AMBIENT;
            case CYAN:
                return SpawnGroup.WATER_AMBIENT;
            default:
            	return null;
        }
    }
    
    public static SpawnGroup get_creature_type_from_code(String type_code)
    {
        if ("hostile".equalsIgnoreCase(type_code))
        {
            return SpawnGroup.MONSTER;
        }
        else if ("passive".equalsIgnoreCase(type_code))
        {
            return SpawnGroup.CREATURE;
        }
        else if ("water".equalsIgnoreCase(type_code))
        {
            return SpawnGroup.WATER_CREATURE;
        }
        else if ("fish".equalsIgnoreCase(type_code))
        {
            return SpawnGroup.WATER_AMBIENT;
        }
        else if ("ambient".equalsIgnoreCase(type_code))
        {
            return SpawnGroup.AMBIENT;
        }
        return null;
    }
    
    
    public static String get_type_string(SpawnGroup type)
    {
        return String.format("%s", type);
    }
    
    public static String get_creature_code_from_string(String str)
    {
        return get_type_string(get_creature_type_from_code(str));
    }
    
    public static List<BaseText> printEntitiesByType(SpawnGroup cat, World worldIn, boolean all) //Class<?> entityType)
    {
        List<BaseText> lst = new ArrayList<>();
        lst.add( Messenger.s(String.format("Loaded entities for %s class:", get_type_string(cat))));
        for (Entity entity : ((ServerWorld)worldIn).getEntitiesByType(TypeFilter.instanceOf(Entity.class), (e) -> e.getType().getSpawnGroup()==cat))
        {
            boolean persistent = entity instanceof MobEntity && ( ((MobEntity) entity).isPersistent() || ((MobEntity) entity).cannotDespawn());
            if (!all && persistent)
                continue;

            EntityType type = entity.getType();
            BlockPos pos = entity.getBlockPos();
            lst.add( Messenger.c(
                    "w  - ",
                    Messenger.tp(persistent?"gb":"wb",pos),
                    String.format(persistent?"g : %s":"w : %s", type.getName().getString())
            ));

        }
        if (lst.size()==1)
        {
            lst.add(Messenger.s(" - Empty."));
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
        for (SpawnGroup enumcreaturetype : SpawnGroup.values())
        {
            if (full)
            {
                spawn_tries.put(enumcreaturetype, 1);
            }
            if (server != null) for (RegistryKey<World> dim : server.getWorldRegistryKeys())
            {
                Pair<RegistryKey<World>, SpawnGroup> key = Pair.of(dim, enumcreaturetype);
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

    private static String getWorldCode(RegistryKey<World> world)
    {
        if (world == World.OVERWORLD) return "";
        return "("+world.getValue().getPath().toUpperCase(Locale.ROOT).replace("THE_","").charAt(0)+")";
    }
    
    public static List<BaseText> tracking_report(World worldIn)
    {

        List<BaseText> report = new ArrayList<>();
        if (track_spawns == 0L)
        {
            report.add(Messenger.c(
                    "w Spawn tracking disabled, type '",
                    "wi /spawn tracking start","/spawn tracking start",
                    "w ' to enable"));
            return report;
        }
        long duration = (long) worldIn.getServer().getTicks() - track_spawns;
        report.add(Messenger.c("bw --------------------"));
        String simulated = mock_spawns?"[SIMULATED] ":"";
        String location = (lower_spawning_limit != null)?String.format("[in (%d, %d, %d)x(%d, %d, %d)]",
                lower_spawning_limit.getX(),lower_spawning_limit.getY(),lower_spawning_limit.getZ(),
                upper_spawning_limit.getX(),upper_spawning_limit.getY(),upper_spawning_limit.getZ() ):"";
        report.add(Messenger.s(String.format("%sSpawn statistics %s: for %.1f min", simulated, location, (duration/72000.0)*60)));
        for (SpawnGroup enumcreaturetype : SpawnGroup.values())
        {
            //String type_code = String.format("%s", enumcreaturetype);
            boolean there_are_mobs_to_list = false;
            for (RegistryKey<World> dim : worldIn.getServer().getWorldRegistryKeys()) //String world_code: new String[] {"", " (N)", " (E)"})
            {
                Pair<RegistryKey<World>, SpawnGroup> code = Pair.of(dim, enumcreaturetype);
                if (spawn_ticks_spawns.get(code) > 0L)
                {
                    there_are_mobs_to_list = true;
                    double hours = overall_spawn_ticks.get(code)/72000.0;
                    report.add(Messenger.s(String.format(" > %s%s (%.1f min), %.1f m/t, %%{%.1fF %.1f- %.1f+}; %.2f s/att",
                        enumcreaturetype.getName().substring(0,3), getWorldCode(dim),
                        60*hours,
                        (1.0D*spawn_cap_count.get(code))/ spawn_attempts.get(code),
                        (100.0D*spawn_ticks_full.get(code))/ spawn_attempts.get(code),
                        (100.0D*spawn_ticks_fail.get(code))/ spawn_attempts.get(code),
                        (100.0D*spawn_ticks_succ.get(code))/ spawn_attempts.get(code),
                        (1.0D*spawn_ticks_spawns.get(code))/(spawn_ticks_fail.get(code)+spawn_ticks_succ.get(code))
                    )));
                    for (EntityType type: spawn_stats.get(code).keySet())
                    {
                        report.add(Messenger.s(String.format("   - %s: %d spawns, %d per hour",
                                type.getName().getString(),
                                spawn_stats.get(code).getLong(type),
                                (72000 * spawn_stats.get(code).getLong(type)/duration ))));
                    }
                }
            }
        }
        return report;
    }

    

    public static void killEntity(LivingEntity entity)
    {
        if (entity.hasVehicle())
        {
            entity.getVehicle().discard();
        }
        if (entity.hasPassengers())
        {
            for (Entity e: entity.getPassengerList())
            {
                e.discard();
            }
        }
        if (entity instanceof OcelotEntity)
        {
            for (Entity e: entity.getEntityWorld().getOtherEntities(entity, entity.getBoundingBox()))
            {
                e.discard();
            }
        }
        entity.discard();
    }

    // yeeted from SpawnHelper - temporary fix
    private static List<SpawnSettings.SpawnEntry> method_29950(ServerWorld serverWorld, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator, SpawnGroup spawnGroup, BlockPos blockPos, /*@Nullable*/ Biome biome) {
        return spawnGroup == SpawnGroup.MONSTER && serverWorld.getBlockState(blockPos.down()).getBlock() == Blocks.NETHER_BRICKS && structureAccessor.getStructureAt(blockPos, false, StructureFeature.FORTRESS).hasChildren() ? StructureFeature.FORTRESS.getMonsterSpawns().getEntries() : chunkGenerator.getEntitySpawnList(biome != null ? biome : serverWorld.getBiome(blockPos), structureAccessor, spawnGroup, blockPos).getEntries();
    }

    public static List<BaseText> report(BlockPos pos, ServerWorld worldIn)
    {
        List<BaseText> rep = new ArrayList<>();
        int x = pos.getX(); int y = pos.getY(); int z = pos.getZ();
        Chunk chunk = worldIn.getChunk(pos);
        int lc = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, x, z) + 1;
        String where = String.format((y >= lc) ? "%d blocks above it." : "%d blocks below it.",  MathHelper.abs(y-lc));
        if (y == lc) where = "right at it.";
        rep.add(Messenger.s(String.format("Maximum spawn Y value for (%+d, %+d) is %d. You are "+where, x, z, lc )));
        rep.add(Messenger.s("Spawns:"));
        for (SpawnGroup enumcreaturetype : SpawnGroup.values())
        {
            String type_code = String.format("%s", enumcreaturetype).substring(0, 3);
            List<SpawnSettings.SpawnEntry> lst = method_29950(worldIn, worldIn.getStructureAccessor(), worldIn.getChunkManager().getChunkGenerator(), enumcreaturetype, pos, worldIn.getBiome(pos) );//  ((ChunkGenerator)worldIn.getChunkManager().getChunkGenerator()).getEntitySpawnList(, worldIn.getStructureAccessor(), enumcreaturetype, pos);
            if (lst != null && !lst.isEmpty())
            {
                for (SpawnSettings.SpawnEntry spawnEntry : lst)
                {
                    if (SpawnRestriction.getLocation(spawnEntry.type)==null)
                        continue; // vanilla bug
                    boolean canspawn = SpawnHelper.canSpawn(SpawnRestriction.getLocation(spawnEntry.type), worldIn, pos, spawnEntry.type);
                    int will_spawn = -1;
                    boolean fits;
                    boolean fits1;
                    
                    MobEntity mob;
                    try
                    {
                        mob = (MobEntity) spawnEntry.type.create(worldIn);
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
                            mob.refreshPositionAndAngles((double)f, (double)y, (double)f1, worldIn.random.nextFloat() * 360.0F, 0.0F);
                            fits1 = worldIn.isSpaceEmpty(mob);
                            EntityType etype = mob.getType();

                            for (int i = 0; i < 20; ++i)
                            {
                                if (
                                        SpawnRestriction.canSpawn(etype,worldIn, SpawnReason.NATURAL, pos, worldIn.random) &&
                                        SpawnHelper.canSpawn(SpawnRestriction.getLocation(etype), worldIn, pos, etype) &&
                                        mob.canSpawn(worldIn, SpawnReason.NATURAL)
                                    // && mob.canSpawn(worldIn) // entity collisions // mostly - except ocelots
                                )
                                {
                                    if (etype == EntityType.OCELOT)
                                    {
                                        BlockState blockState = worldIn.getBlockState(pos.down());
                                        if ((pos.getY() < worldIn.getSeaLevel()) || !(blockState.isOf(Blocks.GRASS_BLOCK) || blockState.isIn(BlockTags.LEAVES))) {
                                           continue;
                                        }
                                    }
                                    will_spawn += 1;
                                }
                            }
                            mob.initialize(worldIn, worldIn.getLocalDifficulty(mob.getBlockPos()), SpawnReason.NATURAL, null, null);
                            // the code invokes onInitialSpawn after getCanSpawHere
                            fits = fits1 && worldIn.isSpaceEmpty(mob);
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
                                mob = (MobEntity) spawnEntry.type.create(worldIn);
                            }
                            catch (Exception exception)
                            {
                                CarpetSettings.LOG.warn("Exception while creating mob for spawn reporter", exception);
                                return rep;
                            }
                        }
                    }
                    
                    String creature_name = mob.getType().getName().getString();
                    String pack_size = String.format("%d", mob.getLimitPerChunk());//String.format("%d-%d", animal.minGroupCount, animal.maxGroupCount);
                    int weight = spawnEntry.getWeight().getValue();
                    if (canspawn)
                    {
                        String c = (fits_true && will_spawn>0)?"e":"gi";
                        rep.add(Messenger.c(
                                String.format("%s %s: %s (%d:%d-%d/%d), can: ",c,type_code,creature_name,weight,spawnEntry.minGroupSize, spawnEntry.maxGroupSize,  mob.getLimitPerChunk()),
                                "l YES",
                                c+" , fit: ",
                                ((fits_true && fits_false)?"y YES and NO":(fits_true?"l YES":"r NO")),
                                c+" , will: ",
                                ((will_spawn>0)?"l ":"r ")+Math.round((double)will_spawn)/10+"%"
                        ));
                    }
                    else
                    {
                        rep.add(Messenger.c(String.format("gi %s: %s (%d:%d-%d/%d), can: ",type_code,creature_name,weight,spawnEntry.minGroupSize, spawnEntry.maxGroupSize, mob.getLimitPerChunk()), "n NO"));
                    }
                    killEntity(mob);
                }
            }
        }
        return rep;
    }
}