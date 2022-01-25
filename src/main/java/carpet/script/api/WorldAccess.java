package carpet.script.api;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.fakes.BiomeArrayInterface;
import carpet.fakes.ChunkGeneratorInterface;
import carpet.fakes.ChunkTicketManagerInterface;
import carpet.fakes.ServerChunkManagerInterface;
import carpet.fakes.ServerWorldInterface;
import carpet.fakes.SpawnHelperInnerInterface;
import carpet.fakes.ThreadedAnvilChunkStorageInterface;
import carpet.helpers.FeatureGenerator;
import carpet.mixins.PointOfInterest_scarpetMixin;
import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Fluff;
import carpet.script.LazyValue;
import carpet.script.argument.BlockArgument;
import carpet.script.argument.Vector3Argument;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ThrowStatement;
import carpet.script.exception.Throwables;
import carpet.script.utils.BiomeInfo;
import carpet.script.utils.InputValidator;
import carpet.script.utils.WorldTools;
import carpet.script.value.BlockValue;
import carpet.script.value.BooleanValue;
import carpet.script.value.EntityValue;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.NullValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;
import carpet.utils.BlockInfo;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CommandBlock;
import net.minecraft.block.JigsawBlock;
import net.minecraft.block.StructureBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.structure.Structure;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.processor.BlockRotStructureProcessor;
import net.minecraft.structure.processor.GravityStructureProcessor;
import net.minecraft.tag.Tag;
import net.minecraft.tag.TagManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Clearable;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.SortedArraySet;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.ConfiguredStructureFeature;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static carpet.script.utils.WorldTools.canHasChunk;

public class WorldAccess {
    private static final Map<String, Direction> DIRECTION_MAP = Arrays.stream(Direction.values()).collect(Collectors.toMap(Direction::getName, (direction) -> direction));
    static {
        DIRECTION_MAP.put("y", Direction.UP);
        DIRECTION_MAP.put("z", Direction.SOUTH);
        DIRECTION_MAP.put("x", Direction.EAST);

    }
    private final static Map<String, ChunkTicketType<?>> ticketTypes = new HashMap<String, ChunkTicketType<?>>(){{
        put("portal", ChunkTicketType.PORTAL);
        put("teleport", ChunkTicketType.POST_TELEPORT);
        put("unknown", ChunkTicketType.UNKNOWN);  // unknown
    }};
    // dummy entity for dummy requirements in the loot tables (see snowball)
    private static FallingBlockEntity DUMMY_ENTITY = null;
    private static Value booleanStateTest(
            Context c,
            String name,
            List<Value> params,
            BiPredicate<BlockState, BlockPos> test
    )
    {
        CarpetContext cc = (CarpetContext) c;
        if (params.size() == 0)
            throw new InternalExpressionException("'" + name + "' requires at least one parameter");
        Value v0 = params.get(0);
        if (v0 instanceof BlockValue)
            return BooleanValue.of(test.test(((BlockValue) v0).getBlockState(), ((BlockValue) v0).getPos()));
        BlockValue block = BlockArgument.findIn(cc, params, 0).block;
        return BooleanValue.of(test.test(block.getBlockState(), block.getPos()));
    }

    private static Value stateStringQuery(
            Context c,
            String name,
            List<Value> params,
            BiFunction<BlockState, BlockPos, String> test
    )
    {
        CarpetContext cc = (CarpetContext) c;
        if (params.size() == 0)
        {
            throw new InternalExpressionException("'" + name + "' requires at least one parameter");
        }

        Value v0 = params.get(0);
        if (v0 instanceof BlockValue)
        {
            String strVal = test.apply( ((BlockValue) v0).getBlockState(), ((BlockValue) v0).getPos());
            return StringValue.of(strVal);
        }
        BlockValue block = BlockArgument.findIn(cc, params, 0).block;
        return StringValue.of(test.apply(block.getBlockState(), block.getPos()));
    }

    private static Value genericStateTest(
            Context c,
            String name,
            List<Value> params,
            Fluff.TriFunction<BlockState, BlockPos, World, Value> test
    )
    {
        CarpetContext cc = (CarpetContext) c;
        if (params.size() == 0)
            throw new InternalExpressionException("'" + name + "' requires at least one parameter");
        Value v0 = params.get(0);
        if (v0 instanceof BlockValue)
        {
            try
            {
                return test.apply(((BlockValue) v0).getBlockState(), ((BlockValue) v0).getPos(), cc.s.getWorld());
            }
            catch (NullPointerException ignored)
            {
                throw new InternalExpressionException("'" + name + "' function requires a block that is positioned in the world");
            }
        }
        BlockValue block = BlockArgument.findIn(cc, params, 0).block;
        return test.apply(block.getBlockState(), block.getPos(), cc.s.getWorld());
    }

    private static <T extends Comparable<T>> BlockState setProperty(Property<T> property, String name, String value,
                                                                    BlockState bs)
    {
        Optional<T> optional = property.parse(value);

        if (optional.isPresent())
        {
            bs = bs.with(property, optional.get());
        }
        else
        {
            throw new InternalExpressionException(value + " is not a valid value for property " + name);
        }
        return bs;
    }

    private static void BooYah(ChunkGenerator generator)
    {
        synchronized (generator)
        {
            ((ChunkGeneratorInterface)generator).initStrongholds();
        }
    }

    public static void apply(Expression expression)
    {
        expression.addContextFunction("block", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            if (lv.size() == 0)
            {
                throw new InternalExpressionException("Block requires at least one parameter");
            }
            BlockValue retval = BlockArgument.findIn(cc, lv, 0, true).block;
            // fixing block state and data
            retval.getBlockState();
            retval.getData();
            return retval;
        });

        expression.addContextFunction("block_data", -1, (c, t, lv) ->
        {
            if (lv.size() == 0)
                throw new InternalExpressionException("Block requires at least one parameter");
            NbtCompound tag = BlockArgument.findIn( (CarpetContext) c, lv, 0, true).block.getData();
            return NBTSerializableValue.of(tag);
        });

