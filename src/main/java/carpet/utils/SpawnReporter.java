
package carpet.utils;

import java.util.HashSet;
import java.util.List;

import java.util.ArrayList;
import java.util.HashMap;

import carpet.CarpetSettings;
import carpet.mixins.WeightedPickerEntryMixin;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.OceanMonumentGenerator;
import net.minecraft.util.registry.Registry;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;

import net.minecraft.util.DyeColor;

import net.minecraft.entity.EntityCategory;
import net.minecraft.world.biome.Biome;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.SpawnHelper;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;

import net.minecraft.entity.passive.OcelotEntity;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.Math;

public class SpawnReporter
{
    public static boolean mock_spawns = false;
    
    public static Long track_spawns = 0L;
    public static final HashMap<DimensionType, Object2IntMap<EntityCategory>> mobCounts = new HashMap<>();
    public static final HashMap<DimensionType, Integer> chunkCounts = new HashMap<>();

    public static final HashMap<Pair<DimensionType, EntityCategory>, Object2LongMap<EntityType>> spawn_stats = new HashMap<>();
    public static double mobcap_exponent = 0.0D;
    
    public static final HashMap<Pair<DimensionType, EntityCategory>, Long> spawn_attempts = new HashMap<>();
    public static final HashMap<Pair<DimensionType, EntityCategory>, Long> overall_spawn_ticks = new HashMap<>();
    public static final HashMap<Pair<DimensionType, EntityCategory>, Long> spawn_ticks_full = new HashMap<>();
    public static final HashMap<Pair<DimensionType, EntityCategory>, Long> spawn_ticks_fail = new HashMap<>();
    public static final HashMap<Pair<DimensionType, EntityCategory>, Long> spawn_ticks_succ = new HashMap<>();
    public static final HashMap<Pair<DimensionType, EntityCategory>, Long> spawn_ticks_spawns = new HashMap<>();
    public static final HashMap<Pair<DimensionType, EntityCategory>, Long> spawn_cap_count = new HashMap<>();
    public static final HashMap<Pair<DimensionType, EntityCategory>, EvictingQueue<Pair<EntityType, BlockPos>>> spawned_mobs = new HashMap<>();
    public static final HashMap<EntityCategory, Integer> spawn_tries = new HashMap<>();
    public static BlockPos lower_spawning_limit = null;
    public static BlockPos upper_spawning_limit = null;
    // in case game gets each thread for each world - these need to belong to workd.
    public static HashMap<EntityCategory, Long> local_spawns = null; // per world
    public static HashSet<EntityCategory> first_chunk_marker = null;

    static {
        reset_spawn_stats(true);
    }

