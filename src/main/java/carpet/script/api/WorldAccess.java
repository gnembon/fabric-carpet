package carpet.script.api;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.fakes.BiomeArrayInterface;
import carpet.fakes.ChunkGeneratorInterface;
import carpet.fakes.ChunkTicketManagerInterface;
import carpet.fakes.ServerChunkManagerInterface;
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
import carpet.script.utils.BiomeInfo;
import carpet.script.utils.WorldTools;
import carpet.script.value.BlockValue;
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
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.structure.StructureStart;
import net.minecraft.tag.TagManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Clearable;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.SortedArraySet;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.ConfiguredStructureFeature;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;

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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static carpet.script.utils.WorldTools.canHasChunk;

public class WorldAccess {
    private static final Map<String, Direction> DIRECTION_MAP = Arrays.stream(Direction.values()).collect(Collectors.toMap(Direction::getName, (direction) -> direction));
    private final static Map<String, ChunkTicketType<?>> ticketTypes = new HashMap<String, ChunkTicketType<?>>(){{
        put("portal", ChunkTicketType.PORTAL);
        put("teleport", ChunkTicketType.POST_TELEPORT);
        put("unknown", ChunkTicketType.UNKNOWN);  // unknown
    }};
    // dummy entity for dummy requirements in the loot tables (see snowball)
    private static FallingBlockEntity DUMMY_ENTITY = new FallingBlockEntity(EntityType.FALLING_BLOCK, null);

    private static LazyValue booleanStateTest(
            Context c,
            String name,
            List<LazyValue> params,
            BiFunction<BlockState, BlockPos, Boolean> test
    )
    {
        CarpetContext cc = (CarpetContext) c;
        if (params.size() == 0)
        {
            throw new InternalExpressionException("'" + name + "' requires at least one parameter");
        }
        Value v0 = params.get(0).evalValue(c);
        if (v0 instanceof BlockValue)
        {
            Value retval = test.apply(((BlockValue) v0).getBlockState(), ((BlockValue) v0).getPos()) ? Value.TRUE : Value.FALSE;
            return (c_, t_) -> retval;
        }
        BlockValue block = BlockArgument.findIn(cc, params, 0).block;
        Value retval = test.apply(block.getBlockState(), block.getPos()) ? Value.TRUE : Value.FALSE;
        return (c_, t_) -> retval;
    }

    private static LazyValue stateStringQuery(
            Context c,
            String name,
            List<LazyValue> params,
            BiFunction<BlockState, BlockPos, String> test
    )
    {
        CarpetContext cc = (CarpetContext) c;
        if (params.size() == 0)
        {
            throw new InternalExpressionException("'" + name + "' requires at least one parameter");
        }

        Value v0 = params.get(0).evalValue(c);
        if (v0 instanceof BlockValue)
        {
            String strVal = test.apply( ((BlockValue) v0).getBlockState(), ((BlockValue) v0).getPos());
            Value retval = strVal != null ? new StringValue(strVal) : Value.NULL;
            return (c_, t_) -> retval;
        }
        BlockValue block = BlockArgument.findIn(cc, params, 0).block;
        String strVal = test.apply(block.getBlockState(), block.getPos());
        Value retval = strVal != null ? new StringValue(strVal) : Value.NULL;
        return (c_, t_) -> retval;
    }

    private static LazyValue genericStateTest(
            Context c,
            String name,
            List<LazyValue> params,
            Fluff.TriFunction<BlockState, BlockPos, World, Value> test
    )
    {
        CarpetContext cc = (CarpetContext) c;
        if (params.size() == 0)
        {
            throw new InternalExpressionException("'" + name + "' requires at least one parameter");
        }
        Value v0 = params.get(0).evalValue(c);
        if (v0 instanceof BlockValue)
        {
            try
            {
                Value retval = test.apply(((BlockValue) v0).getBlockState(), ((BlockValue) v0).getPos(), cc.s.getWorld());
                return (_c, _t) -> retval;
            }
            catch (NullPointerException ignored)
            {
                throw new InternalExpressionException("'" + name + "' function requires a block that is positioned in the world");
            }
        }
        BlockValue block = BlockArgument.findIn(cc, params, 0).block;
        Value retval = test.apply(block.getBlockState(), block.getPos(), cc.s.getWorld());
        return (c_, t_) -> retval;
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

    private static boolean tryBreakBlock_copy_from_ServerPlayerInteractionManager(ServerPlayerEntity player, BlockPos blockPos_1)
    {
        //this could be done little better, by hooking up event handling not in try_break_block but wherever its called
        // so we can safely call it here
        // but that would do for now.
        BlockState blockState_1 = player.world.getBlockState(blockPos_1);
        if (!player.getMainHandStack().getItem().canMine(blockState_1, player.world, blockPos_1, player)) {
            return false;
        } else {
            BlockEntity blockEntity_1 = player.world.getBlockEntity(blockPos_1);
            Block block_1 = blockState_1.getBlock();
            if ((block_1 instanceof CommandBlock || block_1 instanceof StructureBlock || block_1 instanceof JigsawBlock) && !player.isCreativeLevelTwoOp()) {
                player.world.updateListeners(blockPos_1, blockState_1, blockState_1, 3);
                return false;
            } else if (player.isBlockBreakingRestricted(player.world, blockPos_1, player.interactionManager.getGameMode())) {
                return false;
            } else {
                block_1.onBreak(player.world, blockPos_1, blockState_1, player);
                boolean boolean_1 = player.world.removeBlock(blockPos_1, false);
                if (boolean_1) {
                    block_1.onBroken(player.world, blockPos_1, blockState_1);
                }

                if (player.isCreative()) {
                    return true;
                } else {
                    ItemStack itemStack_1 = player.getMainHandStack();
                    boolean boolean_2 = player.isUsingEffectiveTool(blockState_1);
                    itemStack_1.postMine(player.world, blockState_1, blockPos_1, player);
                    if (boolean_1 && boolean_2) {
                        ItemStack itemStack_2 = itemStack_1.isEmpty() ? ItemStack.EMPTY : itemStack_1.copy();
                        block_1.afterBreak(player.world, player, blockPos_1, blockState_1, blockEntity_1, itemStack_2);
                    }

                    return true;
                }
            }
        }
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
        expression.addLazyFunction("block", -1, (c, t, lv) ->
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
            return (c_, t_) -> retval;
        });

        expression.addLazyFunction("block_data", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            if (lv.size() == 0)
            {
                throw new InternalExpressionException("Block requires at least one parameter");
            }
            CompoundTag tag = BlockArgument.findIn(cc, lv, 0, true).block.getData();
            if (tag == null)
                return (c_, t_) -> Value.NULL;
            Value retval = new NBTSerializableValue(tag);
            return (c_, t_) -> retval;
        });

