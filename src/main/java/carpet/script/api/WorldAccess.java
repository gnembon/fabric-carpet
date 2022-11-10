package carpet.script.api;

import carpet.CarpetSettings;
import carpet.fakes.ChunkGeneratorInterface;
import carpet.fakes.ChunkTicketManagerInterface;
import carpet.fakes.ServerChunkManagerInterface;
import carpet.fakes.ServerWorldInterface;
import carpet.fakes.SpawnHelperInnerInterface;
import carpet.fakes.ThreadedAnvilChunkStorageInterface;
import carpet.helpers.FeatureGenerator;
import carpet.mixins.PoiRecord_scarpetMixin;
import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Fluff;
import carpet.script.annotation.Locator;
import carpet.script.annotation.ScarpetFunction;
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
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;
import carpet.utils.BlockInfo;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
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
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.Clearable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.Vec3;

import static carpet.script.utils.WorldTools.canHasChunk;

public class WorldAccess {
    private static final Map<String, Direction> DIRECTION_MAP = Arrays.stream(Direction.values()).collect(Collectors.toMap(Direction::getName, (direction) -> direction));
    static {
        DIRECTION_MAP.put("y", Direction.UP);
        DIRECTION_MAP.put("z", Direction.SOUTH);
        DIRECTION_MAP.put("x", Direction.EAST);

    }
    private static final Map<String, TicketType<?>> ticketTypes = Map.of(
        "portal", TicketType.PORTAL,
        "teleport", TicketType.POST_TELEPORT,
        "unknown", TicketType.UNKNOWN
    );
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
            Fluff.TriFunction<BlockState, BlockPos, Level, Value> test
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
                return test.apply(((BlockValue) v0).getBlockState(), ((BlockValue) v0).getPos(), cc.s.getLevel());
            }
            catch (NullPointerException ignored)
            {
                throw new InternalExpressionException("'" + name + "' function requires a block that is positioned in the world");
            }
        }
        BlockValue block = BlockArgument.findIn(cc, params, 0).block;
        return test.apply(block.getBlockState(), block.getPos(), cc.s.getLevel());
    }

    private static <T extends Comparable<T>> BlockState setProperty(Property<T> property, String name, String value,
                                                                    BlockState bs)
    {
        Optional<T> optional = property.getValue(value);

        if (optional.isPresent())
        {
            bs = bs.setValue(property, optional.get());
        }
        else
        {
            throw new InternalExpressionException(value + " is not a valid value for property " + name);
        }
        return bs;
    }

    private static void nullCheck(Value v, String name) {
        if (v.isNull()) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
    }

    private static float numberGetOrThrow(Value v) {
        double num = v.readDoubleNumber();
        if (Double.isNaN(num)) {
            throw new IllegalArgumentException(v.getString() + " needs to be a numeric value");
        }
        return (float) num;
    }

    private static void BooYah(final ServerLevel level)
    {
        synchronized (level)
        {
            ((ChunkGeneratorInterface)level.getChunkSource().getGenerator()).initStrongholds(level);
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
            CompoundTag tag = BlockArgument.findIn( (CarpetContext) c, lv, 0, true).block.getData();
            return NBTSerializableValue.of(tag);
        });

        // poi_get(pos, radius?, type?, occupation?, column_mode?)
        expression.addContextFunction("poi", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            if (lv.size() == 0) throw new InternalExpressionException("'poi' requires at least one parameter");
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0, false);
            BlockPos pos = locator.block.getPos();
            PoiManager store = cc.s.getLevel().getPoiManager();
            if (lv.size() == locator.offset)
            {
                Optional<Holder<PoiType>> foo = store.getType(pos);
                if (foo.isEmpty()) return Value.NULL;
                PoiType poiType = foo.get().value();

                // this feels wrong, but I don't want to mix-in more than I really need to.
                // also distance adds 0.5 to each point which screws up accurate distance calculations
                // you shoudn't be using POI with that in mind anyways, so I am not worried about it.
                PoiRecord poi = store.getInRange(
                        type -> type.value() == poiType,
                        pos,
                        1,
                        PoiManager.Occupancy.ANY
                ).filter(p -> p.getPos().equals(pos)).findFirst().orElse(null);
                if (poi == null)
                    return Value.NULL;
                return ListValue.of(
                        ValueConversions.of(BuiltInRegistries.POINT_OF_INTEREST_TYPE.getKey(poi.getPoiType().value())),
                        new NumericValue(poiType.maxTickets() - ((PoiRecord_scarpetMixin)poi).getFreeTickets())
                );
            }
            int radius = NumericValue.asNumber(lv.get(locator.offset+0)).getInt();
            if (radius < 0) return ListValue.of();
            Predicate<Holder<PoiType>> condition = p -> true;
            PoiManager.Occupancy status = PoiManager.Occupancy.ANY;
            boolean inColumn = false;
            if (locator.offset + 1 < lv.size())
            {
                String poiType = lv.get(locator.offset+1).getString().toLowerCase(Locale.ROOT);
                if (!"any".equals(poiType))
                {
                    PoiType type =  BuiltInRegistries.POINT_OF_INTEREST_TYPE.getOptional(InputValidator.identifierOf(poiType))
                            .orElseThrow(() -> new ThrowStatement(poiType, Throwables.UNKNOWN_POI));
                    condition = (tt) -> tt.value() == type;
                }
                if (locator.offset + 2 < lv.size())
                {
                    String statusString = lv.get(locator.offset+2).getString().toLowerCase(Locale.ROOT);
                    if ("occupied".equals(statusString))
                        status = PoiManager.Occupancy.IS_OCCUPIED;
                    else if ("available".equals(statusString))
                        status = PoiManager.Occupancy.HAS_SPACE;
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
            Stream<PoiRecord> pois = inColumn?
                    store.getInSquare(condition, pos, radius, status):
                    store.getInRange(condition, pos, radius, status);
            return ListValue.wrap(pois.sorted(Comparator.comparingDouble(p -> p.getPos().distSqr(pos))).map(p ->
                    ListValue.of(
                            ValueConversions.of(BuiltInRegistries.POINT_OF_INTEREST_TYPE.getKey(p.getPoiType().value())),
                            new NumericValue(p.getPoiType().value().maxTickets() - ((PoiRecord_scarpetMixin)p).getFreeTickets()),
                            ValueConversions.of(p.getPos())
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
            PoiManager store = cc.s.getLevel().getPoiManager();
            if (poi.isNull())
            {   // clear poi information
                if (!store.getType(pos).isPresent()) return Value.FALSE;
                store.remove(pos);
                return Value.TRUE;
            }
            String poiTypeString = poi.getString().toLowerCase(Locale.ROOT);
            ResourceLocation resource = InputValidator.identifierOf(poiTypeString);
            PoiType type =  BuiltInRegistries.POINT_OF_INTEREST_TYPE.getOptional(resource)
                    .orElseThrow(() -> new ThrowStatement(poiTypeString, Throwables.UNKNOWN_POI));
            Holder<PoiType> holder = BuiltInRegistries.POINT_OF_INTEREST_TYPE.getHolderOrThrow(ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, resource));

            int occupancy = 0;
            if (locator.offset + 1 < lv.size())
            {
                occupancy = (int)NumericValue.asNumber(lv.get(locator.offset + 1)).getLong();
                if (occupancy < 0) throw new InternalExpressionException("Occupancy cannot be negative");
            }
            if (store.getType(pos).isPresent()) store.remove(pos);
            store.add(pos, holder);
            // setting occupancy for a
            // again - don't want to mix in unnecessarily - peeps not gonna use it that often so not worries about it.
            if (occupancy > 0)
            {
                int finalO = occupancy;
                store.getInSquare((tt) -> tt.value()==type, pos, 1, PoiManager.Occupancy.ANY
                ).filter(p -> p.getPos().equals(pos)).findFirst().ifPresent(p -> {
                    for (int i=0; i < finalO; i++) ((PoiRecord_scarpetMixin)p).callAcquireTicket();
                });
            }
            return Value.TRUE;
        });


        expression.addContextFunction("weather",-1,(c, t, lv) -> {
            ServerLevel world = ((CarpetContext) c).s.getLevel();

            if(lv.size()==0)//cos it can thunder when raining or when clear.
                return new StringValue(world.isThundering() ? "thunder" : (world.isRaining() ? "rain" : "clear"));

            Value weather = lv.get(0);
            ServerLevelData worldProperties = ((ServerWorldInterface) world).getWorldPropertiesCM();
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
                        world.setWeatherParameters(ticks,0,false,false);
                        break;

                    case "rain":
                        world.setWeatherParameters(0,ticks,true,false);
                        break;

                    case "thunder":
                        world.setWeatherParameters(
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
                return ValueConversions.of(pos);
            }
            else if (v instanceof EntityValue)
            {
                Entity e = ((EntityValue) v).getEntity();
                if (e == null)
                    throw new InternalExpressionException("Null entity");
                return ValueConversions.of(e.position());
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
            BlockPos retpos = pos.relative(dir, howMuch);
            return ValueConversions.of(retpos);
        });

        expression.addContextFunction("solid", -1, (c, t, lv) ->
                genericStateTest(c, "solid", lv, (s, p, w) -> BooleanValue.of(s.isRedstoneConductor(w, p)))); // isSimpleFullBlock

        expression.addContextFunction("air", -1, (c, t, lv) ->
                booleanStateTest(c, "air", lv, (s, p) -> s.isAir()));

        expression.addContextFunction("liquid", -1, (c, t, lv) ->
                booleanStateTest(c, "liquid", lv, (s, p) -> !s.getFluidState().isEmpty()));

        expression.addContextFunction("flammable", -1, (c, t, lv) ->
                booleanStateTest(c, "flammable", lv, (s, p) -> s.getMaterial().isFlammable()));

        expression.addContextFunction("transparent", -1, (c, t, lv) ->
                booleanStateTest(c, "transparent", lv, (s, p) -> !s.getMaterial().isSolid()));

        /*this.expr.addContextFunction("opacity", -1, (c, t, lv) ->
                genericStateTest(c, "opacity", lv, (s, p, w) -> new NumericValue(s.getOpacity(w, p))));

        this.expr.addContextFunction("blocks_daylight", -1, (c, t, lv) ->
                genericStateTest(c, "blocks_daylight", lv, (s, p, w) -> new NumericValue(s.propagatesSkylightDown(w, p))));*/ // investigate

        expression.addContextFunction("emitted_light", -1, (c, t, lv) ->
                genericStateTest(c, "emitted_light", lv, (s, p, w) -> new NumericValue(s.getLightEmission())));

        expression.addContextFunction("light", -1, (c, t, lv) ->
                genericStateTest(c, "light", lv, (s, p, w) -> new NumericValue(Math.max(w.getBrightness(LightLayer.BLOCK, p), w.getBrightness(LightLayer.SKY, p)))));

        expression.addContextFunction("block_light", -1, (c, t, lv) ->
                genericStateTest(c, "block_light", lv, (s, p, w) -> new NumericValue(w.getBrightness(LightLayer.BLOCK, p))));

        expression.addContextFunction("sky_light", -1, (c, t, lv) ->
                genericStateTest(c, "sky_light", lv, (s, p, w) -> new NumericValue(w.getBrightness(LightLayer.SKY, p))));

        expression.addContextFunction("effective_light", -1, (c, t, lv) ->
                genericStateTest(c, "effective_light", lv, (s, p, w) -> new NumericValue(w.getMaxLocalRawBrightness(p))));

        expression.addContextFunction("see_sky", -1, (c, t, lv) ->
                genericStateTest(c, "see_sky", lv, (s, p, w) -> BooleanValue.of(w.canSeeSky(p))));

        expression.addContextFunction("brightness", -1, (c, t, lv) ->
                genericStateTest(c, "brightness", lv, (s, p, w) -> new NumericValue(w.getLightLevelDependentMagicValue(p))));

        expression.addContextFunction("hardness", -1, (c, t, lv) ->
                genericStateTest(c, "hardness", lv, (s, p, w) -> new NumericValue(s.getDestroySpeed(w, p))));

        expression.addContextFunction("blast_resistance", -1, (c, t, lv) ->
                genericStateTest(c, "blast_resistance", lv, (s, p, w) -> new NumericValue(s.getBlock().getExplosionResistance())));

        expression.addContextFunction("in_slime_chunk", -1, (c, t, lv) ->
        {
            BlockPos pos = BlockArgument.findIn((CarpetContext)c, lv, 0).block.getPos();
            ChunkPos chunkPos = new ChunkPos(pos);
            return BooleanValue.of(WorldgenRandom.seedSlimeChunk(
                    chunkPos.x, chunkPos.z,
                    ((CarpetContext)c).s.getLevel().getSeed(),
                    987234911L
            ).nextInt(10) == 0);
        });

        expression.addContextFunction("top", -1, (c, t, lv) ->
        {
            String type = lv.get(0).getString().toLowerCase(Locale.ROOT);
            Heightmap.Types htype;
            switch (type)
            {
                //case "light": htype = Heightmap.Type.LIGHT_BLOCKING; break;  //investigate
                case "motion": htype = Heightmap.Types.MOTION_BLOCKING; break;
                case "terrain": htype = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES; break;
                case "ocean_floor": htype = Heightmap.Types.OCEAN_FLOOR; break;
                case "surface": htype = Heightmap.Types.WORLD_SURFACE; break;
                default: throw new InternalExpressionException("Unknown heightmap type: "+type);
            }
            BlockArgument locator = BlockArgument.findIn((CarpetContext)c, lv, 1);
            BlockPos pos = locator.block.getPos();
            int x = pos.getX();
            int z = pos.getZ();
            return new NumericValue(((CarpetContext)c).s.getLevel().getChunk(x >> 4, z >> 4).getHeight(htype, x & 15, z & 15) + 1);
        });

        expression.addContextFunction("loaded", -1, (c, t, lv) ->
                BooleanValue.of((((CarpetContext) c).s.getLevel().hasChunkAt(BlockArgument.findIn((CarpetContext) c, lv, 0).block.getPos()))));

        // Deprecated, use loaded_status as more indicative
        expression.addContextFunction("loaded_ep", -1, (c, t, lv) ->
        {
            c.host.issueDeprecation("loaded_ep(...)");
            BlockPos pos = BlockArgument.findIn((CarpetContext)c, lv, 0).block.getPos();
            return BooleanValue.of(((CarpetContext)c).s.getLevel().isPositionEntityTicking(pos));// 1.17pre1 getChunkManager().shouldTickChunk(new ChunkPos(pos)));
        });

        expression.addContextFunction("loaded_status", -1, (c, t, lv) ->
        {
            BlockPos pos = BlockArgument.findIn((CarpetContext)c, lv, 0).block.getPos();
            LevelChunk chunk = ((CarpetContext)c).s.getLevel().getChunkSource().getChunk(pos.getX()>>4, pos.getZ()>>4, false);
            if (chunk == null) return Value.ZERO;
            return new NumericValue(chunk.getFullStatus().ordinal());
        });

        expression.addContextFunction("is_chunk_generated", -1, (c, t, lv) ->
        {
            BlockArgument locator = BlockArgument.findIn((CarpetContext)c, lv, 0);
            BlockPos pos = locator.block.getPos();
            boolean force = false;
            if (lv.size() > locator.offset)
                force = lv.get(locator.offset).getBoolean();
            return BooleanValue.of(canHasChunk(((CarpetContext)c).s.getLevel(), new ChunkPos(pos), null, force));
        });

        expression.addContextFunction("generation_status", -1, (c, t, lv) ->
        {
            BlockArgument blockArgument = BlockArgument.findIn((CarpetContext)c, lv, 0);
            BlockPos pos = blockArgument.block.getPos();
            boolean forceLoad = false;
            if (lv.size() > blockArgument.offset)
                forceLoad = lv.get(blockArgument.offset).getBoolean();
            ChunkAccess chunk = ((CarpetContext)c).s.getLevel().getChunk(pos.getX()>>4, pos.getZ()>>4, ChunkStatus.EMPTY, forceLoad);
            if (chunk == null) return Value.NULL;
            return new StringValue(chunk.getStatus().getName());
        });

        expression.addContextFunction("chunk_tickets", -1, (c, t, lv) ->
        {
            ServerLevel world = ((CarpetContext) c).s.getLevel();
            Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> levelTickets = (
                    (ChunkTicketManagerInterface) ((ServerChunkManagerInterface) world.getChunkSource())
                            .getCMTicketManager()
            ).getTicketsByPosition();
            List<Value> res = new ArrayList<>();
            if (lv.size() == 0)
            {
                for (long key : levelTickets.keySet())
                {
                    ChunkPos chpos = new ChunkPos(key);
                    for (Ticket<?> ticket : levelTickets.get(key))
                    {
                        res.add(ListValue.of(
                                new StringValue(ticket.getType().toString()),
                                new NumericValue(33 - ticket.getTicketLevel()),
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
                SortedArraySet<Ticket<?>> tickets = levelTickets.get(new ChunkPos(pos).toLong());
                if (tickets != null)
                {
                    for (Ticket<?> ticket : tickets)
                    {
                        res.add(ListValue.of(
                                new StringValue(ticket.getType().toString()),
                                new NumericValue(33 - ticket.getTicketLevel())
                        ));
                    }
                }
            }
            res.sort(Comparator.comparing(e -> ((ListValue) e).getItems().get(1)).reversed());
            return ListValue.wrap(res);
        });

        expression.addContextFunction("suffocates", -1, (c, t, lv) ->
                genericStateTest(c, "suffocates", lv, (s, p, w) -> BooleanValue.of(s.isSuffocating(w, p)))); // canSuffocate

        expression.addContextFunction("power", -1, (c, t, lv) ->
                genericStateTest(c, "power", lv, (s, p, w) -> new NumericValue(w.getBestNeighborSignal(p))));

        expression.addContextFunction("ticks_randomly", -1, (c, t, lv) ->
                booleanStateTest(c, "ticks_randomly", lv, (s, p) -> s.isRandomlyTicking()));

        expression.addContextFunction("update", -1, (c, t, lv) ->
                booleanStateTest(c, "update", lv, (s, p) ->
                {
                    ((CarpetContext) c).s.getLevel().neighborChanged(p, s.getBlock(), p);
                    return true;
                }));

        expression.addContextFunction("block_tick", -1, (c, t, lv) ->
                booleanStateTest(c, "block_tick", lv, (s, p) ->
                {
                    ServerLevel w = ((CarpetContext)c).s.getLevel();
                    s.randomTick(w, p, w.random);
                    return true;
                }));

        expression.addContextFunction("random_tick", -1, (c, t, lv) ->
                booleanStateTest(c, "random_tick", lv, (s, p) ->
                {
                    ServerLevel w = ((CarpetContext)c).s.getLevel();
                    if (s.isRandomlyTicking() || s.getFluidState().isRandomlyTicking())
                        s.randomTick(w, p, w.random);
                    return true;
                }));

        // lazy cause its parked execution
        expression.addLazyFunction("without_updates", 1, (c, t, lv) ->
        {
            boolean previous = CarpetSettings.impendingFillSkipUpdates.get();
            if (previous) return lv.get(0);
            Value [] result = new Value[]{Value.NULL};
            ((CarpetContext)c).s.getServer().executeBlocking( () ->
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

        expression.addContextFunction("set", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerLevel world = cc.s.getLevel();
            BlockArgument targetLocator = BlockArgument.findIn(cc, lv, 0);
            BlockArgument sourceLocator = BlockArgument.findIn(cc, lv, targetLocator.offset, true);
            BlockState sourceBlockState = sourceLocator.block.getBlockState();
            BlockState targetBlockState = world.getBlockState(targetLocator.block.getPos());
            CompoundTag data = null;
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
                StateDefinition<Block, BlockState> states = sourceBlockState.getBlock().getStateDefinition();
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
            CompoundTag finalData = data;

            if (sourceBlockState == targetBlockState && data == null)
                return Value.FALSE;
            BlockState finalSourceBlockState = sourceBlockState;
            BlockPos targetPos = targetLocator.block.getPos();
            Boolean[] result = new Boolean[]{true};
            cc.s.getServer().executeBlocking( () ->
            {
                Clearable.tryClear(world.getBlockEntity(targetPos));
                boolean success = world.setBlock(targetPos, finalSourceBlockState, 2);
                if (finalData != null)
                {
                    BlockEntity be = world.getBlockEntity(targetPos);
                    if (be != null)
                    {
                        CompoundTag destTag = finalData.copy();
                        destTag.putInt("x", targetPos.getX());
                        destTag.putInt("y", targetPos.getY());
                        destTag.putInt("z", targetPos.getZ());
                        be.load(destTag);
                        be.setChanged();
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
            ServerLevel world = cc.s.getLevel();
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
                    item = BuiltInRegistries.ITEM.getOptional(InputValidator.identifierOf(itemString))
                            .orElseThrow(() -> new ThrowStatement(itemString, Throwables.UNKNOWN_ITEM));
                }
            }
            CompoundTag tag = null;
            if (lv.size() > locator.offset+1)
            {
                if (!playerBreak) throw new InternalExpressionException("tag is not necessary with 'destroy' with no item");
                Value tagValue = lv.get(locator.offset+1);
                if (tagValue.isNull())
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
                tool.setTag(tag);
            if (playerBreak && state.getDestroySpeed(world, where) < 0.0) return Value.FALSE;
            boolean removed = world.removeBlock(where, false);
            if (!removed) return Value.FALSE;
            world.levelEvent(null, 2001, where, Block.getId(state));

            boolean toolBroke = false;
            boolean dropLoot = true;
            if (playerBreak)
            {
                boolean isUsingEffectiveTool = !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state);
                //postMine() durability from item classes
                float hardness = state.getDestroySpeed(world, where);
                int damageAmount = 0;
                if ((item instanceof DiggerItem && hardness > 0.0) || item instanceof ShearsItem)
                {
                    damageAmount = 1;
                }
                else if (item instanceof TridentItem || item instanceof SwordItem)
                {
                    damageAmount = 2;
                }
                toolBroke = damageAmount>0 && tool.hurt(damageAmount, world.getRandom(), null);
                if (!isUsingEffectiveTool)
                    dropLoot = false;
            }

            if (dropLoot)
            {
                if (how < 0 || (tag != null && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0))
                {
                    Block.popResource(world, where, new ItemStack(state.getBlock()));
                }
                else
                {
                    if (how > 0)
                        tool.enchant(Enchantments.BLOCK_FORTUNE, (int) how);
                    if (DUMMY_ENTITY == null) DUMMY_ENTITY = new FallingBlockEntity(EntityType.FALLING_BLOCK, null);
                    Block.dropResources(state, world, where, be, DUMMY_ENTITY, tool);
                }
            }
            if (!playerBreak) // no tool info - block brokwn
                return Value.TRUE;
            if (toolBroke)
                return Value.NULL;
            Tag outtag = tool.getTag();
            if (outtag == null)
                return Value.TRUE;
            return new NBTSerializableValue(() -> outtag);

        });

        expression.addContextFunction("harvest", -1, (c, t, lv) ->
        {
            if (lv.size()<2)
                throw new InternalExpressionException("'harvest' takes at least 2 parameters: entity and block, or position, to harvest");
            CarpetContext cc = (CarpetContext)c;
            Level world = cc.s.getLevel();
            Value entityValue = lv.get(0);
            if (!(entityValue instanceof EntityValue))
                return Value.FALSE;
            Entity e = ((EntityValue) entityValue).getEntity();
            if (!(e instanceof ServerPlayer))
                return Value.FALSE;
            ServerPlayer player = (ServerPlayer)e;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 1);
            BlockPos where = locator.block.getPos();
            BlockState state = locator.block.getBlockState();
            Block block = state.getBlock();
            boolean success = false;
            if (!((block == Blocks.BEDROCK || block == Blocks.BARRIER) && player.gameMode.isSurvival()))
                success = player.gameMode.destroyBlock(where);
            if (success)
                world.levelEvent(null, 2001, where, Block.getId(state));
            return BooleanValue.of(success);
        });

        expression.addContextFunction("create_explosion", -1, (c, t, lv) ->
        {
            if (lv.isEmpty())
                throw new InternalExpressionException("'create_explosion' requires at least a position to explode");
            CarpetContext cc = (CarpetContext)c;
            float powah = 4.0f;
            Explosion.BlockInteraction mode = Explosion.BlockInteraction.DESTROY; // should probably read the gamerule for default behaviour
            boolean createFire = false;
            Entity source = null;
            LivingEntity attacker = null;
            Vector3Argument location = Vector3Argument.findIn(lv, 0, false, true);
            Vec3 pos = location.vec;
            if (lv.size() > location.offset)
            {
                powah = NumericValue.asNumber(lv.get(location.offset), "explosion power").getFloat();
                if (powah < 0) throw new InternalExpressionException("Explosion power cannot be negative");
                if (lv.size() > location.offset+1)
                {
                    String strval = lv.get(location.offset+1).getString();
                    try {
                        mode = Explosion.BlockInteraction.valueOf(strval.toUpperCase(Locale.ROOT));
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
                                            ValueConversions.of(BuiltInRegistries.ENTITY_TYPE.getKey(attackingEntity.getType())).getString() +" ain't it.");

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
            Explosion explosion = new Explosion(cc.s.getLevel(), source, null, null, pos.x, pos.y, pos.z, powah, createFire, mode){
                @Override
                public @Nullable LivingEntity getIndirectSourceEntity() {
                    return theAttacker;
                }
            };
            explosion.explode();
            explosion.finalizeExplosion(false);
            if (mode == Explosion.BlockInteraction.KEEP) explosion.clearToBlow();
            cc.s.getLevel().players().forEach(spe -> {
                if (spe.distanceToSqr(pos) < 4096.0D)
                    spe.connection.send(new ClientboundExplodePacket(pos.x, pos.y, pos.z, thePowah, explosion.getToBlow(), explosion.getHitPlayers().get(spe)));
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
            ItemInput stackArg = NBTSerializableValue.parseItem(itemString, cc.s.registryAccess());
            BlockPos where = new BlockPos(locator.vec);
            String facing;
            if (lv.size() > locator.offset) {
                facing = lv.get(locator.offset).getString();
            } else {
                // Paintings throw an exception if their direction is vertical, therefore we change the default here
                facing = stackArg.getItem() != Items.PAINTING ? "up" : "north";
            }
            boolean sneakPlace = false;
            if (lv.size() > locator.offset+1)
                sneakPlace = lv.get(locator.offset+1).getBoolean();

            BlockValue.PlacementContext ctx;
            try
            {
                ctx = BlockValue.PlacementContext.from(cc.s.getLevel(), where, facing, sneakPlace, stackArg.createItemStack(1, false));
            }
            catch (CommandSyntaxException e)
            {
                throw new InternalExpressionException(e.getMessage());
            }

            if (!(stackArg.getItem() instanceof BlockItem))
            {
                InteractionResult useResult = ctx.getItemInHand().useOn(ctx);
                if (useResult == InteractionResult.CONSUME || useResult == InteractionResult.SUCCESS)
                {
                    return Value.TRUE;
                }
            }
            else
            { // not sure we need special case for block items, since useOnBlock can do that as well
                BlockItem blockItem = (BlockItem) stackArg.getItem();
                if (!ctx.canPlace()) return Value.FALSE;
                BlockState placementState = blockItem.getBlock().getStateForPlacement(ctx);
                if (placementState != null)
                {
                    if (placementState.canSurvive(cc.s.getLevel(), where))
                    {
                        cc.s.getLevel().setBlock(where, placementState, 2);
                        SoundType blockSoundGroup = placementState.getSoundType();
                        cc.s.getLevel().playSound(null, where, blockSoundGroup.getPlaceSound(), SoundSource.BLOCKS, (blockSoundGroup.getVolume() + 1.0F) / 2.0F, blockSoundGroup.getPitch() * 0.8F);
                        return Value.TRUE;
                    }
                }
            }
            return Value.FALSE;
        });

        expression.addContextFunction("blocks_movement", -1, (c, t, lv) ->
                booleanStateTest(c, "blocks_movement", lv, (s, p) ->
                        !s.isPathfindable(((CarpetContext) c).s.getLevel(), p, PathComputationType.LAND)));

        expression.addContextFunction("block_sound", -1, (c, t, lv) ->
                stateStringQuery(c, "block_sound", lv, (s, p) ->
                        BlockInfo.soundName.get(s.getSoundType())));

        expression.addContextFunction("material", -1, (c, t, lv) ->
                stateStringQuery(c, "material", lv, (s, p) ->
                        BlockInfo.materialName.get(s.getMaterial())));

        expression.addContextFunction("map_colour", -1, (c, t, lv) ->
                stateStringQuery(c, "map_colour", lv, (s, p) ->
                        BlockInfo.mapColourName.get(s.getMapColor(((CarpetContext)c).s.getLevel(), p))));


        // Deprecated for block_state()
        expression.addContextFunction("property", -1, (c, t, lv) ->
        {
            c.host.issueDeprecation("property(...)");
            BlockArgument locator = BlockArgument.findIn((CarpetContext) c, lv, 0);
            BlockState state = locator.block.getBlockState();
            if (lv.size() <= locator.offset)
                throw new InternalExpressionException("'property' requires to specify a property to query");
            String tag = lv.get(locator.offset).getString();
            StateDefinition<Block, BlockState> states = state.getBlock().getStateDefinition();
            Property<?> property = states.getProperty(tag);
            if (property == null) return Value.NULL;
            return new StringValue(state.getValue(property).toString().toLowerCase(Locale.ROOT));
        });

        // Deprecated for block_state()
        expression.addContextFunction("block_properties", -1, (c, t, lv) ->
        {
            c.host.issueDeprecation("block_properties(...)");
            BlockArgument locator = BlockArgument.findIn((CarpetContext) c, lv, 0);
            BlockState state = locator.block.getBlockState();
            StateDefinition<Block, BlockState> states = state.getBlock().getStateDefinition();
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
            StateDefinition<Block, BlockState> states = state.getBlock().getStateDefinition();
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
                return ListValue.wrap(BuiltInRegistries.BLOCK.keySet().stream().map(ValueConversions::of).collect(Collectors.toList()));
            CarpetContext cc = (CarpetContext)c;
            ResourceLocation tag = InputValidator.identifierOf(lv.get(0).getString());

            Registry<Block> blocks = cc.s.getServer().registryAccess().registryOrThrow(Registries.BLOCK);
            Optional<HolderSet.Named<Block>> tagset = blocks.getTag(TagKey.create(Registries.BLOCK, tag));
            if (tagset.isEmpty()) return Value.NULL;
            return ListValue.wrap(tagset.get().stream().map(b -> ValueConversions.of(blocks.getKey(b.value()))).collect(Collectors.toList()));
        });

        expression.addContextFunction("block_tags", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            Registry<Block> blocks = cc.s.getServer().registryAccess().registryOrThrow(Registries.BLOCK);
            if (lv.size() == 0)
                return ListValue.wrap(blocks.getTagNames().map(ValueConversions::of).collect(Collectors.toList()));
            BlockArgument blockLocator = BlockArgument.findIn(cc, lv, 0, true);
            if (blockLocator.offset == lv.size())
            {
                Block target = blockLocator.block.getBlockState().getBlock();
                return ListValue.wrap( blocks.getTags().filter(e -> e.getSecond().stream().anyMatch(h -> (h.value() == target))).map(e -> ValueConversions.of(e.getFirst())).collect(Collectors.toList()));
            }
            String tag = lv.get(blockLocator.offset).getString();
            Optional<HolderSet.Named<Block>> tagSet = blocks.getTag(TagKey.create(Registries.BLOCK, InputValidator.identifierOf(tag)));
            if (tagSet.isEmpty()) return Value.NULL;
            return BooleanValue.of(blockLocator.block.getBlockState().is(tagSet.get()));


            /* before
            TagContainer tagManager = cc.s.getServer().getTags();
            if (lv.size() == 0)
                return ListValue.wrap(tagManager.getOrEmpty(Registry.BLOCK_REGISTRY).getAvailableTags().stream().map(ValueConversions::of).collect(Collectors.toList()));
            BlockArgument blockLocator = BlockArgument.findIn(cc, lv, 0, true);
            if (blockLocator.offset == lv.size())
            {
                Block target = blockLocator.block.getBlockState().getBlock();
                return ListValue.wrap(tagManager.getOrEmpty(Registry.BLOCK_REGISTRY).getAllTags().entrySet().stream().filter(e -> e.getValue().contains(target)).map(e -> ValueConversions.of(e.getKey())).collect(Collectors.toList()));
            }
            String tag = lv.get(blockLocator.offset).getString();
            net.minecraft.tags.Tag<Block> blockTag = tagManager.getOrEmpty(Registry.BLOCK_REGISTRY).getTag(InputValidator.identifierOf(tag));
            if (blockTag == null) return Value.NULL;
            return BooleanValue.of(blockLocator.block.getBlockState().is(blockTag));
            */
        });

        expression.addContextFunction("biome", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext) c;
            ServerLevel world = cc.s.getLevel();
            if (lv.size() == 0)
                return ListValue.wrap(world.registryAccess().registryOrThrow(Registries.BIOME).keySet().stream().map(ValueConversions::of));

            Biome biome;
            BiomeSource biomeSource = world.getChunkSource().getGenerator().getBiomeSource();
            if (   lv.size() == 1
                && lv.get(0) instanceof MapValue map
                && biomeSource instanceof MultiNoiseBiomeSource mnbs
            ) {
                Value temperature = map.get(new StringValue("temperature"));
                nullCheck(temperature, "temperature");

                Value humidity = map.get(new StringValue("humidity"));
                nullCheck(humidity, "humidity");

                Value continentalness = map.get(new StringValue("continentalness"));
                nullCheck(continentalness, "continentalness");

                Value erosion = map.get(new StringValue("erosion"));
                nullCheck(erosion, "erosion");

                Value depth = map.get(new StringValue("depth"));
                nullCheck(depth, "depth");

                Value weirdness = map.get(new StringValue("weirdness"));
                nullCheck(weirdness, "weirdness");

                Climate.TargetPoint point = new Climate.TargetPoint(
                        Climate.quantizeCoord(numberGetOrThrow(temperature)),
                        Climate.quantizeCoord(numberGetOrThrow(humidity)),
                        Climate.quantizeCoord(numberGetOrThrow(continentalness)),
                        Climate.quantizeCoord(numberGetOrThrow(erosion)),
                        Climate.quantizeCoord(numberGetOrThrow(depth)),
                        Climate.quantizeCoord(numberGetOrThrow(weirdness))
                );
                biome = mnbs.getNoiseBiome(point).value();
                ResourceLocation biomeId = cc.s.getServer().registryAccess().registryOrThrow(Registries.BIOME).getKey(biome);
                return new StringValue(NBTSerializableValue.nameFromRegistryId(biomeId));
            }

            BlockArgument locator = BlockArgument.findIn(cc, lv, 0, false, false, true);

            if (locator.replacement != null)
            {
                biome = world.registryAccess().registryOrThrow(Registries.BIOME).get(InputValidator.identifierOf(locator.replacement));
                if (biome == null) throw new ThrowStatement(locator.replacement, Throwables.UNKNOWN_BIOME);
            }
            else
            {
                BlockPos pos = locator.block.getPos();
                biome = world.getBiome(pos).value();
            }
            // in locatebiome
            if (locator.offset == lv.size())
            {
                ResourceLocation biomeId = cc.s.getServer().registryAccess().registryOrThrow(Registries.BIOME).getKey(biome);
                return new StringValue(NBTSerializableValue.nameFromRegistryId(biomeId));
            }
            String biomeFeature = lv.get(locator.offset).getString();
            BiFunction<ServerLevel, Biome, Value> featureProvider = BiomeInfo.biomeFeatures.get(biomeFeature);
            if (featureProvider == null)
                throw new InternalExpressionException("Unknown biome feature: " + biomeFeature);
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
            Holder<Biome> biome = cc.s.getServer().registryAccess().registryOrThrow(Registries.BIOME).getHolder(ResourceKey.create(Registries.BIOME, InputValidator.identifierOf(biomeName)))
                .orElseThrow(() -> new ThrowStatement(biomeName, Throwables.UNKNOWN_BIOME));


            boolean doImmediateUpdate = true;
            if (lv.size() > locator.offset+1)
            {
                doImmediateUpdate = lv.get(locator.offset+1).getBoolean();
            }
            ServerLevel world = cc.s.getLevel();
            BlockPos pos = locator.block.getPos();
            ChunkAccess chunk = world.getChunk(pos); // getting level chunk instead of protochunk with biomes
            int biomeX = QuartPos.fromBlock(pos.getX());
            int biomeY = QuartPos.fromBlock(pos.getY());
            int biomeZ = QuartPos.fromBlock(pos.getZ());
            try {
                int i = QuartPos.fromBlock(chunk.getMinBuildHeight());
                int j = i + QuartPos.fromBlock(chunk.getHeight()) - 1;
                int k = Mth.clamp(biomeY, i, j);
                int l = chunk.getSectionIndex(QuartPos.toBlock(k));
                // accessing outside of the interface - might be dangerous in the future.
                ((PalettedContainer<Holder<Biome>>) chunk.getSection(l).getBiomes()).set(biomeX & 3, k & 3, biomeZ & 3, biome);
            } catch (Throwable var8) {
                return Value.FALSE;
            }
            if (doImmediateUpdate) WorldTools.forceChunkUpdate(pos, world);
            chunk.setUnsaved(true);
            return Value.TRUE;
        });

        expression.addContextFunction("reload_chunk", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            BlockPos pos = BlockArgument.findIn(cc, lv, 0).block.getPos();
            ServerLevel world = cc.s.getLevel();
            cc.s.getServer().executeBlocking( () -> WorldTools.forceChunkUpdate(pos, world));
            return Value.TRUE;
        });

        expression.addContextFunction("structure_references", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            ServerLevel world = cc.s.getLevel();
            BlockPos pos = locator.block.getPos();
            Map<Structure, LongSet> references = world.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.STRUCTURE_REFERENCES).getAllReferences();
            Registry<Structure> reg = cc.s.getServer().registryAccess().registryOrThrow(Registries.STRUCTURE);
            if (lv.size() == locator.offset)
                return ListValue.wrap(references.entrySet().stream().
                        filter(e -> e.getValue()!= null && !e.getValue().isEmpty()).
                        map(e -> new StringValue(NBTSerializableValue.nameFromRegistryId(reg.getKey(e.getKey())))).collect(Collectors.toList())
                );
            String simpleStructureName = lv.get(locator.offset).getString().toLowerCase(Locale.ROOT);
            Structure structureName = reg.get(InputValidator.identifierOf(simpleStructureName));
            if (structureName == null) return Value.NULL;
            LongSet structureReferences = references.get(structureName);
            if (structureReferences == null || structureReferences.isEmpty()) return ListValue.of();
            return ListValue.wrap(structureReferences.longStream().mapToObj(l -> ListValue.of(
                    new NumericValue(16*ChunkPos.getX(l)),
                    Value.ZERO,
                    new NumericValue(16*ChunkPos.getZ(l)))).collect(Collectors.toList()));
        });

        expression.addContextFunction("structure_eligibility", -1, (c, t, lv) ->
        {// TODO rename structureName to class
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);

            ServerLevel world = cc.s.getLevel();

            // well, because
            BooYah(world);

            BlockPos pos = locator.block.getPos();
            final List<Structure> structure = new ArrayList<>();
            boolean needSize = false;
            boolean singleOutput = false;
            Registry<Structure> reg = cc.s.getServer().registryAccess().registryOrThrow(Registries.STRUCTURE);
            if (lv.size() > locator.offset)
            {
                Value requested = lv.get(locator.offset+0);
                if (!requested.isNull())
                {
                    String reqString = requested.getString();
                    var id = InputValidator.identifierOf(reqString);
                    Structure requestedStructure = reg.get(id);
                    if (requestedStructure != null)
                    {
                        singleOutput = true;
                        structure.add(requestedStructure);
                    }
                    else
                    {
                        StructureType<?> sss = BuiltInRegistries.STRUCTURE_TYPE.get(id);
                        reg.entrySet().stream().filter(e -> e.getValue().type() ==sss).forEach(e -> structure.add(e.getValue()));
                    }
                    if (structure.isEmpty())
                    {
                        throw new ThrowStatement(reqString, Throwables.UNKNOWN_STRUCTURE);
                    }

                }
                else
                {
                    structure.addAll(reg.entrySet().stream().map(Map.Entry::getValue).toList());
                }
                if (lv.size() > locator.offset+1)
                {
                    needSize = lv.get(locator.offset+1).getBoolean();
                }
            }
            else
            {
                structure.addAll(reg.entrySet().stream().map(Map.Entry::getValue).toList());
            }
            if (singleOutput)
            {
                StructureStart start = FeatureGenerator.shouldStructureStartAt(world, pos, structure.get(0), needSize);
                if (start == null) return Value.NULL;
                if (!needSize) return Value.TRUE;
                return ValueConversions.of(start);
            }
            Map<Value, Value> ret = new HashMap<>();
            for(Structure str: structure)
            {
                StructureStart start;
                try
                {
                    start = FeatureGenerator.shouldStructureStartAt(world, pos, str, needSize);
                }
                catch (NullPointerException npe)
                {
                    CarpetSettings.LOG.error("Failed to detect structure: "+ reg.getKey(str));
                    start = null;
                }

                if (start == null) continue;

                Value key = new StringValue(NBTSerializableValue.nameFromRegistryId(reg.getKey(str)));
                ret.put(key, (!needSize)?Value.NULL: ValueConversions.of(start));
            }
            return MapValue.wrap(ret);
        });

        expression.addContextFunction("structures", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);

            ServerLevel world = cc.s.getLevel();
            BlockPos pos = locator.block.getPos();
            Map<Structure, StructureStart> structures = world.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS).getAllStarts();
            Registry<Structure> reg = cc.s.getServer().registryAccess().registryOrThrow(Registries.STRUCTURE);
            if (lv.size() == locator.offset)
            {
                Map<Value, Value> structureList = new HashMap<>();
                for (Map.Entry<Structure, StructureStart> entry : structures.entrySet())
                {
                    StructureStart start = entry.getValue();
                    if (start == StructureStart.INVALID_START)
                        continue;
                    BoundingBox box = start.getBoundingBox();
                    structureList.put(
                            new StringValue(NBTSerializableValue.nameFromRegistryId(reg.getKey(entry.getKey()))),
                            ValueConversions.of(box)
                    );
                }
                return MapValue.wrap(structureList);
            }
            String structureName = lv.get(locator.offset).getString().toLowerCase(Locale.ROOT);
            return ValueConversions.of(structures.get(reg.get(InputValidator.identifierOf(structureName))));
        });

        expression.addContextFunction("set_structure", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);

            ServerLevel world = cc.s.getLevel();
            BlockPos pos = locator.block.getPos();

            if (lv.size() == locator.offset)
                throw new InternalExpressionException("'set_structure requires at least position and a structure name");
            String structureName = lv.get(locator.offset).getString().toLowerCase(Locale.ROOT);
            Structure configuredStructure = FeatureGenerator.resolveConfiguredStructure(structureName, world, pos);
            if (configuredStructure == null) throw new ThrowStatement(structureName, Throwables.UNKNOWN_STRUCTURE);
            // good 'ol pointer
            Value[] result = new Value[]{Value.NULL};
            // technically a world modification. Even if we could let it slide, we will still park it
            ((CarpetContext) c).s.getServer().executeBlocking(() ->
            {
                Map<Structure, StructureStart> structures = world.getChunk(pos).getAllStarts();
                if (lv.size() == locator.offset + 1)
                {
                    Boolean res = FeatureGenerator.plopGrid(configuredStructure, ((CarpetContext) c).s.getLevel(), locator.block.getPos());
                    if (res == null) return;
                    result[0] = res?Value.TRUE:Value.FALSE;
                    return;
                }
                Value newValue = lv.get(locator.offset+1);
                if (newValue.isNull()) // remove structure
                {
                    Structure structure = configuredStructure;
                    if (!structures.containsKey(structure))
                    {
                        return;
                    }
                    StructureStart start = structures.get(structure);
                    ChunkPos structureChunkPos = start.getChunkPos();
                    BoundingBox box = start.getBoundingBox();
                    for (int chx = box.minX() / 16; chx <= box.maxX() / 16; chx++)  // minx maxx
                    {
                        for (int chz = box.minZ() / 16; chz <= box.maxZ() / 16; chz++) //minZ maxZ
                        {
                            ChunkPos chpos = new ChunkPos(chx, chz);
                            // getting a chunk will convert it to full, allowing to modify references
                            Map<Structure, LongSet> references =
                                    world.getChunk(chpos.getWorldPosition()).getAllReferences();
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
/*
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
            CommandHelper.notifyPlayersCommandsChanged(cc.s.getServer());
            return Value.TRUE;
        });
*/
        // todo maybe enable chunk blending?
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


            ServerLevel world = cc.s.getLevel();

            Value [] result = new Value[]{Value.NULL};

            ((CarpetContext)c).s.getServer().executeBlocking( () ->
            {
                Map<String, Integer> report = ((ThreadedAnvilChunkStorageInterface) world.getChunkSource().chunkMap).regenerateChunkRegion(requestedChunks);
                /*for (ChunkPos chpos: requestedChunks) // needed in 1.16 only
                {
                    if (world.getChunk(chpos.x, chpos.z, ChunkStatus.FULL, false) != null)
                    {
                        WorldTools.forceChunkUpdate(chpos.getStartPos(), world);
                    }
                }*/
                result[0] = MapValue.wrap(report.entrySet().stream().collect(Collectors.toMap(
                        e -> new StringValue(e.getKey()),
                        e ->  new NumericValue(e.getValue())
                )));
            });
            return result[0];
        });

        expression.addContextFunction("inhabited_time", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            BlockPos pos = locator.block.getPos();
            return new NumericValue(cc.s.getLevel().getChunk(pos).getInhabitedTime());
        });

        expression.addContextFunction("spawn_potential", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            BlockPos pos = locator.block.getPos();
            double required_charge = 1;
            if (lv.size() > locator.offset)
                required_charge = NumericValue.asNumber(lv.get(locator.offset)).getDouble();
            NaturalSpawner.SpawnState charger = cc.s.getLevel().getChunkSource().getLastSpawnState();
            if (charger == null) return Value.NULL;
            return new NumericValue(
                    ((SpawnHelperInnerInterface)charger).getPotentialCalculator().
                            getPotentialEnergyChange(pos, required_charge )
            );
        });

        expression.addContextFunction("add_chunk_ticket", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            BlockPos pos = locator.block.getPos();
            if (lv.size() != locator.offset+2) throw new InternalExpressionException("'add_chunk_ticket' requires block position, ticket type and radius");
            String type = lv.get(locator.offset).getString();
            TicketType<?> ticket = ticketTypes.get(type.toLowerCase(Locale.ROOT));
            if (ticket == null) throw new InternalExpressionException("Unknown ticket type: "+type);
            int radius = NumericValue.asNumber(lv.get(locator.offset+1)).getInt();
            if (radius < 1 || radius > 32) throw new InternalExpressionException("Ticket radius should be between 1 and 32 chunks");
            // due to types we will wing it:
            ChunkPos target = new ChunkPos(pos);
            if (ticket == TicketType.PORTAL) // portal
                cc.s.getLevel().getChunkSource().addRegionTicket(TicketType.PORTAL, target, radius, pos);
            else if (ticket == TicketType.POST_TELEPORT) // post teleport
                cc.s.getLevel().getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, target, radius, 1);
            else
                cc.s.getLevel().getChunkSource().addRegionTicket(TicketType.UNKNOWN, target, radius, target);
            return new NumericValue(ticket.timeout());
        });

    }

    @ScarpetFunction(maxParams = -1)
    public Value sample_noise(Context c, @Locator.Block BlockPos pos, String... noiseQueries) {
        return Value.NULL;
        /*
        int mappedX = QuartPos.fromBlock(pos.getX());
        int mappedY = QuartPos.fromBlock(pos.getY());
        int mappedZ = QuartPos.fromBlock(pos.getZ());
        Climate.Sampler mns = ((CarpetContext) c).s.getLevel().getChunkSource().getGenerator().climateSampler();
        Map<Value, Value> ret = new HashMap<>();

        if (noiseQueries.length == 0) {
            noiseQueries = new String[]{"continentalness", "erosion", "weirdness", "temperature", "humidity", "depth"};
        }

        for (String noise : noiseQueries) {
            double noiseValue = ((NoiseColumnSamplerInterface) mns).getNoiseSample(noise, mappedX, mappedY, mappedZ);
            ret.put(new StringValue(noise), new NumericValue(noiseValue));
        }
        return MapValue.wrap(ret);*/
    }
}
