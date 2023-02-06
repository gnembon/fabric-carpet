package carpet.script.api;

import carpet.script.CarpetContext;
import carpet.script.CarpetScriptServer;
import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Fluff;
import carpet.script.external.Carpet;
import carpet.script.external.Vanilla;
import carpet.script.utils.FeatureGenerator;
import carpet.mixins.PoiRecord_scarpetMixin;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
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
import net.minecraft.world.level.chunk.ChunkStatus;
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
    private static final Map<String, Direction> DIRECTION_MAP = Arrays.stream(Direction.values()).collect(Collectors.toMap(Direction::getName, (direction) -> direction));

    static
    {
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
            final Context c,
            final String name,
            final List<Value> params,
            final BiPredicate<BlockState, BlockPos> test
    )
    {
        final CarpetContext cc = (CarpetContext) c;
        if (params.size() == 0)
        {
            throw new InternalExpressionException("'" + name + "' requires at least one parameter");
        }
        if (params.get(0) instanceof final BlockValue bv)
        {
            return BooleanValue.of(test.test(bv.getBlockState(), bv.getPos()));
        }
        final BlockValue block = BlockArgument.findIn(cc, params, 0).block;
        return BooleanValue.of(test.test(block.getBlockState(), block.getPos()));
    }

    private static Value stateStringQuery(
            final Context c,
            final String name,
            final List<Value> params,
            final BiFunction<BlockState, BlockPos, String> test
    )
    {
        final CarpetContext cc = (CarpetContext) c;
        if (params.size() == 0)
        {
            throw new InternalExpressionException("'" + name + "' requires at least one parameter");
        }
        if (params.get(0) instanceof final BlockValue bv)
        {
            return StringValue.of(test.apply(bv.getBlockState(), bv.getPos()));
        }
        final BlockValue block = BlockArgument.findIn(cc, params, 0).block;
        return StringValue.of(test.apply(block.getBlockState(), block.getPos()));
    }

    private static Value genericStateTest(
            final Context c,
            final String name,
            final List<Value> params,
            final Fluff.TriFunction<BlockState, BlockPos, Level, Value> test
    )
    {
        final CarpetContext cc = (CarpetContext) c;
        if (params.size() == 0)
        {
            throw new InternalExpressionException("'" + name + "' requires at least one parameter");
        }
        if (params.get(0) instanceof final BlockValue bv)
        {
            try
            {
                return test.apply(bv.getBlockState(), bv.getPos(), cc.level());
            }
            catch (final NullPointerException ignored)
            {
                throw new InternalExpressionException("'" + name + "' function requires a block that is positioned in the world");
            }
        }
        final BlockValue block = BlockArgument.findIn(cc, params, 0).block;
        return test.apply(block.getBlockState(), block.getPos(), cc.level());
    }

    private static <T extends Comparable<T>> BlockState setProperty(final Property<T> property, final String name, final String value,
                                                                    final BlockState bs)
    {
        final Optional<T> optional = property.getValue(value);
        if (optional.isEmpty())
        {
            throw new InternalExpressionException(value + " is not a valid value for property " + name);
        }
        return bs.setValue(property, optional.get());
    }

    private static void nullCheck(final Value v, final String name)
    {
        if (v.isNull())
        {
            throw new IllegalArgumentException(name + " cannot be null");
        }
    }

    private static float numberGetOrThrow(final Value v)
    {
        final double num = v.readDoubleNumber();
        if (Double.isNaN(num))
        {
            throw new IllegalArgumentException(v.getString() + " needs to be a numeric value");
        }
        return (float) num;
    }

    private static void BooYah(final ServerLevel level)
    {
        synchronized (level)
        {
            level.getChunkSource().getGeneratorState().ensureStructuresGenerated();
        }
    }

    public static void apply(final Expression expression)
    {
        expression.addContextFunction("block", -1, (c, t, lv) ->
        {
            final CarpetContext cc = (CarpetContext) c;
            if (lv.size() == 0)
            {
                throw new InternalExpressionException("Block requires at least one parameter");
            }
            final BlockValue retval = BlockArgument.findIn(cc, lv, 0, true).block;
            // fixing block state and data
            retval.getBlockState();
            retval.getData();
            return retval;
        });

        expression.addContextFunction("block_data", -1, (c, t, lv) ->
        {
            if (lv.size() == 0)
            {
                throw new InternalExpressionException("Block requires at least one parameter");
            }
            return NBTSerializableValue.of(BlockArgument.findIn((CarpetContext) c, lv, 0, true).block.getData());
        });

        // poi_get(pos, radius?, type?, occupation?, column_mode?)
        expression.addContextFunction("poi", -1, (c, t, lv) ->
        {
            final CarpetContext cc = (CarpetContext) c;
            if (lv.size() == 0)
            {
                throw new InternalExpressionException("'poi' requires at least one parameter");
            }
            final BlockArgument locator = BlockArgument.findIn(cc, lv, 0, false);
            final BlockPos pos = locator.block.getPos();
            final PoiManager store = cc.level().getPoiManager();
            final Registry<PoiType> poiReg = cc.registry(Registries.POINT_OF_INTEREST_TYPE);
            if (lv.size() == locator.offset)
            {
                final Optional<Holder<PoiType>> foo = store.getType(pos);
                if (foo.isEmpty())
                {
                    return Value.NULL;
                }
                final PoiType poiType = foo.get().value();

                // this feels wrong, but I don't want to mix-in more than I really need to.
                // also distance adds 0.5 to each point which screws up accurate distance calculations
                // you shoudn't be using POI with that in mind anyways, so I am not worried about it.
                final PoiRecord poi = store.getInRange(
                        type -> type.value() == poiType,
                        pos,
                        1,
                        PoiManager.Occupancy.ANY
                ).filter(p -> p.getPos().equals(pos)).findFirst().orElse(null);
                return poi == null ? Value.NULL : ListValue.of(
                        ValueConversions.of(poiReg.getKey(poi.getPoiType().value())),
                        new NumericValue(poiType.maxTickets() - ((PoiRecord_scarpetMixin) poi).getFreeTickets())
                );
            }
            final int radius = NumericValue.asNumber(lv.get(locator.offset + 0)).getInt();
            if (radius < 0)
            {
                return ListValue.of();
            }
            Predicate<Holder<PoiType>> condition = p -> true;
            PoiManager.Occupancy status = PoiManager.Occupancy.ANY;
            boolean inColumn = false;
            if (locator.offset + 1 < lv.size())
            {
                final String poiType = lv.get(locator.offset + 1).getString().toLowerCase(Locale.ROOT);
                if (!"any".equals(poiType))
                {
                    final PoiType type = poiReg.getOptional(InputValidator.identifierOf(poiType))
                            .orElseThrow(() -> new ThrowStatement(poiType, Throwables.UNKNOWN_POI));
                    condition = (tt) -> tt.value() == type;
                }
                if (locator.offset + 2 < lv.size())
                {
                    final String statusString = lv.get(locator.offset + 2).getString().toLowerCase(Locale.ROOT);
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
            final Stream<PoiRecord> pois = inColumn ?
                    store.getInSquare(condition, pos, radius, status) :
                    store.getInRange(condition, pos, radius, status);
            return ListValue.wrap(pois.sorted(Comparator.comparingDouble(p -> p.getPos().distSqr(pos))).map(p ->
                    ListValue.of(
                            ValueConversions.of(poiReg.getKey(p.getPoiType().value())),
                            new NumericValue(p.getPoiType().value().maxTickets() - ((PoiRecord_scarpetMixin) p).getFreeTickets()),
                            ValueConversions.of(p.getPos())
                    )
            ));
        });

        //poi_set(pos, null) poi_set(pos, type, occupied?,
        expression.addContextFunction("set_poi", -1, (c, t, lv) ->
        {
            final CarpetContext cc = (CarpetContext) c;
            if (lv.size() == 0)
            {
                throw new InternalExpressionException("'set_poi' requires at least one parameter");
            }
            final BlockArgument locator = BlockArgument.findIn(cc, lv, 0, false);
            final BlockPos pos = locator.block.getPos();
            if (lv.size() < locator.offset)
            {
                throw new InternalExpressionException("'set_poi' requires the new poi type or null, after position argument");
            }
            final Value poi = lv.get(locator.offset + 0);
            final PoiManager store = cc.level().getPoiManager();
            if (poi.isNull())
            {   // clear poi information
                if (store.getType(pos).isEmpty())
                {
                    return Value.FALSE;
                }
                store.remove(pos);
                return Value.TRUE;
            }
            final String poiTypeString = poi.getString().toLowerCase(Locale.ROOT);
            final ResourceLocation resource = InputValidator.identifierOf(poiTypeString);
            final Registry<PoiType> poiReg = cc.registry(Registries.POINT_OF_INTEREST_TYPE);
            final PoiType type = poiReg.getOptional(resource)
                    .orElseThrow(() -> new ThrowStatement(poiTypeString, Throwables.UNKNOWN_POI));
            final Holder<PoiType> holder = poiReg.getHolderOrThrow(ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, resource));

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
                final int finalO = occupancy;
                store.getInSquare((tt) -> tt.value() == type, pos, 1, PoiManager.Occupancy.ANY
                ).filter(p -> p.getPos().equals(pos)).findFirst().ifPresent(p -> {
                    for (int i = 0; i < finalO; i++)
                    {
                        ((PoiRecord_scarpetMixin) p).callAcquireTicket();
                    }
                });
            }
            return Value.TRUE;
        });


        expression.addContextFunction("weather", -1, (c, t, lv) -> {
            final ServerLevel world = ((CarpetContext) c).level();

            if (lv.size() == 0)//cos it can thunder when raining or when clear.
            {
                return new StringValue(world.isThundering() ? "thunder" : (world.isRaining() ? "rain" : "clear"));
            }

            final Value weather = lv.get(0);
            final ServerLevelData worldProperties = Vanilla.ServerLevel_getWorldProperties(world);
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
                final int ticks = NumericValue.asNumber(lv.get(1), "tick_time in 'weather'").getInt();
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
                final BlockPos pos = bv.getPos();
                if (pos == null)
                {
                    throw new InternalExpressionException("Cannot fetch position of an unrealized block");
                }
                return ValueConversions.of(pos);
            }
            if (v instanceof final EntityValue ev)
            {
                final Entity e = ev.getEntity();
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
            final BlockArgument locator = BlockArgument.findIn((CarpetContext) c, lv, 0);
            final BlockPos pos = locator.block.getPos();
            if (lv.size() <= locator.offset)
            {
                throw new InternalExpressionException("'pos_offset' needs at least position, and direction");
            }
            final String directionString = lv.get(locator.offset).getString();
            final Direction dir = DIRECTION_MAP.get(directionString);
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
            final BlockPos pos = BlockArgument.findIn((CarpetContext) c, lv, 0).block.getPos();
            final ChunkPos chunkPos = new ChunkPos(pos);
            return BooleanValue.of(WorldgenRandom.seedSlimeChunk(
                    chunkPos.x, chunkPos.z,
                    ((CarpetContext) c).level().getSeed(),
                    987234911L
            ).nextInt(10) == 0);
        });

        expression.addContextFunction("top", -1, (c, t, lv) ->
        {
            final String type = lv.get(0).getString().toLowerCase(Locale.ROOT);
            final Heightmap.Types htype = switch (type)
            {
                //case "light": htype = Heightmap.Type.LIGHT_BLOCKING; break;  //investigate
                case "motion" -> Heightmap.Types.MOTION_BLOCKING;
                case "terrain" -> Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
                case "ocean_floor" -> Heightmap.Types.OCEAN_FLOOR;
                case "surface" -> Heightmap.Types.WORLD_SURFACE;
                default -> throw new InternalExpressionException("Unknown heightmap type: " + type);
            };
            final BlockArgument locator = BlockArgument.findIn((CarpetContext) c, lv, 1);
            final BlockPos pos = locator.block.getPos();
            final int x = pos.getX();
            final int z = pos.getZ();
            return new NumericValue(((CarpetContext) c).level().getChunk(x >> 4, z >> 4).getHeight(htype, x & 15, z & 15) + 1);
        });

        expression.addContextFunction("loaded", -1, (c, t, lv) ->
                BooleanValue.of((((CarpetContext) c).level().hasChunkAt(BlockArgument.findIn((CarpetContext) c, lv, 0).block.getPos()))));

        // Deprecated, use loaded_status as more indicative
        expression.addContextFunction("loaded_ep", -1, (c, t, lv) ->
        {
            c.host.issueDeprecation("loaded_ep(...)");
            final BlockPos pos = BlockArgument.findIn((CarpetContext) c, lv, 0).block.getPos();
            return BooleanValue.of(((CarpetContext) c).level().isPositionEntityTicking(pos));
        });

        expression.addContextFunction("loaded_status", -1, (c, t, lv) ->
        {
            final BlockPos pos = BlockArgument.findIn((CarpetContext) c, lv, 0).block.getPos();
            final LevelChunk chunk = ((CarpetContext) c).level().getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, false);
            return chunk == null ? Value.ZERO : new NumericValue(chunk.getFullStatus().ordinal());
        });

        expression.addContextFunction("is_chunk_generated", -1, (c, t, lv) ->
        {
            final BlockArgument locator = BlockArgument.findIn((CarpetContext) c, lv, 0);
            final BlockPos pos = locator.block.getPos();
            boolean force = false;
            if (lv.size() > locator.offset)
            {
                force = lv.get(locator.offset).getBoolean();
            }
            return BooleanValue.of(canHasChunk(((CarpetContext) c).level(), new ChunkPos(pos), null, force));
        });

        expression.addContextFunction("generation_status", -1, (c, t, lv) ->
        {
            final BlockArgument blockArgument = BlockArgument.findIn((CarpetContext) c, lv, 0);
            final BlockPos pos = blockArgument.block.getPos();
            boolean forceLoad = false;
            if (lv.size() > blockArgument.offset)
            {
                forceLoad = lv.get(blockArgument.offset).getBoolean();
            }
            final ChunkAccess chunk = ((CarpetContext) c).level().getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.EMPTY, forceLoad);
            return chunk == null ? Value.NULL : new StringValue(chunk.getStatus().getName());
        });

        expression.addContextFunction("chunk_tickets", -1, (c, t, lv) ->
        {
            final ServerLevel world = ((CarpetContext) c).level();
            DistanceManager foo = Vanilla.ServerChunkCache_getCMTicketManager(world.getChunkSource());
            final Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> levelTickets = Vanilla.ChunkTicketManager_getTicketsByPosition(foo);

            final List<Value> res = new ArrayList<>();
            if (lv.size() == 0)
            {
                for (final long key : levelTickets.keySet())
                {
                    final ChunkPos chpos = new ChunkPos(key);
                    for (final Ticket<?> ticket : levelTickets.get(key))
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
                final BlockArgument blockArgument = BlockArgument.findIn((CarpetContext) c, lv, 0);
                final BlockPos pos = blockArgument.block.getPos();
                final SortedArraySet<Ticket<?>> tickets = levelTickets.get(new ChunkPos(pos).toLong());
                if (tickets != null)
                {
                    for (final Ticket<?> ticket : tickets)
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
                    ((CarpetContext) c).level().neighborChanged(p, s.getBlock(), p);
                    return true;
                }));

        expression.addContextFunction("block_tick", -1, (c, t, lv) ->
                booleanStateTest(c, "block_tick", lv, (s, p) ->
                {
                    final ServerLevel w = ((CarpetContext) c).level();
                    s.randomTick(w, p, w.random);
                    return true;
                }));

        expression.addContextFunction("random_tick", -1, (c, t, lv) ->
                booleanStateTest(c, "random_tick", lv, (s, p) ->
                {
                    final ServerLevel w = ((CarpetContext) c).level();
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
            final Value[] result = new Value[]{Value.NULL};
            ((CarpetContext) c).server().executeBlocking(() ->
            {
                final ThreadLocal<Boolean> skipUpdates = Carpet.getImpendingFillSkipUpdates();
                final boolean previous = skipUpdates.get();
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
            final CarpetContext cc = (CarpetContext) c;
            final ServerLevel world = cc.level();
            final BlockArgument targetLocator = BlockArgument.findIn(cc, lv, 0);
            final BlockArgument sourceLocator = BlockArgument.findIn(cc, lv, targetLocator.offset, true);
            BlockState sourceBlockState = sourceLocator.block.getBlockState();
            final BlockState targetBlockState = world.getBlockState(targetLocator.block.getPos());
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
                    final Map<Value, Value> state = map.getMap();
                    final List<Value> mapargs = new ArrayList<>();
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
                final StateDefinition<Block, BlockState> states = sourceBlockState.getBlock().getStateDefinition();
                for (int i = 0; i < args.size() - 1; i += 2)
                {
                    final String paramString = args.get(i).getString();
                    final Property<?> property = states.getProperty(paramString);
                    if (property == null)
                    {
                        throw new InternalExpressionException("Property " + paramString + " doesn't apply to " + sourceLocator.block.getString());
                    }
                    final String paramValue = args.get(i + 1).getString();
                    sourceBlockState = setProperty(property, paramString, paramValue, sourceBlockState);
                }
            }

            if (data == null)
            {
                data = sourceLocator.block.getData();
            }
            final CompoundTag finalData = data;

            if (sourceBlockState == targetBlockState && data == null)
            {
                return Value.FALSE;
            }
            final BlockState finalSourceBlockState = sourceBlockState;
            final BlockPos targetPos = targetLocator.block.getPos();
            final Boolean[] result = new Boolean[]{true};
            cc.server().executeBlocking(() ->
            {
                Clearable.tryClear(world.getBlockEntity(targetPos));
                boolean success = world.setBlock(targetPos, finalSourceBlockState, 2);
                if (finalData != null)
                {
                    final BlockEntity be = world.getBlockEntity(targetPos);
                    if (be != null)
                    {
                        final CompoundTag destTag = finalData.copy();
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
            return !result[0] ? Value.FALSE : new BlockValue(finalSourceBlockState, world, targetLocator.block.getPos());
        });

        expression.addContextFunction("destroy", -1, (c, t, lv) ->
        {
            final CarpetContext cc = (CarpetContext) c;
            final ServerLevel world = cc.level();
            final BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            final BlockState state = locator.block.getBlockState();
            if (state.isAir())
            {
                return Value.FALSE;
            }
            final BlockPos where = locator.block.getPos();
            final BlockEntity be = world.getBlockEntity(where);
            long how = 0;
            Item item = Items.DIAMOND_PICKAXE;
            boolean playerBreak = false;
            if (lv.size() > locator.offset)
            {
                final Value val = lv.get(locator.offset);
                if (val instanceof final NumericValue number)
                {
                    how = number.getLong();
                }
                else
                {
                    playerBreak = true;
                    final String itemString = val.getString();
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
                final Value tagValue = lv.get(locator.offset + 1);
                if (tagValue.isNull())
                {
                    tag = null;
                }
                else if (tagValue instanceof final NBTSerializableValue nbtsv)
                {
                    tag = nbtsv.getCompoundTag();
                }
                else
                {
                    tag = NBTSerializableValue.parseString(tagValue.getString(), true).getCompoundTag();
                }
            }

            final ItemStack tool = new ItemStack(item, 1);
            if (tag != null)
            {
                tool.setTag(tag);
            }
            if (playerBreak && state.getDestroySpeed(world, where) < 0.0)
            {
                return Value.FALSE;
            }
            final boolean removed = world.removeBlock(where, false);
            if (!removed)
            {
                return Value.FALSE;
            }
            world.levelEvent(null, 2001, where, Block.getId(state));

            boolean toolBroke = false;
            boolean dropLoot = true;
            if (playerBreak)
            {
                final boolean isUsingEffectiveTool = !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state);
                //postMine() durability from item classes
                final float hardness = state.getDestroySpeed(world, where);
                int damageAmount = 0;
                if ((item instanceof DiggerItem && hardness > 0.0) || item instanceof ShearsItem)
                {
                    damageAmount = 1;
                }
                else if (item instanceof TridentItem || item instanceof SwordItem)
                {
                    damageAmount = 2;
                }
                toolBroke = damageAmount > 0 && tool.hurt(damageAmount, world.getRandom(), null);
                if (!isUsingEffectiveTool)
                {
                    dropLoot = false;
                }
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
                    {
                        tool.enchant(Enchantments.BLOCK_FORTUNE, (int) how);
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
            if (toolBroke)
            {
                return Value.NULL;
            }
            final Tag outtag = tool.getTag();
            return outtag == null ? Value.TRUE : new NBTSerializableValue(() -> outtag);

        });

        expression.addContextFunction("harvest", -1, (c, t, lv) ->
        {
            if (lv.size() < 2)
            {
                throw new InternalExpressionException("'harvest' takes at least 2 parameters: entity and block, or position, to harvest");
            }
            final CarpetContext cc = (CarpetContext) c;
            final Level world = cc.level();
            final Value entityValue = lv.get(0);
            if (!(entityValue instanceof final EntityValue ev))
            {
                return Value.FALSE;
            }
            final Entity e = ev.getEntity();
            if (!(e instanceof final ServerPlayer player))
            {
                return Value.FALSE;
            }
            final BlockArgument locator = BlockArgument.findIn(cc, lv, 1);
            final BlockPos where = locator.block.getPos();
            final BlockState state = locator.block.getBlockState();
            final Block block = state.getBlock();
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
            final CarpetContext cc = (CarpetContext) c;
            float powah = 4.0f;
            Explosion.BlockInteraction mode = Explosion.BlockInteraction.DESTROY; // should probably read the gamerule for default behaviour
            boolean createFire = false;
            Entity source = null;
            LivingEntity attacker = null;
            final Vector3Argument location = Vector3Argument.findIn(lv, 0, false, true);
            final Vec3 pos = location.vec;
            if (lv.size() > location.offset)
            {
                powah = NumericValue.asNumber(lv.get(location.offset), "explosion power").getFloat();
                if (powah < 0)
                {
                    throw new InternalExpressionException("Explosion power cannot be negative");
                }
                if (lv.size() > location.offset + 1)
                {
                    final String strval = lv.get(location.offset + 1).getString();
                    try
                    {
                        mode = Explosion.BlockInteraction.valueOf(strval.toUpperCase(Locale.ROOT));
                    }
                    catch (final IllegalArgumentException ile)
                    {
                        throw new InternalExpressionException("Illegal explosions block behaviour: " + strval);
                    }
                    if (lv.size() > location.offset + 2)
                    {
                        createFire = lv.get(location.offset + 2).getBoolean();
                        if (lv.size() > location.offset + 3)
                        {
                            Value enVal = lv.get(location.offset + 3);
                            if (enVal.isNull())
                            {
                            } // is null already
                            else if (enVal instanceof final EntityValue ev)
                            {
                                source = ev.getEntity();
                            }
                            else
                            {
                                throw new InternalExpressionException("Fourth parameter of the explosion has to be an entity, not " + enVal.getTypeString());
                            }
                            if (lv.size() > location.offset + 4)
                            {
                                enVal = lv.get(location.offset + 4);
                                if (enVal.isNull())
                                {
                                } // is null already
                                else if (enVal instanceof final EntityValue ev)
                                {
                                    final Entity attackingEntity = ev.getEntity();
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
            final LivingEntity theAttacker = attacker;
            final float thePowah = powah;

            // copy of ServerWorld.createExplosion #TRACK#
            final Explosion explosion = new Explosion(cc.level(), source, null, null, pos.x, pos.y, pos.z, powah, createFire, mode)
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
            explosion.finalizeExplosion(false);
            if (mode == Explosion.BlockInteraction.KEEP)
            {
                explosion.clearToBlow();
            }
            cc.level().players().forEach(spe -> {
                if (spe.distanceToSqr(pos) < 4096.0D)
                {
                    spe.connection.send(new ClientboundExplodePacket(pos.x, pos.y, pos.z, thePowah, explosion.getToBlow(), explosion.getHitPlayers().get(spe)));
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
            final CarpetContext cc = (CarpetContext) c;
            final String itemString = lv.get(0).getString();
            final Vector3Argument locator = Vector3Argument.findIn(lv, 1);
            final ItemInput stackArg = NBTSerializableValue.parseItem(itemString, cc.registryAccess());
            final BlockPos where = new BlockPos(locator.vec);
            // Paintings throw an exception if their direction is vertical, therefore we change the default here
            final String facing = lv.size() > locator.offset
                    ? lv.get(locator.offset).getString()
                    : stackArg.getItem() != Items.PAINTING ? "up" : "north";
            boolean sneakPlace = false;
            if (lv.size() > locator.offset + 1)
            {
                sneakPlace = lv.get(locator.offset + 1).getBoolean();
            }

            final BlockValue.PlacementContext ctx;
            try
            {
                ctx = BlockValue.PlacementContext.from(cc.level(), where, facing, sneakPlace, stackArg.createItemStack(1, false));
            }
            catch (final CommandSyntaxException e)
            {
                throw new InternalExpressionException(e.getMessage());
            }

            if (!(stackArg.getItem() instanceof final BlockItem blockItem))
            {
                final InteractionResult useResult = ctx.getItemInHand().useOn(ctx);
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
                final BlockState placementState = blockItem.getBlock().getStateForPlacement(ctx);
                if (placementState != null)
                {
                    final Level level = ctx.getLevel();
                    if (placementState.canSurvive(level, where))
                    {
                        level.setBlock(where, placementState, 2);
                        final SoundType blockSoundGroup = placementState.getSoundType();
                        level.playSound(null, where, blockSoundGroup.getPlaceSound(), SoundSource.BLOCKS, (blockSoundGroup.getVolume() + 1.0F) / 2.0F, blockSoundGroup.getPitch() * 0.8F);
                        return Value.TRUE;
                    }
                }
            }
            return Value.FALSE;
        });

        expression.addContextFunction("blocks_movement", -1, (c, t, lv) ->
                booleanStateTest(c, "blocks_movement", lv, (s, p) ->
                        !s.isPathfindable(((CarpetContext) c).level(), p, PathComputationType.LAND)));

        expression.addContextFunction("block_sound", -1, (c, t, lv) ->
                stateStringQuery(c, "block_sound", lv, (s, p) ->
                        Carpet.getSoundTypeNames().get(s.getSoundType())));

        expression.addContextFunction("material", -1, (c, t, lv) ->
                stateStringQuery(c, "material", lv, (s, p) ->
                        Carpet.getMaterialNames().get(s.getMaterial())));

        expression.addContextFunction("map_colour", -1, (c, t, lv) ->
                stateStringQuery(c, "map_colour", lv, (s, p) ->
                        Carpet.getMapColorNames().get(s.getMapColor(((CarpetContext) c).level(), p))));


        // Deprecated for block_state()
        expression.addContextFunction("property", -1, (c, t, lv) ->
        {
            c.host.issueDeprecation("property(...)");
            final BlockArgument locator = BlockArgument.findIn((CarpetContext) c, lv, 0);
            final BlockState state = locator.block.getBlockState();
            if (lv.size() <= locator.offset)
            {
                throw new InternalExpressionException("'property' requires to specify a property to query");
            }
            final String tag = lv.get(locator.offset).getString();
            final StateDefinition<Block, BlockState> states = state.getBlock().getStateDefinition();
            final Property<?> property = states.getProperty(tag);
            return property == null ? Value.NULL : new StringValue(state.getValue(property).toString().toLowerCase(Locale.ROOT));
        });

        // Deprecated for block_state()
        expression.addContextFunction("block_properties", -1, (c, t, lv) ->
        {
            c.host.issueDeprecation("block_properties(...)");
            final BlockArgument locator = BlockArgument.findIn((CarpetContext) c, lv, 0);
            final BlockState state = locator.block.getBlockState();
            final StateDefinition<Block, BlockState> states = state.getBlock().getStateDefinition();
            return ListValue.wrap(states.getProperties().stream().map(
                    p -> new StringValue(p.getName()))
            );
        });

        // block_state(block)
        // block_state(block, property)
        expression.addContextFunction("block_state", -1, (c, t, lv) ->
        {
            final BlockArgument locator = BlockArgument.findIn((CarpetContext) c, lv, 0, true);
            final BlockState state = locator.block.getBlockState();
            final StateDefinition<Block, BlockState> states = state.getBlock().getStateDefinition();
            if (locator.offset == lv.size())
            {
                final Map<Value, Value> properties = new HashMap<>();
                for (final Property<?> p : states.getProperties())
                {
                    properties.put(StringValue.of(p.getName()), ValueConversions.fromProperty(state, p));
                }
                return MapValue.wrap(properties);
            }
            final String tag = lv.get(locator.offset).getString();
            final Property<?> property = states.getProperty(tag);
            return property == null ? Value.NULL : ValueConversions.fromProperty(state, property);
        });

        expression.addContextFunction("block_list", -1, (c, t, lv) ->
        {
            final CarpetContext cc = (CarpetContext) c;
            final Registry<Block> blocks = cc.registry(Registries.BLOCK);
            if (lv.size() == 0)
            {
                return ListValue.wrap(blocks.keySet().stream().map(ValueConversions::of));
            }
            final ResourceLocation tag = InputValidator.identifierOf(lv.get(0).getString());
            final Optional<HolderSet.Named<Block>> tagset = blocks.getTag(TagKey.create(Registries.BLOCK, tag));
            return tagset.isEmpty() ? Value.NULL : ListValue.wrap(tagset.get().stream().map(b -> ValueConversions.of(blocks.getKey(b.value()))));
        });

        expression.addContextFunction("block_tags", -1, (c, t, lv) ->
        {
            final CarpetContext cc = (CarpetContext) c;
            final Registry<Block> blocks = cc.registry(Registries.BLOCK);
            if (lv.size() == 0)
            {
                return ListValue.wrap(blocks.getTagNames().map(ValueConversions::of));
            }
            final BlockArgument blockLocator = BlockArgument.findIn(cc, lv, 0, true);
            if (blockLocator.offset == lv.size())
            {
                final Block target = blockLocator.block.getBlockState().getBlock();
                return ListValue.wrap(blocks.getTags().filter(e -> e.getSecond().stream().anyMatch(h -> (h.value() == target))).map(e -> ValueConversions.of(e.getFirst())));
            }
            final String tag = lv.get(blockLocator.offset).getString();
            final Optional<HolderSet.Named<Block>> tagSet = blocks.getTag(TagKey.create(Registries.BLOCK, InputValidator.identifierOf(tag)));
            return tagSet.isEmpty() ? Value.NULL : BooleanValue.of(blockLocator.block.getBlockState().is(tagSet.get()));
        });

        expression.addContextFunction("biome", -1, (c, t, lv) -> {
            final CarpetContext cc = (CarpetContext) c;
            final ServerLevel world = cc.level();
            if (lv.size() == 0)
            {
                return ListValue.wrap(world.registryAccess().registryOrThrow(Registries.BIOME).keySet().stream().map(ValueConversions::of));
            }

            final Biome biome;
            final BiomeSource biomeSource = world.getChunkSource().getGenerator().getBiomeSource();
            if (lv.size() == 1
                    && lv.get(0) instanceof final MapValue map
                    && biomeSource instanceof final MultiNoiseBiomeSource mnbs
            )
            {
                final Value temperature = map.get(new StringValue("temperature"));
                nullCheck(temperature, "temperature");

                final Value humidity = map.get(new StringValue("humidity"));
                nullCheck(humidity, "humidity");

                final Value continentalness = map.get(new StringValue("continentalness"));
                nullCheck(continentalness, "continentalness");

                final Value erosion = map.get(new StringValue("erosion"));
                nullCheck(erosion, "erosion");

                final Value depth = map.get(new StringValue("depth"));
                nullCheck(depth, "depth");

                final Value weirdness = map.get(new StringValue("weirdness"));
                nullCheck(weirdness, "weirdness");

                final Climate.TargetPoint point = new Climate.TargetPoint(
                        Climate.quantizeCoord(numberGetOrThrow(temperature)),
                        Climate.quantizeCoord(numberGetOrThrow(humidity)),
                        Climate.quantizeCoord(numberGetOrThrow(continentalness)),
                        Climate.quantizeCoord(numberGetOrThrow(erosion)),
                        Climate.quantizeCoord(numberGetOrThrow(depth)),
                        Climate.quantizeCoord(numberGetOrThrow(weirdness))
                );
                biome = mnbs.getNoiseBiome(point).value();
                final ResourceLocation biomeId = cc.registry(Registries.BIOME).getKey(biome);
                return new StringValue(NBTSerializableValue.nameFromRegistryId(biomeId));
            }

            final BlockArgument locator = BlockArgument.findIn(cc, lv, 0, false, false, true);

            if (locator.replacement != null)
            {
                biome = world.registryAccess().registryOrThrow(Registries.BIOME).get(InputValidator.identifierOf(locator.replacement));
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
                final ResourceLocation biomeId = cc.registry(Registries.BIOME).getKey(biome);
                return new StringValue(NBTSerializableValue.nameFromRegistryId(biomeId));
            }
            final String biomeFeature = lv.get(locator.offset).getString();
            final BiFunction<ServerLevel, Biome, Value> featureProvider = BiomeInfo.biomeFeatures.get(biomeFeature);
            if (featureProvider == null)
            {
                throw new InternalExpressionException("Unknown biome feature: " + biomeFeature);
            }
            return featureProvider.apply(world, biome);
        });

        expression.addContextFunction("set_biome", -1, (c, t, lv) ->
        {
            final CarpetContext cc = (CarpetContext) c;
            final BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            if (lv.size() == locator.offset)
            {
                throw new InternalExpressionException("'set_biome' needs a biome name as an argument");
            }
            final String biomeName = lv.get(locator.offset + 0).getString();
            // from locatebiome command code
            final Holder<Biome> biome = cc.registry(Registries.BIOME).getHolder(ResourceKey.create(Registries.BIOME, InputValidator.identifierOf(biomeName)))
                    .orElseThrow(() -> new ThrowStatement(biomeName, Throwables.UNKNOWN_BIOME));
            boolean doImmediateUpdate = true;
            if (lv.size() > locator.offset + 1)
            {
                doImmediateUpdate = lv.get(locator.offset + 1).getBoolean();
            }
            final ServerLevel world = cc.level();
            final BlockPos pos = locator.block.getPos();
            final ChunkAccess chunk = world.getChunk(pos); // getting level chunk instead of protochunk with biomes
            final int biomeX = QuartPos.fromBlock(pos.getX());
            final int biomeY = QuartPos.fromBlock(pos.getY());
            final int biomeZ = QuartPos.fromBlock(pos.getZ());
            try
            {
                final int i = QuartPos.fromBlock(chunk.getMinBuildHeight());
                final int j = i + QuartPos.fromBlock(chunk.getHeight()) - 1;
                final int k = Mth.clamp(biomeY, i, j);
                final int l = chunk.getSectionIndex(QuartPos.toBlock(k));
                // accessing outside of the interface - might be dangerous in the future.
                ((PalettedContainer<Holder<Biome>>) chunk.getSection(l).getBiomes()).set(biomeX & 3, k & 3, biomeZ & 3, biome);
            }
            catch (final Throwable var8)
            {
                return Value.FALSE;
            }
            if (doImmediateUpdate)
            {
                WorldTools.forceChunkUpdate(pos, world);
            }
            chunk.setUnsaved(true);
            return Value.TRUE;
        });

        expression.addContextFunction("reload_chunk", -1, (c, t, lv) -> {
            final CarpetContext cc = (CarpetContext) c;
            final BlockPos pos = BlockArgument.findIn(cc, lv, 0).block.getPos();
            final ServerLevel world = cc.level();
            cc.server().executeBlocking(() -> WorldTools.forceChunkUpdate(pos, world));
            return Value.TRUE;
        });

        expression.addContextFunction("structure_references", -1, (c, t, lv) -> {
            final CarpetContext cc = (CarpetContext) c;
            final BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            final ServerLevel world = cc.level();
            final BlockPos pos = locator.block.getPos();
            final Map<Structure, LongSet> references = world.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.STRUCTURE_REFERENCES).getAllReferences();
            final Registry<Structure> reg = cc.registry(Registries.STRUCTURE);
            if (lv.size() == locator.offset)
            {
                return ListValue.wrap(references.entrySet().stream().
                        filter(e -> e.getValue() != null && !e.getValue().isEmpty()).
                        map(e -> new StringValue(NBTSerializableValue.nameFromRegistryId(reg.getKey(e.getKey()))))
                );
            }
            final String simpleStructureName = lv.get(locator.offset).getString().toLowerCase(Locale.ROOT);
            final Structure structureName = reg.get(InputValidator.identifierOf(simpleStructureName));
            if (structureName == null)
            {
                return Value.NULL;
            }
            final LongSet structureReferences = references.get(structureName);
            if (structureReferences == null || structureReferences.isEmpty())
            {
                return ListValue.of();
            }
            return ListValue.wrap(structureReferences.longStream().mapToObj(l -> ListValue.of(
                    new NumericValue(16 * ChunkPos.getX(l)),
                    Value.ZERO,
                    new NumericValue(16 * ChunkPos.getZ(l)))));
        });

        expression.addContextFunction("structure_eligibility", -1, (c, t, lv) ->
        {// TODO rename structureName to class
            final CarpetContext cc = (CarpetContext) c;
            final BlockArgument locator = BlockArgument.findIn(cc, lv, 0);

            final ServerLevel world = cc.level();

            // well, because
            BooYah(world);

            final BlockPos pos = locator.block.getPos();
            final List<Structure> structure = new ArrayList<>();
            boolean needSize = false;
            boolean singleOutput = false;
            final Registry<Structure> reg = cc.registry(Registries.STRUCTURE);
            if (lv.size() > locator.offset)
            {
                final Value requested = lv.get(locator.offset + 0);
                if (!requested.isNull())
                {
                    final String reqString = requested.getString();
                    final ResourceLocation id = InputValidator.identifierOf(reqString);
                    final Structure requestedStructure = reg.get(id);
                    if (requestedStructure != null)
                    {
                        singleOutput = true;
                        structure.add(requestedStructure);
                    }
                    else
                    {
                        final StructureType<?> sss = cc.registry(Registries.STRUCTURE_TYPE).get(id);
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
                final StructureStart start = FeatureGenerator.shouldStructureStartAt(world, pos, structure.get(0), needSize);
                return start == null ? Value.NULL : !needSize ? Value.TRUE : ValueConversions.of(start, cc.registryAccess());
            }
            final Map<Value, Value> ret = new HashMap<>();
            for (final Structure str : structure)
            {
                StructureStart start;
                try
                {
                    start = FeatureGenerator.shouldStructureStartAt(world, pos, str, needSize);
                }
                catch (final NullPointerException npe)
                {
                    CarpetScriptServer.LOG.error("Failed to detect structure: " + reg.getKey(str));
                    start = null;
                }

                if (start == null)
                {
                    continue;
                }

                final Value key = new StringValue(NBTSerializableValue.nameFromRegistryId(reg.getKey(str)));
                ret.put(key, (!needSize) ? Value.NULL : ValueConversions.of(start, cc.registryAccess()));
            }
            return MapValue.wrap(ret);
        });

        expression.addContextFunction("structures", -1, (c, t, lv) -> {
            final CarpetContext cc = (CarpetContext) c;
            final BlockArgument locator = BlockArgument.findIn(cc, lv, 0);

            final ServerLevel world = cc.level();
            final BlockPos pos = locator.block.getPos();
            final Map<Structure, StructureStart> structures = world.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS).getAllStarts();
            final Registry<Structure> reg = cc.registry(Registries.STRUCTURE);
            if (lv.size() == locator.offset)
            {
                final Map<Value, Value> structureList = new HashMap<>();
                for (final Map.Entry<Structure, StructureStart> entry : structures.entrySet())
                {
                    final StructureStart start = entry.getValue();
                    if (start == StructureStart.INVALID_START)
                    {
                        continue;
                    }
                    final BoundingBox box = start.getBoundingBox();
                    structureList.put(
                            new StringValue(NBTSerializableValue.nameFromRegistryId(reg.getKey(entry.getKey()))),
                            ValueConversions.of(box)
                    );
                }
                return MapValue.wrap(structureList);
            }
            final String structureName = lv.get(locator.offset).getString().toLowerCase(Locale.ROOT);
            return ValueConversions.of(structures.get(reg.get(InputValidator.identifierOf(structureName))), cc.registryAccess());
        });

        expression.addContextFunction("set_structure", -1, (c, t, lv) ->
        {
            final CarpetContext cc = (CarpetContext) c;
            final BlockArgument locator = BlockArgument.findIn(cc, lv, 0);

            final ServerLevel world = cc.level();
            final BlockPos pos = locator.block.getPos();

            if (lv.size() == locator.offset)
            {
                throw new InternalExpressionException("'set_structure requires at least position and a structure name");
            }
            final String structureName = lv.get(locator.offset).getString().toLowerCase(Locale.ROOT);
            final Structure configuredStructure = FeatureGenerator.resolveConfiguredStructure(structureName, world, pos);
            if (configuredStructure == null)
            {
                throw new ThrowStatement(structureName, Throwables.UNKNOWN_STRUCTURE);
            }
            // good 'ol pointer
            final Value[] result = new Value[]{Value.NULL};
            // technically a world modification. Even if we could let it slide, we will still park it
            ((CarpetContext) c).server().executeBlocking(() ->
            {
                final Map<Structure, StructureStart> structures = world.getChunk(pos).getAllStarts();
                if (lv.size() == locator.offset + 1)
                {
                    final Boolean res = FeatureGenerator.plopGrid(configuredStructure, ((CarpetContext) c).level(), locator.block.getPos());
                    if (res == null)
                    {
                        return;
                    }
                    result[0] = res ? Value.TRUE : Value.FALSE;
                    return;
                }
                final Value newValue = lv.get(locator.offset + 1);
                if (newValue.isNull()) // remove structure
                {
                    final Structure structure = configuredStructure;
                    if (!structures.containsKey(structure))
                    {
                        return;
                    }
                    final StructureStart start = structures.get(structure);
                    final ChunkPos structureChunkPos = start.getChunkPos();
                    final BoundingBox box = start.getBoundingBox();
                    for (int chx = box.minX() / 16; chx <= box.maxX() / 16; chx++)  // minx maxx
                    {
                        for (int chz = box.minZ() / 16; chz <= box.maxZ() / 16; chz++) //minZ maxZ
                        {
                            final ChunkPos chpos = new ChunkPos(chx, chz);
                            // getting a chunk will convert it to full, allowing to modify references
                            final Map<Structure, LongSet> references =
                                    world.getChunk(chpos.getWorldPosition()).getAllReferences();
                            if (references.containsKey(structure) && references.get(structure) != null)
                            {
                                references.get(structure).remove(structureChunkPos.toLong());
                            }
                        }
                    }
                    structures.remove(structure);
                    result[0] = Value.TRUE;
                }
            });
            return result[0]; // preventing from lazy evaluating of the result in case a future completes later
        });

        // todo maybe enable chunk blending?
        expression.addContextFunction("reset_chunk", -1, (c, t, lv) ->
        {
            final CarpetContext cc = (CarpetContext) c;
            final List<ChunkPos> requestedChunks = new ArrayList<>();
            if (lv.size() == 1)
            {
                //either one block or list of chunks
                final Value first = lv.get(0);
                if (first instanceof final ListValue list)
                {
                    final List<Value> listVal = list.getItems();
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
                    final BlockArgument locator = BlockArgument.findIn(cc, Collections.singletonList(first), 0);
                    requestedChunks.add(new ChunkPos(locator.block.getPos()));
                }
            }
            else
            {
                BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
                final ChunkPos from = new ChunkPos(locator.block.getPos());
                if (lv.size() > locator.offset)
                {
                    locator = BlockArgument.findIn(cc, lv, locator.offset);
                    final ChunkPos to = new ChunkPos(locator.block.getPos());
                    final int xmax = Math.max(from.x, to.x);
                    final int zmax = Math.max(from.z, to.z);
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
            final ServerLevel world = cc.level();
            final Value[] result = new Value[]{Value.NULL};
            ((CarpetContext) c).server().executeBlocking(() ->
            {
                final Map<String, Integer> report = Vanilla.ChunkMap_regenerateChunkRegion(world.getChunkSource().chunkMap, requestedChunks);
                result[0] = MapValue.wrap(report.entrySet().stream().collect(Collectors.toMap(
                        e -> new StringValue(e.getKey()),
                        e -> new NumericValue(e.getValue())
                )));
            });
            return result[0];
        });

        expression.addContextFunction("inhabited_time", -1, (c, t, lv) ->
        {
            final CarpetContext cc = (CarpetContext) c;
            final BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            final BlockPos pos = locator.block.getPos();
            return new NumericValue(cc.level().getChunk(pos).getInhabitedTime());
        });

        expression.addContextFunction("spawn_potential", -1, (c, t, lv) ->
        {
            final CarpetContext cc = (CarpetContext) c;
            final BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            final BlockPos pos = locator.block.getPos();
            double required_charge = 1;
            if (lv.size() > locator.offset)
            {
                required_charge = NumericValue.asNumber(lv.get(locator.offset)).getDouble();
            }
            final NaturalSpawner.SpawnState charger = cc.level().getChunkSource().getLastSpawnState();
            return charger == null ? Value.NULL : new NumericValue(Vanilla.SpawnState_getPotentialCalculator(charger).getPotentialEnergyChange(pos, required_charge)
            );
        });

        expression.addContextFunction("add_chunk_ticket", -1, (c, t, lv) ->
        {
            final CarpetContext cc = (CarpetContext) c;
            final BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            final BlockPos pos = locator.block.getPos();
            if (lv.size() != locator.offset + 2)
            {
                throw new InternalExpressionException("'add_chunk_ticket' requires block position, ticket type and radius");
            }
            final String type = lv.get(locator.offset).getString();
            final TicketType<?> ticket = ticketTypes.get(type.toLowerCase(Locale.ROOT));
            if (ticket == null)
            {
                throw new InternalExpressionException("Unknown ticket type: " + type);
            }
            final int radius = NumericValue.asNumber(lv.get(locator.offset + 1)).getInt();
            if (radius < 1 || radius > 32)
            {
                throw new InternalExpressionException("Ticket radius should be between 1 and 32 chunks");
            }
            // due to types we will wing it:
            final ChunkPos target = new ChunkPos(pos);
            if (ticket == TicketType.PORTAL) // portal
            {
                cc.level().getChunkSource().addRegionTicket(TicketType.PORTAL, target, radius, pos);
            }
            else if (ticket == TicketType.POST_TELEPORT) // post teleport
            {
                cc.level().getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, target, radius, 1);
            }
            else
            {
                cc.level().getChunkSource().addRegionTicket(TicketType.UNKNOWN, target, radius, target);
            }
            return new NumericValue(ticket.timeout());
        });

        expression.addContextFunction("sample_noise", -1, (c, t, lv) ->
        {
            final CarpetContext cc = (CarpetContext) c;
            if (lv.size() == 0)
            {
                return ListValue.wrap(cc.registry(Registries.DENSITY_FUNCTION).keySet().stream().map(ValueConversions::of));
            }
            final ServerLevel level = cc.level();
            final BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            final BlockPos pos = locator.block.getPos();
            final String[] densityFunctionQueries = lv.stream().skip(locator.offset).map(Value::getString).toArray(String[]::new);
            if (densityFunctionQueries.length == 0)
            {
                return ListValue.wrap(cc.registry(Registries.DENSITY_FUNCTION).keySet().stream().map(ValueConversions::of));
            }
            final NoiseRouter router = level.getChunkSource().randomState().router();
            return densityFunctionQueries.length == 1
                    ? NumericValue.of(sampleNoise(router, level, densityFunctionQueries[0], pos))
                    : ListValue.wrap(Arrays.stream(densityFunctionQueries).map(s -> NumericValue.of(sampleNoise(router, level, s, pos))));
        });
    }

    public static double sampleNoise(final NoiseRouter router, final ServerLevel level, final String what, final BlockPos pos)
    {
        final DensityFunction densityFunction = switch (what)
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
    public static Function<Pair<ServerLevel, String>, DensityFunction> stupidWorldgenNoiseCacheGetter = Util.memoize(pair -> {
        ServerLevel level = pair.getKey();
        String densityFunctionQuery = pair.getValue();
        ChunkGenerator generator = level.getChunkSource().getGenerator();

        if (generator instanceof final NoiseBasedChunkGenerator noiseBasedChunkGenerator)
        {
            Registry<DensityFunction> densityFunctionRegistry = level.registryAccess().registryOrThrow(Registries.DENSITY_FUNCTION);
            final NoiseRouter router = noiseBasedChunkGenerator.generatorSettings().value().noiseRouter();
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
                            ResourceLocation densityFunctionKey = InputValidator.identifierOf(densityFunctionQuery);

                            if (densityFunctionRegistry.containsKey(densityFunctionKey))
                            {
                                yield densityFunctionRegistry.get(densityFunctionKey);
                            }

                            throw new InternalExpressionException("Density function '" + densityFunctionQuery + "' is not defined in the registies.");
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