        // poi_get(pos, radius?, type?, occupation?, column_mode?)
        expression.addLazyFunction("poi", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            if (lv.size() == 0) throw new InternalExpressionException("'poi' requires at least one parameter");
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0, false);
            BlockPos pos = locator.block.getPos();
            PointOfInterestStorage store = cc.s.getWorld().getPointOfInterestStorage();
            if (lv.size() == locator.offset)
            {
                PointOfInterestType poiType = store.getType(pos).orElse(null);
                if (poiType == null) return LazyValue.NULL;

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
                    return LazyValue.NULL;
                Value ret = ListValue.of(
                        new StringValue(poi.getType().toString()),
                        new NumericValue(poiType.getTicketCount() - ((PointOfInterest_scarpetMixin)poi).getFreeTickets())
                );
                return (_c, _t) -> ret;
            }
            int radius = NumericValue.asNumber(lv.get(locator.offset+0).evalValue(c)).getInt();
            if (radius < 0) return ListValue.lazyEmpty();
            Predicate<PointOfInterestType> condition = PointOfInterestType.ALWAYS_TRUE;
            PointOfInterestStorage.OccupationStatus status = PointOfInterestStorage.OccupationStatus.ANY;
            boolean inColumn = false;
            if (locator.offset + 1 < lv.size())
            {
                String poiType = lv.get(locator.offset+1).evalValue(c).getString().toLowerCase(Locale.ROOT);
                if (!"any".equals(poiType))
                {
                    PointOfInterestType type =  Registry.POINT_OF_INTEREST_TYPE.get(new Identifier(poiType));
                    if (type == PointOfInterestType.UNEMPLOYED && !"unemployed".equals(poiType)) return LazyValue.NULL;
                    condition = (tt) -> tt == type;
                }
                if (locator.offset + 2 < lv.size())
                {
                    String statusString = lv.get(locator.offset+2).evalValue(c).getString().toLowerCase(Locale.ROOT);
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
                        inColumn = lv.get(locator.offset+3).evalValue(c, Context.BOOLEAN).getBoolean();
                    }
                }
            }
            Stream<PointOfInterest> pois = inColumn?
                    store.getInSquare(condition, pos, radius, status):
                    store.getInCircle(condition, pos, radius, status);
            Value ret = ListValue.wrap(pois.sorted(Comparator.comparingDouble(p -> p.getPos().getSquaredDistance(pos))).map(p ->
                    ListValue.of(
                            new StringValue(p.getType().toString()),
                            new NumericValue(p.getType().getTicketCount() - ((PointOfInterest_scarpetMixin)p).getFreeTickets()),
                            ListValue.of(new NumericValue(p.getPos().getX()), new NumericValue(p.getPos().getY()), new NumericValue(p.getPos().getZ()))
                    )
            ).collect(Collectors.toList()));

            return (c_, t_) -> ret;
        });

        //poi_set(pos, null) poi_set(pos, type, occupied?,
        expression.addLazyFunction("set_poi", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            if (lv.size() == 0) throw new InternalExpressionException("'set_poi' requires at least one parameter");
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0, false);
            BlockPos pos = locator.block.getPos();
            if (lv.size() < locator.offset) throw new InternalExpressionException("'set_poi' requires the new poi type or null, after position argument");
            Value poi = lv.get(locator.offset+0).evalValue(c);
            PointOfInterestStorage store = cc.s.getWorld().getPointOfInterestStorage();
            if (poi.isNull())
            {   // clear poi information
                if (store.getType(pos).isPresent())
                {
                    store.remove(pos);
                    return LazyValue.TRUE;
                }
                return LazyValue.FALSE;
            }
            String poiTypeString = poi.getString().toLowerCase(Locale.ROOT);
            PointOfInterestType type =  Registry.POINT_OF_INTEREST_TYPE.get(new Identifier(poiTypeString));
            // solving lack of null with defaulted registries
            if (type == PointOfInterestType.UNEMPLOYED && !"unemployed".equals(poiTypeString)) throw new InternalExpressionException("Unknown POI type: "+poiTypeString);
            int occupancy = 0;
            if (locator.offset + 1 < lv.size())
            {
                occupancy = (int)NumericValue.asNumber(lv.get(locator.offset + 1).evalValue(c)).getLong();
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
            return LazyValue.TRUE;
        });


        expression.addLazyFunction("weather",3,(c, t, lv)->{
            ServerWorld world = ((CarpetContext) c).s.getWorld();

            Value clear_value = lv.get(0).evalValue(c);
            Value rain_value = lv.get(1).evalValue(c);

            if(!(clear_value instanceof NumericValue && rain_value instanceof NumericValue))
                throw new InternalExpressionException("'weather' requires numeric argument for ticks");

            world.setWeather(
                    ((NumericValue) clear_value).getInt(),
                    ((NumericValue) rain_value).getInt(),true,
                    lv.get(2).evalValue(c).getBoolean()
            );

            return LazyValue.TRUE;
        });

        expression.addLazyFunction("pos", 1, (c, t, lv) ->
        {
            Value arg = lv.get(0).evalValue(c);
            if (arg instanceof BlockValue)
            {
                BlockPos pos = ((BlockValue) arg).getPos();
                if (pos == null)
                    throw new InternalExpressionException("Cannot fetch position of an unrealized block");
                Value retval = ListValue.of(new NumericValue(pos.getX()), new NumericValue(pos.getY()), new NumericValue(pos.getZ()));
                return (c_, t_) -> retval;
            }
            else if (arg instanceof EntityValue)
            {
                Entity e = ((EntityValue) arg).getEntity();
                if (e == null)
                    throw new InternalExpressionException("Null entity");
                Value retval = ListValue.of(new NumericValue(e.getX()), new NumericValue(e.getY()), new NumericValue(e.getZ()));
                return (c_, t_) -> retval;
            }
            else
            {
                throw new InternalExpressionException("'pos' works only with a block or an entity type");
            }
        });

        expression.addLazyFunction("pos_offset", -1, (c, t, lv) ->
        {
            BlockArgument locator = BlockArgument.findIn((CarpetContext)c, lv, 0);
            BlockPos pos = locator.block.getPos();
            if (lv.size() <= locator.offset)
                throw new InternalExpressionException("'pos_offset' needs at least position, and direction");
            String directionString = lv.get(locator.offset).evalValue(c).getString();
            Direction dir = DIRECTION_MAP.get(directionString);
            if (dir == null)
                throw new InternalExpressionException("Unknown direction: "+directionString);
            int howMuch = 1;
            if (lv.size() > locator.offset+1)
                howMuch = (int) NumericValue.asNumber(lv.get(locator.offset+1).evalValue(c)).getLong();
            BlockPos retpos = pos.offset(dir, howMuch);
            Value ret = ListValue.of(new NumericValue(retpos.getX()), new NumericValue(retpos.getY()), new NumericValue(retpos.getZ()));
            return (cc, tt) -> ret;
        });

        expression.addLazyFunction("solid", -1, (c, t, lv) ->
                genericStateTest(c, "solid", lv, (s, p, w) -> new NumericValue(s.isSolidBlock(w, p)))); // isSimpleFullBlock

        expression.addLazyFunction("air", -1, (c, t, lv) ->
                booleanStateTest(c, "air", lv, (s, p) -> s.isAir()));

        expression.addLazyFunction("liquid", -1, (c, t, lv) ->
                booleanStateTest(c, "liquid", lv, (s, p) -> !s.getFluidState().isEmpty()));

        expression.addLazyFunction("flammable", -1, (c, t, lv) ->
                booleanStateTest(c, "flammable", lv, (s, p) -> s.getMaterial().isBurnable()));

        expression.addLazyFunction("transparent", -1, (c, t, lv) ->
                booleanStateTest(c, "transparent", lv, (s, p) -> !s.getMaterial().isSolid()));

        /*this.expr.addLazyFunction("opacity", -1, (c, t, lv) ->
                genericStateTest(c, "opacity", lv, (s, p, w) -> new NumericValue(s.getOpacity(w, p))));

        this.expr.addLazyFunction("blocks_daylight", -1, (c, t, lv) ->
                genericStateTest(c, "blocks_daylight", lv, (s, p, w) -> new NumericValue(s.propagatesSkylightDown(w, p))));*/ // investigate

        expression.addLazyFunction("emitted_light", -1, (c, t, lv) ->
                genericStateTest(c, "emitted_light", lv, (s, p, w) -> new NumericValue(s.getLuminance())));

        expression.addLazyFunction("light", -1, (c, t, lv) ->
                genericStateTest(c, "light", lv, (s, p, w) -> new NumericValue(Math.max(w.getLightLevel(LightType.BLOCK, p), w.getLightLevel(LightType.SKY, p)))));

        expression.addLazyFunction("block_light", -1, (c, t, lv) ->
                genericStateTest(c, "block_light", lv, (s, p, w) -> new NumericValue(w.getLightLevel(LightType.BLOCK, p))));

        expression.addLazyFunction("sky_light", -1, (c, t, lv) ->
                genericStateTest(c, "sky_light", lv, (s, p, w) -> new NumericValue(w.getLightLevel(LightType.SKY, p))));

        expression.addLazyFunction("see_sky", -1, (c, t, lv) ->
                genericStateTest(c, "see_sky", lv, (s, p, w) -> new NumericValue(w.isSkyVisible(p))));

        expression.addLazyFunction("brightness", -1, (c, t, lv) ->
                genericStateTest(c, "brightness", lv, (s, p, w) -> new NumericValue(w.getBrightness(p))));

        expression.addLazyFunction("hardness", -1, (c, t, lv) ->
                genericStateTest(c, "hardness", lv, (s, p, w) -> new NumericValue(s.getHardness(w, p))));

        expression.addLazyFunction("blast_resistance", -1, (c, t, lv) ->
                genericStateTest(c, "blast_resistance", lv, (s, p, w) -> new NumericValue(s.getBlock().getBlastResistance())));

        expression.addLazyFunction("in_slime_chunk", -1, (c, t, lv) ->
        {
            BlockPos pos = BlockArgument.findIn((CarpetContext)c, lv, 0).block.getPos();
            ChunkPos chunkPos = new ChunkPos(pos);
            Value ret = new NumericValue(ChunkRandom.getSlimeRandom(
                    chunkPos.x, chunkPos.z,
                    ((CarpetContext)c).s.getWorld().getSeed(),
                    987234911L
            ).nextInt(10) == 0);
            return (_c, _t) -> ret;
        });


        expression.addLazyFunction("top", -1, (c, t, lv) ->
        {
            String type = lv.get(0).evalValue(c).getString().toLowerCase(Locale.ROOT);
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
            Value retval = new NumericValue(((CarpetContext)c).s.getWorld().getChunk(x >> 4, z >> 4).sampleHeightmap(htype, x & 15, z & 15) + 1);
            return (c_, t_) -> retval;
            //BlockPos pos = new BlockPos(x,y,z);
            //return new BlockValue(source.getWorld().getBlockState(pos), pos);
        });

        expression.addLazyFunction("loaded", -1, (c, t, lv) ->
        {
            Value retval = ((CarpetContext) c).s.getWorld().isChunkLoaded(BlockArgument.findIn((CarpetContext) c, lv, 0).block.getPos()) ? Value.TRUE : Value.FALSE;
            return (c_, t_) -> retval;
        });

        // Deprecated, use loaded_status as more indicative
        expression.addLazyFunction("loaded_ep", -1, (c, t, lv) ->
        {
            c.host.issueDeprecation("loaded_ep(...)");
            BlockPos pos = BlockArgument.findIn((CarpetContext)c, lv, 0).block.getPos();
            Value retval = ((CarpetContext)c).s.getWorld().getChunkManager().shouldTickChunk(new ChunkPos(pos))?Value.TRUE : Value.FALSE;
            return (c_, t_) -> retval;
        });

        expression.addLazyFunction("loaded_status", -1, (c, t, lv) ->
        {
            BlockPos pos = BlockArgument.findIn((CarpetContext)c, lv, 0).block.getPos();
            WorldChunk chunk = ((CarpetContext)c).s.getWorld().getChunkManager().getWorldChunk(pos.getX()>>4, pos.getZ()>>4, false);
            if (chunk == null)
                return LazyValue.ZERO;
            Value retval = new NumericValue(chunk.getLevelType().ordinal());
            return (c_, t_) -> retval;
        });

        expression.addLazyFunction("is_chunk_generated", -1, (c, t, lv) ->
        {
            BlockArgument locator = BlockArgument.findIn((CarpetContext)c, lv, 0);
            BlockPos pos = locator.block.getPos();
            boolean force = false;
            if (lv.size() > locator.offset)
                force = lv.get(locator.offset).evalValue(c, Context.BOOLEAN).getBoolean();
            Value retval = new NumericValue(canHasChunk(((CarpetContext)c).s.getWorld(), new ChunkPos(pos), null, force));
            return (c_, t_) -> retval;
        });

        expression.addLazyFunction("generation_status", -1, (c, t, lv) ->
        {
            BlockArgument blockArgument = BlockArgument.findIn((CarpetContext)c, lv, 0);
            BlockPos pos = blockArgument.block.getPos();
            boolean forceLoad = false;
            if (lv.size() > blockArgument.offset)
                forceLoad = lv.get(blockArgument.offset).evalValue(c, Context.BOOLEAN).getBoolean();
            Chunk chunk = ((CarpetContext)c).s.getWorld().getChunk(pos.getX()>>4, pos.getZ()>>4, ChunkStatus.EMPTY, forceLoad);
            if (chunk == null)
                return LazyValue.NULL;
            Value retval = new StringValue(chunk.getStatus().getId());
            return (c_, t_) -> retval;
        });

        expression.addLazyFunction("chunk_tickets", -1, (c, t, lv) ->
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
            Value returnValue = ListValue.wrap(res);
            return (_c, _t) -> returnValue;
        });

        expression.addLazyFunction("suffocates", -1, (c, t, lv) ->
                genericStateTest(c, "suffocates", lv, (s, p, w) -> new NumericValue(s.shouldSuffocate(w, p)))); // canSuffocate

        expression.addLazyFunction("power", -1, (c, t, lv) ->
                genericStateTest(c, "power", lv, (s, p, w) -> new NumericValue(w.getReceivedRedstonePower(p))));

        expression.addLazyFunction("ticks_randomly", -1, (c, t, lv) ->
                booleanStateTest(c, "ticks_randomly", lv, (s, p) -> s.hasRandomTicks()));

        expression.addLazyFunction("update", -1, (c, t, lv) ->
                booleanStateTest(c, "update", lv, (s, p) ->
                {
                    ((CarpetContext) c).s.getWorld().updateNeighbor(p, s.getBlock(), p);
                    return true;
                }));

        expression.addLazyFunction("block_tick", -1, (c, t, lv) ->
                booleanStateTest(c, "block_tick", lv, (s, p) ->
                {
                    ServerWorld w = ((CarpetContext)c).s.getWorld();
                    s.randomTick(w, p, w.random);
                    return true;
                }));

        expression.addLazyFunction("random_tick", -1, (c, t, lv) ->
                booleanStateTest(c, "random_tick", lv, (s, p) ->
                {
                    ServerWorld w = ((CarpetContext)c).s.getWorld();
                    if (s.hasRandomTicks() || s.getFluidState().hasRandomTicks())
                        s.randomTick(w, p, w.random);
                    return true;
                }));

        expression.addLazyFunction("without_updates", 1, (c, t, lv) ->
        {
            boolean previous = CarpetSettings.impendingFillSkipUpdates;
            if (previous) return lv.get(0);
            Value [] result = new Value[]{Value.NULL};
            ((CarpetContext)c).s.getMinecraftServer().submitAndJoin( () ->
            {
                try
                {
                    CarpetSettings.impendingFillSkipUpdates = true;
                    result[0] = lv.get(0).evalValue(c, t);
                }
                finally
                {
                    CarpetSettings.impendingFillSkipUpdates = previous;
                }
            });
            return (cc, tt) -> result[0];
        });

        expression.addLazyFunction("set", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerWorld world = cc.s.getWorld();
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
                    args.add(lv.get(i).evalValue(c));
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
            CompoundTag finalData = data;

            if (sourceBlockState == targetBlockState && data == null)
                return (c_, t_) -> Value.FALSE;
            BlockState finalSourceBlockState = sourceBlockState;
            BlockPos targetPos = targetLocator.block.getPos();
            Boolean[] result = new Boolean[]{true};
            cc.s.getMinecraftServer().submitAndJoin( () ->
            {
                Clearable.clear(world.getBlockEntity(targetPos));
                boolean success = world.setBlockState(targetPos, finalSourceBlockState, 2);
                if (success && finalData != null)
                {
                    BlockEntity be = world.getBlockEntity(targetPos);
                    if (be != null)
                    {
                        CompoundTag destTag = finalData.copy();
                        destTag.putInt("x", targetPos.getX());
                        destTag.putInt("y", targetPos.getY());
                        destTag.putInt("z", targetPos.getZ());
                        be.fromTag(finalSourceBlockState, destTag);
                        be.markDirty();
                    }
                }
                result[0] = success;
            });
            if (!result[0]) return LazyValue.FALSE;
            Value retval = new BlockValue(finalSourceBlockState, world, targetLocator.block.getPos());
            return (c_, t_) -> retval;
        });

        expression.addLazyFunction("destroy", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerWorld world = cc.s.getWorld();
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            BlockState state = locator.block.getBlockState();
            if (state.isAir())
                return (c_, t_) -> Value.FALSE;
            BlockPos where = locator.block.getPos();
            BlockEntity be = world.getBlockEntity(where);
            long how = 0;
            Item item = Items.DIAMOND_PICKAXE;
            boolean playerBreak = false;
            if (lv.size() > locator.offset)
            {
                Value val = lv.get(locator.offset).evalValue(c);
                if (val instanceof NumericValue)
                {
                    how = ((NumericValue) val).getLong();
                }
                else
                {
                    playerBreak = true;
                    String itemString = val.getString();
                    item = Registry.ITEM.get(new Identifier(itemString));
                    if (item == Items.AIR && !itemString.equals("air"))
                        throw new InternalExpressionException("Incorrect item: " + itemString);
                }
            }
            CompoundTag tag = null;
            if (lv.size() > locator.offset+1)
            {
                if (!playerBreak) throw new InternalExpressionException("tag is not necessary with 'destroy' with no item");
                Value tagValue = lv.get(locator.offset+1).evalValue(c);
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
                tool.setTag(tag);
            if (playerBreak && state.getHardness(world, where) < 0.0) return LazyValue.FALSE;
            boolean removed = world.removeBlock(where, false);
            if (!removed) return LazyValue.FALSE;
            world.syncWorldEvent(null, 2001, where, Block.getRawIdFromState(state));

            boolean toolBroke = false;
            boolean dropLoot = true;
            if (playerBreak)
            {
                boolean isUsingEffectiveTool = !state.isToolRequired() || tool.isEffectiveOn(state);
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
                    Block.dropStacks(state, world, where, be, DUMMY_ENTITY, tool);
                }
            }
            if (!playerBreak) // no tool info - block brokwn
                return (c_, t_) -> Value.TRUE;
            if (toolBroke)
                return LazyValue.NULL;
            Tag outtag = tool.getTag();
            if (outtag == null)
                return LazyValue.TRUE;
            Value ret = new NBTSerializableValue(() -> outtag);
            return (_c, _t) -> ret;

        });

        expression.addLazyFunction("harvest", -1, (c, t, lv) ->
        {
            if (lv.size()<2)
                throw new InternalExpressionException("'harvest' takes at least 2 parameters: entity and block, or position, to harvest");
            CarpetContext cc = (CarpetContext)c;
            World world = cc.s.getWorld();
            Value entityValue = lv.get(0).evalValue(cc);
            if (!(entityValue instanceof EntityValue))
                return (c_, t_) -> Value.FALSE;
            Entity e = ((EntityValue) entityValue).getEntity();
            if (!(e instanceof ServerPlayerEntity))
                return (c_, t_) -> Value.FALSE;
            ServerPlayerEntity player = (ServerPlayerEntity)e;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 1);
            BlockPos where = locator.block.getPos();
            BlockState state = locator.block.getBlockState();
            Block block = state.getBlock();
            boolean success = false;
            if (!((block == Blocks.BEDROCK || block == Blocks.BARRIER) && player.interactionManager.isSurvivalLike()))
                success = tryBreakBlock_copy_from_ServerPlayerInteractionManager(player, where);
            if (success)
                world.syncWorldEvent(null, 2001, where, Block.getRawIdFromState(state));
            return success ? LazyValue.TRUE : LazyValue.FALSE;
        });

        // TODO rename to use_item
        expression.addLazyFunction("place_item", -1, (c, t, lv) ->
        {
            if (lv.size()<2)
                throw new InternalExpressionException("'place_item' takes at least 2 parameters: item and block, or position, to place onto");
            CarpetContext cc = (CarpetContext) c;
            String itemString = lv.get(0).evalValue(c).getString();
            Vector3Argument locator = Vector3Argument.findIn(cc, lv, 1);
            ItemStackArgument stackArg = NBTSerializableValue.parseItem(itemString);
            BlockPos where = new BlockPos(locator.vec);
            String facing = "up";
            if (lv.size() > locator.offset)
                facing = lv.get(locator.offset).evalValue(c).getString();
            boolean sneakPlace = false;
            if (lv.size() > locator.offset+1)
                sneakPlace = lv.get(locator.offset+1).evalValue(c).getBoolean();

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
                    return LazyValue.TRUE;
                }
            }
            else
            { // not sure we need special case for block items, since useOnBlock can do that as well
                BlockItem blockItem = (BlockItem) stackArg.getItem();
                if (!ctx.canPlace())
                    return (_c, _t) -> Value.FALSE;
                BlockState placementState = blockItem.getBlock().getPlacementState(ctx);
                if (placementState != null)
                {
                    if (placementState.canPlaceAt(cc.s.getWorld(), where))
                    {
                        cc.s.getWorld().setBlockState(where, placementState, 2);
                        BlockSoundGroup blockSoundGroup = placementState.getSoundGroup();
                        cc.s.getWorld().playSound(null, where, blockSoundGroup.getPlaceSound(), SoundCategory.BLOCKS, (blockSoundGroup.getVolume() + 1.0F) / 2.0F, blockSoundGroup.getPitch() * 0.8F);
                        return (_c, _t) -> Value.TRUE;
                    }
                }
            }
            return (_c, _t) -> Value.FALSE;
        });

        expression.addLazyFunction("blocks_movement", -1, (c, t, lv) ->
                booleanStateTest(c, "blocks_movement", lv, (s, p) ->
                        !s.canPathfindThrough(((CarpetContext) c).s.getWorld(), p, NavigationType.LAND)));

        expression.addLazyFunction("block_sound", -1, (c, t, lv) ->
                stateStringQuery(c, "block_sound", lv, (s, p) ->
                        BlockInfo.soundName.get(s.getSoundGroup())));

        expression.addLazyFunction("material", -1, (c, t, lv) ->
                stateStringQuery(c, "material", lv, (s, p) ->
                        BlockInfo.materialName.get(s.getMaterial())));

        expression.addLazyFunction("map_colour", -1, (c, t, lv) ->
                stateStringQuery(c, "map_colour", lv, (s, p) ->
                        BlockInfo.mapColourName.get(s.getTopMaterialColor(((CarpetContext)c).s.getWorld(), p))));


        // Deprecated for block_state()
        expression.addLazyFunction("property", -1, (c, t, lv) ->
        {
            c.host.issueDeprecation("property(...)");
            BlockArgument locator = BlockArgument.findIn((CarpetContext) c, lv, 0);
            BlockState state = locator.block.getBlockState();
            if (lv.size() <= locator.offset)
                throw new InternalExpressionException("'property' requires to specify a property to query");
            String tag = lv.get(locator.offset).evalValue(c).getString();
            StateManager<Block, BlockState> states = state.getBlock().getStateManager();
            Property<?> property = states.getProperty(tag);
            if (property == null)
                return LazyValue.NULL;
            Value retval = new StringValue(state.get(property).toString().toLowerCase(Locale.ROOT));
            return (_c, _t) -> retval;
        });

        // Deprecated for block_state()
        expression.addLazyFunction("block_properties", -1, (c, t, lv) ->
        {
            c.host.issueDeprecation("block_properties(...)");
            BlockArgument locator = BlockArgument.findIn((CarpetContext) c, lv, 0);
            BlockState state = locator.block.getBlockState();
            StateManager<Block, BlockState> states = state.getBlock().getStateManager();
            Value res = ListValue.wrap(states.getProperties().stream().map(
                    p -> new StringValue(p.getName())).collect(Collectors.toList())
            );
            return (_c, _t) -> res;
        });

        // block_state(block)
        // block_state(block, property)
        expression.addLazyFunction("block_state", -1, (c, t, lv) ->
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
                Value ret = MapValue.wrap(properties);
                return (_c, _t) -> ret;
            }
            String tag = lv.get(locator.offset).evalValue(c).getString();
            Property<?> property = states.getProperty(tag);
            if (property == null)
                return LazyValue.NULL;
            Value retval = ValueConversions.fromProperty(state, property);
            return (_c, _t) -> retval;
        });

        expression.addLazyFunction("block_list", -1, (c, t, lv) ->
        {
            if (lv.size() == 0)
            {
                Value ret = ListValue.wrap(Registry.BLOCK.getIds().stream().map(ValueConversions::of).collect(Collectors.toList()));
                return (_c, _t) -> ret;
            }
            CarpetContext cc = (CarpetContext)c;
            TagManager tagManager = cc.s.getMinecraftServer().getTagManager();
            String tag = lv.get(0).evalValue(c).getString();
            net.minecraft.tag.Tag<Block> blockTag = tagManager.getBlocks().getTag(new Identifier(tag));
            if (blockTag == null) return LazyValue.NULL;
            Value ret = ListValue.wrap(blockTag.values().stream().map(b -> ValueConversions.of(Registry.BLOCK.getId(b))).collect(Collectors.toList()));
            return (_c, _t) -> ret;
        });

        expression.addLazyFunction("block_tags", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            TagManager tagManager = cc.s.getMinecraftServer().getTagManager();
            if (lv.size() == 0)
            {
                Value ret = ListValue.wrap(tagManager.getBlocks().getTagIds().stream().map(ValueConversions::of).collect(Collectors.toList()));
                return (_c, _t) -> ret;
            }
            BlockArgument blockLocator = BlockArgument.findIn(cc, lv, 0, true);
            if (blockLocator.offset == lv.size())
            {
                Value ret = ListValue.wrap(tagManager.getBlocks().getTagsFor(blockLocator.block.getBlockState().getBlock()).stream().map(ValueConversions::of).collect(Collectors.toList()));
                return (_c, _t) -> ret;
            }
            String tag = lv.get(blockLocator.offset).evalValue(c).getString();
            net.minecraft.tag.Tag<Block> blockTag = tagManager.getBlocks().getTag(new Identifier(tag));
            if (blockTag == null) return LazyValue.NULL;
            return blockLocator.block.getBlockState().isIn(blockTag)?LazyValue.TRUE:LazyValue.FALSE;
        });


        expression.addLazyFunction("biome", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            ServerWorld world = cc.s.getWorld();
            if (lv.size() == 0)
            {
                Value ret = ListValue.wrap(world.getRegistryManager().get(Registry.BIOME_KEY).getIds().stream().map(ValueConversions::of));
                return (_c, _t) -> ret;
            }
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0, false, false, true);

            Biome biome;
            if (locator.replacement != null)
            {
                biome = world.getRegistryManager().get(Registry.BIOME_KEY).get(new Identifier(locator.replacement));
                if (biome == null) return LazyValue.NULL;
            }
            else
            {
                BlockPos pos = locator.block.getPos();
                biome = world.getBiome(pos);
            }
            // in locatebiome
            if (locator.offset == lv.size())
            {
                Identifier biomeId = cc.s.getMinecraftServer().getRegistryManager().get(Registry.BIOME_KEY).getId(biome);
                Value res = new StringValue(NBTSerializableValue.nameFromRegistryId(biomeId));
                return (_c, _t) -> res;
            }
            String biomeFeature = lv.get(locator.offset).evalValue(c).getString();
            BiFunction<ServerWorld, Biome, Value> featureProvider = BiomeInfo.biomeFeatures.get(biomeFeature);
            if (featureProvider == null) throw new InternalExpressionException("Unknown biome feature: "+biomeFeature);
            Value ret = featureProvider.apply(world, biome);
            return (_c, _t) -> ret;
        });

        expression.addLazyFunction("set_biome", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            if (lv.size() == locator.offset)
                throw new InternalExpressionException("'set_biome' needs a biome name as an argument");
            String biomeName = lv.get(locator.offset+0).evalValue(c).getString();
            // from locatebiome command code
            Biome biome = cc.s.getMinecraftServer().getRegistryManager().get(Registry.BIOME_KEY).get(new Identifier(biomeName));
            if (biome == null)
                throw new InternalExpressionException("Unknown biome: "+biomeName);
            boolean doImmediateUpdate = true;
            if (lv.size() > locator.offset+1)
            {
                doImmediateUpdate = lv.get(locator.offset+1).evalValue(c).getBoolean();
            }
            ServerWorld world = cc.s.getWorld();
            BlockPos pos = locator.block.getPos();
            Chunk chunk = world.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.BIOMES);
            ((BiomeArrayInterface)chunk.getBiomeArray()).setBiomeAtIndex(pos, world,  biome);
            if (doImmediateUpdate) WorldTools.forceChunkUpdate(pos, world);
            return LazyValue.TRUE;
        });

        expression.addLazyFunction("reload_chunk", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            BlockPos pos = BlockArgument.findIn(cc, lv, 0).block.getPos();
            ServerWorld world = cc.s.getWorld();
            cc.s.getMinecraftServer().submitAndJoin( () -> WorldTools.forceChunkUpdate(pos, world));
            return LazyValue.TRUE;
        });

        expression.addLazyFunction("structure_references", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            ServerWorld world = cc.s.getWorld();
            BlockPos pos = locator.block.getPos();
            Map<StructureFeature<?>, LongSet> references = world.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.STRUCTURE_REFERENCES).getStructureReferences();
            if (lv.size() == locator.offset)
            {
                //CarpetSettings.LOG.error(references.keySet().stream().collect(Collectors.joining(",")));
                List<Value> referenceList = references.entrySet().stream().
                        filter(e -> e.getValue()!= null && !e.getValue().isEmpty()).
                        map(e -> new StringValue(NBTSerializableValue.nameFromRegistryId(Registry.STRUCTURE_FEATURE.getId(e.getKey())))).collect(Collectors.toList());
                return (_c, _t ) -> ListValue.wrap(referenceList);
            }
            String simpleStructureName = lv.get(locator.offset).evalValue(c).getString().toLowerCase(Locale.ROOT);
            //CarpetSettings.LOG.error(FeatureGenerator.featureToStructure.keySet().stream().collect(Collectors.joining(",")));
            //CarpetSettings.LOG.error(FeatureGenerator.featureToStructure.values().stream().collect(Collectors.joining(",")));
            StructureFeature<?> structureName = Registry.STRUCTURE_FEATURE.get(new Identifier(simpleStructureName));
            if (structureName == null) return LazyValue.NULL;
            LongSet structureReferences = references.get(structureName);
            if (structureReferences == null || structureReferences.isEmpty()) return ListValue.lazyEmpty();
            Value ret = ListValue.wrap(structureReferences.stream().map(l -> ListValue.of(
                    new NumericValue(16*ChunkPos.getPackedX(l)),
                    Value.ZERO,
                    new NumericValue(16*ChunkPos.getPackedZ(l)))).collect(Collectors.toList()));
            return (_c, _t) -> ret;
        });

        expression.addLazyFunction("structure_eligibility", -1, (c, t, lv) ->
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
                Value requested = lv.get(locator.offset+0).evalValue(c);
                if (!(requested instanceof NullValue))
                {
                    String reqString = requested.getString();
                    structure = Registry.STRUCTURE_FEATURE.get(new Identifier(reqString));
                    if (structure == null) throw new InternalExpressionException("Unknown structure: " + reqString);
                }
                if (lv.size() > locator.offset+1)
                {
                    needSize = lv.get(locator.offset+1).evalValue(c).getBoolean();
                }
            }
            if (structure != null)
            {
                StructureStart<?> start = FeatureGenerator.shouldStructureStartAt(world, pos, structure, needSize);
                if (start == null) return LazyValue.NULL;
                if (!needSize) return LazyValue.TRUE;
                Value ret = ValueConversions.of(start);
                return (_c, _t) -> ret;
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
            Value retMap = MapValue.wrap(ret);
            return (_c, _t) -> retMap;
        });

        expression.addLazyFunction("structures", -1, (c, t, lv) -> {
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
                    BlockBox box = start.getBoundingBox();
                    structureList.put(
                            new StringValue(NBTSerializableValue.nameFromRegistryId(Registry.STRUCTURE_FEATURE.getId(entry.getKey()))),
                            ListValue.of(ListValue.fromTriple(box.minX, box.minY, box.minZ), ListValue.fromTriple(box.maxX, box.maxY, box.maxZ))
                    );
                }
                Value ret = MapValue.wrap(structureList);
                return (_c, _t) -> ret;
            }
            String structureName = lv.get(locator.offset).evalValue(c).getString().toLowerCase(Locale.ROOT);
            StructureStart start = structures.get(Registry.STRUCTURE_FEATURE.get(new Identifier(structureName)));
            Value ret = ValueConversions.of(start);
            return (_c, _t) -> ret;
        });

        expression.addLazyFunction("set_structure", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);

            ServerWorld world = cc.s.getWorld();
            BlockPos pos = locator.block.getPos();

            if (lv.size() == locator.offset)
                throw new InternalExpressionException("'set_structure requires at least position and a structure name");
            String structureName = lv.get(locator.offset).evalValue(c).getString().toLowerCase(Locale.ROOT);
            ConfiguredStructureFeature<?, ?> configuredStructure = FeatureGenerator.resolveConfiguredStructure(structureName, world, pos);
            if (configuredStructure == null) throw new InternalExpressionException("Unknown structure: "+structureName);
            // good 'ol pointer
            Value[] result = new Value[]{Value.NULL};
            // technically a world modification. Even if we could let it slide, we will still park it
            ((CarpetContext) c).s.getMinecraftServer().submitAndJoin(() ->
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
                Value newValue = lv.get(locator.offset+1).evalValue(c);
                if (newValue instanceof NullValue) // remove structure
                {
                    StructureFeature<?> structure = configuredStructure.feature;
                    if (!structures.containsKey(structure))
                    {
                        return;
                    }
                    StructureStart<?> start = structures.get(structure);
                    ChunkPos structureChunkPos = new ChunkPos(start.getChunkX(), start.getChunkZ());
                    BlockBox box = start.getBoundingBox();
                    for (int chx = box.minX / 16; chx <= box.maxX / 16; chx++)
                    {
                        for (int chz = box.minZ / 16; chz <= box.maxZ / 16; chz++)
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
            Value ret = result[0]; // preventing from lazy evaluating of the result in case a future completes later
            return (_c, _t) -> ret;
        });

        expression.addLazyFunction("custom_dimension", -1, (c, t, lv) ->
        {
            if (lv.size() == 0) throw new InternalExpressionException("'custom_dimension' requires at least one argument");
            CarpetContext cc = (CarpetContext)c;
            String worldKey = lv.get(0).evalValue(c).getString();

            Long seed = null;
            if (lv.size() > 1)
            {
                String seedKey = lv.get(1).evalValue(c).getString();
                try
                {
                    seed = Long.parseLong(seedKey);
                }
                catch (NumberFormatException ignored)
                {
                    throw new InternalExpressionException("Incorrect number format for seed: " + seedKey);
                }
            }

            boolean success = WorldTools.createWorld(cc.s.getMinecraftServer(), worldKey, seed);
            if (!success) return LazyValue.FALSE;
            CarpetServer.settingsManager.notifyPlayersCommandsChanged();
            return LazyValue.TRUE;
        });


        expression.addLazyFunction("reset_chunk", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            List<ChunkPos> requestedChunks = new ArrayList<>();
            if (lv.size() == 1)
            {
                //either one block or list of chunks
                Value first = lv.get(0).evalValue(c);
                if (first instanceof ListValue)
                {
                    List<Value> listVal = ((ListValue) first).getItems();
                    int offset = 0;
                    BlockArgument locator = BlockArgument.findInValues(cc, listVal, 0, false, false);
                    requestedChunks.add(new ChunkPos(locator.block.getPos()));
                    while (listVal.size() > locator.offset)
                    {
                        locator = BlockArgument.findInValues(cc, listVal, locator.offset, false, false);
                        requestedChunks.add(new ChunkPos(locator.block.getPos()));
                    }
                }
                else
                {
                    BlockArgument locator = BlockArgument.findInValues(cc, Collections.singletonList(first), 0, false, false);
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
                    //CarpetSettings.LOG.error("Regenerating from "+Math.min(from.x, to.x)+", "+Math.min(from.z, to.z)+" to "+Math.max(from.x, to.x)+", "+Math.max(from.z, to.z));
                }
                else
                {
                    requestedChunks.add(from);
                }
            }


            ServerWorld world = cc.s.getWorld();

            Value [] result = new Value[]{Value.NULL};

            ((CarpetContext)c).s.getMinecraftServer().submitAndJoin( () ->
            {
                Map<String, Integer> report = ((ThreadedAnvilChunkStorageInterface) world.getChunkManager().threadedAnvilChunkStorage).regenerateChunkRegion(requestedChunks);
                for (ChunkPos chpos: requestedChunks)
                {
                    if (world.getChunk(chpos.x, chpos.z, ChunkStatus.FULL, false) != null)
                    {
                        WorldTools.forceChunkUpdate(chpos.getStartPos(), world);
                    }
                }
                result[0] = MapValue.wrap(report.entrySet().stream().collect(Collectors.toMap(
                        e -> new StringValue((String)((Map.Entry) e).getKey()),
                        e ->  new NumericValue((Integer)((Map.Entry) e).getValue())
                )));
            });
            return (_c, _t) -> result[0];
        });

        expression.addLazyFunction("inhabited_time", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            BlockPos pos = locator.block.getPos();
            Value ret = new NumericValue(cc.s.getWorld().getChunk(pos).getInhabitedTime());
            return (_c, _t) -> ret;
        });

        expression.addLazyFunction("spawn_potential", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            BlockPos pos = locator.block.getPos();
            double required_charge = 1;
            if (lv.size() > locator.offset)
                required_charge = NumericValue.asNumber(lv.get(locator.offset).evalValue(c)).getDouble();
            SpawnHelper.Info charger = cc.s.getWorld().getChunkManager().getSpawnInfo();
            if (charger == null) return LazyValue.NULL;
            Value ret = new NumericValue(
                    ((SpawnHelperInnerInterface)charger).getPotentialCalculator().
                            calculate(pos, required_charge )
            );
            return (_c, _t) -> ret;
        });

        expression.addLazyFunction("add_chunk_ticket", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            BlockPos pos = locator.block.getPos();
            if (lv.size() != locator.offset+2) throw new InternalExpressionException("'add_chunk_ticket' requires block position, ticket type and radius");
            String type = lv.get(locator.offset).evalValue(c).getString();
            ChunkTicketType ticket = ticketTypes.get(type.toLowerCase(Locale.ROOT));
            if (ticket == null) throw new InternalExpressionException("Unknown ticket type: "+type);
            int radius = NumericValue.asNumber(lv.get(locator.offset+1).evalValue(c)).getInt();
            if (radius < 1 || radius > 32) throw new InternalExpressionException("Ticket radius should be between 1 and 32 chunks");
            // due to types we will wing it:
            ChunkPos target = new ChunkPos(pos);
            if (ticket == ChunkTicketType.PORTAL) // portal
                cc.s.getWorld().getChunkManager().addTicket(ChunkTicketType.PORTAL, target, radius, pos);
            else if (ticket == ChunkTicketType.POST_TELEPORT) // post teleport
                cc.s.getWorld().getChunkManager().addTicket(ChunkTicketType.POST_TELEPORT, target, radius, 1);
            else
                cc.s.getWorld().getChunkManager().addTicket(ChunkTicketType.UNKNOWN, target, radius, target);
            Value ret = new NumericValue(ticket.getExpiryTicks());
            return (_c, _t) -> ret;
        });

    }
}
