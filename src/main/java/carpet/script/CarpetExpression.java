package carpet.script;

import carpet.CarpetServer;
import carpet.fakes.ChunkTicketManagerInterface;
import carpet.fakes.IngredientInterface;
import carpet.fakes.MinecraftServerInterface;
import carpet.fakes.BiomeArrayInterface;
import carpet.fakes.RecipeManagerInterface;
import carpet.fakes.ServerChunkManagerInterface;
import carpet.fakes.StatTypeInterface;
import carpet.fakes.ThreadedAnvilChunkStorageInterface;
import carpet.helpers.FeatureGenerator;
import carpet.mixins.PointOfInterest_scarpetMixin;
import carpet.script.Fluff.TriFunction;
import carpet.script.argument.BlockArgument;
import carpet.script.argument.FunctionArgument;
import carpet.script.argument.Vector3Argument;
import carpet.script.bundled.Module;
import carpet.CarpetSettings;
import carpet.script.exception.BreakStatement;
import carpet.script.exception.CarpetExpressionException;
import carpet.script.exception.ContinueStatement;
import carpet.script.exception.ExitStatement;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.InternalExpressionException;
import carpet.script.utils.ShapeDispatcher;
import carpet.script.value.BlockValue;
import carpet.script.value.EntityValue;
import carpet.script.value.FormattedTextValue;
import carpet.script.value.FunctionValue;
import carpet.script.value.LazyListValue;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.NullValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.ThreadValue;
import carpet.script.value.Value;
import carpet.utils.BlockInfo;
import carpet.utils.Messenger;
import com.google.common.collect.Sets;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.BarrierBlock;
import net.minecraft.block.BedrockBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPlacementEnvironment;
import net.minecraft.block.BlockState;
import net.minecraft.block.CommandBlock;
import net.minecraft.block.JigsawBlock;
import net.minecraft.block.StructureBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.network.packet.s2c.play.ContainerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket;
import net.minecraft.command.arguments.ItemStackArgument;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.CuttingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatType;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.structure.StructureFeatures;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Clearable;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.ChunkRandom;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static carpet.script.utils.WorldTools.canHasChunk;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class CarpetExpression
{

    private final ServerCommandSource source;
    private final BlockPos origin;
    private final Expression expr;
    public Expression getExpr() {return expr;}
    private static long tickStart = 0L;
    // dummy entity for dummy requirements in the loot tables (see snowball)
    private static FallingBlockEntity DUMMY_ENTITY = new FallingBlockEntity(EntityType.FALLING_BLOCK, null);
    public static final String MARKER_STRING = "__scarpet_marker";

    private static boolean stopAll = false;

    private static final Map<String, Direction> DIRECTION_MAP = Arrays.stream(Direction.values()).collect(Collectors.toMap(Direction::getName, (direction) -> direction));

    private LazyValue booleanStateTest(
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

    private LazyValue stateStringQuery(
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

    private LazyValue genericStateTest(
            Context c,
            String name,
            List<LazyValue> params,
            TriFunction<BlockState, BlockPos, World, Value> test
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

    private <T extends Comparable<T>> BlockState setProperty(Property<T> property, String name, String value,
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


    private static void forceChunkUpdate(BlockPos pos, ServerWorld world)
    {
        Chunk chunk = world.getChunk(pos);
        chunk.setShouldSave(true);
        for (int i = 0; i<16; i++)
        {
            BlockPos section = new BlockPos((pos.getX()>>4 <<4)+8, 16*i, (pos.getZ()>>4 <<4)+8);
            world.getChunkManager().markForUpdate(section);
            world.getChunkManager().markForUpdate(section.east());
            world.getChunkManager().markForUpdate(section.west());
            world.getChunkManager().markForUpdate(section.north());
        }
    }

    private boolean tryBreakBlock_copy_from_ServerPlayerInteractionManager(ServerPlayerEntity player, BlockPos blockPos_1)
    {
        //this could be done little better, by hooking up event handling not in try_break_block but wherever its called
        // so we can safely call it here
        // but that woudl do for now.
        BlockState blockState_1 = player.world.getBlockState(blockPos_1);
        if (!player.getMainHandStack().getItem().canMine(blockState_1, player.world, blockPos_1, player)) {
            return false;
        } else {
            BlockEntity blockEntity_1 = player.world.getBlockEntity(blockPos_1);
            Block block_1 = blockState_1.getBlock();
            if ((block_1 instanceof CommandBlock || block_1 instanceof StructureBlock || block_1 instanceof JigsawBlock) && !player.isCreativeLevelTwoOp()) {
                player.world.updateListeners(blockPos_1, blockState_1, blockState_1, 3);
                return false;
            } else if (player.canMine(player.world, blockPos_1, player.interactionManager.getGameMode())) {
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



    private static Value structureToValue(StructureStart structure)
    {
        if (structure == null || structure == StructureStart.DEFAULT) return Value.NULL;
        List<Value> pieces = new ArrayList<>();
        for (StructurePiece piece : structure.getChildren())
        {
            BlockBox box = piece.getBoundingBox();
            pieces.add(ListValue.of(
                    new StringValue( NBTSerializableValue.nameFromRegistryId(Registry.STRUCTURE_PIECE.getId(piece.getType()))),
                    (piece.getFacing()== null)?Value.NULL: new StringValue(piece.getFacing().getName()),
                    ListValue.fromTriple(box.minX, box.minY, box.minZ),
                    ListValue.fromTriple(box.maxX, box.maxY, box.maxZ)
            ));
        };
        BlockBox boundingBox = structure.getBoundingBox();
        Map<Value, Value> ret = new HashMap<>();
        ret.put(new StringValue("box"), ListValue.of(
                ListValue.fromTriple(boundingBox.minX, boundingBox.minY, boundingBox.minZ),
                ListValue.fromTriple(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ)
        ));
        ret.put(new StringValue("pieces"), ListValue.wrap(pieces));
        return MapValue.wrap(ret);
    }

    private static long lastSeed = -1;
    private void BooYah(ChunkGenerator generator)
    {
        synchronized (generator)
        {
            if (generator.getSeed() != lastSeed)
            {
                StructureFeatures.STRONGHOLD.shouldStartAt(null, generator, null, 0, 0, null);
                lastSeed = generator.getSeed();
            }
        }
    }
    private void API_BlockManipulation()
    {
        this.expr.addLazyFunction("block", -1, (c, t, lv) ->
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

        this.expr.addLazyFunction("block_data", -1, (c, t, lv) ->
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

        // poi_get(pos, radius?, type?, occupation?)
        this.expr.addLazyFunction("poi", -1, (c, t, lv) ->
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
                PointOfInterest poi = store.get(
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
            long radius = NumericValue.asNumber(lv.get(locator.offset+0).evalValue(c)).getLong();
            if (radius < 0) return ListValue.lazyEmpty();
            Predicate<PointOfInterestType> condition = PointOfInterestType.ALWAYS_TRUE;
            if (locator.offset + 1 < lv.size())
            {
                String poiType = lv.get(locator.offset+1).evalValue(c).getString().toLowerCase(Locale.ROOT);
                if (!"any".equals(poiType))
                {
                    PointOfInterestType type =  Registry.POINT_OF_INTEREST_TYPE.get(new Identifier(poiType));
                    if (type == PointOfInterestType.UNEMPLOYED && !"unemployed".equals(poiType)) return LazyValue.NULL;
                    condition = (tt) -> tt == type;
                }
            }
            PointOfInterestStorage.OccupationStatus status = PointOfInterestStorage.OccupationStatus.ANY;
            if (locator.offset + 2 < lv.size())
            {
                String statusString = lv.get(locator.offset+2).evalValue(c).getString().toLowerCase(Locale.ROOT);
                if ("occupied".equals(statusString))
                    status = PointOfInterestStorage.OccupationStatus.IS_OCCUPIED;
                else if ("available".equals(statusString))
                    status = PointOfInterestStorage.OccupationStatus.HAS_SPACE;
                else
                    return LazyValue.NULL;
            }

            Value ret = ListValue.wrap(store.get(condition, pos, (int)radius, status).sorted(Comparator.comparingDouble(p -> p.getPos().getSquaredDistance(pos))).map(p ->
                    ListValue.of(
                            new StringValue(p.getType().toString()),
                            new NumericValue(p.getType().getTicketCount() - ((PointOfInterest_scarpetMixin)p).getFreeTickets()),
                            ListValue.of(new NumericValue(p.getPos().getX()), new NumericValue(p.getPos().getY()), new NumericValue(p.getPos().getZ()))
                    )
            ).collect(Collectors.toList()));

            return (c_, t_) -> ret;
        });

        //poi_set(pos, null) poi_set(pos, type, occupied?,
        this.expr.addLazyFunction("set_poi", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            if (lv.size() == 0) throw new InternalExpressionException("'set_poi' requires at least one parameter");
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0, false);
            BlockPos pos = locator.block.getPos();
            if (lv.size() < locator.offset) throw new InternalExpressionException("'set_poi' requires the new poi type or null, after position argument");
            Value poi = lv.get(locator.offset+0).evalValue(c);
            PointOfInterestStorage store = cc.s.getWorld().getPointOfInterestStorage();
            if (poi == Value.NULL)
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
                store.get((tt) -> tt==type, pos, 1, PointOfInterestStorage.OccupationStatus.ANY
                ).filter(p -> p.getPos().equals(pos)).findFirst().ifPresent(p -> {
                    for (int i=0; i < finalO; i++) ((PointOfInterest_scarpetMixin)p).callReserveTicket();
                });
            }
            return LazyValue.TRUE;
        });


        this.expr.addLazyFunction("pos", 1, (c, t, lv) ->
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

        this.expr.addLazyFunction("pos_offset", -1, (c, t, lv) ->
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

        this.expr.addLazyFunction("solid", -1, (c, t, lv) ->
                genericStateTest(c, "solid", lv, (s, p, w) -> new NumericValue(s.isSimpleFullBlock(w, p))));

        this.expr.addLazyFunction("air", -1, (c, t, lv) ->
                booleanStateTest(c, "air", lv, (s, p) -> s.isAir()));

        this.expr.addLazyFunction("liquid", -1, (c, t, lv) ->
                booleanStateTest(c, "liquid", lv, (s, p) -> !s.getFluidState().isEmpty()));

        this.expr.addLazyFunction("flammable", -1, (c, t, lv) ->
                booleanStateTest(c, "flammable", lv, (s, p) -> s.getMaterial().isBurnable()));

        this.expr.addLazyFunction("transparent", -1, (c, t, lv) ->
                booleanStateTest(c, "transparent", lv, (s, p) -> !s.getMaterial().isSolid()));

        /*this.expr.addLazyFunction("opacity", -1, (c, t, lv) ->
                genericStateTest(c, "opacity", lv, (s, p, w) -> new NumericValue(s.getOpacity(w, p))));

        this.expr.addLazyFunction("blocks_daylight", -1, (c, t, lv) ->
                genericStateTest(c, "blocks_daylight", lv, (s, p, w) -> new NumericValue(s.propagatesSkylightDown(w, p))));*/ // investigate

        this.expr.addLazyFunction("emitted_light", -1, (c, t, lv) ->
                genericStateTest(c, "emitted_light", lv, (s, p, w) -> new NumericValue(s.getLuminance())));

        this.expr.addLazyFunction("light", -1, (c, t, lv) ->
                genericStateTest(c, "light", lv, (s, p, w) -> new NumericValue(Math.max(w.getLightLevel(LightType.BLOCK, p), w.getLightLevel(LightType.SKY, p)))));

        this.expr.addLazyFunction("block_light", -1, (c, t, lv) ->
                genericStateTest(c, "block_light", lv, (s, p, w) -> new NumericValue(w.getLightLevel(LightType.BLOCK, p))));

        this.expr.addLazyFunction("sky_light", -1, (c, t, lv) ->
                genericStateTest(c, "sky_light", lv, (s, p, w) -> new NumericValue(w.getLightLevel(LightType.SKY, p))));

        this.expr.addLazyFunction("see_sky", -1, (c, t, lv) ->
                genericStateTest(c, "see_sky", lv, (s, p, w) -> new NumericValue(w.isSkyVisible(p))));

        this.expr.addLazyFunction("brightness", -1, (c, t, lv) ->
                genericStateTest(c, "brightness", lv, (s, p, w) -> new NumericValue(w.getBrightness(p))));

        this.expr.addLazyFunction("hardness", -1, (c, t, lv) ->
                genericStateTest(c, "hardness", lv, (s, p, w) -> new NumericValue(s.getHardness(w, p))));

        this.expr.addLazyFunction("blast_resistance", -1, (c, t, lv) ->
                genericStateTest(c, "blast_resistance", lv, (s, p, w) -> new NumericValue(s.getBlock().getBlastResistance())));

        this.expr.addLazyFunction("in_slime_chunk", -1, (c, t, lv) ->
        {
            BlockPos pos = BlockArgument.findIn((CarpetContext)c, lv, 0).block.getPos();
            ChunkPos chunkPos = new ChunkPos(pos);
            Value ret = new NumericValue(ChunkRandom.create(
                    chunkPos.x, chunkPos.z,
                    ((CarpetContext)c).s.getWorld().getSeed(),
                    987234911L
            ).nextInt(10) == 0);
            return (_c, _t) -> ret;
        });


        this.expr.addLazyFunction("top", -1, (c, t, lv) ->
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

        this.expr.addLazyFunction("loaded", -1, (c, t, lv) ->
        {
            Value retval = ((CarpetContext) c).s.getWorld().isChunkLoaded(BlockArgument.findIn((CarpetContext) c, lv, 0).block.getPos()) ? Value.TRUE : Value.FALSE;
            return (c_, t_) -> retval;
        });

        // Deprecated, use loaded_status as more indicative
        this.expr.addLazyFunction("loaded_ep", -1, (c, t, lv) ->
        {
            BlockPos pos = BlockArgument.findIn((CarpetContext)c, lv, 0).block.getPos();
            Value retval = ((CarpetContext)c).s.getWorld().getChunkManager().shouldTickChunk(new ChunkPos(pos))?Value.TRUE : Value.FALSE;
            return (c_, t_) -> retval;
        });

        this.expr.addLazyFunction("loaded_status", -1, (c, t, lv) ->
        {
            BlockPos pos = BlockArgument.findIn((CarpetContext)c, lv, 0).block.getPos();
            WorldChunk chunk = ((CarpetContext)c).s.getWorld().getChunkManager().getWorldChunk(pos.getX()>>4, pos.getZ()>>4, false);
            if (chunk == null)
                return LazyValue.ZERO;
            Value retval = new NumericValue(chunk.getLevelType().ordinal());
            return (c_, t_) -> retval;
        });

        this.expr.addLazyFunction("is_chunk_generated", -1, (c, t, lv) ->
        {
            BlockArgument locator = BlockArgument.findIn((CarpetContext)c, lv, 0);
            BlockPos pos = locator.block.getPos();
            boolean force = false;
            if (lv.size() > locator.offset)
                force = lv.get(locator.offset).evalValue(c, Context.BOOLEAN).getBoolean();
            Value retval = new NumericValue(canHasChunk(((CarpetContext)c).s.getWorld(), new ChunkPos(pos), null, force));
            return (c_, t_) -> retval;
        });

        this.expr.addLazyFunction("generation_status", -1, (c, t, lv) ->
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

        this.expr.addLazyFunction("chunk_tickets", -1, (c, t, lv) ->
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

        this.expr.addLazyFunction("suffocates", -1, (c, t, lv) ->
                genericStateTest(c, "suffocates", lv, (s, p, w) -> new NumericValue(s.canSuffocate(w, p))));

        this.expr.addLazyFunction("power", -1, (c, t, lv) ->
                genericStateTest(c, "power", lv, (s, p, w) -> new NumericValue(w.getReceivedRedstonePower(p))));

        this.expr.addLazyFunction("ticks_randomly", -1, (c, t, lv) ->
                booleanStateTest(c, "ticks_randomly", lv, (s, p) -> s.hasRandomTicks()));

        this.expr.addLazyFunction("update", -1, (c, t, lv) ->
                booleanStateTest(c, "update", lv, (s, p) ->
                {
                    ((CarpetContext) c).s.getWorld().updateNeighbor(p, s.getBlock(), p);
                    return true;
                }));

        this.expr.addLazyFunction("block_tick", -1, (c, t, lv) ->
                booleanStateTest(c, "block_tick", lv, (s, p) ->
                {
                    ServerWorld w = ((CarpetContext)c).s.getWorld();
                    s.randomTick(w, p, w.random);
                    return true;
                }));

        this.expr.addLazyFunction("random_tick", -1, (c, t, lv) ->
                booleanStateTest(c, "random_tick", lv, (s, p) ->
                {
                    ServerWorld w = ((CarpetContext)c).s.getWorld();
                    if (s.hasRandomTicks() || s.getFluidState().hasRandomTicks())
                        s.randomTick(w, p, w.random);
                    return true;
                }));

        this.expr.addLazyFunction("without_updates", 1, (c, t, lv) ->
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

        this.expr.addLazyFunction("set", -1, (c, t, lv) ->
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
            cc.s.getMinecraftServer().submitAndJoin( () ->
            {
                Clearable.clear(world.getBlockEntity(targetPos));
                world.setBlockState(targetPos, finalSourceBlockState, 2);
                if (finalData != null)
                {
                    BlockEntity be = world.getBlockEntity(targetPos);
                    if (be != null)
                    {
                        CompoundTag destTag = finalData.copy();
                        destTag.putInt("x", targetPos.getX());
                        destTag.putInt("y", targetPos.getY());
                        destTag.putInt("z", targetPos.getZ());
                        be.fromTag(destTag);
                        be.markDirty();
                    }
                }
            });
            Value retval = new BlockValue(finalSourceBlockState, world, targetLocator.block.getPos());
            return (c_, t_) -> retval;
        });

        this.expr.addLazyFunction("destroy", -1, (c, t, lv) ->
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
                    NBTSerializableValue readTag = NBTSerializableValue.parseString(tagValue.getString());
                    if (readTag == null)
                        throw new InternalExpressionException("Incorrect tag: "+tagValue.getString());
                    tag = readTag.getCompoundTag();
                }
            }

            ItemStack tool = new ItemStack(item, 1);
            if (tag != null)
                tool.setTag(tag);
            if (playerBreak && state.getHardness(world, where) < 0.0) return LazyValue.FALSE;
            boolean removed = world.removeBlock(where, false);
            if (!removed) return LazyValue.FALSE;
            world.playLevelEvent(null, 2001, where, Block.getRawIdFromState(state));

            boolean toolBroke = false;
            boolean dropLoot = true;
            if (playerBreak)
            {
                boolean isUsingEffectiveTool = state.getMaterial().canBreakByHand() || tool.isEffectiveOn(state);
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

        this.expr.addLazyFunction("harvest", -1, (c, t, lv) ->
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
            if (!((block instanceof BedrockBlock || block instanceof BarrierBlock) && player.interactionManager.isSurvivalLike()))
                success = tryBreakBlock_copy_from_ServerPlayerInteractionManager(player, where);
            if (success)
                world.playLevelEvent(null, 2001, where, Block.getRawIdFromState(state));
            return success ? LazyValue.TRUE : LazyValue.FALSE;
        });

        // TODO rename to use_item
        this.expr.addLazyFunction("place_item", -1, (c, t, lv) ->
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

        this.expr.addLazyFunction("blocks_movement", -1, (c, t, lv) ->
                booleanStateTest(c, "blocks_movement", lv, (s, p) ->
                        !s.canPlaceAtSide(((CarpetContext) c).s.getWorld(), p, BlockPlacementEnvironment.LAND)));

        this.expr.addLazyFunction("block_sound", -1, (c, t, lv) ->
                stateStringQuery(c, "block_sound", lv, (s, p) ->
                        BlockInfo.soundName.get(s.getSoundGroup())));

        this.expr.addLazyFunction("material", -1, (c, t, lv) ->
                stateStringQuery(c, "material", lv, (s, p) ->
                        BlockInfo.materialName.get(s.getMaterial())));

        this.expr.addLazyFunction("map_colour", -1, (c, t, lv) ->
                stateStringQuery(c, "map_colour", lv, (s, p) ->
                        BlockInfo.mapColourName.get(s.getTopMaterialColor(((CarpetContext)c).s.getWorld(), p))));

        this.expr.addLazyFunction("property", -1, (c, t, lv) ->
        {
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

        this.expr.addLazyFunction("block_properties", -1, (c, t, lv) ->
        {
            BlockArgument locator = BlockArgument.findIn((CarpetContext) c, lv, 0);
            BlockState state = locator.block.getBlockState();
            StateManager<Block, BlockState> states = state.getBlock().getStateManager();
            Value res = ListValue.wrap(states.getProperties().stream().map(
                    p -> new StringValue(p.getName())).collect(Collectors.toList())
            );
            return (_c, _t) -> res;
        });

        this.expr.addLazyFunction("biome", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            ServerWorld world = cc.s.getWorld();
            BlockPos pos = locator.block.getPos();
            Biome biome = world.getBiome(pos);
            Value res = new StringValue(biome.getTranslationKey().replaceFirst("^biome\\.minecraft\\.", ""));
            return (_c, _t) -> res;
        });

        this.expr.addLazyFunction("set_biome", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            if (lv.size() == locator.offset)
                throw new InternalExpressionException("'set_biome' needs a biome name as an argument");
            String biomeName = lv.get(locator.offset+0).evalValue(c).getString();
            Biome biome = Registry.BIOME.get(new Identifier(biomeName));
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
            if (doImmediateUpdate) forceChunkUpdate(pos, world);
            return LazyValue.TRUE;
        });

        this.expr.addLazyFunction("reload_chunk", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            BlockPos pos = BlockArgument.findIn(cc, lv, 0).block.getPos();
            ServerWorld world = cc.s.getWorld();
            cc.s.getMinecraftServer().submitAndJoin( () -> forceChunkUpdate(pos, world));
            return LazyValue.TRUE;
        });

        this.expr.addLazyFunction("structure_references", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            ServerWorld world = cc.s.getWorld();
            BlockPos pos = locator.block.getPos();
            Map<String, LongSet> references = world.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.STRUCTURE_REFERENCES).getStructureReferences();
            if (lv.size() == locator.offset)
            {
                List<Value> referenceList = references.entrySet().stream().
                        filter(e -> e.getValue()!= null && !e.getValue().isEmpty()).
                        map(e -> new StringValue(FeatureGenerator.structureToFeature.get(e.getKey()).get(0))).collect(Collectors.toList());
                return (_c, _t ) -> ListValue.wrap(referenceList);
            }
            String simpleStructureName = lv.get(locator.offset).evalValue(c).getString().toLowerCase(Locale.ROOT);
            String structureName = FeatureGenerator.featureToStructure.get(simpleStructureName);
            if (structureName == null) return LazyValue.NULL;
            LongSet structureReferences = references.get(structureName);
            if (structureReferences == null || structureReferences.isEmpty()) return ListValue.lazyEmpty();
            Value ret = ListValue.wrap(structureReferences.stream().map(l -> ListValue.of(
                    new NumericValue(16*ChunkPos.getPackedX(l)),
                    Value.ZERO,
                    new NumericValue(16*ChunkPos.getPackedZ(l)))).collect(Collectors.toList()));
            return (_c, _t) -> ret;
        });

        this.expr.addLazyFunction("structure_eligibility", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);

            ServerWorld world = cc.s.getWorld();

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
                    String structureName = FeatureGenerator.featureToStructure.get(reqString);
                    if (structureName == null) throw new InternalExpressionException("Unknown structure: " + reqString);
                    structure = Feature.STRUCTURES.get(structureName.toLowerCase(Locale.ROOT));
                    if (structure == null) throw new InternalExpressionException("Unsupported structure: " + structureName);
                }
                if (lv.size() > locator.offset+1)
                {
                    needSize = lv.get(locator.offset+1).evalValue(c).getBoolean();
                }
            }
            if (structure != null)
            {
                StructureStart start = FeatureGenerator.shouldStructureStartAt(world, pos, structure, needSize);
                if (start == null) return LazyValue.NULL;
                if (!needSize) return LazyValue.TRUE;
                Value ret = structureToValue(start);
                return (_c, _t) -> ret;
            }
            Map<Value, Value> ret = new HashMap<>();
            for(StructureFeature<?> str : Feature.STRUCTURES.values())
            {
                StructureStart start = FeatureGenerator.shouldStructureStartAt(world, pos, str, needSize);
                if (start == null) continue;

                Value key = new StringValue(FeatureGenerator.structureToFeature.get(str.getName()).get(0));
                ret.put(key, (!needSize)?Value.NULL:structureToValue(start));
            }
            Value retMap = MapValue.wrap(ret);
            return (_c, _t) -> retMap;
        });

        this.expr.addLazyFunction("structures", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);

            ServerWorld world = cc.s.getWorld();
            BlockPos pos = locator.block.getPos();
            Map<String, StructureStart> structures = world.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS).getStructureStarts();
            if (lv.size() == locator.offset)
            {
                Map<Value, Value> structureList = new HashMap<>();
                for (Map.Entry<String, StructureStart> entry : structures.entrySet())
                {
                    StructureStart start = entry.getValue();
                    if (start == StructureStart.DEFAULT)
                        continue;
                    BlockBox box = start.getBoundingBox();
                    structureList.put(
                            new StringValue(FeatureGenerator.structureToFeature.get(entry.getKey()).get(0)),
                            ListValue.of(ListValue.fromTriple(box.minX, box.minY, box.minZ), ListValue.fromTriple(box.maxX, box.maxY, box.maxZ))
                    );
                }
                Value ret = MapValue.wrap(structureList);
                return (_c, _t) -> ret;
            }
            String structureName = lv.get(locator.offset).evalValue(c).getString().toLowerCase(Locale.ROOT);
            StructureStart start = structures.get(FeatureGenerator.featureToStructure.get(structureName));
            Value ret = structureToValue(start);
            return (_c, _t) -> ret;
        });

        this.expr.addLazyFunction("set_structure", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);

            ServerWorld world = cc.s.getWorld();
            BlockPos pos = locator.block.getPos();

            if (lv.size() == locator.offset)
                throw new InternalExpressionException("'set_structure requires at least position and a structure name");
            String structureName = lv.get(locator.offset).evalValue(c).getString().toLowerCase(Locale.ROOT);
            String structureId = FeatureGenerator.featureToStructure.get(structureName);
            if (structureId == null) throw new InternalExpressionException("Unknown structure: "+structureName);
            // good 'ol pointer
            Value[] result = new Value[]{Value.NULL};
            // technically a world modification. Even if we could let it slide, we will still park it
            ((CarpetContext) c).s.getMinecraftServer().submitAndJoin(() ->
            {
                Map<String, StructureStart> structures = world.getChunk(pos).getStructureStarts();
                if (lv.size() == locator.offset + 1)
                {
                    Boolean res = FeatureGenerator.gridStructure(structureName, ((CarpetContext) c).s.getWorld(), locator.block.getPos());
                    if (res == null) return;
                    result[0] = res?Value.TRUE:Value.FALSE;
                    return;
                }
                Value newValue = lv.get(locator.offset+1).evalValue(c);
                if (newValue instanceof NullValue) // remove structure
                {
                    if (!structures.containsKey(structureId))
                    {
                        return;
                    }
                    StructureStart start = structures.get(structureId);
                    ChunkPos structureChunkPos = new ChunkPos(start.getChunkX(), start.getChunkZ());
                    BlockBox box = start.getBoundingBox();
                    for (int chx = box.minX / 16; chx <= box.maxX / 16; chx++)
                    {
                        for (int chz = box.minZ / 16; chz <= box.maxZ / 16; chz++)
                        {
                            ChunkPos chpos = new ChunkPos(chx, chz);
                            // getting a chunk will convert it to full, allowing to modify references
                            Map<String, LongSet> references = world.getChunk(chpos.getCenterBlockPos()).getStructureReferences();
                            if (references.containsKey(structureId) && references.get(structureId) != null)
                                references.get(structureId).remove(structureChunkPos.toLong());
                        }
                    }
                    structures.remove(structureId);
                    result[0] = Value.TRUE;
                }
            });
            Value ret = result[0]; // preventing from lazy evaluating of the result in case a future completes later
            return (_c, _t) -> ret;
        });

        this.expr.addLazyFunction("reset_chunk", -1, (c, t, lv) ->
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
                        forceChunkUpdate(chpos.getCenterBlockPos(), world);
                    }
                }
                result[0] = MapValue.wrap(report.entrySet().stream().collect(Collectors.toMap(
                        e -> new StringValue((String)((Map.Entry) e).getKey()),
                        e ->  new NumericValue((Integer)((Map.Entry) e).getValue())
                )));
            });
            return (_c, _t) -> result[0];
        });

        this.expr.addLazyFunction("inhabited_time", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            BlockPos pos = locator.block.getPos();
            Value ret = new NumericValue(cc.s.getWorld().getChunk(pos).getInhabitedTime());
            return (_c, _t) -> ret;
        });

        this.expr.addLazyFunction("add_chunk_ticket", -1, (c, t, lv) ->
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
            if (ticket == ChunkTicketType.PORTAL)
                cc.s.getWorld().getChunkManager().addTicket(ChunkTicketType.PORTAL, target, radius, pos);
            else if (ticket == ChunkTicketType.POST_TELEPORT)
                cc.s.getWorld().getChunkManager().addTicket(ChunkTicketType.POST_TELEPORT, target, radius, 1);
            else
                cc.s.getWorld().getChunkManager().addTicket(ChunkTicketType.UNKNOWN, target, radius, target);
            Value ret = new NumericValue(ticket.getExpiryTicks());
            return (_c, _t) -> ret;
        });

    }
    private Map<String, ChunkTicketType<?>> ticketTypes = new HashMap<String, ChunkTicketType<?>>(){{
        put("portal", ChunkTicketType.PORTAL);
        put("teleport", ChunkTicketType.POST_TELEPORT);
        put("unknown", ChunkTicketType.UNKNOWN);
    }};

    private static String getScoreboardKeyFromValue(Value keyValue)
    {
        if (keyValue instanceof EntityValue)
        {
            Entity e = ((EntityValue) keyValue).getEntity();
            if (e instanceof PlayerEntity)
            {
                return e.getName().getString();
            }
            else
            {
                return e.getUuidAsString();
            }
        }
        else
        {
            return keyValue.getString();
        }
    }

    private void API_Scoreboard()
    {
        // scoreboard(player,'objective')
        // scoreboard(player, objective, newValue)
        this.expr.addLazyFunction("scoreboard", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            Scoreboard scoreboard =  cc.s.getMinecraftServer().getScoreboard();
            if (lv.size()==0)
            {
                Value ret = ListValue.wrap(scoreboard.getObjectiveNames().stream().map(StringValue::new).collect(Collectors.toList()));
                return (_c, _t) -> ret;
            }
            String objectiveName = lv.get(0).evalValue(c).getString();
            ScoreboardObjective objective = scoreboard.getObjective(objectiveName);
            if (objective == null) throw new InternalExpressionException("Unknown objective: "+objectiveName);
            if (lv.size()==1)
            {
                Value ret = ListValue.wrap(scoreboard.getAllPlayerScores(objective).stream().map(s -> new StringValue(s.getPlayerName())).collect(Collectors.toList()));
                return (_c, _t) -> ret;
            }
            String key = getScoreboardKeyFromValue(lv.get(1).evalValue(c));
            if (!scoreboard.playerHasObjective(key, objective) && lv.size()==2)
                return LazyValue.NULL;
            ScoreboardPlayerScore scoreboardPlayerScore = scoreboard.getPlayerScore(key, objective);
            Value retval = new NumericValue(scoreboardPlayerScore.getScore());
            if (lv.size() > 2)
            {
                scoreboardPlayerScore.setScore(NumericValue.asNumber(lv.get(2).evalValue(c)).getInt());
            }
            return (_c, _t) -> retval;
        });

        this.expr.addLazyFunction("scoreboard_remove", -1, (c, t, lv)-> {
            if (lv.size()==0) throw new InternalExpressionException("'scoreboard_remove' requires at least one parameter");
            CarpetContext cc = (CarpetContext)c;
            Scoreboard scoreboard =  cc.s.getMinecraftServer().getScoreboard();
            String objectiveName = lv.get(0).evalValue(c).getString();
            ScoreboardObjective objective = scoreboard.getObjective(objectiveName);
            if (objective == null)
                return LazyValue.FALSE;
            if (lv.size() == 1)
            {
                scoreboard.removeObjective(objective);
                return LazyValue.TRUE;
            }
            String key = getScoreboardKeyFromValue(lv.get(1).evalValue(c));
            if (!scoreboard.playerHasObjective(key, objective)) return LazyValue.NULL;
            ScoreboardPlayerScore scoreboardPlayerScore = scoreboard.getPlayerScore(key, objective);
            Value previous = new NumericValue(scoreboardPlayerScore.getScore());
            scoreboard.resetPlayerScore(key, objective);
            return (c_, t_) -> previous;
        });

        // objective_add('lvl','level')
        // objective_add('counter')

        this.expr.addLazyFunction("scoreboard_add", -1, (c, t, lv)-> {
            CarpetContext cc = (CarpetContext)c;
            Scoreboard scoreboard =  cc.s.getMinecraftServer().getScoreboard();
            if (lv.size() == 0 || lv.size()>2) throw new InternalExpressionException("'scoreboard_add' should have one or two parameters");
            String objectiveName = lv.get(0).evalValue(c).getString();
            ScoreboardCriterion criterion;
            if (lv.size() == 1 )
            {
                criterion = ScoreboardCriterion.DUMMY;
            }
            else
            {
                String critetionName = lv.get(1).evalValue(c).getString();
                criterion = ScoreboardCriterion.createStatCriterion(critetionName).orElse(null);
                if (criterion==null)
                {
                    throw new InternalExpressionException("Unknown scoreboard criterion: "+critetionName);
                }
            }

            ScoreboardObjective objective = scoreboard.getObjective(objectiveName);
            if (objective != null)
                return LazyValue.FALSE;

            scoreboard.addObjective(objectiveName, criterion, new LiteralText(objectiveName), criterion.getCriterionType());
            return LazyValue.TRUE;
        });

        this.expr.addLazyFunction("scoreboard_display", 2, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            Scoreboard scoreboard =  cc.s.getMinecraftServer().getScoreboard();
            String location = lv.get(0).evalValue(c).getString();
            int slot = Scoreboard.getDisplaySlotId(location);
            if (slot < 0) throw new InternalExpressionException("Invalid objective slot: "+location);
            Value target = lv.get(1).evalValue(c);
            if (target instanceof NullValue)
            {
                scoreboard.setObjectiveSlot(slot, null);
                return (_c, _t) -> new NumericValue(slot);
            }
            String objectiveString = target.getString();
            ScoreboardObjective objective = scoreboard.getObjective(objectiveString);
            if (objective == null) throw new InternalExpressionException("Objective doesn't exist: "+objectiveString);
            scoreboard.setObjectiveSlot(slot, objective);
            return (_c, _t) -> new NumericValue(slot);
        });
    }

    private void API_InventoryManipulation()
    {
        this.expr.addLazyFunction("stack_limit", 1, (c, t, lv) ->
        {
            ItemStackArgument item = NBTSerializableValue.parseItem(lv.get(0).evalValue(c).getString());
            Value res = new NumericValue(item.getItem().getMaxCount());
            return (_c, _t) -> res;
        });

        this.expr.addLazyFunction("item_category", 1, (c, t, lv) ->
        {
            ItemStackArgument item = NBTSerializableValue.parseItem(lv.get(0).evalValue(c).getString());
            Value res = new StringValue(item.getItem().getGroup().getName());
            return (_c, _t) -> res;
        });

        this.expr.addLazyFunction("recipe_data", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            if (lv.size() < 1) throw new InternalExpressionException("'recipe_data' requires at least one argument");
            String recipeName = lv.get(0).evalValue(c).getString();
            RecipeType type = RecipeType.CRAFTING;
            if (lv.size() > 1)
            {
                String recipeType = lv.get(1).evalValue(c).getString();
                try
                {
                    type = Registry.RECIPE_TYPE.get(new Identifier(recipeType));
                }
                catch (InvalidIdentifierException ignored)
                {
                    throw new InternalExpressionException("Unknown crafting category: " + recipeType);
                }
            }
            List<Recipe<?>> recipes;
            try
            {
                recipes = ((RecipeManagerInterface) cc.s.getMinecraftServer().getRecipeManager()).getAllMatching(type, new Identifier(recipeName));
            }
            catch (InvalidIdentifierException ignored)
            {
                return LazyValue.NULL;
            }
            if (recipes.isEmpty())
                return LazyValue.NULL;
            List<Value> recipesOutput = new ArrayList<>();
            for (Recipe<?> recipe: recipes)
            {
                ItemStack result = recipe.getOutput();
                List<Value> ingredientValue = new ArrayList<>();
                recipe.getPreviewInputs().forEach(
                        ingredient ->
                        {
                            // I am flattening ingredient lists per slot.
                            // consider recipe_data('wooden_sword','crafting') and ('iron_nugget', 'blasting') and notice difference
                            // in depths of lists.
                            List<Collection<ItemStack>> stacks = ((IngredientInterface) (Object) ingredient).getRecipeStacks();
                            if (stacks.isEmpty())
                            {
                                ingredientValue.add(Value.NULL);
                            }
                            else
                            {
                                List<Value> alternatives = new ArrayList<>();
                                stacks.forEach(col -> col.stream().map(ListValue::fromItemStack).forEach(alternatives::add));
                                ingredientValue.add(ListValue.wrap(alternatives));
                            }
                        }
                );
                Value recipeSpec;
                if (recipe instanceof ShapedRecipe)
                {
                    recipeSpec = ListValue.of(
                            new StringValue("shaped"),
                            new NumericValue(((ShapedRecipe) recipe).getWidth()),
                            new NumericValue(((ShapedRecipe) recipe).getHeight())
                    );
                }
                else if (recipe instanceof ShapelessRecipe)
                {
                    recipeSpec = ListValue.of(new StringValue("shapeless"));
                }
                else if (recipe instanceof AbstractCookingRecipe)
                {
                    recipeSpec = ListValue.of(
                            new StringValue("smelting"),
                            new NumericValue(((AbstractCookingRecipe) recipe).getCookTime()),
                            new NumericValue(((AbstractCookingRecipe) recipe).getExperience())
                    );
                }
                else if (recipe instanceof CuttingRecipe)
                {
                    recipeSpec = ListValue.of(new StringValue("cutting"));
                }
                else if (recipe instanceof SpecialCraftingRecipe)
                {
                    recipeSpec = ListValue.of(new StringValue("special"));
                }
                else
                {
                    recipeSpec = ListValue.of(new StringValue("custom"));
                }

                recipesOutput.add(ListValue.of(ListValue.fromItemStack(result), ListValue.wrap(ingredientValue), recipeSpec));
            }
            Value ret = ListValue.wrap(recipesOutput);
            return (_c, _t) -> ret;
        });

        this.expr.addLazyFunction("crafting_remaining_item", 1, (c, t, lv) ->
        {
            String itemStr = lv.get(0).evalValue(c).getString();
            Item item;
            try
            {
                Identifier id = new Identifier(itemStr);
                item = Registry.ITEM.get(id);
                if (item == Items.AIR && !id.getPath().equalsIgnoreCase("air"))
                    throw new InvalidIdentifierException("boo");
            }
            catch (InvalidIdentifierException ignored)
            {
                throw new InternalExpressionException("Incorrect item: "+itemStr);
            }
            if (!item.hasRecipeRemainder()) return LazyValue.NULL;
            Value ret = new StringValue(NBTSerializableValue.nameFromRegistryId(Registry.ITEM.getId(item.getRecipeRemainder())));
            return (_c, _t ) -> ret;
        });


        this.expr.addLazyFunction("inventory_size", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null)
                return (_c, _t) -> Value.NULL;
            Value res = new NumericValue(inventoryLocator.inventory.getInvSize());
            return (_c, _t) -> res;
        });

        this.expr.addLazyFunction("inventory_has_items", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null)
                return (_c, _t) -> Value.NULL;
            Value res = new NumericValue(!inventoryLocator.inventory.isInvEmpty());
            return (_c, _t) -> res;
        });

        //inventory_get(<b, e>, <n>) -> item_triple
        this.expr.addLazyFunction("inventory_get", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null)
                return (_c, _t) -> Value.NULL;
            if (lv.size() == inventoryLocator.offset)
            {
                List<Value> fullInventory = new ArrayList<>();
                for (int i = 0, maxi = inventoryLocator.inventory.getInvSize(); i < maxi; i++)
                {
                    fullInventory.add(ListValue.fromItemStack(inventoryLocator.inventory.getInvStack(i)));
                }
                Value res = ListValue.wrap(fullInventory);
                return (_c, _t) -> res;
            }
            int slot = (int)NumericValue.asNumber(lv.get(inventoryLocator.offset).evalValue(c)).getLong();
            slot = NBTSerializableValue.validateSlot(slot, inventoryLocator.inventory);
            if (slot == inventoryLocator.inventory.getInvSize())
                return (_c, _t) -> Value.NULL;
            Value res = ListValue.fromItemStack(inventoryLocator.inventory.getInvStack(slot));
            return (_c, _t) -> res;
        });

        //inventory_set(<b,e>, <n>, <count>, <item>, <nbt>)
        this.expr.addLazyFunction("inventory_set", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null)
                return (_c, _t) -> Value.NULL;
            if (lv.size() < inventoryLocator.offset+2)
                throw new InternalExpressionException("'inventory_set' requires at least slot number and new stack size, and optional new item");
            int slot = (int) NumericValue.asNumber(lv.get(inventoryLocator.offset+0).evalValue(c)).getLong();
            slot = NBTSerializableValue.validateSlot(slot, inventoryLocator.inventory);
            if (slot == inventoryLocator.inventory.getInvSize())
                return (_c, _t) -> Value.NULL;
            int count = (int) NumericValue.asNumber(lv.get(inventoryLocator.offset+1).evalValue(c)).getLong();
            if (count == 0)
            {
                // clear slot
                ItemStack removedStack = inventoryLocator.inventory.removeInvStack(slot);
                syncPlayerInventory(inventoryLocator, slot);
                //Value res = ListValue.fromItemStack(removedStack); // that tuple will be read only but cheaper if noone cares
                return (_c, _t) -> ListValue.fromItemStack(removedStack);
            }
            if (lv.size() < inventoryLocator.offset+3)
            {
                ItemStack previousStack = inventoryLocator.inventory.getInvStack(slot);
                ItemStack newStack = previousStack.copy();
                newStack.setCount(count);
                inventoryLocator.inventory.setInvStack(slot, newStack);
                syncPlayerInventory(inventoryLocator, slot);
                return (_c, _t) -> ListValue.fromItemStack(previousStack);
            }
            CompoundTag nbt = null; // skipping one argument
            if (lv.size() > inventoryLocator.offset+3)
            {
                Value nbtValue = lv.get(inventoryLocator.offset+3).evalValue(c);
                if (nbtValue instanceof NBTSerializableValue)
                    nbt = ((NBTSerializableValue)nbtValue).getCompoundTag();
                else if (nbtValue instanceof NullValue)
                    nbt = null;
                else
                    nbt = new NBTSerializableValue(nbtValue.getString()).getCompoundTag();
            }
            ItemStackArgument newitem = NBTSerializableValue.parseItem(
                    lv.get(inventoryLocator.offset+2).evalValue(c).getString(),
                    nbt
            );

            ItemStack previousStack = inventoryLocator.inventory.getInvStack(slot);
            try
            {
                inventoryLocator.inventory.setInvStack(slot, newitem.createStack(count, false));
                syncPlayerInventory(inventoryLocator, slot);
            }
            catch (CommandSyntaxException e)
            {
                throw new InternalExpressionException(e.getMessage());
            }
            return (_c, _t) -> ListValue.fromItemStack(previousStack);
        });

        //inventory_find(<b, e>, <item> or null (first empty slot), <start_from=0> ) -> <N> or null
        this.expr.addLazyFunction("inventory_find", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null)
                return (_c, _t) -> Value.NULL;
            ItemStackArgument itemArg = null;
            if (lv.size() > inventoryLocator.offset)
            {
                Value secondArg = lv.get(inventoryLocator.offset+0).evalValue(c);
                if (!(secondArg instanceof NullValue))
                    itemArg = NBTSerializableValue.parseItem(secondArg.getString());
            }
            int startIndex = 0;
            if (lv.size() > inventoryLocator.offset+1)
            {
                startIndex = (int) NumericValue.asNumber(lv.get(inventoryLocator.offset+1).evalValue(c)).getLong();
            }
            startIndex = NBTSerializableValue.validateSlot(startIndex, inventoryLocator.inventory);
            for (int i = startIndex, maxi = inventoryLocator.inventory.getInvSize(); i < maxi; i++)
            {
                ItemStack stack = inventoryLocator.inventory.getInvStack(i);
                if ( (itemArg == null && stack.isEmpty()) || (itemArg != null && itemArg.getItem().equals(stack.getItem())) )
                {
                    Value res = new NumericValue(i);
                    return (_c, _t) -> res;
                }
            }
            return (_c, _t) -> Value.NULL;
        });



        //inventory_remove(<b, e>, <item>, <amount=1>) -> bool

        this.expr.addLazyFunction("inventory_remove", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null)
                return (_c, _t) -> Value.NULL;
            if (lv.size() <= inventoryLocator.offset)
                throw new InternalExpressionException("'inventory_remove' requires at least an item to be removed");
            ItemStackArgument searchItem = NBTSerializableValue.parseItem(lv.get(inventoryLocator.offset).evalValue(c).getString());
            int amount = 1;
            if (lv.size() > inventoryLocator.offset+1)
            {
                amount = (int)NumericValue.asNumber(lv.get(inventoryLocator.offset+1).evalValue(c)).getLong();
            }
            // not enough
            if (((amount == 1) && (!inventoryLocator.inventory.containsAnyInInv(Sets.newHashSet(searchItem.getItem()))))
                    || (inventoryLocator.inventory.countInInv(searchItem.getItem()) < amount))
            {
                return (_c, _t) -> Value.FALSE;
            }
            for (int i = 0, maxi = inventoryLocator.inventory.getInvSize(); i < maxi; i++)
            {
                ItemStack stack = inventoryLocator.inventory.getInvStack(i);
                if (stack.isEmpty())
                    continue;
                if (!stack.getItem().equals(searchItem.getItem()))
                    continue;
                int left = stack.getCount()-amount;
                if (left > 0)
                {
                    stack.setCount(left);
                    inventoryLocator.inventory.setInvStack(i, stack);
                    syncPlayerInventory(inventoryLocator, i);
                    return (_c, _t) -> Value.TRUE;
                }
                else
                {
                    inventoryLocator.inventory.removeInvStack(i);
                    syncPlayerInventory(inventoryLocator, i);
                    amount -= stack.getCount();
                }
            }
            if (amount > 0)
                throw new InternalExpressionException("Something bad happened - cannot pull all items from inventory");
            return (_c, _t) -> Value.TRUE;
        });

        //inventory_drop(<b, e>, <n>, <amount=1, 0-whatever's there>) -> entity_item (and sets slot) or null if cannot
        this.expr.addLazyFunction("drop_item", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null)
                return (_c, _t) -> Value.NULL;
            if (lv.size() == inventoryLocator.offset)
                throw new InternalExpressionException("Slot number is required for inventory_drop");
            int slot = (int)NumericValue.asNumber(lv.get(inventoryLocator.offset).evalValue(c)).getLong();
            slot = NBTSerializableValue.validateSlot(slot, inventoryLocator.inventory);
            if (slot == inventoryLocator.inventory.getInvSize())
                return (_c, _t) -> Value.NULL;
            int amount = 0;
            if (lv.size() > inventoryLocator.offset+1)
                amount = (int)NumericValue.asNumber(lv.get(inventoryLocator.offset+1).evalValue(c)).getLong();
            if (amount < 0)
                throw new InternalExpressionException("Cannot throw negative number of items");
            ItemStack stack = inventoryLocator.inventory.getInvStack(slot);
            if (stack == null || stack.isEmpty())
                return (_c, _t) -> Value.ZERO;
            if (amount == 0)
                amount = stack.getCount();
            ItemStack droppedStack = inventoryLocator.inventory.takeInvStack(slot, amount);
            if (droppedStack.isEmpty())
            {
                return (_c, _t) -> Value.ZERO;
            }
            Object owner = inventoryLocator.owner;
            ItemEntity item;
            if (owner instanceof PlayerEntity)
            {
                item = ((PlayerEntity) owner).dropItem(droppedStack, false, true);
            }
            else if (owner instanceof LivingEntity)
            {
                LivingEntity villager = (LivingEntity)owner;
                // stolen from LookTargetUtil.give((VillagerEntity)owner, droppedStack, (LivingEntity) owner);
                double double_1 = villager.getY() - 0.30000001192092896D + (double)villager.getStandingEyeHeight();
                item = new ItemEntity(villager.world, villager.getX(), double_1, villager.getZ(), droppedStack);
                Vec3d vec3d_1 = villager.getRotationVec(1.0F).normalize().multiply(0.3);//  new Vec3d(0, 0.3, 0);
                item.setVelocity(vec3d_1);
                item.setToDefaultPickupDelay();
                cc.s.getWorld().spawnEntity(item);
            }
            else
            {
                Vec3d point = new Vec3d(inventoryLocator.position);
                item = new ItemEntity(cc.s.getWorld(), point.x+0.5, point.y+0.5, point.z+0.5, droppedStack);
                item.setToDefaultPickupDelay();
                cc.s.getWorld().spawnEntity(item);
            }
            Value res = new NumericValue(item.getStack().getCount());
            return (_c, _t) -> res;
        });
    }
    private void syncPlayerInventory(NBTSerializableValue.InventoryLocator inventory, int int_1)
    {
        if (inventory.owner instanceof ServerPlayerEntity && !inventory.isEnder)
        {
            ServerPlayerEntity player = (ServerPlayerEntity) inventory.owner;
            player.networkHandler.sendPacket(new ContainerSlotUpdateS2CPacket(
                    -2,
                    int_1,
                    inventory.inventory.getInvStack(int_1)
            ));
        }
    }

    private void API_EntityManipulation()
    {
        this.expr.addLazyFunction("player", -1, (c, t, lv) -> {
            if (lv.size() ==0)
            {

                Entity callingEntity = ((CarpetContext)c).s.getEntity();
                if (callingEntity instanceof PlayerEntity)
                {
                    Value retval = new EntityValue(callingEntity);
                    return (_c, _t) -> retval;
                }
                Vec3d pos = ((CarpetContext)c).s.getPosition();
                PlayerEntity closestPlayer = ((CarpetContext)c).s.getWorld().getClosestPlayer(pos.x, pos.y, pos.z, -1.0, EntityPredicates.VALID_ENTITY);
                if (closestPlayer != null)
                {
                    Value retval = new EntityValue(closestPlayer);
                    return (_c, _t) -> retval;
                }
                return (_c, _t) -> Value.NULL;
            }
            String playerName = lv.get(0).evalValue(c).getString();
            Value retval = Value.NULL;
            if ("all".equalsIgnoreCase(playerName))
            {
                retval = ListValue.wrap(
                        ((CarpetContext)c).s.getMinecraftServer().getPlayerManager().getPlayerList().
                                stream().map(EntityValue::new).collect(Collectors.toList()));
            }
            else if ("*".equalsIgnoreCase(playerName))
            {
                retval = ListValue.wrap(
                        ((CarpetContext)c).s.getWorld().getPlayers().
                                stream().map(EntityValue::new).collect(Collectors.toList()));
            }
            else if ("survival".equalsIgnoreCase(playerName))
            {
                retval =  ListValue.wrap(
                        ((CarpetContext)c).s.getWorld().getPlayers((p) -> p.interactionManager.isSurvivalLike()).
                                stream().map(EntityValue::new).collect(Collectors.toList()));
            }
            else if ("creative".equalsIgnoreCase(playerName))
            {
                retval = ListValue.wrap(
                        ((CarpetContext)c).s.getWorld().getPlayers(PlayerEntity::isCreative).
                                stream().map(EntityValue::new).collect(Collectors.toList()));
            }
            else if ("spectating".equalsIgnoreCase(playerName))
            {
                retval = ListValue.wrap(
                        ((CarpetContext)c).s.getWorld().getPlayers(PlayerEntity::isSpectator).
                                stream().map(EntityValue::new).collect(Collectors.toList()));
            }
            else if ("!spectating".equalsIgnoreCase(playerName))
            {
                retval = ListValue.wrap(
                        ((CarpetContext)c).s.getWorld().getPlayers((p) -> !p.isSpectator()).
                                stream().map(EntityValue::new).collect(Collectors.toList()));
            }
            else
            {
                ServerPlayerEntity player = ((CarpetContext) c).s.getMinecraftServer().getPlayerManager().getPlayer(playerName);
                if (player != null)
                    retval = new EntityValue(player);
            }
            Value finalVar = retval;
            return (cc, tt) -> finalVar;
        });

        this.expr.addLazyFunction("spawn", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            if (lv.size() < 2)
                throw new InternalExpressionException("'spawn' function takes mob name, and position to spawn");
            String entityString = lv.get(0).evalValue(c).getString();
            Identifier entityId;
            try
            {
                entityId = Identifier.fromCommandInput(new StringReader(entityString));
                EntityType type = Registry.ENTITY_TYPE.getOrEmpty(entityId).orElse(null);
                if (type == null || !type.isSummonable())
                    return LazyValue.NULL;
            }
            catch (CommandSyntaxException exception)
            {
                 return LazyValue.NULL;
            }

            Vector3Argument position = Vector3Argument.findIn(cc, lv, 1);
            if (position.fromBlock)
                position.vec = position.vec.subtract(0, 0.5, 0);
            CompoundTag tag = new CompoundTag();
            boolean hasTag = false;
            if (lv.size() > position.offset)
            {
                Value nbt = lv.get(position.offset).evalValue(c);
                NBTSerializableValue v = (nbt instanceof NBTSerializableValue) ? (NBTSerializableValue) nbt
                        : NBTSerializableValue.parseString(nbt.getString());
                hasTag = true;
                tag = v.getCompoundTag();

            }
            tag.putString("id", entityId.toString());
            Vec3d vec3d = position.vec;

            if (EntityType.getId(EntityType.LIGHTNING_BOLT).equals(entityId)) {
                LightningEntity lightningEntity_1 = new LightningEntity(cc.s.getWorld(), vec3d.x, vec3d.y, vec3d.z, false);
                cc.s.getWorld().addLightning(lightningEntity_1);
                return LazyValue.NULL;
            } else {
                ServerWorld serverWorld = cc.s.getWorld();
                Entity entity_1 = EntityType.loadEntityWithPassengers(tag, serverWorld, (entity_1x) -> {
                    entity_1x.refreshPositionAndAngles(vec3d.x, vec3d.y, vec3d.z, entity_1x.yaw, entity_1x.pitch);
                    return !serverWorld.tryLoadEntity(entity_1x) ? null : entity_1x;
                });
                if (entity_1 == null) {
                    return LazyValue.NULL;
                } else {
                    if (!hasTag && entity_1 instanceof MobEntity) {
                        ((MobEntity)entity_1).initialize(serverWorld, serverWorld.getLocalDifficulty(entity_1.getBlockPos()), SpawnType.COMMAND, null, null);
                    }
                    Value res = new EntityValue(entity_1);
                    return (_c, _t) -> res;
                }
            }
        });

        this.expr.addLazyFunction("entity_id", 1, (c, t, lv) ->
        {
            Value who = lv.get(0).evalValue(c);
            Entity e;
            if (who instanceof NumericValue)
            {
                e = ((CarpetContext)c).s.getWorld().getEntityById((int)((NumericValue) who).getLong());
            }
            else
            {
                e = ((CarpetContext)c).s.getWorld().getEntity(UUID.fromString(who.getString()));
            }
            if (e==null)
            {
                return LazyValue.NULL;
            }
            return (cc, tt) -> new EntityValue(e);
        });

        this.expr.addLazyFunction("entity_list", 1, (c, t, lv) -> {
            String who = lv.get(0).evalValue(c).getString();
            Pair<EntityType<?>, Predicate<? super Entity>> pair = EntityValue.getPredicate(who);
            if (pair == null)
            {
                throw new InternalExpressionException("Unknown entity selection criterion: "+who);
            }
            List<Entity> entityList = ((CarpetContext)c).s.getWorld().getEntities(pair.getKey(), pair.getValue());
            Value retval = ListValue.wrap(entityList.stream().map(EntityValue::new).collect(Collectors.toList()));
            return (_c, _t ) -> retval;
        });

        this.expr.addLazyFunction("entity_area", 7, (c, t, lv) -> {
            BlockPos center = new BlockPos(
                    NumericValue.asNumber(lv.get(1).evalValue(c)).getDouble(),
                    NumericValue.asNumber(lv.get(2).evalValue(c)).getDouble(),
                    NumericValue.asNumber(lv.get(3).evalValue(c)).getDouble()
            );
            Box area = new Box(center).expand(
                    NumericValue.asNumber(lv.get(4).evalValue(c)).getDouble(),
                    NumericValue.asNumber(lv.get(5).evalValue(c)).getDouble(),
                    NumericValue.asNumber(lv.get(6).evalValue(c)).getDouble()
            );
            String who = lv.get(0).evalValue(c).getString();
            Pair<EntityType<?>, Predicate<? super Entity>> pair = EntityValue.getPredicate(who);
            if (pair == null)
            {
                throw new InternalExpressionException("Unknown entity selection criterion: "+who);
            }
            List<Entity> entityList = ((CarpetContext)c).s.getWorld().getEntities((EntityType<Entity>) pair.getKey(), area, pair.getValue());
            Value retval = ListValue.wrap(entityList.stream().map(EntityValue::new).collect(Collectors.toList()));
            return (_c, _t ) -> retval;
        });

        this.expr.addLazyFunction("entity_selector", -1, (c, t, lv) ->
        {
            String selector = lv.get(0).evalValue(c).getString();
            List<Value> retlist = new ArrayList<>();
            for (Entity e: EntityValue.getEntitiesFromSelector(((CarpetContext)c).s, selector))
            {
                retlist.add(new EntityValue(e));
            }
            return (c_, t_) -> ListValue.wrap(retlist);
        });

        this.expr.addLazyFunction("query", -1, (c, t, lv) -> {
            if (lv.size()<2)
            {
                throw new InternalExpressionException("'query' takes entity as a first argument, and queried feature as a second");
            }
            Value v = lv.get(0).evalValue(c);
            if (!(v instanceof EntityValue))
                throw new InternalExpressionException("First argument to query should be an entity");
            String what = lv.get(1).evalValue(c).getString();
            Value retval;
            if (lv.size()==2)
                retval = ((EntityValue) v).get(what, null);
            else if (lv.size()==3)
                retval = ((EntityValue) v).get(what, lv.get(2).evalValue(c));
            else
                retval = ((EntityValue) v).get(what, ListValue.wrap(lv.subList(2, lv.size()).stream().map((vv) -> vv.evalValue(c)).collect(Collectors.toList())));
            return (cc, tt) -> retval;
        });

        // or update
        this.expr.addLazyFunction("modify", -1, (c, t, lv) -> {
            if (lv.size()<2)
            {
                throw new InternalExpressionException("'modify' takes entity as a first argument, and queried feature as a second");
            }
            Value v = lv.get(0).evalValue(c);
            if (!(v instanceof EntityValue))
                throw new InternalExpressionException("First argument to modify should be an entity");
            String what = lv.get(1).evalValue(c).getString();
            if (lv.size()==2)
                ((EntityValue) v).set(what, null);
            else if (lv.size()==3)
                ((EntityValue) v).set(what, lv.get(2).evalValue(c));
            else
                ((EntityValue) v).set(what, ListValue.wrap(lv.subList(2, lv.size()).stream().map((vv) -> vv.evalValue(c)).collect(Collectors.toList())));
            return (cc, tt) -> v;
        });

        // or update
        this.expr.addLazyFunction("entity_event", -1, (c, t, lv) ->
        {
            if (lv.size()<3)
                throw new InternalExpressionException("'entity_event' requires at least 3 arguments, entity, event to be handled, and function name, with optional arguments");
            Value v = lv.get(0).evalValue(c);
            if (!(v instanceof EntityValue))
                throw new InternalExpressionException("First argument to entity_event should be an entity");
            String what = lv.get(1).evalValue(c).getString();
            Value functionValue = lv.get(2).evalValue(c);
            if (functionValue instanceof NullValue)
                functionValue = null;
            else if (!(functionValue instanceof FunctionValue))
            {
                String name = functionValue.getString();
                functionValue = c.host.getAssertFunction(this.expr.module, name);
            }
            FunctionValue function = (FunctionValue)functionValue;
            List<Value> args = null;
            if (lv.size()==4)
                args = Collections.singletonList(lv.get(3).evalValue(c));
            else if (lv.size()>4)
            {
                args = lv.subList(3, lv.size()).stream().map((vv) -> vv.evalValue(c)).collect(Collectors.toList());
            }

            ((EntityValue) v).setEvent((CarpetContext)c, what, function, args);

            return LazyValue.NULL;
        });
    }

    private void API_IteratingOverAreasOfBlocks()
    {
        this.expr.addLazyFunction("scan", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            BlockArgument centerLocator = BlockArgument.findIn(cc, lv, 0);
            Vector3Argument rangeLocator = Vector3Argument.findIn(cc, lv, centerLocator.offset);
            BlockPos center = centerLocator.block.getPos();
            Vec3i range;

            if (rangeLocator.fromBlock)
            {
                range = new Vec3i(
                        abs(rangeLocator.vec.x - center.getX()),
                        abs(rangeLocator.vec.y - center.getY()),
                        abs(rangeLocator.vec.z - center.getZ())
                );
                //if (lv.size() > rangeLocator.offset+1) throw new InternalExpressionException("'scan' takes two block positions, and the expression")
            }
            else
            {
                range = new Vec3i(abs(rangeLocator.vec.x), abs(rangeLocator.vec.y), abs(rangeLocator.vec.z));
            }
            Vec3i upperRange = range;
            if (lv.size() > rangeLocator.offset+1) // +1 cause we still need the expression
            {
                rangeLocator = Vector3Argument.findIn(cc, lv, rangeLocator.offset);
                if (rangeLocator.fromBlock)
                {
                    upperRange = new Vec3i(
                            abs(rangeLocator.vec.x - center.getX()),
                            abs(rangeLocator.vec.y - center.getY()),
                            abs(rangeLocator.vec.z - center.getZ())
                    );
                    //if (lv.size() > rangeLocator.offset+1) throw new InternalExpressionException("'scan' takes two block positions, and the expression")
                }
                else
                {
                    upperRange = new Vec3i(abs(rangeLocator.vec.x), abs(rangeLocator.vec.y), abs(rangeLocator.vec.z));
                }
            }
            if (lv.size() != rangeLocator.offset+1)
            {
                throw new InternalExpressionException("'scan' takes two, or three block positions, and an expression: "+lv.size()+" "+rangeLocator.offset);
            }
            LazyValue expr = lv.get(rangeLocator.offset);

            int cx = center.getX();
            int cy = center.getY();
            int cz = center.getZ();
            int xrange = range.getX();
            int yrange = range.getY();
            int zrange = range.getZ();
            int xprange = upperRange.getX();
            int yprange = upperRange.getY();
            int zprange = upperRange.getZ();

            //saving outer scope
            LazyValue _x = c.getVariable("_x");
            LazyValue _y = c.getVariable("_y");
            LazyValue _z = c.getVariable("_z");
            LazyValue __ = c.getVariable("_");
            int sCount = 0;
            outer:for (int y=cy-yrange; y <= cy+yprange; y++)
            {
                int yFinal = y;
                c.setVariable("_y", (c_, t_) -> new NumericValue(yFinal).bindTo("_y"));
                for (int x=cx-xrange; x <= cx+xprange; x++)
                {
                    int xFinal = x;
                    c.setVariable("_x", (c_, t_) -> new NumericValue(xFinal).bindTo("_x"));
                    for (int z=cz-zrange; z <= cz+zprange; z++)
                    {
                        int zFinal = z;

                        c.setVariable("_z", (c_, t_) -> new NumericValue(zFinal).bindTo("_z"));
                        Value blockValue = BlockValue.fromCoords(((CarpetContext)c), xFinal,yFinal,zFinal).bindTo("_");
                        c.setVariable( "_", (cc_, t_c) -> blockValue);
                        Value result;
                        try
                        {
                            result = expr.evalValue(c, t);
                        }
                        catch (ContinueStatement notIgnored)
                        {
                            result = notIgnored.retval;
                        }
                        catch (BreakStatement notIgnored)
                        {
                            break outer;
                        }
                        if (t != Context.VOID && result.getBoolean())
                        {
                            sCount += 1;
                        }
                    }
                }
            }
            //restoring outer scope
            c.setVariable("_x", _x);
            c.setVariable("_y", _y);
            c.setVariable("_z", _z);
            c.setVariable("_", __);
            int finalSCount = sCount;
            return (c_, t_) -> new NumericValue(finalSCount);
        });

        this.expr.addLazyFunction("volume", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;

            BlockArgument pos1Locator = BlockArgument.findIn(cc, lv, 0);
            BlockArgument pos2Locator = BlockArgument.findIn(cc, lv, pos1Locator.offset);
            BlockPos pos1 = pos1Locator.block.getPos();
            BlockPos pos2 = pos2Locator.block.getPos();

            int x1 = pos1.getX();
            int y1 = pos1.getY();
            int z1 = pos1.getZ();
            int x2 = pos2.getX();
            int y2 = pos2.getY();
            int z2 = pos2.getZ();
            int minx = min(x1, x2);
            int miny = min(y1, y2);
            int minz = min(z1, z2);
            int maxx = max(x1, x2);
            int maxy = max(y1, y2);
            int maxz = max(z1, z2);
            LazyValue expr = lv.get(pos2Locator.offset);

            //saving outer scope
            LazyValue _x = c.getVariable("_x");
            LazyValue _y = c.getVariable("_y");
            LazyValue _z = c.getVariable("_z");
            LazyValue __ = c.getVariable("_");
            int sCount = 0;
            outer:for (int y=miny; y <= maxy; y++)
            {
                int yFinal = y;
                c.setVariable("_y", (c_, t_) -> new NumericValue(yFinal).bindTo("_y"));
                for (int x=minx; x <= maxx; x++)
                {
                    int xFinal = x;
                    c.setVariable("_x", (c_, t_) -> new NumericValue(xFinal).bindTo("_x"));
                    for (int z=minz; z <= maxz; z++)
                    {
                        int zFinal = z;
                        c.setVariable("_z", (c_, t_) -> new NumericValue(zFinal).bindTo("_z"));
                        Value blockValue = BlockValue.fromCoords(((CarpetContext)c), xFinal,yFinal,zFinal).bindTo("_");
                        c.setVariable( "_", (cc_, t_c) -> blockValue);
                        Value result;
                        try
                        {
                            result = expr.evalValue(c, t);
                        }
                        catch (ContinueStatement notIgnored)
                        {
                            result = notIgnored.retval;
                        }
                        catch (BreakStatement notIgnored)
                        {
                            break outer;
                        }
                        if (t != Context.VOID && result.getBoolean())
                        {
                            sCount += 1;
                        }
                    }
                }
            }
            //restoring outer scope
            c.setVariable("_x", _x);
            c.setVariable("_y", _y);
            c.setVariable("_z", _z);
            c.setVariable("_", __);
            int finalSCount = sCount;
            return (c_, t_) -> new NumericValue(finalSCount);
        });

        this.expr.addLazyFunction("neighbours", -1, (c, t, lv)->
        {

            BlockPos center = BlockArgument.findIn((CarpetContext) c, lv,0).block.getPos();
            ServerWorld world = ((CarpetContext) c).s.getWorld();

            List<Value> neighbours = new ArrayList<>();
            neighbours.add(new BlockValue(null, world, center.up()));
            neighbours.add(new BlockValue(null, world, center.down()));
            neighbours.add(new BlockValue(null, world, center.north()));
            neighbours.add(new BlockValue(null, world, center.south()));
            neighbours.add(new BlockValue(null, world, center.east()));
            neighbours.add(new BlockValue(null, world, center.west()));
            Value retval = ListValue.wrap(neighbours);
            return (c_, t_) -> retval;
        });

        this.expr.addLazyFunction("rect", -1, (c, t, lv)->
        {
            CarpetContext cc = (CarpetContext) c;
            int cx, cy, cz;
            int sminx, sminy, sminz;
            int smaxx, smaxy, smaxz;
            BlockArgument cposLocator = BlockArgument.findIn(cc, lv, 0);
            BlockPos cpos = cposLocator.block.getPos();
            cx = cpos.getX();
            cy = cpos.getY();
            cz = cpos.getZ();
            if (lv.size() > cposLocator.offset)
            {
                Vector3Argument diffLocator = Vector3Argument.findIn(cc, lv, cposLocator.offset);
                if (diffLocator.fromBlock)
                {
                    sminx = MathHelper.floor(abs(diffLocator.vec.x - cx));
                    sminy = MathHelper.floor(abs(diffLocator.vec.y - cx));
                    sminz = MathHelper.floor(abs(diffLocator.vec.z - cx));
                }
                else
                {
                    sminx = MathHelper.floor(abs(diffLocator.vec.x));
                    sminy = MathHelper.floor(abs(diffLocator.vec.y));
                    sminz = MathHelper.floor(abs(diffLocator.vec.z));
                }
                if (lv.size() > diffLocator.offset)
                {
                    Vector3Argument posDiff = Vector3Argument.findIn(cc, lv, diffLocator.offset);
                    if (posDiff.fromBlock)
                    {
                        smaxx = MathHelper.floor(abs(posDiff.vec.x - cx));
                        smaxy = MathHelper.floor(abs(posDiff.vec.y - cx));
                        smaxz = MathHelper.floor(abs(posDiff.vec.z - cx));
                    }
                    else
                    {
                        smaxx = MathHelper.floor(abs(posDiff.vec.x));
                        smaxy = MathHelper.floor(abs(posDiff.vec.y));
                        smaxz = MathHelper.floor(abs(posDiff.vec.z));
                    }
                }
                else
                {
                    smaxx = sminx;
                    smaxy = sminy;
                    smaxz = sminz;
                }
            }
            else
            {
                sminx = 1;
                sminy = 1;
                sminz = 1;
                smaxx = 1;
                smaxy = 1;
                smaxz = 1;
            }

            return (c_, t_) -> new LazyListValue()
            {
                final int minx = cx-sminx;
                final int miny = cy-sminy;
                final int minz = cz-sminz;
                final int maxx = cx+smaxx;
                final int maxy = cy+smaxy;
                final int maxz = cz+smaxz;

                int x;
                int y;
                int z;
                {
                    reset();
                }
                @Override
                public boolean hasNext()
                {
                    return y <= maxy;
                }

                @Override
                public Value next()
                {
                    Value r = BlockValue.fromCoords(cc, x,y,z);
                    //possibly reroll context
                    x++;
                    if (x > maxx)
                    {
                        x = minx;
                        z++;
                        if (z > maxz)
                        {
                            z = minz;
                            y++;
                            // hasNext should fail if we went over
                        }
                    }

                    return r;
                }

                @Override
                public void fatality()
                {
                    // possibly return original x, y, z
                    super.fatality();
                }

                @Override
                public void reset()
                {
                    x = minx;
                    y = miny;
                    z = minz;
                }

                @Override
                public String getString()
                {
                    return String.format(Locale.ROOT, "rect[(%d,%d,%d),..,(%d,%d,%d)]",minx, miny, minz, maxx, maxy, maxz);
                }
            };
        });

        this.expr.addLazyFunction("diamond", -1, (c, t, lv)->
        {
            CarpetContext cc = (CarpetContext)c;

            BlockArgument cposLocator=BlockArgument.findIn((CarpetContext)c,lv,0);
            BlockPos cpos = cposLocator.block.getPos();

            int cx;
            int cy;
            int cz;
            int width;
            int height;
            try
            {
                cx = cpos.getX();
                cy = cpos.getY();
                cz = cpos.getZ();

                if (lv.size()==cposLocator.offset)
                {
                    Value retval = ListValue.of(
                            BlockValue.fromCoords(cc, cx, cy-1, cz),
                            BlockValue.fromCoords(cc, cx, cy, cz),
                            BlockValue.fromCoords(cc, cx-1, cy, cz),
                            BlockValue.fromCoords(cc, cx, cy, cz-1),
                            BlockValue.fromCoords(cc, cx+1, cy, cz),
                            BlockValue.fromCoords(cc, cx, cy, cz+1),
                            BlockValue.fromCoords(cc, cx, cy+1, cz)
                    );
                    return (_c, _t ) -> retval;
                }
                else if (lv.size()==1+cposLocator.offset)
                {
                    width = (int) ((NumericValue) lv.get(cposLocator.offset).evalValue(c)).getLong();
                    height = 0;
                }
                else if(lv.size()==2+cposLocator.offset)
                {
                    width = (int) ((NumericValue) lv.get(cposLocator.offset).evalValue(c)).getLong();
                    height = (int) ((NumericValue) lv.get(cposLocator.offset+1).evalValue(c)).getLong();
                } else{
                    throw new InternalExpressionException("Incorrect number of arguments for 'diamond'");
                }
            }
            catch (ClassCastException exc)
            {
                throw new InternalExpressionException("Attempted to pass a non-number to 'diamond'");
            }
            if (height == 0)
            {
                return (c_, t_) -> new LazyListValue()
                {
                    int curradius;
                    int curpos;
                    {
                        reset();
                    }
                    @Override
                    public boolean hasNext()
                    {
                        return curradius <= width;
                    }

                    @Override
                    public Value next()
                    {
                        if (curradius == 0)
                        {
                            curradius = 1;
                            return BlockValue.fromCoords(cc, cx, cy, cz);
                        }
                        // x = 3-|i-6|
                        // z = |( (i-3)%12-6|-3
                        Value block = BlockValue.fromCoords(cc, cx+(curradius-abs(curpos-2*curradius)), cy, cz-curradius+abs( abs(curpos-curradius)%(4*curradius) -2*curradius ));
                        curpos++;
                        if (curpos>=curradius*4)
                        {
                            curradius++;
                            curpos = 0;
                        }
                        return block;

                    }

                    @Override
                    public void reset()
                    {
                        curradius = 0;
                        curpos = 0;
                    }

                    @Override
                    public String getString()
                    {
                        return String.format(Locale.ROOT, "diamond[(%d,%d,%d),%d,0]",cx, cy, cz, width);
                    }
                };
            }
            else
            {
                return (c_, t_) -> new LazyListValue()
                {
                    int curradius;
                    int curpos;
                    int curheight;
                    {
                        reset();
                    }
                    @Override
                    public boolean hasNext()
                    {
                        return curheight <= height;
                    }

                    @Override
                    public Value next()
                    {
                        if (curheight == -height || curheight == height)
                        {
                            return BlockValue.fromCoords(cc, cx, cy+curheight++, cz);
                        }
                        if (curradius == 0)
                        {
                            curradius++;
                            return BlockValue.fromCoords(cc, cx, cy+curheight, cz);
                        }
                        // x = 3-|i-6|
                        // z = |( (i-3)%12-6|-3

                        Value block = BlockValue.fromCoords(cc, cx+(curradius-abs(curpos-2*curradius)), cy+curheight, cz-curradius+abs( abs(curpos-curradius)%(4*curradius) -2*curradius ));
                        curpos++;
                        if (curpos>=curradius*4)
                        {
                            curradius++;
                            curpos = 0;
                            if (curradius>width -abs(width*curheight/height))
                            {
                                curheight++;
                                curradius = 0;
                                curpos = 0;
                            }
                        }
                        return block;
                    }

                    @Override
                    public void reset()
                    {
                        curradius = 0;
                        curpos = 0;
                        curheight = -height;
                    }

                    @Override
                    public String getString()
                    {
                        return String.format(Locale.ROOT, "diamond[(%d,%d,%d),%d,%d]",cx, cy, cz, width, height);
                    }
                };
            }
        });
    }

    private static Map<String,SoundCategory> mixerMap = Arrays.stream(SoundCategory.values()).collect(Collectors.toMap(SoundCategory::getName, k -> k));

    private void API_Interapperability()
    {
        //todo doc and test all
        this.expr.addLazyFunction("loaded_apps", 0, (c, t, lv) ->
        {
            // return a set of all loaded apps
            Value ret = new MapValue(((CarpetScriptHost) c.host).getScriptServer().modules.keySet().stream().map(StringValue::new).collect(Collectors.toSet()));
            return (cc, tt) -> ret;
        });

        this.expr.addLazyFunction("is_app_loaded", 1, (c, t, lv) ->
        {
            Value ret = new NumericValue(((CarpetScriptHost) c.host).getScriptServer().modules.containsKey(lv.get(0).evalValue(c).getString()));
            return (cc, tt) -> ret;
        });

    }

    public static String recognizeResource(Value value)
    {
        String origfile = value.getString();
        String file = origfile.toLowerCase(Locale.ROOT).replaceAll("[^A-Za-z0-9\\-+_/]", "");
        file = Arrays.stream(file.split("/+")).filter(s -> !s.isEmpty()).collect(Collectors.joining("/"));
        if (file.isEmpty())
        {
            throw new InternalExpressionException("Cannot use "+origfile+" as resource name - must have some letters and numbers");
        }
        return file;
    }

    private void API_AuxiliaryAspects()
    {
        this.expr.addLazyFunction("sound", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            Identifier soundName = new Identifier(lv.get(0).evalValue(c).getString());
            Vector3Argument locator = Vector3Argument.findIn(cc, lv, 1);
            if (Registry.SOUND_EVENT.get(soundName) == null)
                throw new InternalExpressionException("No such sound: "+soundName.getPath());
            float volume = 1.0F;
            float pitch = 1.0F;
            SoundCategory mixer = SoundCategory.MASTER;
            if (lv.size() > 0+locator.offset)
            {
                volume = (float) NumericValue.asNumber(lv.get(0+locator.offset).evalValue(c)).getDouble();
                if (lv.size() > 1+locator.offset)
                {
                    pitch = (float) NumericValue.asNumber(lv.get(1+locator.offset).evalValue(c)).getDouble();
                    if (lv.size() > 2+locator.offset)
                    {
                        String mixerName = lv.get(2+locator.offset).evalValue(c).getString();
                        mixer = mixerMap.get(mixerName.toLowerCase(Locale.ROOT));
                        if (mixer == null) throw  new InternalExpressionException(mixerName +" is not a valid mixer name");
                    }
                }
            }
            Vec3d vec = locator.vec;
            double d0 = Math.pow(volume > 1.0F ? (double)(volume * 16.0F) : 16.0D, 2.0D);
            int count = 0;
            for (ServerPlayerEntity player : cc.s.getWorld().getPlayers( (p) -> p.squaredDistanceTo(vec) < d0))
            {
                count++;
                player.networkHandler.sendPacket(new PlaySoundIdS2CPacket(soundName, mixer, vec, volume, pitch));
            }
            int totalPlayed = count;
            return (_c, _t) -> new NumericValue(totalPlayed);
        });

        this.expr.addLazyFunction("particle", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            MinecraftServer ms = cc.s.getMinecraftServer();
            ServerWorld world = cc.s.getWorld();
            Vector3Argument locator = Vector3Argument.findIn(cc, lv, 1);
            String particleName = lv.get(0).evalValue(c).getString();
            int count = 10;
            double speed = 0;
            float spread = 0.5f;
            ServerPlayerEntity player = null;
            if (lv.size() > locator.offset)
            {
                count = (int) NumericValue.asNumber(lv.get(locator.offset).evalValue(c)).getLong();
                if (lv.size() > 1+locator.offset)
                {
                    spread = (float) NumericValue.asNumber(lv.get(1+locator.offset).evalValue(c)).getDouble();
                    if (lv.size() > 2+locator.offset)
                    {
                        speed = NumericValue.asNumber(lv.get(2 + locator.offset).evalValue(c)).getDouble();
                        if (lv.size() > 3 + locator.offset) // should accept entity as well as long as it is player
                        {
                            player = ms.getPlayerManager().getPlayer(lv.get(3 + locator.offset).evalValue(c).getString());
                        }
                    }
                }
            }
            ParticleEffect particle = ShapeDispatcher.getParticleData(particleName);
            Vec3d vec = locator.vec;
            if (player == null)
            {
                for (PlayerEntity p : (world.getPlayers()))
                {
                    world.spawnParticles((ServerPlayerEntity)p, particle, true, vec.x, vec.y, vec.z, count,
                            spread, spread, spread, speed);
                }
            }
            else
            {
                world.spawnParticles(player,
                        particle, true, vec.x, vec.y, vec.z, count,
                        spread, spread, spread, speed);
            }

            return (c_, t_) -> Value.TRUE;
        });

        this.expr.addLazyFunction("particle_line", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerWorld world = cc.s.getWorld();
            String particleName = lv.get(0).evalValue(c).getString();
            ParticleEffect particle = ShapeDispatcher.getParticleData(particleName);
            Vector3Argument pos1 = Vector3Argument.findIn(cc, lv, 1);
            Vector3Argument pos2 = Vector3Argument.findIn(cc, lv, pos1.offset);
            double density = 1.0;
            ServerPlayerEntity player = null;
            if (lv.size() > pos2.offset+0 )
            {
                density = NumericValue.asNumber(lv.get(pos2.offset+0).evalValue(c)).getDouble();
                if (density <= 0)
                {
                    throw new InternalExpressionException("Particle density should be positive");
                }
                if (lv.size() > pos2.offset+1)
                {
                    Value playerValue = lv.get(pos2.offset+1).evalValue(c);
                    if (playerValue instanceof EntityValue)
                    {
                        Entity e = ((EntityValue) playerValue).getEntity();
                        if (!(e instanceof ServerPlayerEntity)) throw new InternalExpressionException("'particle_line' player argument has to be a player");
                        player = (ServerPlayerEntity) e;
                    }
                    else
                    {
                        player = cc.s.getMinecraftServer().getPlayerManager().getPlayer(playerValue.getString());
                    }
                }
            }

            Value retval = new NumericValue(ShapeDispatcher.drawParticleLine(
                    (player == null)?world.getPlayers():Collections.singletonList(player),
                    particle, pos1.vec, pos2.vec, density));

            return (c_, t_) -> retval;
        });

        this.expr.addLazyFunction("particle_box", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerWorld world = cc.s.getWorld();
            String particleName = lv.get(0).evalValue(c).getString();
            ParticleEffect particle = ShapeDispatcher.getParticleData(particleName);
            Vector3Argument pos1 = Vector3Argument.findIn(cc, lv, 1);
            Vector3Argument pos2 = Vector3Argument.findIn(cc, lv, pos1.offset);

            double density = 1.0;
            ServerPlayerEntity player = null;
            if (lv.size() > pos2.offset+0 )
            {
                density = NumericValue.asNumber(lv.get(pos2.offset+0).evalValue(c)).getDouble();
                if (density <= 0)
                {
                    throw new InternalExpressionException("Particle density should be positive");
                }
                if (lv.size() > pos2.offset+1)
                {
                    Value playerValue = lv.get(pos2.offset+1).evalValue(c);
                    if (playerValue instanceof EntityValue)
                    {
                        Entity e = ((EntityValue) playerValue).getEntity();
                        if (!(e instanceof ServerPlayerEntity)) throw new InternalExpressionException("'particle_box' player argument has to be a player");
                        player = (ServerPlayerEntity) e;
                    }
                    else
                    {
                        player = cc.s.getMinecraftServer().getPlayerManager().getPlayer(playerValue.getString());
                    }
                }
            }
            Vec3d a = pos1.vec;
            Vec3d b = pos2.vec;
            Vec3d from = new Vec3d(min(a.x, b.x), min(a.y, b.y), min(a.z, b.z));
            Vec3d to = new Vec3d(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z));
            int particleCount = ShapeDispatcher.Box.particleMesh(
                    player==null?world.getPlayers():Collections.singletonList(player),
                    particle, density, from, to
            );
            return (c_, t_) -> new NumericValue(particleCount);
        });
        // deprecated
        this.expr.alias("particle_rect", "particle_box");


        this.expr.addLazyFunction("draw_shape", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerPlayerEntity player[] = {null};
            List<Pair<ShapeDispatcher.ExpiringShape, Map<String,Value>>> shapes = new ArrayList<>();
            if (lv.size() == 1) // bulk
            {
                Value specLoad = lv.get(0).evalValue(c);
                if (!(specLoad instanceof ListValue)) throw new InternalExpressionException("In bulk mode - shapes need to be provided as a list of shape specs");
                for (Value list : ((ListValue) specLoad).getItems())
                {
                    if (!(list instanceof ListValue))  throw new InternalExpressionException("In bulk mode - shapes need to be provided as a list of shape specs");
                    shapes.add( ShapeDispatcher.fromFunctionArgs(cc, ((ListValue) list).getItems(), player));
                }
            }
            else
            {
                List<Value> params = new ArrayList<>();
                for (LazyValue v : lv) params.add(v.evalValue(c));
                shapes.add(ShapeDispatcher.fromFunctionArgs(cc, params, player));
            }

            ShapeDispatcher.sendShape(
                    (player[0]==null)?cc.s.getWorld().getPlayers():Collections.singletonList(player[0]),
                    shapes
            );
            return LazyValue.TRUE;
        });

        this.expr.addLazyFunction("create_marker", -1, (c, t, lv) ->{
            CarpetContext cc = (CarpetContext)c;
            BlockState targetBlock = null;
            Vector3Argument pointLocator;
            boolean interactable = true;
            String name;
            try
            {
                Value nameValue = lv.get(0).evalValue(c);
                name = nameValue instanceof NullValue ? "" : nameValue.getString();
                pointLocator = Vector3Argument.findIn(cc, lv, 1, true);
                if (lv.size()>pointLocator.offset)
                {
                    BlockArgument blockLocator = BlockArgument.findIn(cc, lv, pointLocator.offset, true, true);
                    if (blockLocator.block != null) targetBlock = blockLocator.block.getBlockState();
                    if (lv.size() > blockLocator.offset)
                    {
                        interactable = lv.get(blockLocator.offset).evalValue(c, Context.BOOLEAN).getBoolean();
                    }
                }
            }
            catch (IndexOutOfBoundsException e)
            {
                throw new InternalExpressionException("'create_marker' requires a name and three coordinates, with optional direction, and optional block on its head");
            }

            ArmorStandEntity armorstand = new ArmorStandEntity(EntityType.ARMOR_STAND, cc.s.getWorld());
            armorstand.refreshPositionAndAngles(
                    pointLocator.vec.x,
                    pointLocator.vec.y - ((targetBlock==null)?(armorstand.getHeight()+0.41):(armorstand.getHeight()-0.3)),
                    pointLocator.vec.z,
                    (float)pointLocator.yaw,
                    (float) pointLocator.pitch
            );
            armorstand.addScoreboardTag(MARKER_STRING+"_"+((cc.host.getName()==null)?"":cc.host.getName()));
            armorstand.addScoreboardTag(MARKER_STRING);
            if (targetBlock != null)
                armorstand.equipStack(EquipmentSlot.HEAD, new ItemStack(targetBlock.getBlock().asItem()));
            if (!name.isEmpty())
            {
                armorstand.setCustomName(new LiteralText(name));
                armorstand.setCustomNameVisible(true);
            }
            armorstand.setHeadRotation(new EulerAngle((int)pointLocator.pitch,0,0));
            armorstand.setNoGravity(true);
            armorstand.setInvisible(true);
            armorstand.setInvulnerable(true);
            armorstand.getDataTracker().set(ArmorStandEntity.ARMOR_STAND_FLAGS, (byte)(interactable?8 : 16|8));
            cc.s.getWorld().spawnEntity(armorstand);
            EntityValue result = new EntityValue(armorstand);
            return (_c, _t) -> result;
        });

        this.expr.addLazyFunction("remove_all_markers", 0, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            int total = 0;
            String markerName = MARKER_STRING+"_"+((cc.host.getName()==null)?"":cc.host.getName());
            for (Entity e : cc.s.getWorld().getEntities(EntityType.ARMOR_STAND, (as) -> as.getScoreboardTags().contains(markerName)))
            {
                total ++;
                e.remove();
            }
            int finalTotal = total;
            return (_cc, _tt) -> new NumericValue(finalTotal);
        });

        this.expr.addLazyFunction("nbt", 1, (c, t, lv) -> {
            Value ret = NBTSerializableValue.fromValue(lv.get(0).evalValue(c));
            return (cc, tt) -> ret;
        });

        this.expr.addLazyFunction("escape_nbt", 1, (c, t, lv) -> {
            Value v = lv.get(0).evalValue(c);
            String string = v.getString();
            Value ret = new StringValue(StringTag.escape(string));
            return (cc, tt) -> ret;
        });

        this.expr.addLazyFunction("parse_nbt", 1, (c, t, lv) -> {
            Value v = lv.get(0).evalValue(c);
            if (v instanceof NBTSerializableValue)
            {
                Value parsed = ((NBTSerializableValue) v).toValue();
                return (cc, tt) -> parsed;
            }
            NBTSerializableValue ret = NBTSerializableValue.parseString(v.getString());
            if (ret == null)
                return LazyValue.NULL;
            Value parsed = ret.toValue();
            return (cc, tt) -> parsed;
        });

        this.expr.addLazyFunction("encode_nbt", -1, (c, t, lv) -> {
            int argSize = lv.size();
            if (argSize==0 || argSize > 2) throw new InternalExpressionException("'encode_nbt' requires 1 or 2 parameters");
            Value v = lv.get(0).evalValue(c);
            boolean force = (argSize > 1) && lv.get(1).evalValue(c).getBoolean();
            Tag tag;
            try
            {
                tag = v.toTag(force);
            }
            catch (NBTSerializableValue.IncompatibleTypeException ignored)
            {
                throw new InternalExpressionException("cannot reliably encode to a tag the value of '"+ignored.val.getPrettyString()+"'");
            }
            Value tagValue = new NBTSerializableValue(tag);
            return (cc, tt) -> tagValue;
        });

        //"overridden" native call that prints to stderr
        this.expr.addLazyFunction("print", -1, (c, t, lv) ->
        {
            if (lv.size() == 0 || lv.size() > 2) throw new InternalExpressionException("'print' takes one or two arguments");
            ServerCommandSource s = ((CarpetContext)c).s;
            Value res = lv.get(0).evalValue(c);
            if (lv.size() == 2)
            {
                ServerPlayerEntity player = EntityValue.getPlayerByValue(s.getMinecraftServer(), res);
                s = player.getCommandSource();
                res = lv.get(1).evalValue(c);
            }
            if (res instanceof FormattedTextValue)
            {
                s.sendFeedback(((FormattedTextValue) res).getText(), false);
            }
            else
            {
                if (s.getEntity() instanceof PlayerEntity)
                {
                    Messenger.m((PlayerEntity) s.getEntity(), "w " + res.getString());
                }
                else
                {
                    Messenger.m(s, "w " + res.getString());
                }
            }
            Value finalRes = res;
            return (_c, _t) -> finalRes; // pass through for variables
        });

        this.expr.addLazyFunction("format", -1, (c, t, lv) -> {
            if (lv.size() == 0 ) throw new InternalExpressionException("'format' requires at least one component");
            List<Value> values = lv.stream().map(lazy -> lazy.evalValue(c)).collect(Collectors.toList());
            if (values.get(0) instanceof ListValue && values.size()==1)
                values = ((ListValue) values.get(0)).getItems();
            Value ret = new FormattedTextValue(Messenger.c(values.stream().map(Value::getString).toArray()));
            return (cc, tt) -> ret;
        });

        //"overidden" native call to cancel if on main thread
        this.expr.addLazyFunction("task_join", 1, (c, t, lv) -> {
            if (((CarpetContext)c).s.getMinecraftServer().isOnThread())
                throw new InternalExpressionException("'task_join' cannot be called from main thread to avoid deadlocks");
            Value v = lv.get(0).evalValue(c);
            if (!(v instanceof ThreadValue))
                throw new InternalExpressionException("'task_join' could only be used with a task value");
            Value ret =  ((ThreadValue) v).join();
            return (_c, _t) -> ret;
        });

        this.expr.addLazyFunctionWithDelegation("task_dock", 1, (c, t, expr, tok, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            MinecraftServer server = cc.s.getMinecraftServer();
            if (server.isOnThread()) return lv.get(0); // pass through for on thread tasks
            Value[] result = new Value[]{Value.NULL};
            RuntimeException[] internal = new RuntimeException[]{null};
            try
            {
                ((CarpetContext) c).s.getMinecraftServer().submitAndJoin(() ->
                {
                    try
                    {
                        result[0] = lv.get(0).evalValue(c, t);
                    }
                    catch (ExpressionException exc)
                    {
                        internal[0] = exc;
                    }
                    catch (InternalExpressionException exc)
                    {
                        internal[0] = new ExpressionException(c, expr, tok, exc.getMessage(), exc.stack);
                    }

                    catch (ArithmeticException exc)
                    {
                        internal[0] = new ExpressionException(c, expr, tok, "Your math is wrong, "+exc.getMessage());
                    }
                });
            }
            catch (CompletionException exc)
            {
                throw new InternalExpressionException("Error while executing docked task section, internal stack trace is gone");
            }
            if (internal[0] != null)
            {
                throw internal[0];
            }
            Value ret = result[0]; // preventing from lazy evaluating of the result in case a future completes later
            return (_c, _t) -> ret;
            // pass through placeholder
            // implmenetation should dock the task on the main thread.
        });

        this.expr.addLazyFunction("run", 1, (c, t, lv) -> {
            BlockPos target = ((CarpetContext)c).origin;
            Vec3d posf = new Vec3d((double)target.getX()+0.5D,(double)target.getY(),(double)target.getZ()+0.5D);
            ServerCommandSource s = ((CarpetContext)c).s;
            try
            {
                Value retval = new NumericValue(s.getMinecraftServer().getCommandManager().execute(
                        s.withPosition(posf).withSilent().withLevel(CarpetSettings.runPermissionLevel), lv.get(0).evalValue(c).getString()));
                return (c_, t_) -> retval;
            }
            catch (Exception ignored) {}
            return LazyValue.NULL;
        });

        this.expr.addLazyFunction("save", 0, (c, t, lv) -> {
            ServerCommandSource s = ((CarpetContext)c).s;
            s.getMinecraftServer().getPlayerManager().saveAllPlayerData();
            s.getMinecraftServer().save(true,true,true);
            s.getWorld().getChunkManager().tick(() -> true);
            CarpetSettings.LOG.warn("Saved chunks");
            return (cc, tt) -> Value.TRUE;
        });

        this.expr.addLazyFunction("tick_time", 0, (c, t, lv) ->
        {
            Value time = new NumericValue(((CarpetContext) c).s.getMinecraftServer().getTicks());
            return (cc, tt) -> time;
        });

        this.expr.addLazyFunction("world_time", 0, (c, t, lv) ->
        {
            Value time = new NumericValue(((CarpetContext) c).s.getWorld().getTime());
            return (cc, tt) -> time;
        });

        this.expr.addLazyFunction("day_time", -1, (c, t, lv) ->
        {
            Value time = new NumericValue(((CarpetContext) c).s.getWorld().getTimeOfDay());
            if (lv.size() > 0)
            {
                long newTime = NumericValue.asNumber(lv.get(0).evalValue(c)).getLong();
                if (newTime < 0) newTime = 0;
                ((CarpetContext) c).s.getWorld().setTimeOfDay(newTime);
            }
            return (cc, tt) -> time;
        });

        this.expr.addLazyFunction("last_tick_times", -1, (c, t, lv) ->
        {
            //assuming we are in the tick world section
            // might be off one tick when run in the off tasks or asynchronously.
            int currentReportedTick = ((CarpetContext) c).s.getMinecraftServer().getTicks()-1;
            List<Value> ticks = new ArrayList<>(100);
            final long[] tickArray = ((CarpetContext) c).s.getMinecraftServer().lastTickLengths;
            for (int i=currentReportedTick+100; i > currentReportedTick; i--)
            {
                ticks.add(new NumericValue(((double)tickArray[i % 100])/1000000.0));
            }
            Value ret = ListValue.wrap(ticks);
            return (cc, tt) -> ret;
        });


        this.expr.addLazyFunction("game_tick", -1, (c, t, lv) -> {
            ServerCommandSource s = ((CarpetContext)c).s;
            if (!s.getMinecraftServer().isOnThread()) throw new InternalExpressionException("Unable to run ticks from threads");
            ((MinecraftServerInterface)s.getMinecraftServer()).forceTick( () -> System.nanoTime()- CarpetServer.scriptServer.tickStart<50000000L);
            if (lv.size()>0)
            {
                long ms_total = NumericValue.asNumber(lv.get(0).evalValue(c)).getLong();
                long end_expected = CarpetServer.scriptServer.tickStart+ms_total*1000000L;
                long wait = end_expected-System.nanoTime();
                if (wait > 0L)
                {
                    try
                    {
                        Thread.sleep(wait/1000000L);
                    }
                    catch (InterruptedException ignored)
                    {
                    }
                }
            }
            CarpetServer.scriptServer.tickStart = System.nanoTime(); // for the next tick
            Thread.yield();
            if(CarpetServer.scriptServer.stopAll)
                throw new ExitStatement(Value.NULL);
            return (cc, tt) -> Value.TRUE;
        });

        this.expr.addLazyFunction("seed", -1, (c, t, lv) -> {
            ServerCommandSource s = ((CarpetContext)c).s;
            Value ret = new StringValue(Long.toString(s.getWorld().getSeed()));
            return (cc, tt) -> ret;
        });

        this.expr.addLazyFunction("relight", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            BlockArgument locator = BlockArgument.findIn(cc, lv, 0);
            BlockPos pos = locator.block.getPos();
            ServerWorld world = cc.s.getWorld();
            ((ThreadedAnvilChunkStorageInterface) world.getChunkManager().threadedAnvilChunkStorage).relightChunk(new ChunkPos(pos));
            forceChunkUpdate(pos, world);
            return LazyValue.TRUE;
        });

        this.expr.addLazyFunction("current_dimension", 0, (c, t, lv) -> {
            ServerCommandSource s = ((CarpetContext)c).s;
            Value retval = new StringValue(NBTSerializableValue.nameFromRegistryId(Registry.DIMENSION_TYPE.getId(s.getWorld().dimension.getType())));
            return (cc, tt) -> retval;
        });

        this.expr.addLazyFunction("view_distance", 0, (c, t, lv) -> {
            ServerCommandSource s = ((CarpetContext)c).s;
            Value retval = new NumericValue(s.getMinecraftServer().getPlayerManager().getViewDistance());
            return (cc, tt) -> retval;
        });

        this.expr.addLazyFunction("in_dimension", 2, (c, t, lv) -> {
            ServerCommandSource outerSource = ((CarpetContext)c).s;
            Value dimensionValue = lv.get(0).evalValue(c);
            ServerCommandSource innerSource = outerSource;
            if (dimensionValue instanceof EntityValue)
            {
                innerSource = outerSource.withWorld((ServerWorld) ((EntityValue)dimensionValue).getEntity().getEntityWorld());
            }
            else if (dimensionValue instanceof BlockValue)
            {
                BlockValue bv = (BlockValue)dimensionValue;
                if (bv.getWorld() != null)
                {
                    innerSource = outerSource.withWorld(bv.getWorld());
                }
                else
                {
                    throw new InternalExpressionException("'in_dimension' accepts only world-localized block arguments");
                }
            }
            else
            {
                String dimString = dimensionValue.getString().toLowerCase(Locale.ROOT);
                switch (dimString)
                {
                    case "nether":
                    case "the_nether":
                        innerSource = outerSource.withWorld(outerSource.getMinecraftServer().getWorld(DimensionType.THE_NETHER));
                        break;
                    case "end":
                    case "the_end":
                        innerSource = outerSource.withWorld(outerSource.getMinecraftServer().getWorld(DimensionType.THE_END));
                        break;
                    case "overworld":
                    case "over_world":
                        innerSource = outerSource.withWorld(outerSource.getMinecraftServer().getWorld(DimensionType.OVERWORLD));
                        break;
                    default:
                        throw new InternalExpressionException("Incorrect dimension string: "+dimString);
                }
            };
            if (innerSource.getWorld() == outerSource.getWorld()) return lv.get(1);
            Context newCtx = c.recreate();
            ((CarpetContext) newCtx).s = innerSource;
            newCtx.variables = c.variables;
            Value retval = lv.get(1).evalValue(newCtx);
            return (cc, tt) -> retval;
        });

        this.expr.addLazyFunction("plop", 4, (c, t, lv) ->{
            BlockArgument locator = BlockArgument.findIn((CarpetContext)c, lv, 0);
            if (lv.size() <= locator.offset)
                throw new InternalExpressionException("'plop' needs extra argument indicating what to plop");
            String what = lv.get(locator.offset).evalValue(c).getString();
            Value [] result = new Value[]{Value.NULL};
            ((CarpetContext)c).s.getMinecraftServer().submitAndJoin( () ->
            {

                Boolean res = FeatureGenerator.spawn(what, ((CarpetContext) c).s.getWorld(), locator.block.getPos());
                if (res == null)
                    return;
                if (what.equalsIgnoreCase("boulder"))  // there might be more of those
                    forceChunkUpdate(locator.block.getPos(), ((CarpetContext) c).s.getWorld());
                result[0] = new NumericValue(res);
            });
            Value ret = result[0]; // preventing from lazy evaluating of the result in case a future completes later
            return (_c, _t) -> ret;
        });

        this.expr.addLazyFunction("schedule", -1, (c, t, lv) -> {
            if (lv.size()<2)
                throw new InternalExpressionException("'schedule' should have at least 2 arguments, delay and call name");
            long delay = NumericValue.asNumber(lv.get(0).evalValue(c)).getLong();

            FunctionArgument functionArgument = FunctionArgument.findIn(c, this.expr.module, lv, 1, true, true);

            CarpetServer.scriptServer.events.scheduleCall(
                    (CarpetContext) c,
                    functionArgument.function,
                    functionArgument.resolveArgs(c, Context.NONE),
                    delay
            );
            return (c_, t_) -> Value.TRUE;
        });

        this.expr.addLazyFunction("logger", 1, (c, t, lv) ->
        {
            Value res = lv.get(0).evalValue(c);
            CarpetSettings.LOG.error(res.getString());
            return (_c, _t) -> res; // pass through for variables
        });

        this.expr.addLazyFunction("read_file", 2, (c, t, lv) -> {
            String resource = recognizeResource(lv.get(0).evalValue(c));
            String origtype = lv.get(1).evalValue(c).getString().toLowerCase(Locale.ROOT);
            boolean shared = origtype.startsWith("shared_");
            String type = shared ? origtype.substring(7) : origtype; //len(shared_)
            if (!type.equals("raw") && !type.equals("text") && !type.equals("nbt"))
                throw new InternalExpressionException("Unsupported file type: "+origtype);
            Value retVal;
            if (type.equals("nbt"))
            {
                Tag state = ((CarpetScriptHost)((CarpetContext)c).host).readFileTag(resource, shared);
                if (state == null) return LazyValue.NULL;
                retVal = new NBTSerializableValue(state);
            }
            else
            {
                List<String> content = ((CarpetScriptHost) ((CarpetContext) c).host).readTextResource(resource, shared);
                if (content == null) return LazyValue.NULL;
                retVal = ListValue.wrap(content.stream().map(StringValue::new).collect(Collectors.toList()));
            }
            return (cc, tt) -> retVal;
        });

        this.expr.addLazyFunction("delete_file", 2, (c, t, lv) -> {
            String resource = recognizeResource(lv.get(0).evalValue(c));
            String origtype = lv.get(1).evalValue(c).getString().toLowerCase(Locale.ROOT);
            boolean shared = origtype.startsWith("shared_");
            String type = shared ? origtype.substring(7) : origtype; //len(shared_)
            if (!type.equals("raw") && !type.equals("text") && !type.equals("nbt"))
                throw new InternalExpressionException("Unsupported file type: "+origtype);
            boolean success = ((CarpetScriptHost)((CarpetContext)c).host).removeResourceFile(resource, shared, type);
            return success?LazyValue.TRUE:LazyValue.FALSE;
        });

        this.expr.addLazyFunction("write_file", -1, (c, t, lv) -> {
            if (lv.size() < 3) throw new InternalExpressionException("'write_file' requires three or more arguments");
            String resource = recognizeResource(lv.get(0).evalValue(c));
            String origtype = lv.get(1).evalValue(c).getString().toLowerCase(Locale.ROOT);
            boolean shared = origtype.startsWith("shared_");
            String type = shared ? origtype.substring(7) : origtype; //len(shared_)
            if (!type.equals("raw") && !type.equals("text") && !type.equals("nbt"))
                throw new InternalExpressionException("Unsupported file type: "+origtype);
            boolean success;
            if (type.equals("nbt"))
            {
                Value val = lv.get(2).evalValue(c);
                NBTSerializableValue tagValue =  (val instanceof NBTSerializableValue)
                        ? (NBTSerializableValue) val
                        : new NBTSerializableValue(val.getString());
                Tag tag = tagValue.getTag();
                success = ((CarpetScriptHost)((CarpetContext)c).host).writeTagFile(tag, resource, shared);
            }
            else
            {
                List<String> data = new ArrayList<>();
                if (lv.size()==3)
                {
                    Value val = lv.get(2).evalValue(c);
                    if (val instanceof ListValue)
                    {
                        List<Value> lval = ((ListValue) val).getItems();
                        lval.forEach(v -> data.add(v.getString()));
                    }
                    else
                    {
                        data.add(val.getString());
                    }
                }
                else
                {
                    for(int i = 2; i < lv.size(); i++)
                    {
                        data.add(lv.get(i).evalValue(c).getString());
                    }
                }
                success = ((CarpetScriptHost) ((CarpetContext) c).host).appendLogFile(resource, shared, type, data);
            }
            return success?LazyValue.TRUE:LazyValue.FALSE;
        });

        //write_file

        this.expr.addLazyFunction("load_app_data", -1, (c, t, lv) ->
        {
            String file = null;
            boolean shared = false;
            if (lv.size()>0)
            {
                file = recognizeResource(lv.get(0).evalValue(c));
                if (lv.size() > 1)
                {
                    shared = lv.get(1).evalValue(c).getBoolean();
                }
            }
            Tag state = ((CarpetScriptHost)((CarpetContext)c).host).readFileTag(file, shared);
            if (state == null)
                return (cc, tt) -> Value.NULL;
            Value retVal = new NBTSerializableValue(state);
            return (cc, tt) -> retVal;
        });

        this.expr.addLazyFunction("store_app_data", -1, (c, t, lv) ->
        {
            if (lv.size() == 0)
                throw new InternalExpressionException("'store_app_data' needs NBT tag and an optional file");
            Value val = lv.get(0).evalValue(c);
            String file = null;
            boolean shared = false;
            if (lv.size()>1)
            {
                file = recognizeResource(lv.get(1).evalValue(c));
                if (lv.size() > 2)
                {
                    shared = lv.get(2).evalValue(c).getBoolean();
                }
            }
            NBTSerializableValue tagValue =  (val instanceof NBTSerializableValue)
                    ? (NBTSerializableValue) val
                    : new NBTSerializableValue(val.getString());
            Tag tag = tagValue.getTag();
            boolean success = ((CarpetScriptHost)((CarpetContext)c).host).writeTagFile(tag, file, shared);
            return success?LazyValue.TRUE:LazyValue.FALSE;
        });

        this.expr.addLazyFunction("statistic", 3, (c, t, lv) ->
        {
            Value playerValue = lv.get(0).evalValue(c);
            CarpetContext cc = (CarpetContext)c;
            ServerPlayerEntity player = EntityValue.getPlayerByValue(cc.s.getMinecraftServer(), playerValue);
            if (player == null) return LazyValue.NULL;
            Identifier category;
            Identifier statName;
            try
            {
                category = new Identifier(lv.get(1).evalValue(c).getString());
                statName = new Identifier(lv.get(2).evalValue(c).getString());
            }
            catch (InvalidIdentifierException e)
            {
                return LazyValue.NULL;
            }
            StatType<?> type = Registry.STAT_TYPE.get(category);
            if (type == null)
                return LazyValue.NULL;
            Stat<?> stat = getStat(type, statName);
            if (stat == null)
                return LazyValue.NULL;
            return (_c, _t) -> new NumericValue(player.getStatHandler().getStat(stat));
        });
    }

    private <T> Stat<T> getStat(StatType<T> type, Identifier id)
    {
        T key = type.getRegistry().get(id);
        if (key == null || !((StatTypeInterface)type).hasStatCreated(key))
            return null;
        return type.getOrCreateStat(key);
    }


    /**
     * <h1>.</h1>
     * @param expression expression
     * @param source source
     * @param origin origin
     */
    public CarpetExpression(Module module, String expression, ServerCommandSource source, BlockPos origin)
    {
        this.origin = origin;
        this.source = source;
        this.expr = new Expression(expression);
        this.expr.asAModule(module);


        API_BlockManipulation();
        API_EntityManipulation();
        API_InventoryManipulation();
        API_IteratingOverAreasOfBlocks();
        API_AuxiliaryAspects();
        API_Scoreboard();
        API_Interapperability();
    }

    public boolean fillAndScanCommand(ScriptHost host, int x, int y, int z)
    {
        if (CarpetServer.scriptServer.stopAll)
            return false;
        try
        {
            Context context = new CarpetContext(host, source, origin).
                    with("x", (c, t) -> new NumericValue(x - origin.getX()).bindTo("x")).
                    with("y", (c, t) -> new NumericValue(y - origin.getY()).bindTo("y")).
                    with("z", (c, t) -> new NumericValue(z - origin.getZ()).bindTo("z")).
                    with("_", (c, t) -> new BlockValue(null, source.getWorld(), new BlockPos(x, y, z)).bindTo("_"));
            Entity e = source.getEntity();
            if (e==null)
            {
                Value nullPlayer = Value.NULL.reboundedTo("p");
                context.with("p", (cc, tt) -> nullPlayer );
            }
            else
            {
                Value playerValue = new EntityValue(e).bindTo("p");
                context.with("p", (cc, tt) -> playerValue);
            }
            return this.expr.eval(context).getBoolean();
        }
        catch (ExpressionException e)
        {
            throw new CarpetExpressionException(e.getMessage(), e.stack);
        }
        catch (ArithmeticException ae)
        {
            throw new CarpetExpressionException("Math doesn't compute... "+ae.getMessage(), null);
        }
    }

    public String scriptRunCommand(ScriptHost host, BlockPos pos)
    {
        if (CarpetServer.scriptServer.stopAll)
            return "SCRIPTING PAUSED";
        try
        {
            Context context = new CarpetContext(host, source, origin).
                    with("x", (c, t) -> new NumericValue(pos.getX() - origin.getX()).bindTo("x")).
                    with("y", (c, t) -> new NumericValue(pos.getY() - origin.getY()).bindTo("y")).
                    with("z", (c, t) -> new NumericValue(pos.getZ() - origin.getZ()).bindTo("z"));
            Entity e = source.getEntity();
            if (e==null)
            {
                Value nullPlayer = Value.NULL.reboundedTo("p");
                context.with("p", (cc, tt) -> nullPlayer );
            }
            else
            {
                Value playerValue = new EntityValue(e).bindTo("p");
                context.with("p", (cc, tt) -> playerValue);
            }
            return this.expr.eval(context).getString();
        }
        catch (ExpressionException e)
        {
            throw new CarpetExpressionException(e.getMessage(), e.stack);
        }
        catch (ArithmeticException ae)
        {
            throw new CarpetExpressionException("Math doesn't compute... "+ae.getMessage(), null);
        }
    }
}
