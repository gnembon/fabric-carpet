package carpet.script.api;

import carpet.script.CarpetContext;
import carpet.script.CarpetScriptServer;
import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Fluff;
import carpet.script.external.Carpet;
import carpet.script.external.Vanilla;
import carpet.script.utils.Colors;
import carpet.script.utils.FeatureGenerator;
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

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.tuple.Pair;

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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

import static carpet.script.utils.WorldTools.canHasChunk;

public class WorldAccess
{
    private static final Map<String, Direction> DIRECTION_MAP = Arrays.stream(Direction.values()).collect(Collectors.toMap(Direction::getName, Function.identity()));

    static
    {
        DIRECTION_MAP.put("y", Direction.UP);
        DIRECTION_MAP.put("z", Direction.SOUTH);
        DIRECTION_MAP.put("x", Direction.EAST);
    }

    private static final Map<String, TicketType<?>> ticketTypes = Map.of(
            "portal", TicketType.PORTAL,
            "teleport", TicketType.ENDER_PEARL,
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
        if (params.isEmpty())
        {
            throw new InternalExpressionException("'" + name + "' requires at least one parameter");
        }
        if (params.get(0) instanceof final BlockValue bv)
        {
            return BooleanValue.of(test.test(bv.getBlockState(), bv.getPos()));
        }
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
        if (params.isEmpty())
        {
            throw new InternalExpressionException("'" + name + "' requires at least one parameter");
        }
        if (params.get(0) instanceof final BlockValue bv)
        {
            return StringValue.of(test.apply(bv.getBlockState(), bv.getPos()));
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
        if (params.isEmpty())
        {
            throw new InternalExpressionException("'" + name + "' requires at least one parameter");
        }
        if (params.get(0) instanceof final BlockValue bv)
        {
            try
            {
                return test.apply(bv.getBlockState(), bv.getPos(), cc.level());
            }
            catch (NullPointerException ignored)
            {
                throw new InternalExpressionException("'" + name + "' function requires a block that is positioned in the world");
            }
        }
        BlockValue block = BlockArgument.findIn(cc, params, 0).block;
        return test.apply(block.getBlockState(), block.getPos(), cc.level());
    }

    private static <T extends Comparable<T>> BlockState setProperty(Property<T> property, String name, String value,
                                                                    BlockState bs)
    {
        Optional<T> optional = property.getValue(value);
        if (optional.isEmpty())
        {
            throw new InternalExpressionException(value + " is not a valid value for property " + name);
        }
        return bs.setValue(property, optional.get());
    }

    private static void nullCheck(Value v, String name)
    {
        if (v.isNull())
        {
            throw new IllegalArgumentException(name + " cannot be null");
        }
    }

    private static float numberGetOrThrow(Value v)
    {
        double num = v.readDoubleNumber();
        if (Double.isNaN(num))
        {
            throw new IllegalArgumentException(v.getString() + " needs to be a numeric value");
        }
        return (float) num;
    }

    private static void theBooYah(ServerLevel level)
    {
        synchronized (level)
        {
            level.getChunkSource().getGeneratorState().ensureStructuresGenerated();
        }
    }

    public static void apply(Expression expression)
    {
        expression.addContextFunction("block", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            if (lv.isEmpty())
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
            if (lv.isEmpty())
            {
                throw new InternalExpressionException("Block requires at least one parameter");
            }
            return NBTSerializableValue.of(BlockArgument.findIn((CarpetContext) c, lv, 0, true).block.getData());
        });

        // poi_get(pos, radius?, type?, occupation?, column_mode?)
        expression.addContextFunction("poi", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            if (lv.isEmpty())
            {
                throw new InternalExpressionException("'poi' requires at least one parameter");
            }
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0, false);
            BlockPos pos = locator.block.getPos();
            PoiManager store = cc.level().getPoiManager();
            Registry<PoiType> poiReg = cc.registry(Registries.POINT_OF_INTEREST_TYPE);
            if (lv.size() == locator.offset)
            {
                Optional<Holder<PoiType>> foo = store.getType(pos);
                if (foo.isEmpty())
                {
                    return Value.NULL;
                }
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
                return poi == null ? Value.NULL : ListValue.of(
                        ValueConversions.of(poiReg.getKey(poi.getPoiType().value())),
                        new NumericValue(poiType.maxTickets() - Vanilla.PoiRecord_getFreeTickets(poi))
                );
            }
            int radius = NumericValue.asNumber(lv.get(locator.offset)).getInt();
            if (radius < 0)
            {
                return ListValue.of();
            }
            Predicate<Holder<PoiType>> condition = p -> true;
            PoiManager.Occupancy status = PoiManager.Occupancy.ANY;
            boolean inColumn = false;
            if (locator.offset + 1 < lv.size())
            {
                String poiType = lv.get(locator.offset + 1).getString().toLowerCase(Locale.ROOT);
                if (!"any".equals(poiType))
                {
                    PoiType type = poiReg.getOptional(InputValidator.identifierOf(poiType))
                            .orElseThrow(() -> new ThrowStatement(poiType, Throwables.UNKNOWN_POI));
                    condition = tt -> tt.value() == type;
                }
                if (locator.offset + 2 < lv.size())
                {
                    String statusString = lv.get(locator.offset + 2).getString().toLowerCase(Locale.ROOT);
                    if ("occupied".equals(statusString))
                    {
                        status = PoiManager.Occupancy.IS_OCCUPIED;
                    }
                    else if ("available".equals(statusString))
                    {
                        status = PoiManager.Occupancy.HAS_SPACE;
                    }
                    else if (!("any".equals(statusString)))
                    {
                        throw new InternalExpressionException(
                                "Incorrect POI occupation status " + status + " use `any`, " + "`occupied` or `available`"
                        );
                    }
                    if (locator.offset + 3 < lv.size())
                    {
                        inColumn = lv.get(locator.offset + 3).getBoolean();
                    }
                }
            }
            Stream<PoiRecord> pois = inColumn ?
                    store.getInSquare(condition, pos, radius, status) :
                    store.getInRange(condition, pos, radius, status);
            return ListValue.wrap(pois.sorted(Comparator.comparingDouble(p -> p.getPos().distSqr(pos))).map(p ->
                    ListValue.of(
                            ValueConversions.of(poiReg.getKey(p.getPoiType().value())),
                            new NumericValue(p.getPoiType().value().maxTickets() - Vanilla.PoiRecord_getFreeTickets(p)),
                            ValueConversions.of(p.getPos())
                    )
            ));
        });

        //poi_set(pos, null) poi_set(pos, type, occupied?,
        expression.addContextFunction("set_poi", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            if (lv.isEmpty())
            {
                throw new InternalExpressionException("'set_poi' requires at least one parameter");
            }
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0, false);
            BlockPos pos = locator.block.getPos();
            if (lv.size() < locator.offset)
            {
                throw new InternalExpressionException("'set_poi' requires the new poi type or null, after position argument");
            }
            Value poi = lv.get(locator.offset);
            PoiManager store = cc.level().getPoiManager();
            if (poi.isNull())
            {   // clear poi information
                if (store.getType(pos).isEmpty())
                {
                    return Value.FALSE;
                }
                store.remove(pos);
                return Value.TRUE;
            }
            String poiTypeString = poi.getString().toLowerCase(Locale.ROOT);
            ResourceLocation resource = InputValidator.identifierOf(poiTypeString);
            Registry<PoiType> poiReg = cc.registry(Registries.POINT_OF_INTEREST_TYPE);
            PoiType type = poiReg.getOptional(resource)
                    .orElseThrow(() -> new ThrowStatement(poiTypeString, Throwables.UNKNOWN_POI));
            Holder<PoiType> holder = poiReg.getOrThrow(ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, resource));

            int occupancy = 0;
            if (locator.offset + 1 < lv.size())
            {
                occupancy = (int) NumericValue.asNumber(lv.get(locator.offset + 1)).getLong();
                if (occupancy < 0)
                {
                    throw new InternalExpressionException("Occupancy cannot be negative");
                }
            }
            if (store.getType(pos).isPresent())
            {
                store.remove(pos);
            }
            store.add(pos, holder);
            // setting occupancy for a
            // again - don't want to mix in unnecessarily - peeps not gonna use it that often so not worries about it.
            if (occupancy > 0)
            {
                int finalO = occupancy;
                store.getInSquare(tt -> tt.value() == type, pos, 1, PoiManager.Occupancy.ANY
                ).filter(p -> p.getPos().equals(pos)).findFirst().ifPresent(p -> {
                    for (int i = 0; i < finalO; i++)
                    {
                        Vanilla.PoiRecord_callAcquireTicket(p);
                    }
                });
            }
            return Value.TRUE;
        });


        expression.addContextFunction("weather", -1, (c, t, lv) -> {
            ServerLevel world = ((CarpetContext) c).level();

            if (lv.isEmpty())//cos it can thunder when raining or when clear.
            {
                return new StringValue(world.isThundering() ? "thunder" : (world.isRaining() ? "rain" : "clear"));
            }

            Value weather = lv.get(0);
            ServerLevelData worldProperties = Vanilla.ServerLevel_getWorldProperties(world);
            if (lv.size() == 1)
            {
                return new NumericValue(switch (weather.getString().toLowerCase(Locale.ROOT))
                {
                    case "clear" -> worldProperties.getClearWeatherTime();
                    case "rain" -> world.isRaining() ? worldProperties.getRainTime() : 0;//cos if not it gives 1 for some reason
                    case "thunder" -> world.isThundering() ? worldProperties.getThunderTime() : 0;//same dealio here
                    default -> throw new InternalExpressionException("Weather can only be 'clear', 'rain' or 'thunder'");
                });
            }
            if (lv.size() == 2)
            {
                int ticks = NumericValue.asNumber(lv.get(1), "tick_time in 'weather'").getInt();
                switch (weather.getString().toLowerCase(Locale.ROOT))
                {
                    case "clear" -> world.setWeatherParameters(ticks, 0, false, false);
                    case "rain" -> world.setWeatherParameters(0, ticks, true, false);
                    case "thunder" -> world.setWeatherParameters(
                            0,
                            ticks,//this is used to set thunder time, idk why...
                            true,
                            true
                    );
                    default -> throw new InternalExpressionException("Weather can only be 'clear', 'rain' or 'thunder'");
                }
                return NumericValue.of(ticks);
            }
            throw new InternalExpressionException("'weather' requires 0, 1 or 2 arguments");
        });

        expression.addUnaryFunction("pos", v ->
        {
            if (v instanceof final BlockValue bv)
            {
                BlockPos pos = bv.getPos();
                if (pos == null)
                {
                    throw new InternalExpressionException("Cannot fetch position of an unrealized block");
                }
                return ValueConversions.of(pos);
            }
            if (v instanceof final EntityValue ev)
            {
                Entity e = ev.getEntity();
                if (e == null)
                {
                    throw new InternalExpressionException("Null entity");
                }
                return ValueConversions.of(e.position());
            }
            throw new InternalExpressionException("'pos' works only with a block or an entity type");
        });

        expression.addContextFunction("pos_offset", -1, (c, t, lv) ->
        {
            BlockArgument locator = BlockArgument.findIn((CarpetContext) c, lv, 0);
            BlockPos pos = locator.block.getPos();
            if (lv.size() <= locator.offset)
            {
                throw new InternalExpressionException("'pos_offset' needs at least position, and direction");
            }
            String directionString = lv.get(locator.offset).getString();
            Direction dir = DIRECTION_MAP.get(directionString);
            if (dir == null)
            {
                throw new InternalExpressionException("Unknown direction: " + directionString);
            }
            int howMuch = 1;
            if (lv.size() > locator.offset + 1)
            {
                howMuch = (int) NumericValue.asNumber(lv.get(locator.offset + 1)).getLong();
            }
            return ValueConversions.of(pos.relative(dir, howMuch));
        });

        expression.addContextFunction("solid", -1, (c, t, lv) ->
                genericStateTest(c, "solid", lv, (s, p, w) -> BooleanValue.of(s.isRedstoneConductor(w, p)))); // isSimpleFullBlock

        expression.addContextFunction("air", -1, (c, t, lv) ->
                booleanStateTest(c, "air", lv, (s, p) -> s.isAir()));

        expression.addContextFunction("liquid", -1, (c, t, lv) ->
                booleanStateTest(c, "liquid", lv, (s, p) -> !s.getFluidState().isEmpty()));

        expression.addContextFunction("flammable", -1, (c, t, lv) ->
                booleanStateTest(c, "flammable", lv, (s, p) -> s.ignitedByLava()));

        expression.addContextFunction("transparent", -1, (c, t, lv) ->
                booleanStateTest(c, "transparent", lv, (s, p) -> !s.isSolid()));

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
            BlockPos pos = BlockArgument.findIn((CarpetContext) c, lv, 0).block.getPos();
            ChunkPos chunkPos = new ChunkPos(pos);
            return BooleanValue.of(WorldgenRandom.seedSlimeChunk(
                    chunkPos.x, chunkPos.z,
                    ((CarpetContext) c).level().getSeed(),
                    987234911L
            ).nextInt(10) == 0);
        });

        expression.addContextFunction("top", -1, (c, t, lv) ->
        {
            String type = lv.get(0).getString().toLowerCase(Locale.ROOT);
            Heightmap.Types htype = switch (type)
            {
                //case "light": htype = Heightmap.Type.LIGHT_BLOCKING; break;  //investigate
                case "motion" -> Heightmap.Types.MOTION_BLOCKING;
                case "terrain" -> Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
                case "ocean_floor" -> Heightmap.Types.OCEAN_FLOOR;
                case "surface" -> Heightmap.Types.WORLD_SURFACE;
                default -> throw new InternalExpressionException("Unknown heightmap type: " + type);
            };
            BlockArgument locator = BlockArgument.findIn((CarpetContext) c, lv, 1);
            BlockPos pos = locator.block.getPos();
            int x = pos.getX();
            int z = pos.getZ();
            return new NumericValue(((CarpetContext) c).level().getChunk(x >> 4, z >> 4).getHeight(htype, x & 15, z & 15) + 1);
        });

        expression.addContextFunction("loaded", -1, (c, t, lv) ->
                BooleanValue.of((((CarpetContext) c).level().hasChunkAt(BlockArgument.findIn((CarpetContext) c, lv, 0).block.getPos()))));

        // Deprecated, use loaded_status as more indicative
        expression.addContextFunction("loaded_ep", -1, (c, t, lv) ->
        {
            c.host.issueDeprecation("loaded_ep(...)");
            BlockPos pos = BlockArgument.findIn((CarpetContext) c, lv, 0).block.getPos();
            return BooleanValue.of(((CarpetContext) c).level().isPositionEntityTicking(pos));
        });

        expression.addContextFunction("loaded_status", -1, (c, t, lv) ->
        {
            BlockPos pos = BlockArgument.findIn((CarpetContext) c, lv, 0).block.getPos();
            LevelChunk chunk = ((CarpetContext) c).level().getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, false);
            return chunk == null ? Value.ZERO : new NumericValue(chunk.getFullStatus().ordinal());
        });

        expression.addContextFunction("is_chunk_generated", -1, (c, t, lv) ->
        {
            BlockArgument locator = BlockArgument.findIn((CarpetContext) c, lv, 0);
            BlockPos pos = locator.block.getPos();
            boolean force = false;
            if (lv.size() > locator.offset)
            {
                force = lv.get(locator.offset).getBoolean();
            }
            return BooleanValue.of(canHasChunk(((CarpetContext) c).level(), new ChunkPos(pos), null, force));
        });

        expression.addContextFunction("generation_status", -1, (c, t, lv) ->
        {
            BlockArgument blockArgument = BlockArgument.findIn((CarpetContext) c, lv, 0);
            BlockPos pos = blockArgument.block.getPos();
            boolean forceLoad = false;
            if (lv.size() > blockArgument.offset)
            {
                forceLoad = lv.get(blockArgument.offset).getBoolean();
            }
            ChunkAccess chunk = ((CarpetContext) c).level().getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.EMPTY, forceLoad);
            return chunk == null ? Value.NULL : ValueConversions.of(BuiltInRegistries.CHUNK_STATUS.getKey(chunk.getPersistedStatus()));
        });

        expression.addContextFunction("chunk_tickets", -1, (c, t, lv) ->
        {
            ServerLevel world = ((CarpetContext) c).level();
            DistanceManager foo = world.getChunkSource().chunkMap.getDistanceManager();
            Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> levelTickets = Vanilla.ChunkTicketManager_getTicketsByPosition(foo);

            List<Value> res = new ArrayList<>();
            if (lv.isEmpty())
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
                    ((CarpetContext) c).level().neighborChanged(p, s.getBlock(), null);
                    return true;
                }));

        expression.addContextFunction("block_tick", -1, (c, t, lv) ->
                booleanStateTest(c, "block_tick", lv, (s, p) ->
                {
                    ServerLevel w = ((CarpetContext) c).level();
                    s.randomTick(w, p, w.random);
                    return true;
                }));

        expression.addContextFunction("random_tick", -1, (c, t, lv) ->
                booleanStateTest(c, "random_tick", lv, (s, p) ->
                {
                    ServerLevel w = ((CarpetContext) c).level();
                    if (s.isRandomlyTicking() || s.getFluidState().isRandomlyTicking())
                    {
                        s.randomTick(w, p, w.random);
                    }
                    return true;
                }));

        // lazy cause its parked execution
        expression.addLazyFunction("without_updates", 1, (c, t, lv) ->
        {
            if (Carpet.getImpendingFillSkipUpdates().get())
            {
                return lv.get(0);
            }
            Value[] result = new Value[]{Value.NULL};
            ((CarpetContext) c).server().executeBlocking(() ->
            {
                ThreadLocal<Boolean> skipUpdates = Carpet.getImpendingFillSkipUpdates();
                boolean previous = skipUpdates.get();
                try
                {
                    skipUpdates.set(true);
                    result[0] = lv.get(0).evalValue(c, t);
                }
                finally
                {
                    skipUpdates.set(previous);
                }
            });
            return (cc, tt) -> result[0];
        });

        expression.addContextFunction("set", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            ServerLevel world = cc.level();
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
                if (args.get(0) instanceof final ListValue list)
                {
                    if (args.size() == 2 && NBTSerializableValue.fromValue(args.get(1)) instanceof final NBTSerializableValue nbtsv)
                    {
                        data = nbtsv.getCompoundTag();
                    }
                    args = list.getItems();
                }
                else if (args.get(0) instanceof final MapValue map)
                {
                    if (args.size() == 2 && NBTSerializableValue.fromValue(args.get(1)) instanceof final NBTSerializableValue nbtsv)
                    {
                        data = nbtsv.getCompoundTag();
                    }
                    Map<Value, Value> state = map.getMap();
                    List<Value> mapargs = new ArrayList<>();
                    state.forEach((k, v) -> {
                        mapargs.add(k);
                        mapargs.add(v);
                    });
                    args = mapargs;
                }
                else
                {
                    if ((args.size() & 1) == 1 && NBTSerializableValue.fromValue(args.get(args.size() - 1)) instanceof final NBTSerializableValue nbtsv)
                    {
                        data = nbtsv.getCompoundTag();
                    }
                }
                StateDefinition<Block, BlockState> states = sourceBlockState.getBlock().getStateDefinition();
                for (int i = 0; i < args.size() - 1; i += 2)
                {
                    String paramString = args.get(i).getString();
                    Property<?> property = states.getProperty(paramString);
                    if (property == null)
                    {
                        throw new InternalExpressionException("Property " + paramString + " doesn't apply to " + sourceLocator.block.getString());
                    }
                    String paramValue = args.get(i + 1).getString();
                    sourceBlockState = setProperty(property, paramString, paramValue, sourceBlockState);
                }
            }

            if (data == null)
            {
                data = sourceLocator.block.getData();
            }
            CompoundTag finalData = data;

            if (sourceBlockState == targetBlockState && data == null)
            {
                return Value.FALSE;
            }
            BlockState finalSourceBlockState = sourceBlockState;
            BlockPos targetPos = targetLocator.block.getPos();
            Boolean[] result = new Boolean[]{true};
            cc.server().executeBlocking(() ->
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
                        be.loadWithComponents(destTag, world.registryAccess());
                        be.setChanged();
                        success = true;
                    }
                }
                result[0] = success;
            });
            return !result[0] ? Value.FALSE : new BlockValue(finalSourceBlockState, world, targetLocator.block.getPos());
        });

        expression.addContextFunction("destroy", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            RegistryAccess regs = cc.registryAccess();
            ServerLevel world = cc.level();
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            BlockState state = locator.block.getBlockState();
            if (state.isAir())
            {
                return Value.FALSE;
            }
            BlockPos where = locator.block.getPos();
            BlockEntity be = world.getBlockEntity(where);
            long how = 0;
            Item item = Items.DIAMOND_PICKAXE;
            boolean playerBreak = false;
            if (lv.size() > locator.offset)
            {
                Value val = lv.get(locator.offset);
                if (val instanceof final NumericValue number)
                {
                    how = number.getLong();
                }
                else
                {
                    playerBreak = true;
                    String itemString = val.getString();
                    item = cc.registry(Registries.ITEM).getOptional(InputValidator.identifierOf(itemString))
                            .orElseThrow(() -> new ThrowStatement(itemString, Throwables.UNKNOWN_ITEM));
                }
            }
            CompoundTag tag = null;
            if (lv.size() > locator.offset + 1)
            {
                if (!playerBreak)
                {
                    throw new InternalExpressionException("tag is not necessary with 'destroy' with no item");
                }
                Value tagValue = lv.get(locator.offset + 1);
                if (!tagValue.isNull())
                {
                    tag = tagValue instanceof final NBTSerializableValue nbtsv
                            ? nbtsv.getCompoundTag()
                            : NBTSerializableValue.parseStringOrFail(tagValue.getString()).getCompoundTag();
                }
            }
            ItemStack tool;
            if (tag != null)
            {
                tool = ItemStack.parseOptional(regs, tag);
            }
            else
            {
                tool = new ItemStack(item, 1);
            }
            if (playerBreak && state.getDestroySpeed(world, where) < 0.0)
            {
                return Value.FALSE;
            }
            boolean removed = world.removeBlock(where, false);
            if (!removed)
            {
                return Value.FALSE;
            }
            world.levelEvent(null, 2001, where, Block.getId(state));

            final MutableBoolean toolBroke = new MutableBoolean(false);
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
                final int finalDamageAmount = damageAmount;
                tool.hurtAndBreak(damageAmount, world, null, (i) ->  { if (finalDamageAmount > 0) toolBroke.setTrue(); } );
                if (!isUsingEffectiveTool)
                {
                    dropLoot = false;
                }
            }

            if (dropLoot)
            {
                if (how < 0 || (tag != null && EnchantmentHelper.getItemEnchantmentLevel(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), tool) > 0))
                {
                    Block.popResource(world, where, new ItemStack(state.getBlock()));
                }
                else
                {
                    if (how > 0)
                    {
                        tool.enchant(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), (int) how);
                    }
                    if (DUMMY_ENTITY == null)
                    {
                        DUMMY_ENTITY = new FallingBlockEntity(EntityType.FALLING_BLOCK, null);
                    }
                    Block.dropResources(state, world, where, be, DUMMY_ENTITY, tool);
                }
            }
            if (!playerBreak) // no tool info - block brokwn
            {
                return Value.TRUE;
            }
            if (toolBroke.booleanValue())
            {
                return Value.NULL;
            }
            return new NBTSerializableValue(() -> tool.saveOptional(regs));

        });

        expression.addContextFunction("harvest", -1, (c, t, lv) ->
        {
            if (lv.size() < 2)
            {
                throw new InternalExpressionException("'harvest' takes at least 2 parameters: entity and block, or position, to harvest");
            }
            CarpetContext cc = (CarpetContext) c;
            Level world = cc.level();
            Value entityValue = lv.get(0);
            if (!(entityValue instanceof final EntityValue ev))
            {
                return Value.FALSE;
            }
            Entity e = ev.getEntity();
            if (!(e instanceof final ServerPlayer player))
            {
                return Value.FALSE;
            }
            BlockArgument locator = BlockArgument.findIn(cc, lv, 1);
            BlockPos where = locator.block.getPos();
            BlockState state = locator.block.getBlockState();
            Block block = state.getBlock();
            boolean success = false;
            if (!((block == Blocks.BEDROCK || block == Blocks.BARRIER) && player.gameMode.isSurvival()))
            {
                success = player.gameMode.destroyBlock(where);
            }
            if (success)
            {
                world.levelEvent(null, 2001, where, Block.getId(state));
            }
            return BooleanValue.of(success);
        });

        expression.addContextFunction("create_explosion", -1, (c, t, lv) ->
        {
            if (lv.isEmpty())
            {
                throw new InternalExpressionException("'create_explosion' requires at least a position to explode");
            }
            CarpetContext cc = (CarpetContext) c;
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
                if (powah < 0)
                {
                    throw new InternalExpressionException("Explosion power cannot be negative");
                }
                if (lv.size() > location.offset + 1)
                {
                    String strval = lv.get(location.offset + 1).getString();
                    try
                    {
                        mode = Explosion.BlockInteraction.valueOf(strval.toUpperCase(Locale.ROOT));
                    }
                    catch (IllegalArgumentException ile)
                    {
                        throw new InternalExpressionException("Illegal explosions block behaviour: " + strval);
                    }
                    if (lv.size() > location.offset + 2)
                    {
                        createFire = lv.get(location.offset + 2).getBoolean();
                        if (lv.size() > location.offset + 3)
                        {
                            Value enVal = lv.get(location.offset + 3);
                            if (!enVal.isNull())
                            {
                                if (enVal instanceof final EntityValue ev)
                                {
                                    source = ev.getEntity();
                                }
                                else
                                {
                                    throw new InternalExpressionException("Fourth parameter of the explosion has to be an entity, not " + enVal.getTypeString());
                                }
                            }
                            if (lv.size() > location.offset + 4)
                            {
                                enVal = lv.get(location.offset + 4);
                                if (!enVal.isNull())
                                {
                                    if (enVal instanceof final EntityValue ev)
                                    {
                                        Entity attackingEntity = ev.getEntity();
                                        if (attackingEntity instanceof final LivingEntity le)
                                        {
                                            attacker = le;
                                        }
                                        else
                                        {
                                            throw new InternalExpressionException("Attacking entity needs to be a living thing, " +
                                                    ValueConversions.of(cc.registry(Registries.ENTITY_TYPE).getKey(attackingEntity.getType())).getString() + " ain't it.");
                                        }
                                    }
                                    else
                                    {
                                        throw new InternalExpressionException("Fifth parameter of the explosion has to be a living entity, not " + enVal.getTypeString());
                                    }
                                }
                            }
                        }
                    }
                }
            }
            LivingEntity theAttacker = attacker;

            // copy of ServerWorld.createExplosion #TRACK#
            ServerExplosion explosion = new ServerExplosion(cc.level(), source, null, null, pos, powah, createFire, mode)
            {
                @Override
                @Nullable
                public
                LivingEntity getIndirectSourceEntity()
                {
                    return theAttacker;
                }
            };
            explosion.explode();
            ParticleOptions explosionParticle = explosion.isSmall() ? ParticleTypes.EXPLOSION : ParticleTypes.EXPLOSION_EMITTER;
            cc.level().players().forEach(spe -> {
                if (spe.distanceToSqr(pos) < 4096.0D)
                {
                    spe.connection.send(new ClientboundExplodePacket(pos, Optional.ofNullable(explosion.getHitPlayers().get(spe)), explosionParticle, SoundEvents.GENERIC_EXPLODE));
                }
            });
            return Value.TRUE;
        });

        // TODO rename to use_item
        expression.addContextFunction("place_item", -1, (c, t, lv) ->
        {
            if (lv.size() < 2)
            {
                throw new InternalExpressionException("'place_item' takes at least 2 parameters: item and block, or position, to place onto");
            }
            CarpetContext cc = (CarpetContext) c;
            String itemString = lv.get(0).getString();
            Vector3Argument locator = Vector3Argument.findIn(lv, 1);
            ItemStack stackArg = NBTSerializableValue.parseItem(itemString, cc.registryAccess());
            BlockPos where = BlockPos.containing(locator.vec);
            // Paintings throw an exception if their direction is vertical, therefore we change the default here
            String facing = lv.size() > locator.offset
                    ? lv.get(locator.offset).getString()
                    : stackArg.getItem() != Items.PAINTING ? "up" : "north";
            boolean sneakPlace = false;
            if (lv.size() > locator.offset + 1)
            {
                sneakPlace = lv.get(locator.offset + 1).getBoolean();
            }

            BlockValue.PlacementContext ctx = BlockValue.PlacementContext.from(cc.level(), where, facing, sneakPlace, stackArg);

            if (!(stackArg.getItem() instanceof final BlockItem blockItem))
            {
                InteractionResult useResult = ctx.getItemInHand().useOn(ctx);
                if (useResult == InteractionResult.CONSUME || useResult == InteractionResult.SUCCESS)
                {
                    return Value.TRUE;
                }
            }
            else
            { // not sure we need special case for block items, since useOnBlock can do that as well
                if (!ctx.canPlace())
                {
                    return Value.FALSE;
                }
                BlockState placementState = blockItem.getBlock().getStateForPlacement(ctx);
                if (placementState != null)
                {
                    Level level = ctx.getLevel();
                    if (placementState.canSurvive(level, where))
                    {
                        level.setBlock(where, placementState, 2);
                        SoundType blockSoundGroup = placementState.getSoundType();
                        level.playSound(null, where, blockSoundGroup.getPlaceSound(), SoundSource.BLOCKS, (blockSoundGroup.getVolume() + 1.0F) / 2.0F, blockSoundGroup.getPitch() * 0.8F);
                        return Value.TRUE;
                    }
                }
            }
            return Value.FALSE;
        });

        expression.addContextFunction("blocks_movement", -1, (c, t, lv) ->
                booleanStateTest(c, "blocks_movement", lv, (s, p) ->
                        !s.isPathfindable(PathComputationType.LAND)));

        expression.addContextFunction("block_sound", -1, (c, t, lv) ->
                stateStringQuery(c, "block_sound", lv, (s, p) ->
                        Colors.soundName.get(s.getSoundType())));

        expression.addContextFunction("material", -1, (c, t, lv) -> {
            c.host.issueDeprecation("material(...)"); // deprecated for block_state()
            return StringValue.of("unknown");
        });

        expression.addContextFunction("map_colour", -1, (c, t, lv) ->
                stateStringQuery(c, "map_colour", lv, (s, p) ->
                        Colors.mapColourName.get(s.getMapColor(((CarpetContext) c).level(), p))));

        // Deprecated for block_state()
        expression.addContextFunction("property", -1, (c, t, lv) ->
        {
            c.host.issueDeprecation("property(...)");
            BlockArgument locator = BlockArgument.findIn((CarpetContext) c, lv, 0);
            BlockState state = locator.block.getBlockState();
            if (lv.size() <= locator.offset)
            {
                throw new InternalExpressionException("'property' requires to specify a property to query");
            }
            String tag = lv.get(locator.offset).getString();
            StateDefinition<Block, BlockState> states = state.getBlock().getStateDefinition();
            Property<?> property = states.getProperty(tag);
            return property == null ? Value.NULL : new StringValue(state.getValue(property).toString().toLowerCase(Locale.ROOT));
        });

        // Deprecated for block_state()
        expression.addContextFunction("block_properties", -1, (c, t, lv) ->
        {
            c.host.issueDeprecation("block_properties(...)");
            BlockArgument locator = BlockArgument.findIn((CarpetContext) c, lv, 0);
            BlockState state = locator.block.getBlockState();
            StateDefinition<Block, BlockState> states = state.getBlock().getStateDefinition();
            return ListValue.wrap(states.getProperties().stream().map(
                    p -> new StringValue(p.getName()))
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
                Map<Value, Value> properties = new HashMap<>();
                for (Property<?> p : states.getProperties())
                {
                    properties.put(StringValue.of(p.getName()), ValueConversions.fromProperty(state, p));
                }
                return MapValue.wrap(properties);
            }
            String tag = lv.get(locator.offset).getString();
            Property<?> property = states.getProperty(tag);
            return property == null ? Value.NULL : ValueConversions.fromProperty(state, property);
        });

        expression.addContextFunction("block_list", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            Registry<Block> blocks = cc.registry(Registries.BLOCK);
            if (lv.isEmpty())
            {
                return ListValue.wrap(blocks.listElements().map(blockReference -> ValueConversions.of(blockReference.key().location())));
            }
            ResourceLocation tag = InputValidator.identifierOf(lv.get(0).getString());
            Optional<HolderSet.Named<Block>> tagset = blocks.get(TagKey.create(Registries.BLOCK, tag));
            return tagset.isEmpty() ? Value.NULL : ListValue.wrap(tagset.get().stream().map(b -> ValueConversions.of(blocks.getKey(b.value()))));
        });

        expression.addContextFunction("block_tags", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            Registry<Block> blocks = cc.registry(Registries.BLOCK);
            if (lv.isEmpty())
            {
                return ListValue.wrap(blocks.getTags().map(ValueConversions::of));
            }
            BlockArgument blockLocator = BlockArgument.findIn(cc, lv, 0, true);
            if (blockLocator.offset == lv.size())
            {
                Block target = blockLocator.block.getBlockState().getBlock();
                return ListValue.wrap(blocks.getTags().filter(e -> e.stream().anyMatch(h -> (h.value() == target))).map(ValueConversions::of));
            }
            String tag = lv.get(blockLocator.offset).getString();
            Optional<HolderSet.Named<Block>> tagSet = blocks.get(TagKey.create(Registries.BLOCK, InputValidator.identifierOf(tag)));
            return tagSet.isEmpty() ? Value.NULL : BooleanValue.of(blockLocator.block.getBlockState().is(tagSet.get()));
        });

        expression.addContextFunction("biome", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext) c;
            ServerLevel world = cc.level();
            if (lv.isEmpty())
            {
                return ListValue.wrap(cc.registry(Registries.BIOME).listElements().map(biomeReference -> ValueConversions.of(biomeReference.key().location())));
            }

            Biome biome;
            BiomeSource biomeSource = world.getChunkSource().getGenerator().getBiomeSource();
            if (lv.size() == 1
                    && lv.get(0) instanceof final MapValue map
                    && biomeSource instanceof final MultiNoiseBiomeSource mnbs
            )
            {
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
                ResourceLocation biomeId = cc.registry(Registries.BIOME).getKey(biome);
                return NBTSerializableValue.nameFromRegistryId(biomeId);
            }

            BlockArgument locator = BlockArgument.findIn(cc, lv, 0, false, false, true);

            if (locator.replacement != null)
            {
                biome = world.registryAccess().lookupOrThrow(Registries.BIOME).getValue(InputValidator.identifierOf(locator.replacement));
                if (biome == null)
                {
                    throw new ThrowStatement(locator.replacement, Throwables.UNKNOWN_BIOME);
                }
            }
            else
            {
                biome = world.getBiome(locator.block.getPos()).value();
            }
            // in locatebiome
            if (locator.offset == lv.size())
            {
                ResourceLocation biomeId = cc.registry(Registries.BIOME).getKey(biome);
                return NBTSerializableValue.nameFromRegistryId(biomeId);
            }
            String biomeFeature = lv.get(locator.offset).getString();
            BiFunction<ServerLevel, Biome, Value> featureProvider = BiomeInfo.biomeFeatures.get(biomeFeature);
            if (featureProvider == null)
            {
                throw new InternalExpressionException("Unknown biome feature: " + biomeFeature);
            }
            return featureProvider.apply(world, biome);
        });

        expression.addContextFunction("set_biome", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            if (lv.size() == locator.offset)
            {
                throw new InternalExpressionException("'set_biome' needs a biome name as an argument");
            }
            String biomeName = lv.get(locator.offset).getString();
            // from locatebiome command code
            Holder<Biome> biome = cc.registry(Registries.BIOME).get(ResourceKey.create(Registries.BIOME, InputValidator.identifierOf(biomeName)))
                    .orElseThrow(() -> new ThrowStatement(biomeName, Throwables.UNKNOWN_BIOME));
            boolean doImmediateUpdate = true;
            if (lv.size() > locator.offset + 1)
            {
                doImmediateUpdate = lv.get(locator.offset + 1).getBoolean();
            }
            ServerLevel world = cc.level();
            BlockPos pos = locator.block.getPos();
            ChunkAccess chunk = world.getChunk(pos); // getting level chunk instead of protochunk with biomes
            int biomeX = QuartPos.fromBlock(pos.getX());
            int biomeY = QuartPos.fromBlock(pos.getY());
            int biomeZ = QuartPos.fromBlock(pos.getZ());
            try
            {
                int i = QuartPos.fromBlock(chunk.getMinY());
                int j = i + QuartPos.fromBlock(chunk.getHeight()) - 1;
                int k = Mth.clamp(biomeY, i, j);
                int l = chunk.getSectionIndex(QuartPos.toBlock(k));
                // accessing outside of the interface - might be dangerous in the future.
                ((PalettedContainer<Holder<Biome>>) chunk.getSection(l).getBiomes()).set(biomeX & 3, k & 3, biomeZ & 3, biome);
            }
            catch (Throwable var8)
            {
                return Value.FALSE;
            }
            if (doImmediateUpdate)
            {
                WorldTools.forceChunkUpdate(pos, world);
            }
            chunk.markUnsaved();
            return Value.TRUE;
        });

        expression.addContextFunction("reload_chunk", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext) c;
            BlockPos pos = BlockArgument.findIn(cc, lv, 0).block.getPos();
            ServerLevel world = cc.level();
            cc.server().executeBlocking(() -> WorldTools.forceChunkUpdate(pos, world));
            return Value.TRUE;
        });

        expression.addContextFunction("structure_references", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext) c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            ServerLevel world = cc.level();
            BlockPos pos = locator.block.getPos();
            Map<Structure, LongSet> references = world.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.STRUCTURE_REFERENCES).getAllReferences();
            Registry<Structure> reg = cc.registry(Registries.STRUCTURE);
            if (lv.size() == locator.offset)
            {
                return ListValue.wrap(references.entrySet().stream().
                        filter(e -> e.getValue() != null && !e.getValue().isEmpty()).
                        map(e -> NBTSerializableValue.nameFromRegistryId(reg.getKey(e.getKey())))
                );
            }
            String simpleStructureName = lv.get(locator.offset).getString().toLowerCase(Locale.ROOT);
            Structure structureName = reg.getValue(InputValidator.identifierOf(simpleStructureName));
            if (structureName == null)
            {
                return Value.NULL;
            }
            LongSet structureReferences = references.get(structureName);
            if (structureReferences == null || structureReferences.isEmpty())
            {
                return ListValue.of();
            }
            return ListValue.wrap(structureReferences.longStream().mapToObj(l -> ListValue.of(
                    new NumericValue(16L * ChunkPos.getX(l)),
                    Value.ZERO,
                    new NumericValue(16L * ChunkPos.getZ(l)))));
        });

        expression.addContextFunction("structure_eligibility", -1, (c, t, lv) ->
        {// TODO rename structureName to class
            CarpetContext cc = (CarpetContext) c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);

            ServerLevel world = cc.level();

            // well, because
            theBooYah(world);

            BlockPos pos = locator.block.getPos();
            List<Structure> structure = new ArrayList<>();
            boolean needSize = false;
            boolean singleOutput = false;
            Registry<Structure> reg = cc.registry(Registries.STRUCTURE);
            if (lv.size() > locator.offset)
            {
                Value requested = lv.get(locator.offset);
                if (!requested.isNull())
                {
                    String reqString = requested.getString();
                    ResourceLocation id = InputValidator.identifierOf(reqString);
                    Structure requestedStructure = reg.getValue(id);
                    if (requestedStructure != null)
                    {
                        singleOutput = true;
                        structure.add(requestedStructure);
                    }
                    else
                    {
                        StructureType<?> sss = cc.registry(Registries.STRUCTURE_TYPE).getValue(id);
                        reg.entrySet().stream().filter(e -> e.getValue().type() == sss).forEach(e -> structure.add(e.getValue()));
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
                if (lv.size() > locator.offset + 1)
                {
                    needSize = lv.get(locator.offset + 1).getBoolean();
                }
            }
            else
            {
                structure.addAll(reg.entrySet().stream().map(Map.Entry::getValue).toList());
            }
            if (singleOutput)
            {
                StructureStart start = FeatureGenerator.shouldStructureStartAt(world, pos, structure.get(0), needSize);
                return start == null ? Value.NULL : !needSize ? Value.TRUE : ValueConversions.of(start, cc.registryAccess());
            }
            Map<Value, Value> ret = new HashMap<>();
            for (Structure str : structure)
            {
                StructureStart start;
                try
                {
                    start = FeatureGenerator.shouldStructureStartAt(world, pos, str, needSize);
                }
                catch (NullPointerException npe)
                {
                    CarpetScriptServer.LOG.error("Failed to detect structure: " + reg.getKey(str));
                    start = null;
                }

                if (start == null)
                {
                    continue;
                }

                Value key = NBTSerializableValue.nameFromRegistryId(reg.getKey(str));
                ret.put(key, (!needSize) ? Value.NULL : ValueConversions.of(start, cc.registryAccess()));
            }
            return MapValue.wrap(ret);
        });

        expression.addContextFunction("structures", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext) c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);

            ServerLevel world = cc.level();
            BlockPos pos = locator.block.getPos();
            Map<Structure, StructureStart> structures = world.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS).getAllStarts();
            Registry<Structure> reg = cc.registry(Registries.STRUCTURE);
            if (lv.size() == locator.offset)
            {
                Map<Value, Value> structureList = new HashMap<>();
                for (Map.Entry<Structure, StructureStart> entry : structures.entrySet())
                {
                    StructureStart start = entry.getValue();
                    if (start == StructureStart.INVALID_START)
                    {
                        continue;
                    }
                    BoundingBox box = start.getBoundingBox();
                    structureList.put(
                            NBTSerializableValue.nameFromRegistryId(reg.getKey(entry.getKey())),
                            ValueConversions.of(box)
                    );
                }
                return MapValue.wrap(structureList);
            }
            String structureName = lv.get(locator.offset).getString().toLowerCase(Locale.ROOT);
            return ValueConversions.of(structures.get(reg.getValue(InputValidator.identifierOf(structureName))), cc.registryAccess());
        });

        expression.addContextFunction("set_structure", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);

            ServerLevel world = cc.level();
            BlockPos pos = locator.block.getPos();

            if (lv.size() == locator.offset)
            {
                throw new InternalExpressionException("'set_structure requires at least position and a structure name");
            }
            String structureName = lv.get(locator.offset).getString().toLowerCase(Locale.ROOT);
            Structure configuredStructure = FeatureGenerator.resolveConfiguredStructure(structureName, world, pos);
            if (configuredStructure == null)
            {
                throw new ThrowStatement(structureName, Throwables.UNKNOWN_STRUCTURE);
            }
            // good 'ol pointer
            Value[] result = new Value[]{Value.NULL};
            // technically a world modification. Even if we could let it slide, we will still park it
            ((CarpetContext) c).server().executeBlocking(() ->
            {
                Map<Structure, StructureStart> structures = world.getChunk(pos).getAllStarts();
                if (lv.size() == locator.offset + 1)
                {
                    boolean res = FeatureGenerator.plopGrid(configuredStructure, ((CarpetContext) c).level(), locator.block.getPos());
                    result[0] = res ? Value.TRUE : Value.FALSE;
                    return;
                }
                Value newValue = lv.get(locator.offset + 1);
                if (newValue.isNull()) // remove structure
                {
                    if (!structures.containsKey(configuredStructure))
                    {
                        return;
                    }
                    StructureStart start = structures.get(configuredStructure);
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
                            if (references.containsKey(configuredStructure) && references.get(configuredStructure) != null)
                            {
                                references.get(configuredStructure).remove(structureChunkPos.toLong());
                            }
                        }
                    }
                    structures.remove(configuredStructure);
                    result[0] = Value.TRUE;
                }
            });
            return result[0]; // preventing from lazy evaluating of the result in case a future completes later
        });

        // todo maybe enable chunk blending?
        expression.addContextFunction("reset_chunk", -1, (c, t, lv) ->
        {
            return Value.NULL;
            /*
            CarpetContext cc = (CarpetContext) c;
            List<ChunkPos> requestedChunks = new ArrayList<>();
            if (lv.size() == 1)
            {
                //either one block or list of chunks
                Value first = lv.get(0);
                if (first instanceof final ListValue list)
                {
                    List<Value> listVal = list.getItems();
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
                    for (int x = Math.min(from.x, to.x); x <= xmax; x++)
                    {
                        for (int z = Math.min(from.z, to.z); z <= zmax; z++)
                        {
                            requestedChunks.add(new ChunkPos(x, z));
                        }
                    }
                }
                else
                {
                    requestedChunks.add(from);
                }
            }
            ServerLevel world = cc.level();
            Value[] result = new Value[]{Value.NULL};
            ((CarpetContext) c).server().executeBlocking(() ->
            {
                Map<String, Integer> report = Vanilla.ChunkMap_regenerateChunkRegion(world.getChunkSource().chunkMap, requestedChunks);
                result[0] = MapValue.wrap(report.entrySet().stream().collect(Collectors.toMap(
                        e -> new StringValue(e.getKey()),
                        e -> new NumericValue(e.getValue())
                )));
            });
            return result[0];

             */
        });

        expression.addContextFunction("inhabited_time", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            BlockPos pos = locator.block.getPos();
            return new NumericValue(cc.level().getChunk(pos).getInhabitedTime());
        });

        expression.addContextFunction("spawn_potential", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            BlockPos pos = locator.block.getPos();
            double requiredCharge = 1;
            if (lv.size() > locator.offset)
            {
                requiredCharge = NumericValue.asNumber(lv.get(locator.offset)).getDouble();
            }
            NaturalSpawner.SpawnState charger = cc.level().getChunkSource().getLastSpawnState();
            return charger == null ? Value.NULL : new NumericValue(Vanilla.SpawnState_getPotentialCalculator(charger).getPotentialEnergyChange(pos, requiredCharge)
            );
        });

        expression.addContextFunction("add_chunk_ticket", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            BlockPos pos = locator.block.getPos();
            if (lv.size() != locator.offset + 2)
            {
                throw new InternalExpressionException("'add_chunk_ticket' requires block position, ticket type and radius");
            }
            String type = lv.get(locator.offset).getString();
            TicketType<?> ticket = ticketTypes.get(type.toLowerCase(Locale.ROOT));
            if (ticket == null)
            {
                throw new InternalExpressionException("Unknown ticket type: " + type);
            }
            int radius = NumericValue.asNumber(lv.get(locator.offset + 1)).getInt();
            if (radius < 1 || radius > 32)
            {
                throw new InternalExpressionException("Ticket radius should be between 1 and 32 chunks");
            }
            // due to types we will wing it:
            ChunkPos target = new ChunkPos(pos);
            if (ticket == TicketType.PORTAL) // portal
            {
                cc.level().getChunkSource().addRegionTicket(TicketType.PORTAL, target, radius, pos);
            }
            else if (ticket == TicketType.ENDER_PEARL) // post teleport
            {
                cc.level().getChunkSource().addRegionTicket(TicketType.ENDER_PEARL, target, radius, target);
            }
            else
            {
                cc.level().getChunkSource().addRegionTicket(TicketType.UNKNOWN, target, radius, target);
            }
            return new NumericValue(ticket.timeout());
        });

        expression.addContextFunction("sample_noise", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            if (lv.isEmpty())
            {
                return ListValue.wrap(cc.registry(Registries.DENSITY_FUNCTION).keySet().stream().map(ValueConversions::of));
            }
            ServerLevel level = cc.level();
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            BlockPos pos = locator.block.getPos();
            String[] densityFunctionQueries = lv.stream().skip(locator.offset).map(Value::getString).toArray(String[]::new);
            if (densityFunctionQueries.length == 0)
            {
                return ListValue.wrap(cc.registry(Registries.DENSITY_FUNCTION).keySet().stream().map(ValueConversions::of));
            }
            NoiseRouter router = level.getChunkSource().randomState().router();
            return densityFunctionQueries.length == 1
                    ? NumericValue.of(sampleNoise(router, level, densityFunctionQueries[0], pos))
                    : ListValue.wrap(Arrays.stream(densityFunctionQueries).map(s -> NumericValue.of(sampleNoise(router, level, s, pos))));
        });
    }

    public static double sampleNoise(NoiseRouter router, ServerLevel level, String what, BlockPos pos)
    {
        DensityFunction densityFunction = switch (what)
        {
            case "barrier_noise" -> router.barrierNoise();
            case "fluid_level_floodedness_noise" -> router.fluidLevelFloodednessNoise();
            case "fluid_level_spread_noise" -> router.fluidLevelSpreadNoise();
            case "lava_noise" -> router.lavaNoise();
            case "temperature" -> router.temperature();
            case "vegetation" -> router.vegetation();
            case "continents" -> router.continents();
            case "erosion" -> router.erosion();
            case "depth" -> router.depth();
            case "ridges" -> router.ridges();
            case "initial_density_without_jaggedness" -> router.initialDensityWithoutJaggedness();
            case "final_density" -> router.finalDensity();
            case "vein_toggle" -> router.veinToggle();
            case "vein_ridged" -> router.veinRidged();
            case "vein_gap" -> router.veinGap();
            default -> stupidWorldgenNoiseCacheGetter.apply(Pair.of(level, what));
        };
        return densityFunction.compute(new DensityFunction.SinglePointContext(pos.getX(), pos.getY(), pos.getZ()));
    }

    // to be used with future seedable noise
    public static final Function<Pair<ServerLevel, String>, DensityFunction> stupidWorldgenNoiseCacheGetter = Util.memoize(pair -> {
        ServerLevel level = pair.getKey();
        String densityFunctionQuery = pair.getValue();
        ChunkGenerator generator = level.getChunkSource().getGenerator();

        if (generator instanceof final NoiseBasedChunkGenerator noiseBasedChunkGenerator)
        {
            Registry<DensityFunction> densityFunctionRegistry = level.registryAccess().lookupOrThrow(Registries.DENSITY_FUNCTION);
            NoiseRouter router = noiseBasedChunkGenerator.generatorSettings().value().noiseRouter();
            DensityFunction densityFunction = switch (densityFunctionQuery)
                    {
                        case "barrier_noise" -> router.barrierNoise();
                        case "fluid_level_floodedness_noise" -> router.fluidLevelFloodednessNoise();
                        case "fluid_level_spread_noise" -> router.fluidLevelSpreadNoise();
                        case "lava_noise" -> router.lavaNoise();
                        case "temperature" -> router.temperature();
                        case "vegetation" -> router.vegetation();
                        case "continents" -> router.continents();
                        case "erosion" -> router.erosion();
                        case "depth" -> router.depth();
                        case "ridges" -> router.ridges();
                        case "initial_density_without_jaggedness" -> router.initialDensityWithoutJaggedness();
                        case "final_density" -> router.finalDensity();
                        case "vein_toggle" -> router.veinToggle();
                        case "vein_ridged" -> router.veinRidged();
                        case "vein_gap" -> router.veinGap();
                        default -> {
                            DensityFunction result = densityFunctionRegistry.getValue(InputValidator.identifierOf(densityFunctionQuery));
                            if (result == null)
                            {
                                throw new InternalExpressionException("Density function '" + densityFunctionQuery + "' is not defined in the registies.");
                            }
                            yield result;
                        }
                    };

            RandomState randomState = RandomState.create(
                    noiseBasedChunkGenerator.generatorSettings().value(),
                    level.registryAccess().lookupOrThrow(Registries.NOISE), level.getSeed()
            );
            DensityFunction.Visitor visitor = Vanilla.RandomState_getVisitor(randomState);

            return densityFunction.mapAll(visitor);
        }
        return DensityFunctions.zero();
    });
}