        // poi_get(pos, radius?, type?, occupation?, column_mode?)
        expression.addContextFunction("poi", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            if (lv.size() == 0) throw new InternalExpressionException("'poi' requires at least one parameter");
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0, false);
            BlockPos pos = locator.block.getPos();
            PointOfInterestStorage store = cc.s.getWorld().getPointOfInterestStorage();
            if (lv.size() == locator.offset)
            {
                PointOfInterestType poiType = store.getType(pos).orElse(null);
                if (poiType == null) return Value.NULL;

                // this feels wrong, but I don't want to mix-in more than I really need to.
                // also distance adds 0.5 to each point which screws up accurate distance calculations
                // you shoudn't be using POI with that in mind anyways, so I am not worried about it.
                PointOfInterest poi = store.getInCircle(
                        poiType.getCompletionCondition(),
                        pos,
                        1,
                        PointOfInterestStorage.OccupationStatus.ANY
                ).filter(p -> p.getPos().equals(pos)).findFirst().orElse(null);
                if (poi == null)
                    return Value.NULL;
                return ListValue.of(
                        new StringValue(poi.getType().toString()),
                        new NumericValue(poiType.getTicketCount() - ((PointOfInterest_scarpetMixin)poi).getFreeTickets())
                );
            }
            int radius = NumericValue.asNumber(lv.get(locator.offset+0)).getInt();
            if (radius < 0) return ListValue.of();
            Predicate<PointOfInterestType> condition = PointOfInterestType.ALWAYS_TRUE;
            PointOfInterestStorage.OccupationStatus status = PointOfInterestStorage.OccupationStatus.ANY;
            boolean inColumn = false;
            if (locator.offset + 1 < lv.size())
            {
                String poiType = lv.get(locator.offset+1).getString().toLowerCase(Locale.ROOT);
                if (!"any".equals(poiType))
                {
                    PointOfInterestType type =  Registry.POINT_OF_INTEREST_TYPE.getOrEmpty(InputValidator.identifierOf(poiType))
                            .orElseThrow(() -> new ThrowStatement(poiType, Throwables.UNKNOWN_POI));
                    condition = (tt) -> tt == type;
                }
                if (locator.offset + 2 < lv.size())
                {
                    String statusString = lv.get(locator.offset+2).getString().toLowerCase(Locale.ROOT);
                    if ("occupied".equals(statusString))
                        status = PointOfInterestStorage.OccupationStatus.IS_OCCUPIED;
                    else if ("available".equals(statusString))
                        status = PointOfInterestStorage.OccupationStatus.HAS_SPACE;
                    else if (!("any".equals(statusString)))
                        throw new InternalExpressionException(
                                "Incorrect POI occupation status "+status+ " use `any`, " + "`occupied` or `available`"
                        );
                    if (locator.offset + 3 < lv.size())
                    {
                        inColumn = lv.get(locator.offset+3).getBoolean();
                    }
                }
            }
            Stream<PointOfInterest> pois = inColumn?
                    store.getInSquare(condition, pos, radius, status):
                    store.getInCircle(condition, pos, radius, status);
            return ListValue.wrap(pois.sorted(Comparator.comparingDouble(p -> p.getPos().getSquaredDistance(pos))).map(p ->
                    ListValue.of(
                            new StringValue(p.getType().toString()),
                            new NumericValue(p.getType().getTicketCount() - ((PointOfInterest_scarpetMixin)p).getFreeTickets()),
                            ListValue.of(new NumericValue(p.getPos().getX()), new NumericValue(p.getPos().getY()), new NumericValue(p.getPos().getZ()))
                    )
            ).collect(Collectors.toList()));
        });

        //poi_set(pos, null) poi_set(pos, type, occupied?,
        expression.addContextFunction("set_poi", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            if (lv.size() == 0) throw new InternalExpressionException("'set_poi' requires at least one parameter");
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0, false);
            BlockPos pos = locator.block.getPos();
            if (lv.size() < locator.offset) throw new InternalExpressionException("'set_poi' requires the new poi type or null, after position argument");
            Value poi = lv.get(locator.offset+0);
            PointOfInterestStorage store = cc.s.getWorld().getPointOfInterestStorage();
            if (poi.isNull())
            {   // clear poi information
                if (!store.getType(pos).isPresent()) return Value.FALSE;
                store.remove(pos);
                return Value.TRUE;
            }
            String poiTypeString = poi.getString().toLowerCase(Locale.ROOT);
            PointOfInterestType type =  Registry.POINT_OF_INTEREST_TYPE.getOrEmpty(InputValidator.identifierOf(poiTypeString))
            		.orElseThrow(() -> new ThrowStatement(poiTypeString, Throwables.UNKNOWN_POI));
            int occupancy = 0;
            if (locator.offset + 1 < lv.size())
            {
                occupancy = (int)NumericValue.asNumber(lv.get(locator.offset + 1)).getLong();
                if (occupancy < 0) throw new InternalExpressionException("Occupancy cannot be negative");
            }
            if (store.getType(pos).isPresent()) store.remove(pos);
            store.add(pos, type);
            // setting occupancy for a
            // again - don't want to mix in unnecessarily - peeps not gonna use it that often so not worries about it.
            if (occupancy > 0)
            {
                int finalO = occupancy;
                store.getInSquare((tt) -> tt==type, pos, 1, PointOfInterestStorage.OccupationStatus.ANY
                ).filter(p -> p.getPos().equals(pos)).findFirst().ifPresent(p -> {
                    for (int i=0; i < finalO; i++) ((PointOfInterest_scarpetMixin)p).callReserveTicket();
                });
            }
            return Value.TRUE;
        });


        expression.addContextFunction("weather",-1,(c, t, lv) -> {
            ServerWorld world = ((CarpetContext) c).s.getWorld();

            if(lv.size()==0)//cos it can thunder when raining or when clear.
                return new StringValue(world.isThundering() ? "thunder" : (world.isRaining() ? "rain" : "clear"));

            Value weather = lv.get(0);
            ServerWorldProperties worldProperties = ((ServerWorldInterface) world).getWorldPropertiesCM();
            if(lv.size()==1)
            {
                int ticks;
                switch (weather.getString().toLowerCase(Locale.ROOT)) {
                    case "clear":
                        ticks = worldProperties.getClearWeatherTime();
                        break;
                    case "rain":
                        ticks = world.isRaining()? worldProperties.getRainTime():0;//cos if not it gives 1 for some reason
                        break;
                    case "thunder":
                        ticks = world.isThundering()? worldProperties.getThunderTime():0;//same dealio here
                        break;
                    default:
                        throw new InternalExpressionException("Weather can only be 'clear', 'rain' or 'thunder'");
                }
                return new NumericValue(ticks);
            }
            if(lv.size()==2)
            {
                int ticks = NumericValue.asNumber(lv.get(1), "tick_time in 'weather'").getInt();
                switch (weather.getString().toLowerCase(Locale.ROOT))
                {
                    case "clear":
                        world.setWeather(ticks,0,false,false);
                        break;

                    case "rain":
                        world.setWeather(0,ticks,true,false);
                        break;

                    case "thunder":
                        world.setWeather(
                                0,
                                ticks,//this is used to set thunder time, idk why...
                                true,
                                true
                        );
                        break;

                    default:
                        throw new InternalExpressionException("Weather can only be 'clear', 'rain' or 'thunder'");
                }
                return NumericValue.of(ticks);
            }
            throw new InternalExpressionException("'weather' requires 0, 1 or 2 arguments");
        });

        expression.addUnaryFunction("pos", v ->
        {
            if (v instanceof BlockValue)
            {
                BlockPos pos = ((BlockValue) v).getPos();
                if (pos == null)
                    throw new InternalExpressionException("Cannot fetch position of an unrealized block");
                return ListValue.of(new NumericValue(pos.getX()), new NumericValue(pos.getY()), new NumericValue(pos.getZ()));
            }
            else if (v instanceof EntityValue)
            {
                Entity e = ((EntityValue) v).getEntity();
                if (e == null)
                    throw new InternalExpressionException("Null entity");
                return ListValue.of(new NumericValue(e.getX()), new NumericValue(e.getY()), new NumericValue(e.getZ()));
            }
            else
            {
                throw new InternalExpressionException("'pos' works only with a block or an entity type");
            }
        });

        expression.addContextFunction("pos_offset", -1, (c, t, lv) ->
        {
            BlockArgument locator = BlockArgument.findIn((CarpetContext)c, lv, 0);
            BlockPos pos = locator.block.getPos();
            if (lv.size() <= locator.offset)
                throw new InternalExpressionException("'pos_offset' needs at least position, and direction");
            String directionString = lv.get(locator.offset).getString();
            Direction dir = DIRECTION_MAP.get(directionString);
            if (dir == null)
                throw new InternalExpressionException("Unknown direction: "+directionString);
            int howMuch = 1;
            if (lv.size() > locator.offset+1)
                howMuch = (int) NumericValue.asNumber(lv.get(locator.offset+1)).getLong();
            BlockPos retpos = pos.offset(dir, howMuch);
            return ListValue.of(new NumericValue(retpos.getX()), new NumericValue(retpos.getY()), new NumericValue(retpos.getZ()));
        });

        expression.addContextFunction("solid", -1, (c, t, lv) ->
                genericStateTest(c, "solid", lv, (s, p, w) -> BooleanValue.of(s.isSolidBlock(w, p)))); // isSimpleFullBlock

        expression.addContextFunction("air", -1, (c, t, lv) ->
                booleanStateTest(c, "air", lv, (s, p) -> s.isAir()));

        expression.addContextFunction("liquid", -1, (c, t, lv) ->
                booleanStateTest(c, "liquid", lv, (s, p) -> !s.getFluidState().isEmpty()));

        expression.addContextFunction("flammable", -1, (c, t, lv) ->
                booleanStateTest(c, "flammable", lv, (s, p) -> s.getMaterial().isBurnable()));

        expression.addContextFunction("transparent", -1, (c, t, lv) ->
                booleanStateTest(c, "transparent", lv, (s, p) -> !s.getMaterial().isSolid()));

        /*this.expr.addContextFunction("opacity", -1, (c, t, lv) ->
                genericStateTest(c, "opacity", lv, (s, p, w) -> new NumericValue(s.getOpacity(w, p))));

        this.expr.addContextFunction("blocks_daylight", -1, (c, t, lv) ->
                genericStateTest(c, "blocks_daylight", lv, (s, p, w) -> new NumericValue(s.propagatesSkylightDown(w, p))));*/ // investigate

        expression.addContextFunction("emitted_light", -1, (c, t, lv) ->
                genericStateTest(c, "emitted_light", lv, (s, p, w) -> new NumericValue(s.getLuminance())));

        expression.addContextFunction("light", -1, (c, t, lv) ->
                genericStateTest(c, "light", lv, (s, p, w) -> new NumericValue(Math.max(w.getLightLevel(LightType.BLOCK, p), w.getLightLevel(LightType.SKY, p)))));

        expression.addContextFunction("block_light", -1, (c, t, lv) ->
                genericStateTest(c, "block_light", lv, (s, p, w) -> new NumericValue(w.getLightLevel(LightType.BLOCK, p))));

        expression.addContextFunction("sky_light", -1, (c, t, lv) ->
                genericStateTest(c, "sky_light", lv, (s, p, w) -> new NumericValue(w.getLightLevel(LightType.SKY, p))));

        expression.addContextFunction("see_sky", -1, (c, t, lv) ->
                genericStateTest(c, "see_sky", lv, (s, p, w) -> BooleanValue.of(w.isSkyVisible(p))));

        expression.addContextFunction("brightness", -1, (c, t, lv) ->
                genericStateTest(c, "brightness", lv, (s, p, w) -> new NumericValue(w.getBrightness(p))));

        expression.addContextFunction("hardness", -1, (c, t, lv) ->
                genericStateTest(c, "hardness", lv, (s, p, w) -> new NumericValue(s.getHardness(w, p))));

        expression.addContextFunction("blast_resistance", -1, (c, t, lv) ->
                genericStateTest(c, "blast_resistance", lv, (s, p, w) -> new NumericValue(s.getBlock().getBlastResistance())));

        expression.addContextFunction("in_slime_chunk", -1, (c, t, lv) ->
        {
            BlockPos pos = BlockArgument.findIn((CarpetContext)c, lv, 0).block.getPos();
            ChunkPos chunkPos = new ChunkPos(pos);
            return BooleanValue.of(ChunkRandom.getSlimeRandom(
                    chunkPos.x, chunkPos.z,
                    ((CarpetContext)c).s.getWorld().getSeed(),
                    987234911L
            ).nextInt(10) == 0);
        });

        expression.addContextFunction("top", -1, (c, t, lv) ->
        {
            String type = lv.get(0).getString().toLowerCase(Locale.ROOT);
            Heightmap.Type htype;
            switch (type)
            {
                //case "light": htype = Heightmap.Type.LIGHT_BLOCKING; break;  //investigate
                case "motion": htype = Heightmap.Type.MOTION_BLOCKING; break;
                case "terrain": htype = Heightmap.Type.MOTION_BLOCKING_NO_LEAVES; break;
                case "ocean_floor": htype = Heightmap.Type.OCEAN_FLOOR; break;
                case "surface": htype = Heightmap.Type.WORLD_SURFACE; break;
                default: throw new InternalExpressionException("Unknown heightmap type: "+type);
            }
            BlockArgument locator = BlockArgument.findIn((CarpetContext)c, lv, 1);
            BlockPos pos = locator.block.getPos();
            int x = pos.getX();
            int z = pos.getZ();
            return new NumericValue(((CarpetContext)c).s.getWorld().getChunk(x >> 4, z >> 4).sampleHeightmap(htype, x & 15, z & 15) + 1);
        });

        expression.addContextFunction("loaded", -1, (c, t, lv) ->
                BooleanValue.of((((CarpetContext) c).s.getWorld().isChunkLoaded(BlockArgument.findIn((CarpetContext) c, lv, 0).block.getPos()))));

        // Deprecated, use loaded_status as more indicative
        expression.addContextFunction("loaded_ep", -1, (c, t, lv) ->
        {
            c.host.issueDeprecation("loaded_ep(...)");
            BlockPos pos = BlockArgument.findIn((CarpetContext)c, lv, 0).block.getPos();
            return BooleanValue.of(((CarpetContext)c).s.getWorld().method_37118(pos));// 1.17pre1 getChunkManager().shouldTickChunk(new ChunkPos(pos)));
        });

        expression.addContextFunction("loaded_status", -1, (c, t, lv) ->
        {
            BlockPos pos = BlockArgument.findIn((CarpetContext)c, lv, 0).block.getPos();
            WorldChunk chunk = ((CarpetContext)c).s.getWorld().getChunkManager().getWorldChunk(pos.getX()>>4, pos.getZ()>>4, false);
            if (chunk == null) return Value.ZERO;
            return new NumericValue(chunk.getLevelType().ordinal());
        });

        expression.addContextFunction("is_chunk_generated", -1, (c, t, lv) ->
        {
            BlockArgument locator = BlockArgument.findIn((CarpetContext)c, lv, 0);
            BlockPos pos = locator.block.getPos();
            boolean force = false;
            if (lv.size() > locator.offset)
                force = lv.get(locator.offset).getBoolean();
            return BooleanValue.of(canHasChunk(((CarpetContext)c).s.getWorld(), new ChunkPos(pos), null, force));
        });

        expression.addContextFunction("generation_status", -1, (c, t, lv) ->
        {
            BlockArgument blockArgument = BlockArgument.findIn((CarpetContext)c, lv, 0);
            BlockPos pos = blockArgument.block.getPos();
            boolean forceLoad = false;
            if (lv.size() > blockArgument.offset)
                forceLoad = lv.get(blockArgument.offset).getBoolean();
            Chunk chunk = ((CarpetContext)c).s.getWorld().getChunk(pos.getX()>>4, pos.getZ()>>4, ChunkStatus.EMPTY, forceLoad);
            if (chunk == null) return Value.NULL;
            return new StringValue(chunk.getStatus().getId());
        });

        expression.addContextFunction("chunk_tickets", -1, (c, t, lv) ->
        {
            ServerWorld world = ((CarpetContext) c).s.getWorld();
            Long2ObjectOpenHashMap<SortedArraySet<ChunkTicket<?>>> levelTickets = (
                    (ChunkTicketManagerInterface) ((ServerChunkManagerInterface) world.getChunkManager())
                            .getCMTicketManager()
            ).getTicketsByPosition();
            List<Value> res = new ArrayList<>();
            if (lv.size() == 0)
            {
                for (long key : levelTickets.keySet())
                {
                    ChunkPos chpos = new ChunkPos(key);
                    for (ChunkTicket ticket : levelTickets.get(key))
                    {
                        res.add(ListValue.of(
                                new StringValue(ticket.getType().toString()),
                                new NumericValue(33 - ticket.getLevel()),
                                new NumericValue(chpos.x),
                                new NumericValue(chpos.z)
                        ));
                    }
                }
            }
            else
            {
                BlockArgument blockArgument = BlockArgument.findIn((CarpetContext) c, lv, 0);
                BlockPos pos = blockArgument.block.getPos();
                SortedArraySet<ChunkTicket<?>> tickets = levelTickets.get(new ChunkPos(pos).toLong());
                if (tickets != null)
                {
                    for (ChunkTicket ticket : tickets)
                    {
                        res.add(ListValue.of(
                                new StringValue(ticket.getType().toString()),
                                new NumericValue(33 - ticket.getLevel())
                        ));
                    }
                }
            }
            res.sort(Comparator.comparing(e -> ((ListValue) e).getItems().get(1)).reversed());
            return ListValue.wrap(res);
        });

        expression.addContextFunction("suffocates", -1, (c, t, lv) ->
                genericStateTest(c, "suffocates", lv, (s, p, w) -> BooleanValue.of(s.shouldSuffocate(w, p)))); // canSuffocate

        expression.addContextFunction("power", -1, (c, t, lv) ->
                genericStateTest(c, "power", lv, (s, p, w) -> new NumericValue(w.getReceivedRedstonePower(p))));

        expression.addContextFunction("ticks_randomly", -1, (c, t, lv) ->
                booleanStateTest(c, "ticks_randomly", lv, (s, p) -> s.hasRandomTicks()));

        expression.addContextFunction("update", -1, (c, t, lv) ->
                booleanStateTest(c, "update", lv, (s, p) ->
                {
                    ((CarpetContext) c).s.getWorld().updateNeighbor(p, s.getBlock(), p);
                    return true;
                }));

        expression.addContextFunction("block_tick", -1, (c, t, lv) ->
                booleanStateTest(c, "block_tick", lv, (s, p) ->
                {
                    ServerWorld w = ((CarpetContext)c).s.getWorld();
                    s.randomTick(w, p, w.random);
                    return true;
                }));

        expression.addContextFunction("random_tick", -1, (c, t, lv) ->
                booleanStateTest(c, "random_tick", lv, (s, p) ->
                {
                    ServerWorld w = ((CarpetContext)c).s.getWorld();
                    if (s.hasRandomTicks() || s.getFluidState().hasRandomTicks())
                        s.randomTick(w, p, w.random);
                    return true;
                }));

        // lazy cause its parked execution
        expression.addLazyFunction("without_updates", 1, (c, t, lv) ->
        {
            boolean previous = CarpetSettings.impendingFillSkipUpdates.get();
            if (previous) return lv.get(0);
            Value [] result = new Value[]{Value.NULL};
            ((CarpetContext)c).s.getServer().submitAndJoin( () ->
            {
                try
                {
                    CarpetSettings.impendingFillSkipUpdates.set(true);
                    result[0] = lv.get(0).evalValue(c, t);
                }
                finally
                {
                    CarpetSettings.impendingFillSkipUpdates.set(previous);
                }
            });
            return (cc, tt) -> result[0];
        });
        // =======================================
        expression.addContextFunction("save_structure_template", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext) c;
            ServerWorld lv2 = cc.s.getWorld();

            StructureManager lv3 = lv2.getStructureManager();
            Structure lv6;
            String name;

            boolean returnnbt=lv.get(0).isNull();
            Identifier struident=null;
            if (!returnnbt){
                name = lv.get(0).getString();
                struident = Identifier.tryParse(name);
                if (name.isEmpty()||name.endsWith(":")||struident == null) {
                    return Value.NULL;
                }if(lv.size()==2&&lv.get(1).isNull()){lv3.unloadStructure(struident);return Value.NULL;}
                lv6 = lv3.getStructureOrBlank(struident);
            }
            else{
                lv6= new Structure();
            }
            BlockArgument start = BlockArgument.findIn((CarpetContext)c, lv, 1);
            BlockArgument end = BlockArgument.findIn((CarpetContext)c, lv, start.offset);
            BlockArgument ignoredblock = BlockArgument.findIn((CarpetContext)c, lv, end.offset+1,true,true,false);
            
            BlockPos startBlockPos = new BlockPos(
                Math.min(start.block.getPos().getX(), end.block.getPos().getX()),
                Math.min(start.block.getPos().getY(), end.block.getPos().getY()),
                Math.min(start.block.getPos().getZ(), end.block.getPos().getZ())
            );
            BlockPos endBlockPos = new BlockPos(
                Math.max(start.block.getPos().getX(), end.block.getPos().getX()),
                Math.max(start.block.getPos().getY(), end.block.getPos().getY()),
                Math.max(start.block.getPos().getZ(), end.block.getPos().getZ())
            );
            BlockPos size = endBlockPos.subtract(startBlockPos).add(1,1,1);
            // name,start,end, ignoreEntities, ignoredBlock,~~author~~,disk
            lv6.saveFromWorld(lv2,
                    startBlockPos,
                    size,
                    !lv.get(end.offset).getBoolean(),
                    ignoredblock.block==null?null:ignoredblock.block.getBlockState().getBlock());
            // lv6.setAuthor(lv.get(9).getString());//MC-140821
            if (!returnnbt){
                if (lv.get(ignoredblock.offset).getBoolean()) {
                    return BooleanValue.of(lv3.saveStructure(struident));
                }
            }
            else{
                return new NBTSerializableValue(lv6.writeNbt(new NbtCompound()));
            }

            return Value.TRUE;
        });

        // =====================================
        // =======================================
        expression.addContextFunction("load_structure_template", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext) c;
            ServerWorld world = cc.s.getWorld();
            boolean recievnbt=(lv.get(0) instanceof NBTSerializableValue) && (((NBTSerializableValue)lv.get(0)).getTag() instanceof NbtCompound);
            Structure thestr;
            if(!recievnbt){
                String name = lv.get(0).getString();

                StructureManager sm = world.getStructureManager();
                Optional<Structure> optional;

                Identifier struident = Identifier.tryParse(name);
                if (name.isEmpty()||name.endsWith(":")||struident == null) {
                    return Value.NULL;
                }
                optional = sm.getStructure(struident);
                if (!optional.isPresent()) {
                    return Value.FALSE;
                }
                thestr=optional.get();
            }else{
                thestr=world.getStructureManager().createStructure((NbtCompound)((NBTSerializableValue)lv.get(0)).getTag());
            }
            BlockArgument start = BlockArgument.findIn((CarpetContext)c, lv, 1);
            // name,pos,
            // ignoreEntities,integrity,awake,noupdate,fluid,gravity,rotation,mirror
            StructurePlacementData lv4 = new StructurePlacementData();
            lv4.clearProcessors();

            lv4.setIgnoreEntities(lv.get(start.offset).getBoolean())
                    .setInitializeMobs(lv.get(start.offset+2).getBoolean())
                    .setUpdateNeighbors(lv.get(start.offset+3).getBoolean())
                    .setPlaceFluids(lv.get(start.offset+4).getBoolean())
                    .setMirror(lv.get(start.offset+7).getString().equalsIgnoreCase("Z") ? BlockMirror.LEFT_RIGHT
                            : lv.get(start.offset+7).getString().equalsIgnoreCase("X") ? BlockMirror.FRONT_BACK : BlockMirror.NONE)
                    .setRotation(BlockRotation.values()[(((NumericValue) lv.get(start.offset+6)).getInt() % 4 + 4) % 4]);

            if (!lv.get(start.offset+5).isNull()) {
                lv4.addProcessor(new GravityStructureProcessor(Heightmap.Type.WORLD_SURFACE,
                        ((NumericValue) lv.get(start.offset+5)).getInt()));
            }
            if (!lv.get(start.offset+1).isNull()) {
                lv4.addProcessor(new BlockRotStructureProcessor(
                        MathHelper.clamp(((NumericValue) lv.get(start.offset+1)).getFloat(), 0.0f, 1.0f)));
            }

            BlockPos lv5 = start.block.getPos();
            thestr.place(world, lv5, lv5, lv4, new Random(), Block.NOTIFY_LISTENERS);
            return Value.TRUE;
        });

        // =====================================
        // =======================================
        expression.addContextFunction("unload_structure_template", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext) c;
            ServerWorld world = cc.s.getWorld();
            String name = lv.get(0).getString();

            StructureManager sm = world.getStructureManager();

            Identifier struident = Identifier.tryParse(name);
            if (struident == null) {
                return Value.NULL;
            }
            sm.unloadStructure(struident);
            return Value.NULL;

        });

        // =====================================
        expression.addContextFunction("set", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerWorld world = cc.s.getWorld();
            BlockArgument targetLocator = BlockArgument.findIn(cc, lv, 0);
            BlockArgument sourceLocator = BlockArgument.findIn(cc, lv, targetLocator.offset, true);
            BlockState sourceBlockState = sourceLocator.block.getBlockState();
            BlockState targetBlockState = world.getBlockState(targetLocator.block.getPos());
            NbtCompound data = null;
            if (lv.size() > sourceLocator.offset)
            {
                List<Value> args = new ArrayList<>();
                for (int i = sourceLocator.offset, m = lv.size(); i < m; i++)
                {
                    args.add(lv.get(i));
                }
                if (args.get(0) instanceof ListValue)
                {
                    if (args.size() == 2)
                    {
                        Value dataValue = NBTSerializableValue.fromValue( args.get(1));
                        if (dataValue instanceof NBTSerializableValue)
                        {
                            data = ((NBTSerializableValue) dataValue).getCompoundTag();
                        }
                    }
                    args = ((ListValue) args.get(0)).getItems();
                }
                else if (args.get(0) instanceof MapValue)
                {
                    if (args.size() == 2)
                    {
                        Value dataValue = NBTSerializableValue.fromValue( args.get(1));
                        if (dataValue instanceof NBTSerializableValue)
                        {
                            data = ((NBTSerializableValue) dataValue).getCompoundTag();
                        }
                    }
                    Map<Value, Value> state = ((MapValue) args.get(0)).getMap();
                    List<Value> mapargs = new ArrayList<>();
                    state.forEach( (k, v) -> {mapargs.add(k); mapargs.add(v);});
                    args = mapargs;
                }
                else
                {
                    if ((args.size() & 1) == 1)
                    {
                        Value dataValue = NBTSerializableValue.fromValue( args.get(args.size()-1));
                        if (dataValue instanceof NBTSerializableValue)
                        {
                            data = ((NBTSerializableValue) dataValue).getCompoundTag();
                        }
                    }
                }
                StateManager<Block, BlockState> states = sourceBlockState.getBlock().getStateManager();
                for (int i = 0; i < args.size()-1; i += 2)
                {
                    String paramString = args.get(i).getString();
                    Property<?> property = states.getProperty(paramString);
                    if (property == null)
                        throw new InternalExpressionException("Property " + paramString + " doesn't apply to " + sourceLocator.block.getString());
                    String paramValue = args.get(i + 1).getString();
                    sourceBlockState = setProperty(property, paramString, paramValue, sourceBlockState);
                }
            }

            if (data == null) data = sourceLocator.block.getData();
            NbtCompound finalData = data;

            if (sourceBlockState == targetBlockState && data == null)
                return Value.FALSE;
            BlockState finalSourceBlockState = sourceBlockState;
            BlockPos targetPos = targetLocator.block.getPos();
            Boolean[] result = new Boolean[]{true};
            cc.s.getServer().submitAndJoin( () ->
            {
                Clearable.clear(world.getBlockEntity(targetPos));
                boolean success = world.setBlockState(targetPos, finalSourceBlockState, 2);
                if (finalData != null)
                {
                    BlockEntity be = world.getBlockEntity(targetPos);
                    if (be != null)
                    {
                        NbtCompound destTag = finalData.copy();
                        destTag.putInt("x", targetPos.getX());
                        destTag.putInt("y", targetPos.getY());
                        destTag.putInt("z", targetPos.getZ());
                        be.readNbt(destTag);
                        be.markDirty();
                        success = true;
                    }
                }
                result[0] = success;
            });
            if (!result[0]) return Value.FALSE;
            return new BlockValue(finalSourceBlockState, world, targetLocator.block.getPos());
        });

        expression.addContextFunction("destroy", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerWorld world = cc.s.getWorld();
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            BlockState state = locator.block.getBlockState();
            if (state.isAir()) return Value.FALSE;
            BlockPos where = locator.block.getPos();
            BlockEntity be = world.getBlockEntity(where);
            long how = 0;
            Item item = Items.DIAMOND_PICKAXE;
            boolean playerBreak = false;
            if (lv.size() > locator.offset)
            {
                Value val = lv.get(locator.offset);
                if (val instanceof NumericValue)
                {
                    how = ((NumericValue) val).getLong();
                }
                else
                {
                    playerBreak = true;
                    String itemString = val.getString();
                    item = Registry.ITEM.getOrEmpty(InputValidator.identifierOf(itemString))
                            .orElseThrow(() -> new ThrowStatement(itemString, Throwables.UNKNOWN_ITEM));
                }
            }
            NbtCompound tag = null;
            if (lv.size() > locator.offset+1)
            {
                if (!playerBreak) throw new InternalExpressionException("tag is not necessary with 'destroy' with no item");
                Value tagValue = lv.get(locator.offset+1);
                if (tagValue instanceof NullValue)
                    tag = null;
                else if (tagValue instanceof NBTSerializableValue)
                    tag = ((NBTSerializableValue) tagValue).getCompoundTag();
                else
                {
                    NBTSerializableValue readTag = NBTSerializableValue.parseString(tagValue.getString(), true);
                    tag = readTag.getCompoundTag();
                }
            }

            ItemStack tool = new ItemStack(item, 1);
            if (tag != null)
                tool.setNbt(tag);
            if (playerBreak && state.getHardness(world, where) < 0.0) return Value.FALSE;
            boolean removed = world.removeBlock(where, false);
            if (!removed) return Value.FALSE;
            world.syncWorldEvent(null, 2001, where, Block.getRawIdFromState(state));

            boolean toolBroke = false;
            boolean dropLoot = true;
            if (playerBreak)
            {
                boolean isUsingEffectiveTool = !state.isToolRequired() || tool.isSuitableFor(state);
                //postMine() durability from item classes
                float hardness = state.getHardness(world, where);
                int damageAmount = 0;
                if ((item instanceof MiningToolItem && hardness > 0.0) || item instanceof ShearsItem)
                {
                    damageAmount = 1;
                }
                else if (item instanceof TridentItem || item instanceof SwordItem)
                {
                    damageAmount = 2;
                }
                toolBroke = damageAmount>0 && tool.damage(damageAmount, world.getRandom(), null);
                if (!isUsingEffectiveTool)
                    dropLoot = false;
            }

            if (dropLoot)
            {
                if (how < 0 || (tag != null && EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, tool) > 0))
                {
                    Block.dropStack(world, where, new ItemStack(state.getBlock()));
                }
                else
                {
                    if (how > 0)
                        tool.addEnchantment(Enchantments.FORTUNE, (int) how);
                    if (DUMMY_ENTITY == null) DUMMY_ENTITY = new FallingBlockEntity(EntityType.FALLING_BLOCK, null);
                    Block.dropStacks(state, world, where, be, DUMMY_ENTITY, tool);
                }
            }
            if (!playerBreak) // no tool info - block brokwn
                return Value.TRUE;
            if (toolBroke)
                return Value.NULL;
            NbtElement outtag = tool.getNbt();
            if (outtag == null)
                return Value.TRUE;
            return new NBTSerializableValue(() -> outtag);

        });

        expression.addContextFunction("harvest", -1, (c, t, lv) ->
        {
            if (lv.size()<2)
                throw new InternalExpressionException("'harvest' takes at least 2 parameters: entity and block, or position, to harvest");
            CarpetContext cc = (CarpetContext)c;
            World world = cc.s.getWorld();
            Value entityValue = lv.get(0);
            if (!(entityValue instanceof EntityValue))
                return Value.FALSE;
            Entity e = ((EntityValue) entityValue).getEntity();
            if (!(e instanceof ServerPlayerEntity))
                return Value.FALSE;
            ServerPlayerEntity player = (ServerPlayerEntity)e;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 1);
            BlockPos where = locator.block.getPos();
            BlockState state = locator.block.getBlockState();
            Block block = state.getBlock();
            boolean success = false;
            if (!((block == Blocks.BEDROCK || block == Blocks.BARRIER) && player.interactionManager.isSurvivalLike()))
                success = player.interactionManager.tryBreakBlock(where);
            if (success)
                world.syncWorldEvent(null, 2001, where, Block.getRawIdFromState(state));
            return BooleanValue.of(success);
        });

        expression.addContextFunction("create_explosion", -1, (c, t, lv) ->
        {
            if (lv.isEmpty())
                throw new InternalExpressionException("'create_explosion' requires at least a position to explode");
            CarpetContext cc = (CarpetContext)c;
            float powah = 4.0f;
            Explosion.DestructionType mode = Explosion.DestructionType.BREAK;
            boolean createFire = false;
            Entity source = null;
            LivingEntity attacker = null;
            Vector3Argument location = Vector3Argument.findIn(lv, 0, false, true);
            Vec3d pos = location.vec;
            if (lv.size() > location.offset)
            {
                powah = NumericValue.asNumber(lv.get(location.offset), "explosion power").getFloat();
                if (powah < 0) throw new InternalExpressionException("Explosion power cannot be negative");
                if (lv.size() > location.offset+1)
                {
                    String strval = lv.get(location.offset+1).getString();
                    try {
                        mode = Explosion.DestructionType.valueOf(strval.toUpperCase(Locale.ROOT));
                    }
                    catch (IllegalArgumentException ile) { throw new InternalExpressionException("Illegal explosions block behaviour: "+strval); }
                    if (lv.size() > location.offset+2)
                    {
                        createFire = lv.get(location.offset+2).getBoolean();
                        if (lv.size() > location.offset+3)
                        {
                            Value enVal= lv.get(location.offset+3);
                            if (enVal.isNull()) {} // is null already
                            else if (enVal instanceof EntityValue)
                            {
                                source = ((EntityValue) enVal).getEntity();
                            }
                            else
                            {
                                throw new InternalExpressionException("Fourth parameter of the explosion has to be an entity, not "+enVal.getTypeString());
                            }
                            if (lv.size() > location.offset+4)
                            {
                                enVal = lv.get(location.offset+4);
                                if (enVal.isNull()) {} // is null already
                                else if (enVal instanceof EntityValue)
                                {
                                    Entity attackingEntity =  ((EntityValue) enVal).getEntity();
                                    if (attackingEntity instanceof LivingEntity)
                                    {
                                        attacker = (LivingEntity) attackingEntity;
                                    }
                                    else throw new InternalExpressionException("Attacking entity needs to be a living thing, "+
                                            ValueConversions.of(Registry.ENTITY_TYPE.getId(attackingEntity.getType())).getString() +" ain't it.");

                                }
                                else
                                {
                                    throw new InternalExpressionException("Fifth parameter of the explosion has to be a living entity, not "+enVal.getTypeString());
                                }
                            }
                        }
                    }
                }
            }
            LivingEntity theAttacker = attacker;
            float thePowah = powah;

            // copy of ServerWorld.createExplosion #TRACK#
            Explosion explosion = new Explosion(cc.s.getWorld(), source, null, null, pos.x, pos.y, pos.z, powah, createFire, mode){
                @Override
                public @Nullable LivingEntity getCausingEntity() {
                    return theAttacker;
                }
            };
            explosion.collectBlocksAndDamageEntities();
            explosion.affectWorld(false);
            if (mode == Explosion.DestructionType.NONE) explosion.clearAffectedBlocks();
            cc.s.getWorld().getPlayers().forEach(spe -> {
                if (spe.squaredDistanceTo(pos) < 4096.0D)
                    spe.networkHandler.sendPacket(new ExplosionS2CPacket(pos.x, pos.y, pos.z, thePowah, explosion.getAffectedBlocks(), explosion.getAffectedPlayers().get(spe)));
            });
            return Value.TRUE;
        });

        // TODO rename to use_item
        expression.addContextFunction("place_item", -1, (c, t, lv) ->
        {
            if (lv.size()<2)
                throw new InternalExpressionException("'place_item' takes at least 2 parameters: item and block, or position, to place onto");
            CarpetContext cc = (CarpetContext) c;
            String itemString = lv.get(0).getString();
            Vector3Argument locator = Vector3Argument.findIn(lv, 1);
            ItemStackArgument stackArg = NBTSerializableValue.parseItem(itemString);
            BlockPos where = new BlockPos(locator.vec);
            String facing = "up";
            if (lv.size() > locator.offset)
                facing = lv.get(locator.offset).getString();
            boolean sneakPlace = false;
            if (lv.size() > locator.offset+1)
                sneakPlace = lv.get(locator.offset+1).getBoolean();

            BlockValue.PlacementContext ctx;
            try
            {
                ctx = BlockValue.PlacementContext.from(cc.s.getWorld(), where, facing, sneakPlace, stackArg.createStack(1, false));
            }
            catch (CommandSyntaxException e)
            {
                throw new InternalExpressionException(e.getMessage());
            }

            if (!(stackArg.getItem() instanceof BlockItem))
            {
                ActionResult useResult = ctx.getStack().useOnBlock(ctx);
                if (useResult == ActionResult.CONSUME || useResult == ActionResult.SUCCESS)
                {
                    return Value.TRUE;
                }
            }
            else
            { // not sure we need special case for block items, since useOnBlock can do that as well
                BlockItem blockItem = (BlockItem) stackArg.getItem();
                if (!ctx.canPlace()) return Value.FALSE;
                BlockState placementState = blockItem.getBlock().getPlacementState(ctx);
                if (placementState != null)
                {
                    if (placementState.canPlaceAt(cc.s.getWorld(), where))
                    {
                        cc.s.getWorld().setBlockState(where, placementState, 2);
                        BlockSoundGroup blockSoundGroup = placementState.getSoundGroup();
                        cc.s.getWorld().playSound(null, where, blockSoundGroup.getPlaceSound(), SoundCategory.BLOCKS, (blockSoundGroup.getVolume() + 1.0F) / 2.0F, blockSoundGroup.getPitch() * 0.8F);
                        return Value.TRUE;
                    }
                }
            }
            return Value.FALSE;
        });

        expression.addContextFunction("blocks_movement", -1, (c, t, lv) ->
                booleanStateTest(c, "blocks_movement", lv, (s, p) ->
                        !s.canPathfindThrough(((CarpetContext) c).s.getWorld(), p, NavigationType.LAND)));

        expression.addContextFunction("block_sound", -1, (c, t, lv) ->
                stateStringQuery(c, "block_sound", lv, (s, p) ->
                        BlockInfo.soundName.get(s.getSoundGroup())));

        expression.addContextFunction("material", -1, (c, t, lv) ->
                stateStringQuery(c, "material", lv, (s, p) ->
                        BlockInfo.materialName.get(s.getMaterial())));

        expression.addContextFunction("map_colour", -1, (c, t, lv) ->
                stateStringQuery(c, "map_colour", lv, (s, p) ->
                        BlockInfo.mapColourName.get(s.getMapColor(((CarpetContext)c).s.getWorld(), p))));


        // Deprecated for block_state()
        expression.addContextFunction("property", -1, (c, t, lv) ->
        {
            c.host.issueDeprecation("property(...)");
            BlockArgument locator = BlockArgument.findIn((CarpetContext) c, lv, 0);
            BlockState state = locator.block.getBlockState();
            if (lv.size() <= locator.offset)
                throw new InternalExpressionException("'property' requires to specify a property to query");
            String tag = lv.get(locator.offset).getString();
            StateManager<Block, BlockState> states = state.getBlock().getStateManager();
            Property<?> property = states.getProperty(tag);
            if (property == null) return Value.NULL;
            return new StringValue(state.get(property).toString().toLowerCase(Locale.ROOT));
        });

        // Deprecated for block_state()
        expression.addContextFunction("block_properties", -1, (c, t, lv) ->
        {
            c.host.issueDeprecation("block_properties(...)");
            BlockArgument locator = BlockArgument.findIn((CarpetContext) c, lv, 0);
            BlockState state = locator.block.getBlockState();
            StateManager<Block, BlockState> states = state.getBlock().getStateManager();
            return ListValue.wrap(states.getProperties().stream().map(
                    p -> new StringValue(p.getName())).collect(Collectors.toList())
            );
        });

        // block_state(block)
        // block_state(block, property)
        expression.addContextFunction("block_state", -1, (c, t, lv) ->
        {
            BlockArgument locator = BlockArgument.findIn((CarpetContext) c, lv, 0, true);
            BlockState state = locator.block.getBlockState();
            StateManager<Block, BlockState> states = state.getBlock().getStateManager();
            if (locator.offset == lv.size())
            {
                Map<Value,Value> properties = new HashMap<>();
                for(Property<?> p : states.getProperties())
                {
                    properties.put(StringValue.of(p.getName()), ValueConversions.fromProperty(state, p));// ValueConversions.fromObject(state.get(p), false));
                }
                return MapValue.wrap(properties);
            }
            String tag = lv.get(locator.offset).getString();
            Property<?> property = states.getProperty(tag);
            if (property == null)
                return Value.NULL;
            return ValueConversions.fromProperty(state, property);
        });

        expression.addContextFunction("block_list", -1, (c, t, lv) ->
        {
            if (lv.size() == 0)
                return ListValue.wrap(Registry.BLOCK.getIds().stream().map(ValueConversions::of).collect(Collectors.toList()));
            CarpetContext cc = (CarpetContext)c;
            TagManager tagManager = cc.s.getServer().getTagManager();
            String tag = lv.get(0).getString();
            net.minecraft.tag.Tag<Block> blockTag = tagManager.getOrCreateTagGroup(Registry.BLOCK_KEY).getTag(InputValidator.identifierOf(tag));
            if (blockTag == null) return Value.NULL;
            return ListValue.wrap(blockTag.values().stream().map(b -> ValueConversions.of(Registry.BLOCK.getId(b))).collect(Collectors.toList()));
        });

        expression.addContextFunction("block_tags", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            TagManager tagManager = cc.s.getServer().getTagManager();
            if (lv.size() == 0)
                return ListValue.wrap(tagManager.getOrCreateTagGroup(Registry.BLOCK_KEY).getTagIds().stream().map(ValueConversions::of).collect(Collectors.toList()));
            BlockArgument blockLocator = BlockArgument.findIn(cc, lv, 0, true);
            if (blockLocator.offset == lv.size())
            {
                Block target = blockLocator.block.getBlockState().getBlock();
                return ListValue.wrap(tagManager.getOrCreateTagGroup(Registry.BLOCK_KEY).getTags().entrySet().stream().filter(e -> e.getValue().contains(target)).map(e -> ValueConversions.of(e.getKey())).collect(Collectors.toList()));
            }
            String tag = lv.get(blockLocator.offset).getString();
            Tag<Block> blockTag = tagManager.getOrCreateTagGroup(Registry.BLOCK_KEY).getTag(InputValidator.identifierOf(tag));
            if (blockTag == null) return Value.NULL;
            return BooleanValue.of(blockLocator.block.getBlockState().isIn(blockTag));
        });


        expression.addContextFunction("biome", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            ServerWorld world = cc.s.getWorld();
            if (lv.size() == 0)
                return ListValue.wrap(world.getRegistryManager().get(Registry.BIOME_KEY).getIds().stream().map(ValueConversions::of));
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0, false, false, true);

            Biome biome;
            if (locator.replacement != null)
            {
                biome = world.getRegistryManager().get(Registry.BIOME_KEY).get(InputValidator.identifierOf(locator.replacement));
                if (biome == null) throw new ThrowStatement(locator.replacement, Throwables.UNKNOWN_BIOME) ;
            }
            else
            {
                BlockPos pos = locator.block.getPos();
                biome = world.getBiome(pos);
            }
            // in locatebiome
            if (locator.offset == lv.size())
            {
                Identifier biomeId = cc.s.getServer().getRegistryManager().get(Registry.BIOME_KEY).getId(biome);
                return new StringValue(NBTSerializableValue.nameFromRegistryId(biomeId));
            }
            String biomeFeature = lv.get(locator.offset).getString();
            BiFunction<ServerWorld, Biome, Value> featureProvider = BiomeInfo.biomeFeatures.get(biomeFeature);
            if (featureProvider == null) throw new InternalExpressionException("Unknown biome feature: "+biomeFeature);
            return featureProvider.apply(world, biome);
        });

        expression.addContextFunction("set_biome", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            if (lv.size() == locator.offset)
                throw new InternalExpressionException("'set_biome' needs a biome name as an argument");
            String biomeName = lv.get(locator.offset+0).getString();
            // from locatebiome command code
            Biome biome = cc.s.getServer().getRegistryManager().get(Registry.BIOME_KEY).getOrEmpty(InputValidator.identifierOf(biomeName))
                .orElseThrow(() -> new ThrowStatement(biomeName, Throwables.UNKNOWN_BIOME));
            boolean doImmediateUpdate = true;
            if (lv.size() > locator.offset+1)
            {
                doImmediateUpdate = lv.get(locator.offset+1).getBoolean();
            }
            ServerWorld world = cc.s.getWorld();
            BlockPos pos = locator.block.getPos();
            Chunk chunk = world.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.BIOMES);
            ((BiomeArrayInterface)chunk.getBiomeArray()).setBiomeAtIndex(pos, world,  biome);
            if (doImmediateUpdate) WorldTools.forceChunkUpdate(pos, world);
            return Value.TRUE;
        });

        expression.addContextFunction("reload_chunk", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            BlockPos pos = BlockArgument.findIn(cc, lv, 0).block.getPos();
            ServerWorld world = cc.s.getWorld();
            cc.s.getServer().submitAndJoin( () -> WorldTools.forceChunkUpdate(pos, world));
            return Value.TRUE;
        });

        expression.addContextFunction("structure_references", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            ServerWorld world = cc.s.getWorld();
            BlockPos pos = locator.block.getPos();
            Map<StructureFeature<?>, LongSet> references = world.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.STRUCTURE_REFERENCES).getStructureReferences();
            if (lv.size() == locator.offset)
                return ListValue.wrap(references.entrySet().stream().
                        filter(e -> e.getValue()!= null && !e.getValue().isEmpty()).
                        map(e -> new StringValue(NBTSerializableValue.nameFromRegistryId(Registry.STRUCTURE_FEATURE.getId(e.getKey())))).collect(Collectors.toList())
                );
            String simpleStructureName = lv.get(locator.offset).getString().toLowerCase(Locale.ROOT);
            //CarpetSettings.LOG.error(FeatureGenerator.featureToStructure.keySet().stream().collect(Collectors.joining(",")));
            //CarpetSettings.LOG.error(FeatureGenerator.featureToStructure.values().stream().collect(Collectors.joining(",")));
            StructureFeature<?> structureName = Registry.STRUCTURE_FEATURE.get(InputValidator.identifierOf(simpleStructureName));
            if (structureName == null) return Value.NULL;
            LongSet structureReferences = references.get(structureName);
            if (structureReferences == null || structureReferences.isEmpty()) return ListValue.of();
            return ListValue.wrap(structureReferences.stream().map(l -> ListValue.of(
                    new NumericValue(16*ChunkPos.getPackedX(l)),
                    Value.ZERO,
                    new NumericValue(16*ChunkPos.getPackedZ(l)))).collect(Collectors.toList()));
        });

        expression.addContextFunction("structure_eligibility", -1, (c, t, lv) ->
        {// TODO rename structureName to class
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);

            ServerWorld world = cc.s.getWorld();

            // well, because
            BooYah(world.getChunkManager().getChunkGenerator());

            BlockPos pos = locator.block.getPos();
            StructureFeature<?> structure = null;
            boolean needSize = false;
            if (lv.size() > locator.offset)
            {
                Value requested = lv.get(locator.offset+0);
                if (!(requested instanceof NullValue))
                {
                    String reqString = requested.getString();
                    structure = Registry.STRUCTURE_FEATURE.getOrEmpty(InputValidator.identifierOf(reqString))
                            .orElseThrow(() -> new ThrowStatement(reqString, Throwables.UNKNOWN_STRUCTURE));
                }
                if (lv.size() > locator.offset+1)
                {
                    needSize = lv.get(locator.offset+1).getBoolean();
                }
            }
            if (structure != null)
            {
                StructureStart<?> start = FeatureGenerator.shouldStructureStartAt(world, pos, structure, needSize);
                if (start == null) return Value.NULL;
                if (!needSize) return Value.TRUE;
                return ValueConversions.of(start);
            }
            Map<Value, Value> ret = new HashMap<>();
            for(StructureFeature<?> str : StructureFeature.STRUCTURES.values())
            {
                StructureStart<?> start;
                try
                {
                    start = FeatureGenerator.shouldStructureStartAt(world, pos, str, needSize);
                }
                catch (NullPointerException npe)
                {
                    CarpetSettings.LOG.error("Failed to detect structure: "+str.getName());
                    start = null;
                }

                if (start == null) continue;

                Value key = new StringValue(NBTSerializableValue.nameFromRegistryId(Registry.STRUCTURE_FEATURE.getId(str)));
                ret.put(key, (!needSize)?Value.NULL: ValueConversions.of(start));
            }
            return MapValue.wrap(ret);
        });

        expression.addContextFunction("structures", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);

            ServerWorld world = cc.s.getWorld();
            BlockPos pos = locator.block.getPos();
            Map<StructureFeature<?>, StructureStart<?>> structures = world.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS).getStructureStarts();
            if (lv.size() == locator.offset)
            {
                Map<Value, Value> structureList = new HashMap<>();
                for (Map.Entry<StructureFeature<?>, StructureStart<?>> entry : structures.entrySet())
                {
                    StructureStart<?> start = entry.getValue();
                    if (start == StructureStart.DEFAULT)
                        continue;
                    BlockBox box = start.setBoundingBoxFromChildren();
                    structureList.put(
                            new StringValue(NBTSerializableValue.nameFromRegistryId(Registry.STRUCTURE_FEATURE.getId(entry.getKey()))),
                            ListValue.of(ListValue.fromTriple(box.getMinX(), box.getMinY(), box.getMinZ()), ListValue.fromTriple(box.getMaxX(), box.getMaxY(), box.getMaxZ()))
                    );
                }
                return MapValue.wrap(structureList);
            }
            String structureName = lv.get(locator.offset).getString().toLowerCase(Locale.ROOT);
            return ValueConversions.of(structures.get(Registry.STRUCTURE_FEATURE.get(InputValidator.identifierOf(structureName))));
        });

        expression.addContextFunction("set_structure", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);

            ServerWorld world = cc.s.getWorld();
            BlockPos pos = locator.block.getPos();

            if (lv.size() == locator.offset)
                throw new InternalExpressionException("'set_structure requires at least position and a structure name");
            String structureName = lv.get(locator.offset).getString().toLowerCase(Locale.ROOT);
            ConfiguredStructureFeature<?, ?> configuredStructure = FeatureGenerator.resolveConfiguredStructure(structureName, world, pos);
            if (configuredStructure == null) throw new ThrowStatement(structureName, Throwables.UNKNOWN_STRUCTURE);
            // good 'ol pointer
            Value[] result = new Value[]{Value.NULL};
            // technically a world modification. Even if we could let it slide, we will still park it
            ((CarpetContext) c).s.getServer().submitAndJoin(() ->
            {
                Map<StructureFeature<?>, StructureStart<?>> structures = world.getChunk(pos).getStructureStarts();
                if (lv.size() == locator.offset + 1)
                {
                    Boolean res = FeatureGenerator.plopGrid(configuredStructure, ((CarpetContext) c).s.getWorld(), locator.block.getPos());
                    //Boolean res = FeatureGenerator.gridStructure(structureName, ((CarpetContext) c).s.getWorld(), locator.block.getPos());
                    if (res == null) return;
                    result[0] = res?Value.TRUE:Value.FALSE;
                    return;
                }
                Value newValue = lv.get(locator.offset+1);
                if (newValue instanceof NullValue) // remove structure
                {
                    StructureFeature<?> structure = configuredStructure.feature;
                    if (!structures.containsKey(structure))
                    {
                        return;
                    }
                    StructureStart<?> start = structures.get(structure);
                    ChunkPos structureChunkPos = start.getPos(); //   new ChunkPos(start.getChunkX(), start.getChunkZ());
                    BlockBox box = start.setBoundingBoxFromChildren();
                    for (int chx = box.getMinX() / 16; chx <= box.getMaxX() / 16; chx++)  // minx maxx
                    {
                        for (int chz = box.getMinZ() / 16; chz <= box.getMaxZ() / 16; chz++) //minZ maxZ
                        {
                            ChunkPos chpos = new ChunkPos(chx, chz);
                            // getting a chunk will convert it to full, allowing to modify references
                            Map<StructureFeature<?>, LongSet> references =
                                    world.getChunk(chpos.getStartPos()).getStructureReferences();
                            if (references.containsKey(structure) && references.get(structure) != null)
                                references.get(structure).remove(structureChunkPos.toLong());
                        }
                    }
                    structures.remove(structure);
                    result[0] = Value.TRUE;
                }
            });
            return result[0]; // preventing from lazy evaluating of the result in case a future completes later
        });

        expression.addContextFunction("custom_dimension", -1, (c, t, lv) ->
        {
            if (lv.size() == 0) throw new InternalExpressionException("'custom_dimension' requires at least one argument");
            CarpetContext cc = (CarpetContext)c;
            cc.host.issueDeprecation("custom_dimension()");
            String worldKey = lv.get(0).getString();

            Long seed = null;
            if (lv.size() > 1)
            {
                String seedKey = lv.get(1).getString();
                try
                {
                    seed = Long.parseLong(seedKey);
                }
                catch (NumberFormatException ignored)
                {
                    throw new InternalExpressionException("Incorrect number format for seed: " + seedKey);
                }
            }

            boolean success = WorldTools.createWorld(cc.s.getServer(), worldKey, seed);
            if (!success) return Value.FALSE;
            CarpetServer.settingsManager.notifyPlayersCommandsChanged();
            return Value.TRUE;
        });


        expression.addContextFunction("reset_chunk", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            List<ChunkPos> requestedChunks = new ArrayList<>();
            if (lv.size() == 1)
            {
                //either one block or list of chunks
                Value first = lv.get(0);
                if (first instanceof ListValue)
                {
                    List<Value> listVal = ((ListValue) first).getItems();
                    int offset = 0;
                    BlockArgument locator = BlockArgument.findIn(cc, listVal, 0);
                    requestedChunks.add(new ChunkPos(locator.block.getPos()));
                    while (listVal.size() > locator.offset)
                    {
                        locator = BlockArgument.findIn(cc, listVal, locator.offset);
                        requestedChunks.add(new ChunkPos(locator.block.getPos()));
                    }
                }
                else
                {
                    BlockArgument locator = BlockArgument.findIn(cc, Collections.singletonList(first), 0);
                    requestedChunks.add(new ChunkPos(locator.block.getPos()));
                }
            }
            else
            {
                BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
                ChunkPos from = new ChunkPos(locator.block.getPos());
                if (lv.size() > locator.offset)
                {
                    locator = BlockArgument.findIn(cc, lv, locator.offset);
                    ChunkPos to = new ChunkPos(locator.block.getPos());
                    int xmax = Math.max(from.x, to.x);
                    int zmax = Math.max(from.z, to.z);
                    for (int x = Math.min(from.x, to.x); x <= xmax; x++) for (int z = Math.min(from.z, to.z); z <= zmax; z++)
                    {
                        requestedChunks.add(new ChunkPos(x,z));
                    }
                }
                else
                {
                    requestedChunks.add(from);
                }
            }


            ServerWorld world = cc.s.getWorld();

            Value [] result = new Value[]{Value.NULL};

            ((CarpetContext)c).s.getServer().submitAndJoin( () ->
            {
                Map<String, Integer> report = ((ThreadedAnvilChunkStorageInterface) world.getChunkManager().threadedAnvilChunkStorage).regenerateChunkRegion(requestedChunks);
                /*for (ChunkPos chpos: requestedChunks) // needed in 1.16 only
                {
                    if (world.getChunk(chpos.x, chpos.z, ChunkStatus.FULL, false) != null)
                    {
                        WorldTools.forceChunkUpdate(chpos.getStartPos(), world);
                    }
                }*/
                result[0] = MapValue.wrap(report.entrySet().stream().collect(Collectors.toMap(
                        e -> new StringValue((String)((Map.Entry) e).getKey()),
                        e ->  new NumericValue((Integer)((Map.Entry) e).getValue())
                )));
            });
            return result[0];
        });

        expression.addContextFunction("inhabited_time", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            BlockPos pos = locator.block.getPos();
            return new NumericValue(cc.s.getWorld().getChunk(pos).getInhabitedTime());
        });

        expression.addContextFunction("spawn_potential", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            BlockPos pos = locator.block.getPos();
            double required_charge = 1;
            if (lv.size() > locator.offset)
                required_charge = NumericValue.asNumber(lv.get(locator.offset)).getDouble();
            SpawnHelper.Info charger = cc.s.getWorld().getChunkManager().getSpawnInfo();
            if (charger == null) return Value.NULL;
            return new NumericValue(
                    ((SpawnHelperInnerInterface)charger).getPotentialCalculator().
                            calculate(pos, required_charge )
            );
        });

        expression.addContextFunction("add_chunk_ticket", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            BlockPos pos = locator.block.getPos();
            if (lv.size() != locator.offset+2) throw new InternalExpressionException("'add_chunk_ticket' requires block position, ticket type and radius");
            String type = lv.get(locator.offset).getString();
            ChunkTicketType ticket = ticketTypes.get(type.toLowerCase(Locale.ROOT));
            if (ticket == null) throw new InternalExpressionException("Unknown ticket type: "+type);
            int radius = NumericValue.asNumber(lv.get(locator.offset+1)).getInt();
            if (radius < 1 || radius > 32) throw new InternalExpressionException("Ticket radius should be between 1 and 32 chunks");
            // due to types we will wing it:
            ChunkPos target = new ChunkPos(pos);
            if (ticket == ChunkTicketType.PORTAL) // portal
                cc.s.getWorld().getChunkManager().addTicket(ChunkTicketType.PORTAL, target, radius, pos);
            else if (ticket == ChunkTicketType.POST_TELEPORT) // post teleport
                cc.s.getWorld().getChunkManager().addTicket(ChunkTicketType.POST_TELEPORT, target, radius, 1);
            else
                cc.s.getWorld().getChunkManager().addTicket(ChunkTicketType.UNKNOWN, target, radius, target);
            return new NumericValue(ticket.getExpiryTicks());
        });

    }
}