    public static void registerSpawn(DimensionType dim, MobEntity mob, EntityCategory cat, BlockPos pos)
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
        Pair<DimensionType, EntityCategory> key = Pair.of(mob.dimension, cat);
        long count = spawn_stats.get(key).getOrDefault(mob.getType(), 0L);
        spawn_stats.get(key).put(mob.getType(), count + 1);
        spawned_mobs.get(key).put(Pair.of(mob.getType(), pos));
        local_spawns.put(cat, local_spawns.get(cat)+1);
    }


    public static List<BaseComponent> printMobcapsForDimension(DimensionType dim, boolean multiline)
    {
        String name = dim.toString();
        List<BaseComponent> lst = new ArrayList<>();
        if (multiline)
            lst.add(Messenger.s(String.format("Mobcaps for %s:",name)));
        Object2IntMap<EntityCategory> dimCounts = mobCounts.get(dim);
        int chunkcount = chunkCounts.getOrDefault(dim, -1);
        if (dimCounts == null || chunkcount < 0)
        {
            lst.add(Messenger.c("g   --UNAVAILABLE--"));
            return lst;
        }

        List<String> shortCodes = new ArrayList<>();
        for (EntityCategory enumcreaturetype : EntityCategory.values())
        {
            int cur = dimCounts.getOrDefault(enumcreaturetype, -1);
            int max = chunkcount * enumcreaturetype.getSpawnCap() / (int)Math.pow(17.0D, 2.0D); // from ServerChunkManager.CHUNKS_ELIGIBLE_FOR_SPAWNING
            String color = Messenger.heatmap_color(cur, max);
            String mobColor = Messenger.creatureTypeColor(enumcreaturetype);
            if (multiline)
            {
                int rounds = spawn_tries.get(enumcreaturetype);
                lst.add(Messenger.c(String.format("w   %s: ", enumcreaturetype.getName()),
                        (cur < 0) ? "g -" : (color + " " + cur), "g  / ", mobColor + " " + max,
                        (rounds == 1) ? "w " : String.format("fi  (%d rounds/tick)", spawn_tries.get(enumcreaturetype))
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
    
    public static List<BaseComponent> recent_spawns(World world, EntityCategory creature_type)
    {
        List<BaseComponent> lst = new ArrayList<>();
        if ((track_spawns == 0L))
        {
            lst.add(Messenger.s("Spawn tracking not started"));
            return lst;
        }
        String type_code = creature_type.getName();
        
        lst.add(Messenger.s(String.format("Recent %s spawns:",type_code)));
        for (Pair<EntityType, BlockPos> pair : spawned_mobs.get(Pair.of(world.getDimension().getType(),creature_type)).keySet())
        {
            lst.add( Messenger.c(
                    String.format("w  - %s ", Registry.ENTITY_TYPE.getId(pair.getLeft()).toString().replaceFirst("minecraft.","")),
                    Messenger.tp("wb",pair.getRight())));
        }
        
        if (lst.size()==1)
        {
            lst.add(Messenger.s(" - Nothing spawned yet, sorry."));
        }
        return lst;

    }
    
    public static List<BaseComponent> show_mobcaps(BlockPos pos, World worldIn)
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
                return new ArrayList<>();
            }
        }
        EntityCategory creature_type = get_type_code_from_wool_code(under);
        if (creature_type != null)
        {
            if (track_spawns > 0L)
            {
                return recent_spawns(worldIn, creature_type);
            }
            else
            {
                return printEntitiesByType(creature_type, worldIn);
                
            }
            
        }
        if (track_spawns > 0L)
        {
            return tracking_report(worldIn);
        }
        else
        {
            return tracking_report(worldIn);
            //// TODO
            //return print_general_mobcaps(worldIn);
        }
        
    }
    
    public static EntityCategory get_type_code_from_wool_code(DyeColor color)
    {
        switch (color)
        {
            case RED:
                return EntityCategory.MONSTER;
            case GREEN:
                return EntityCategory.CREATURE;
            case BLUE:
                return EntityCategory.WATER_CREATURE;
            case BROWN:
                return EntityCategory.AMBIENT;
        }
        return null;
    }
    
    public static EntityCategory get_creature_type_from_code(String type_code)
    {
        if ("hostile".equalsIgnoreCase(type_code))
        {
            return EntityCategory.MONSTER;
        }
        else if ("passive".equalsIgnoreCase(type_code))
        {
            return EntityCategory.CREATURE;
        }
        else if ("water".equalsIgnoreCase(type_code))
        {
            return EntityCategory.WATER_CREATURE;
        }
        else if ("ambient".equalsIgnoreCase(type_code))
        {
            return EntityCategory.AMBIENT;
        }
        return null;
    }
    
    
    public static String get_type_string(EntityCategory type)
    {
        return String.format("%s", type);
    }
    
    public static String get_creature_code_from_string(String str)
    {
        return get_type_string(get_creature_type_from_code(str));
    }
    
    public static List<BaseComponent> printEntitiesByType(EntityCategory type, World worldIn) //Class<?> entityType)
    {
        CarpetSettings.LOG.error("\n\n\nprinting spawnlist\n\n\n");
        List<BaseComponent> lst = new ArrayList<>();
        Class<?> cls = type.getDeclaringClass();// getBaseClass();// getCreatureClass();
        lst.add( Messenger.s(String.format("Loaded entities for %s class:", get_type_string(type))));

        lst.add(Messenger.s(" <sorry, this list will be populated, once M figures out what counts>"));
        for (Entity entity : ((ServerWorld)worldIn).getEntities(null, (e) -> e.getType().getCategory()==type))
        {
            if (!(entity instanceof MobEntity) || !((MobEntity)entity).isPersistent())
            {
                lst.add(Messenger.c("w  - ",
                        Messenger.tp("w", entity.x, entity.y, entity.z),"w  : "+EntityType.getId(entity.getType()).toString().replaceFirst("minecraft.","")));
            }
        }
        if (lst.size()==1)
        {
            lst.add(Messenger.s(" - Empty."));
        }
        return lst;
    }
    
    public static void initialize_mocking()
    {
        reset_spawn_stats(false);
        mock_spawns = true;
        
    }
    public static void stop_mocking()
    {
        reset_spawn_stats(false);
        mock_spawns = false;
        
    }
    public static void reset_spawn_stats(boolean full)
    {

        spawn_stats.clear();
        spawned_mobs.clear();
        for (EntityCategory enumcreaturetype : EntityCategory.values())
        {
            if (full)
            {
                spawn_tries.put(enumcreaturetype, 1);
            }
            for (DimensionType dim : DimensionType.getAll())
            {
                Pair<DimensionType, EntityCategory> key = Pair.of(dim, enumcreaturetype);
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
    
    public static List<BaseComponent> tracking_report(World worldIn)
    {

        List<BaseComponent> report = new ArrayList<>();
        if (track_spawns == 0L)
        {
            report.add(Messenger.c(
                    "w Spawn tracking disabled, type '",
                    "wi /spawn tracking start","/spawn tracking start",
                    "w ' to enable"));
            return report;
        }
        Long duration = (long) worldIn.getServer().getTicks() - track_spawns;
        report.add(Messenger.c("bw --------------------"));
        String simulated = mock_spawns?"[SIMULATED] ":"";
        String location = (lower_spawning_limit != null)?String.format("[in (%d, %d, %d)x(%d, %d, %d)]",
                lower_spawning_limit.getX(),lower_spawning_limit.getY(),lower_spawning_limit.getZ(),
                upper_spawning_limit.getX(),upper_spawning_limit.getY(),upper_spawning_limit.getZ() ):"";
        report.add(Messenger.s(String.format("%sSpawn statistics %s: for %.1f min", simulated, location, (duration/72000.0)*60)));
        for (EntityCategory enumcreaturetype : EntityCategory.values())
        {
            //String type_code = String.format("%s", enumcreaturetype);
            boolean there_are_mobs_to_list = false;
            for (DimensionType dim: DimensionType.getAll()) //String world_code: new String[] {"", " (N)", " (E)"})
            {
                Pair<DimensionType, EntityCategory> code = Pair.of(dim, enumcreaturetype);
                if (spawn_ticks_spawns.get(code) > 0L)
                {
                    there_are_mobs_to_list = true;
                    double hours = overall_spawn_ticks.get(code)/72000.0;
                    report.add(Messenger.s(String.format(" > %s (%.1f min), %.1f m/t, {%.1f%%F / %.1f%%- / %.1f%%+}; %.2f s/att",
                        code.getRight()+((code.getLeft()==DimensionType.OVERWORLD)?"":( (code.getLeft()==DimensionType.THE_NETHER)?"(N)":"(E)" )),
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
                                Registry.ENTITY_TYPE.getId(type).toString().replaceFirst("minecraft:",""),
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
            entity.getVehicle().remove();
        }
        if (entity.hasPassengers())
        {
            for (Entity e: entity.getPassengerList())
            {
                e.remove();
            }
        }
        if (entity instanceof OcelotEntity)
        {
            for (Entity e: entity.getEntityWorld().getEntities(OcelotEntity.class, entity.getBoundingBox()))
            {
                e.remove();
            }
        }
        entity.remove();
    }

    public static List<BaseComponent> report(BlockPos pos, World worldIn)
    {
        List<BaseComponent> rep = new ArrayList<>();
        int x = pos.getX(); int y = pos.getY(); int z = pos.getZ();
        Chunk chunk = worldIn.getChunk(pos);
        int lc = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, x, z) + 1;
        String where = String.format((y >= lc) ? "%d blocks above it." : "%d blocks below it.",  MathHelper.abs(y-lc));
        if (y == lc) where = "right at it.";
        rep.add(Messenger.s(String.format("Maximum spawn Y value for (%+d, %+d) is %d. You are "+where, x, z, lc )));
        rep.add(Messenger.s("Spawns:"));
        for (EntityCategory enumcreaturetype : EntityCategory.values())
        {
            String type_code = String.format("%s", enumcreaturetype).substring(0, 3);
            List<Biome.SpawnEntry> lst = ((ChunkGenerator)worldIn.getChunkManager().getChunkGenerator()).getEntitySpawnList(enumcreaturetype, pos);
            if (lst != null && !lst.isEmpty())
            {
                for (Biome.SpawnEntry spawnEntry : lst)
                {
                    boolean canspawn = SpawnHelper.canSpawn(SpawnRestriction.getLocation(spawnEntry.type), worldIn, pos, spawnEntry.type);
                    int will_spawn = -1;
                    boolean fits = false;
                    boolean fits1 = false;
                    
                    MobEntity mob;
                    try
                    {
                        mob = (MobEntity) spawnEntry.type.create(worldIn);
                    }
                    catch (Exception exception)
                    {
                        exception.printStackTrace();
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
                            mob.setPositionAndAngles((double)f, (double)y, (double)f1, worldIn.random.nextFloat() * 360.0F, 0.0F);
                            fits1 = worldIn.doesNotCollide(mob);
                            
                            for (int i = 0; i < 20; ++i)
                            {
                                if (mob.canSpawn(worldIn, SpawnType.NATURAL))
                                {
                                    will_spawn += 1;
                                }
                            }
                            mob.initialize(worldIn, worldIn.getLocalDifficulty(new BlockPos(mob)), SpawnType.NATURAL, null, null);
                            // the code invokes onInitialSpawn after getCanSpawHere
                            fits = fits1 && worldIn.doesNotCollide(mob);
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
                                exception.printStackTrace();
                                return rep;
                            }
                        }
                    }
                    
                    String creature_name = Registry.ENTITY_TYPE.getId(mob.getType()).toString().replaceFirst("minecraft:","");
                    String pack_size = String.format("%d", mob.getLimitPerChunk());//String.format("%d-%d", animal.minGroupCount, animal.maxGroupCount);
                    int weight = ((WeightedPickerEntryMixin) spawnEntry).getWeight();
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