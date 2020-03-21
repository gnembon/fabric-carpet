package carpet.script;

import carpet.CarpetServer;
import carpet.fakes.MinecraftServerInterface;
import carpet.fakes.BiomeArrayInterface;
import carpet.fakes.StatTypeInterface;
import carpet.fakes.ThreadedAnvilChunkStorageInterface;
import carpet.helpers.FeatureGenerator;
import carpet.mixins.ChunkTicketManager_scarpetMixin;
import carpet.mixins.PointOfInterest_scarpetMixin;
import carpet.mixins.ServerChunkManager_scarpetMixin;
import carpet.script.Fluff.TriFunction;
import carpet.script.bundled.Module;
import carpet.script.exception.CarpetExpressionException;
import carpet.script.exception.ExitStatement;
import carpet.script.exception.ExpressionException;
import carpet.script.exception.InternalExpressionException;
import carpet.CarpetSettings;
import carpet.script.value.BlockValue;
import carpet.script.value.EntityValue;
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
import net.minecraft.command.arguments.ParticleArgumentType;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatType;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.LiteralText;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;

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
        BlockValue block = BlockValue.fromParams(cc, params, 0).block;
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
        BlockValue block = BlockValue.fromParams(cc, params, 0).block;
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
        BlockValue block = BlockValue.fromParams(cc, params, 0).block;
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

    private static final Map<String, ParticleEffect> particleCache = new HashMap<>();
    private ParticleEffect getParticleData(String name)
    {
        ParticleEffect particle = particleCache.get(name);
        if (particle != null)
            return particle;
        try
        {
            particle = ParticleArgumentType.readParameters(new StringReader(name));
        }
        catch (CommandSyntaxException e)
        {
            throw new InternalExpressionException("No such particle: "+name);
        }
        particleCache.put(name, particle);
        return particle;
    }


    private boolean isStraight(Vec3d from, Vec3d to, double density)
    {
        if ( (from.x == to.x && from.y == to.y) || (from.x == to.x && from.z == to.z) || (from.y == to.y && from.z == to.z))
            return from.distanceTo(to) / density > 20;
        return false;
    }

    private int drawOptimizedParticleLine(ServerWorld world, ParticleEffect particle, Vec3d from, Vec3d to, double density)
    {
        double distance = from.distanceTo(to);
        int particles = (int)(distance/density);
        Vec3d towards = to.subtract(from);
        int parts = 0;
        for (PlayerEntity player : world.getPlayers())
        {
            world.spawnParticles((ServerPlayerEntity)player, particle, true,
                    (towards.x)/2+from.x, (towards.y)/2+from.y, (towards.z)/2+from.z, particles/3,
                    towards.x/6, towards.y/6, towards.z/6, 0.0);
            world.spawnParticles((ServerPlayerEntity)player, particle, true,
                    from.x, from.y, from.z,1,0.0,0.0,0.0,0.0);
            world.spawnParticles((ServerPlayerEntity)player, particle, true,
                    to.x, to.y, to.z,1,0.0,0.0,0.0,0.0);
            parts += particles/3+2;
        }
        int divider = 6;
        while (particles/divider > 1)
        {
            int center = (divider*2)/3;
            int dev = 2*divider;
            for (PlayerEntity player : world.getPlayers())
            {
                world.spawnParticles((ServerPlayerEntity)player, particle, true,
                        (towards.x)/center+from.x, (towards.y)/center+from.y, (towards.z)/center+from.z, particles/divider,
                        towards.x/dev, towards.y/dev, towards.z/dev, 0.0);
                world.spawnParticles((ServerPlayerEntity)player, particle, true,
                        (towards.x)*(1.0-1.0/center)+from.x, (towards.y)*(1.0-1.0/center)+from.y, (towards.z)*(1.0-1.0/center)+from.z, particles/divider,
                        towards.x/dev, towards.y/dev, towards.z/dev, 0.0);
            }
            parts += 2*particles/divider;
            divider = 2*divider;
        }
        return parts;
    }

    private int drawParticleLine(ServerWorld world, ParticleEffect particle, Vec3d from, Vec3d to, double density)
    {
        if (isStraight(from, to, density)) return drawOptimizedParticleLine(world, particle, from, to, density);
        double lineLengthSq = from.squaredDistanceTo(to);
        if (lineLengthSq == 0) return 0;
        Vec3d incvec = to.subtract(from).multiply(2*density/sqrt(lineLengthSq));
        int pcount = 0;
        for (Vec3d delta = new Vec3d(0.0,0.0,0.0);
             delta.lengthSquared()<lineLengthSq;
             delta = delta.add(incvec.multiply(Expression.randomizer.nextFloat())))
        {
            for (PlayerEntity player : world.getPlayers())
            {
                world.spawnParticles((ServerPlayerEntity)player, particle, true,
                        delta.x+from.x, delta.y+from.y, delta.z+from.z, 1,
                        0.0, 0.0, 0.0, 0.0);
                pcount ++;
            }
        }
        return pcount;
    }

    private void forceChunkUpdate(BlockPos pos, ServerWorld world)
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

    public void API_BlockManipulation()
    {
        this.expr.addLazyFunction("block", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            if (lv.size() == 0)
            {
                throw new InternalExpressionException("Block requires at least one parameter");
            }
            BlockValue retval = BlockValue.fromParams(cc, lv, 0, true).block;
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
            CompoundTag tag = BlockValue.fromParams(cc, lv, 0, true).block.getData();
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
            BlockValue.LocatorResult locator = BlockValue.fromParams(cc, lv, 0, false);
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

            Value ret = ListValue.wrap(store.get(condition, pos, (int)radius, status).map( p ->
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
            BlockValue.LocatorResult locator = BlockValue.fromParams(cc, lv, 0, false);
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
            BlockValue.LocatorResult locator = BlockValue.fromParams((CarpetContext)c, lv, 0);
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
            BlockPos pos = BlockValue.fromParams((CarpetContext)c, lv, 0).block.getPos();
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
            BlockValue.LocatorResult locator = BlockValue.fromParams((CarpetContext)c, lv, 1);
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
            Value retval = ((CarpetContext) c).s.getWorld().isChunkLoaded(BlockValue.fromParams((CarpetContext) c, lv, 0).block.getPos()) ? Value.TRUE : Value.FALSE;
            return (c_, t_) -> retval;
        });

        // Deprecated, use loaded_status as more indicative
        this.expr.addLazyFunction("loaded_ep", -1, (c, t, lv) ->
        {
            BlockPos pos = BlockValue.fromParams((CarpetContext)c, lv, 0).block.getPos();
            Value retval = ((CarpetContext)c).s.getWorld().getChunkManager().shouldTickChunk(new ChunkPos(pos))?Value.TRUE : Value.FALSE;
            return (c_, t_) -> retval;
        });

        this.expr.addLazyFunction("loaded_status", -1, (c, t, lv) ->
        {
            BlockPos pos = BlockValue.fromParams((CarpetContext)c, lv, 0).block.getPos();
            WorldChunk chunk = ((CarpetContext)c).s.getWorld().getChunkManager().getWorldChunk(pos.getX()>>4, pos.getZ()>>4, false);
            if (chunk == null)
                return LazyValue.ZERO;
            Value retval = new NumericValue(chunk.getLevelType().ordinal());
            return (c_, t_) -> retval;
        });

        this.expr.addLazyFunction("generation_status", -1, (c, t, lv) ->
        {
            BlockValue.LocatorResult locatorResult = BlockValue.fromParams((CarpetContext)c, lv, 0);
            BlockPos pos = locatorResult.block.getPos();
            boolean forceLoad = false;
            if (lv.size() > locatorResult.offset)
                forceLoad = lv.get(locatorResult.offset).evalValue(c, Context.BOOLEAN).getBoolean();
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
                    (ChunkTicketManager_scarpetMixin) ((ServerChunkManager_scarpetMixin) world.getChunkManager())
                            .getTicketManager()
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
                BlockValue.LocatorResult locatorResult = BlockValue.fromParams((CarpetContext) c, lv, 0);
                BlockPos pos = locatorResult.block.getPos();
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
            try
            {
                CarpetSettings.impendingFillSkipUpdates = true;
                Value ret = lv.get(0).evalValue(c, t);
                return (cc, tt) -> ret;
            }
            finally
            {
                CarpetSettings.impendingFillSkipUpdates = previous;
            }
        });

        this.expr.addLazyFunction("set", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerWorld world = cc.s.getWorld();
            if (lv.size() < 2 || lv.size() % 2 == 1)
                throw new InternalExpressionException("'set' should have at least 2 params and odd attributes");
            BlockValue.LocatorResult targetLocator = BlockValue.fromParams(cc, lv, 0);
            BlockValue.LocatorResult sourceLocator = BlockValue.fromParams(cc, lv, targetLocator.offset, true);
            BlockState sourceBlockState = sourceLocator.block.getBlockState();
            BlockState targetBlockState = world.getBlockState(targetLocator.block.getPos());
            if (sourceLocator.offset < lv.size())
            {
                StateManager<Block, BlockState> states = sourceBlockState.getBlock().getStateManager();
                for (int i = sourceLocator.offset; i < lv.size(); i += 2)
                {
                    String paramString = lv.get(i).evalValue(c).getString();
                    Property<?> property = states.getProperty(paramString);
                    if (property == null)
                        throw new InternalExpressionException("Property " + paramString + " doesn't apply to " + sourceLocator.block.getString());
                    String paramValue = lv.get(i + 1).evalValue(c).getString();
                    sourceBlockState = setProperty(property, paramString, paramValue, sourceBlockState);
                }
            }

            CompoundTag data = sourceLocator.block.getData();

            if (sourceBlockState == targetBlockState && data == null)
                return (c_, t_) -> Value.FALSE;
            BlockState finalSourceBlockState = sourceBlockState;
            BlockPos targetPos = targetLocator.block.getPos();
            cc.s.getMinecraftServer().submitAndJoin( () ->
            {
                Clearable.clear(world.getBlockEntity(targetPos));
                world.setBlockState(targetPos, finalSourceBlockState, 2);
                if (data != null)
                {
                    BlockEntity be = world.getBlockEntity(targetPos);
                    if (be != null)
                    {
                        CompoundTag destTag = data.copy();
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
            BlockValue.LocatorResult locator = BlockValue.fromParams(cc, lv, 0);
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
            Value ret = new NBTSerializableValue(outtag);
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
            BlockValue.LocatorResult locator = BlockValue.fromParams(cc, lv, 1);
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

        this.expr.addLazyFunction("place_item", -1, (c, t, lv) ->
        {
            if (lv.size()<2)
                throw new InternalExpressionException("'place_item' takes at least 2 parameters: item and block, or position, to place onto");
            CarpetContext cc = (CarpetContext) c;
            String itemString = lv.get(0).evalValue(c).getString();
            BlockValue.VectorLocator locator = BlockValue.locateVec(cc, lv, 1);
            ItemStackArgument stackArg = NBTSerializableValue.parseItem(itemString);
            BlockPos where = new BlockPos(locator.vec);
            String facing = "up";
            if (lv.size() > locator.offset)
                facing = lv.get(locator.offset).evalValue(c).getString();
            boolean sneakPlace = false;
            if (lv.size() > locator.offset+1)
                sneakPlace = lv.get(locator.offset+1).evalValue(c).getBoolean();
            if (stackArg.getItem() instanceof BlockItem)
            {
                BlockItem blockItem = (BlockItem) stackArg.getItem();
                BlockValue.PlacementContext ctx;
                try
                {
                    ctx = BlockValue.PlacementContext.from(cc.s.getWorld(), where, facing, sneakPlace, stackArg.createStack(1, false));
                }
                catch (CommandSyntaxException e)
                {
                    throw new InternalExpressionException(e.getMessage());
                }
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
            BlockValue.LocatorResult locator = BlockValue.fromParams((CarpetContext) c, lv, 0);
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
            BlockValue.LocatorResult locator = BlockValue.fromParams((CarpetContext) c, lv, 0);
            BlockState state = locator.block.getBlockState();
            StateManager<Block, BlockState> states = state.getBlock().getStateManager();
            Value res = ListValue.wrap(states.getProperties().stream().map(
                    p -> new StringValue(p.getName())).collect(Collectors.toList())
            );
            return (_c, _t) -> res;
        });

        this.expr.addLazyFunction("biome", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            BlockValue.LocatorResult locator = BlockValue.fromParams(cc, lv, 0);
            ServerWorld world = cc.s.getWorld();
            BlockPos pos = locator.block.getPos();
            Biome biome = world.getBiome(pos);
            Value res = new StringValue(biome.getTranslationKey().replaceFirst("^biome\\.minecraft\\.", ""));
            return (_c, _t) -> res;
        });

        this.expr.addLazyFunction("set_biome", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            BlockValue.LocatorResult locator = BlockValue.fromParams(cc, lv, 0);
            if (lv.size() == locator.offset)
                throw new InternalExpressionException("'set_biome' needs a biome name as an argument");
            String biomeName = lv.get(locator.offset+0).evalValue(c).getString();
            Biome biome = Registry.BIOME.get(new Identifier(biomeName));
            if (biome == null)
                throw new InternalExpressionException("Unknown biome: "+biomeName);
            ServerWorld world = cc.s.getWorld();
            BlockPos pos = locator.block.getPos();
            Chunk chunk = world.getChunk(pos);
            ((BiomeArrayInterface)chunk.getBiomeArray()).setBiomeAtIndex(pos, world,  biome);
            this.forceChunkUpdate(pos, world);
            return LazyValue.NULL;
        });

        this.expr.addLazyFunction("structure_references", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            BlockValue.LocatorResult locator = BlockValue.fromParams(cc, lv, 0);
            ServerWorld world = cc.s.getWorld();
            BlockPos pos = locator.block.getPos();
            Map<String, LongSet> references = world.getChunk(pos).getStructureReferences();
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

        this.expr.addLazyFunction("structures", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            BlockValue.LocatorResult locator = BlockValue.fromParams(cc, lv, 0);

            ServerWorld world = cc.s.getWorld();
            BlockPos pos = locator.block.getPos();
            Map<String, StructureStart> structures = world.getChunk(pos).getStructureStarts();
            if (lv.size() == locator.offset)
            {
                Map<Value, Value> structureList = new HashMap<>();
                for (Map.Entry<String, StructureStart> entry : structures.entrySet())
                {
                    StructureStart start = entry.getValue();
                    if (start == StructureStart.DEFAULT)
                        continue;
                    BlockBox box = start.getBoundingBox();
                    ListValue coord1 = ListValue.of(new NumericValue(box.minX), new NumericValue(box.minY), new NumericValue(box.minZ));
                    ListValue coord2 = ListValue.of(new NumericValue(box.maxX), new NumericValue(box.maxY), new NumericValue(box.maxZ));
                    structureList.put(new StringValue(FeatureGenerator.structureToFeature.get(entry.getKey()).get(0)), ListValue.of(coord1, coord2));
                }
                Value ret = MapValue.wrap(structureList);
                return (_c, _t) -> ret;
            }
            String structureName = lv.get(locator.offset).evalValue(c).getString().toLowerCase(Locale.ROOT);
            StructureStart start = structures.get(FeatureGenerator.featureToStructure.get(structureName));
            if (start == null || start == StructureStart.DEFAULT) return LazyValue.NULL;
            List<Value> pieces = new ArrayList<>();
            for (StructurePiece piece : start.getChildren())
            {
                BlockBox box = piece.getBoundingBox();
                pieces.add(ListValue.of(
                        new StringValue( NBTSerializableValue.nameFromRegistryId(Registry.STRUCTURE_PIECE.getId(piece.getType()))),
                        (piece.getFacing()== null)?Value.NULL: new StringValue(piece.getFacing().getName()),
                        ListValue.of(new NumericValue(box.minX), new NumericValue(box.minY), new NumericValue(box.minZ)),
                        ListValue.of(new NumericValue(box.maxX), new NumericValue(box.maxY), new NumericValue(box.maxZ))
                ));
            }
            Value ret = ListValue.wrap(pieces);
            return (_c, _t) -> ret;
        });

        this.expr.addLazyFunction("set_structure", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            BlockValue.LocatorResult locator = BlockValue.fromParams(cc, lv, 0);

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
                    BlockValue.LocatorResult locator = BlockValue.fromParamValues(cc, listVal, 0, false, false);
                    requestedChunks.add(new ChunkPos(locator.block.getPos()));
                    while (listVal.size() > locator.offset)
                    {
                        locator = BlockValue.fromParamValues(cc, listVal, locator.offset, false, false);
                        requestedChunks.add(new ChunkPos(locator.block.getPos()));
                    }
                }
                else
                {
                    BlockValue.LocatorResult locator = BlockValue.fromParamValues(cc, Collections.singletonList(first), 0, false, false);
                    requestedChunks.add(new ChunkPos(locator.block.getPos()));
                }
            }
            else
            {
                BlockValue.LocatorResult locator = BlockValue.fromParams(cc, lv, 0);
                ChunkPos from = new ChunkPos(locator.block.getPos());
                if (lv.size() > locator.offset)
                {
                    locator = BlockValue.fromParams(cc, lv, locator.offset);
                    ChunkPos to = new ChunkPos(locator.block.getPos());
                    int xmax = Math.max(from.x, to.x);
                    int zmax = Math.max(from.z, to.z);
                    for (int x = Math.min(from.x, to.x); x <= xmax; x++) for (int z = Math.min(from.z, to.z); z <= zmax; z++)
                    {
                        requestedChunks.add(new ChunkPos(x,z));
                    }
                    CarpetSettings.LOG.error("Regenerating from "+Math.min(from.x, to.x)+", "+Math.min(from.z, to.z)+" to "+Math.max(from.x, to.x)+", "+Math.max(from.z, to.z));
                }
                else
                {
                    requestedChunks.add(from);
                }
            }


            ServerWorld world = cc.s.getWorld();

            ((CarpetContext)c).s.getMinecraftServer().submitAndJoin( () ->
            {
                ((ThreadedAnvilChunkStorageInterface) world.getChunkManager().threadedAnvilChunkStorage).regenerateChunkRegion(requestedChunks);
                for (ChunkPos chpos: requestedChunks)
                    if (world.getChunk(chpos.x, chpos.z, ChunkStatus.FULL, false) != null)
                        this.forceChunkUpdate(chpos.getCenterBlockPos(), world);
            });
            return LazyValue.NULL;
        });

    }

    public void API_InventoryManipulation()
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

    public void API_EntityManipulation()
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

            BlockValue.VectorLocator position = BlockValue.locateVec(cc, lv, 1);
            if (position.fromBlock)
                position.vec = position.vec.subtract(0, 0.5, 0);
            CompoundTag tag = new CompoundTag();
            boolean hasTag = false;
            if (lv.size() > position.offset)
            {
                Value nbt = lv.get(position.offset).evalValue(c);
                NBTSerializableValue v = (nbt instanceof NBTSerializableValue) ? (NBTSerializableValue) nbt
                        : NBTSerializableValue.parseString(nbt.getString());
                if (v != null)
                {
                    hasTag = true;
                    tag = v.getCompoundTag();
                }
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
            return lv.get(0);
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

    public void API_IteratingOverAreasOfBlocks()
    {
        this.expr.addLazyFunction("scan", -1, (c, t, lv) ->
        {
            int lvsise = lv.size();
            if (lvsise != 7 && lvsise != 10)
                throw new InternalExpressionException("'scan' takes 2, or 3 triples of coords, and the expression");
            int cx = (int) NumericValue.asNumber(lv.get(0).evalValue(c)).getLong();
            int cy = (int) NumericValue.asNumber(lv.get(1).evalValue(c)).getLong();
            int cz = (int) NumericValue.asNumber(lv.get(2).evalValue(c)).getLong();
            int xrange = (int) NumericValue.asNumber(lv.get(3).evalValue(c)).getLong();
            int yrange = (int) NumericValue.asNumber(lv.get(4).evalValue(c)).getLong();
            int zrange = (int) NumericValue.asNumber(lv.get(5).evalValue(c)).getLong();
            int xprange = xrange;
            int yprange = yrange;
            int zprange = zrange;
            LazyValue expr;
            if (lvsise == 7)
            {
                expr = lv.get(6);
            }
            else
            {
                xprange = (int) NumericValue.asNumber(lv.get(6).evalValue(c)).getLong();
                yprange = (int) NumericValue.asNumber(lv.get(7).evalValue(c)).getLong();
                zprange = (int) NumericValue.asNumber(lv.get(8).evalValue(c)).getLong();
                expr = lv.get(9);
            }

            //saving outer scope
            LazyValue _x = c.getVariable("_x");
            LazyValue _y = c.getVariable("_y");
            LazyValue _z = c.getVariable("_z");
            LazyValue __ = c.getVariable("_");
            int sCount = 0;
            for (int y=cy-yrange; y <= cy+yprange; y++)
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
                        Value result = expr.evalValue(c, t);
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

        this.expr.addLazyFunction("volume", 7, (c, t, lv) ->
        {
            int xi = (int) NumericValue.asNumber(lv.get(0).evalValue(c)).getLong();
            int yi = (int) NumericValue.asNumber(lv.get(1).evalValue(c)).getLong();
            int zi = (int) NumericValue.asNumber(lv.get(2).evalValue(c)).getLong();
            int xj = (int) NumericValue.asNumber(lv.get(3).evalValue(c)).getLong();
            int yj = (int) NumericValue.asNumber(lv.get(4).evalValue(c)).getLong();
            int zj = (int) NumericValue.asNumber(lv.get(5).evalValue(c)).getLong();
            int minx = min(xi, xj);
            int miny = min(yi, yj);
            int minz = min(zi, zj);
            int maxx = max(xi, xj);
            int maxy = max(yi, yj);
            int maxz = max(zi, zj);
            LazyValue expr = lv.get(6);

            //saving outer scope
            LazyValue _x = c.getVariable("_x");
            LazyValue _y = c.getVariable("_y");
            LazyValue _z = c.getVariable("_z");
            LazyValue __ = c.getVariable("_");
            int sCount = 0;
            for (int y=miny; y <= maxy; y++)
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
                        Value result = expr.evalValue(c, t);
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

            BlockPos center = BlockValue.fromParams((CarpetContext) c, lv,0).block.getPos();
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
            if (lv.size() != 3 && lv.size() != 6 && lv.size() != 9)
            {
                throw new InternalExpressionException("Rectangular region should be specified with 3, 6, or 9 coordinates");
            }
            int cx;
            int cy;
            int cz;
            int sminx;
            int sminy;
            int sminz;
            int smaxx;
            int smaxy;
            int smaxz;
            try
            {
                cx = (int)((NumericValue) lv.get(0).evalValue(c)).getLong();
                cy = (int)((NumericValue) lv.get(1).evalValue(c)).getLong();
                cz = (int)((NumericValue) lv.get(2).evalValue(c)).getLong();
                if (lv.size()==3) // only done this way because of stupid Java lambda final reqs
                {
                    sminx = 1;
                    sminy = 1;
                    sminz = 1;
                    smaxx = 1;
                    smaxy = 1;
                    smaxz = 1;
                }
                else if (lv.size()==6)
                {
                    sminx = (int) ((NumericValue) lv.get(3).evalValue(c)).getLong();
                    sminy = (int) ((NumericValue) lv.get(4).evalValue(c)).getLong();
                    sminz = (int) ((NumericValue) lv.get(5).evalValue(c)).getLong();
                    smaxx = sminx;
                    smaxy = sminy;
                    smaxz = sminz;
                }
                else // size == 9
                {
                    sminx = (int) ((NumericValue) lv.get(3).evalValue(c)).getLong();
                    sminy = (int) ((NumericValue) lv.get(4).evalValue(c)).getLong();
                    sminz = (int) ((NumericValue) lv.get(5).evalValue(c)).getLong();
                    smaxx = (int)((NumericValue) lv.get(6).evalValue(c)).getLong();
                    smaxy = (int)((NumericValue) lv.get(7).evalValue(c)).getLong();
                    smaxz = (int)((NumericValue) lv.get(8).evalValue(c)).getLong();
                }
            }
            catch (ClassCastException exc)
            {
                throw new InternalExpressionException("Attempted to pass a non-number to rect");
            }
            CarpetContext cc = (CarpetContext)c;
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
            if (lv.size() != 3 && lv.size() != 4 && lv.size() != 5)
            {
                throw new InternalExpressionException("'diamond' region should be specified with 3 to 5 coordinates");
            }

            int cx;
            int cy;
            int cz;
            int width;
            int height;
            try
            {
                cx = (int)((NumericValue) lv.get(0).evalValue(c)).getLong();
                cy = (int)((NumericValue) lv.get(1).evalValue(c)).getLong();
                cz = (int)((NumericValue) lv.get(2).evalValue(c)).getLong();
                if (lv.size()==3)
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
                else if (lv.size()==4)
                {
                    width = (int) ((NumericValue) lv.get(3).evalValue(c)).getLong();
                    height = 0;
                }
                else // size == 5
                {
                    width = (int) ((NumericValue) lv.get(3).evalValue(c)).getLong();
                    height = (int) ((NumericValue) lv.get(4).evalValue(c)).getLong();
                }
            }
            catch (ClassCastException exc)
            {
                throw new InternalExpressionException("Attempted to pass a non-number to diamond");
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

    public void API_AuxiliaryAspects()
    {
        this.expr.addLazyFunction("sound", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            Identifier soundName = new Identifier(lv.get(0).evalValue(c).getString());
            BlockValue.VectorLocator locator = BlockValue.locateVec(cc, lv, 1);
            if (Registry.SOUND_EVENT.get(soundName) == null)
                throw new InternalExpressionException("No such sound: "+soundName.getPath());
            float volume = 1.0F;
            float pitch = 1.0F;
            if (lv.size() > 0+locator.offset)
            {
                volume = (float) NumericValue.asNumber(lv.get(0+locator.offset).evalValue(c)).getDouble();
                if (lv.size() > 1+locator.offset)
                {
                    pitch = (float) NumericValue.asNumber(lv.get(1+locator.offset).evalValue(c)).getDouble();
                }
            }
            Vec3d vec = locator.vec;
            double d0 = Math.pow(volume > 1.0F ? (double)(volume * 16.0F) : 16.0D, 2.0D);
            int count = 0;
            for (ServerPlayerEntity player : cc.s.getWorld().getPlayers( (p) -> p.squaredDistanceTo(vec) < d0))
            {
                count++;
                player.networkHandler.sendPacket(new PlaySoundIdS2CPacket(soundName, SoundCategory.PLAYERS, vec, volume, pitch));
            }
            int totalPlayed = count;
            return (_c, _t) -> new NumericValue(totalPlayed);
        });

        this.expr.addLazyFunction("particle", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            MinecraftServer ms = cc.s.getMinecraftServer();
            ServerWorld world = cc.s.getWorld();
            BlockValue.VectorLocator locator = BlockValue.locateVec(cc, lv, 1);
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
            ParticleEffect particle = getParticleData(particleName);
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
            ParticleEffect particle = getParticleData(particleName);
            BlockValue.VectorLocator pos1 = BlockValue.locateVec(cc, lv, 1);
            BlockValue.VectorLocator pos2 = BlockValue.locateVec(cc, lv, pos1.offset);
            int offset = pos2.offset;
            double density = (lv.size() > offset)? NumericValue.asNumber(lv.get(offset).evalValue(c)).getDouble():1.0;
            if (density <= 0)
            {
                throw new InternalExpressionException("Particle density should be positive");
            }
            Value retval = new NumericValue(drawParticleLine(world, particle, pos1.vec, pos2.vec, density));
            return (c_, t_) -> retval;
        });

        this.expr.addLazyFunction("particle_rect", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            ServerWorld world = cc.s.getWorld();
            String particleName = lv.get(0).evalValue(c).getString();
            ParticleEffect particle = getParticleData(particleName);
            BlockValue.VectorLocator pos1 = BlockValue.locateVec(cc, lv, 1);
            BlockValue.VectorLocator pos2 = BlockValue.locateVec(cc, lv, pos1.offset);
            int offset = pos2.offset;
            double density = 1.0;
            if (lv.size() > offset)
            {
                density = NumericValue.asNumber(lv.get(offset).evalValue(c)).getDouble();
            }
            if (density <= 0)
            {
                throw new InternalExpressionException("Particle density should be positive");
            }
            Vec3d a = pos1.vec;
            Vec3d b = pos2.vec;
            double ax = min(a.x, b.x);
            double ay = min(a.y, b.y);
            double az = min(a.z, b.z);
            double bx = max(a.x, b.x);
            double by = max(a.y, b.y);
            double bz = max(a.z, b.z);
            int pc = 0;
            pc += drawParticleLine(world, particle, new Vec3d(ax, ay, az), new Vec3d(ax, by, az), density);
            pc += drawParticleLine(world, particle, new Vec3d(ax, by, az), new Vec3d(bx, by, az), density);
            pc += drawParticleLine(world, particle, new Vec3d(bx, by, az), new Vec3d(bx, ay, az), density);
            pc += drawParticleLine(world, particle, new Vec3d(bx, ay, az), new Vec3d(ax, ay, az), density);

            pc += drawParticleLine(world, particle, new Vec3d(ax, ay, bz), new Vec3d(ax, by, bz), density);
            pc += drawParticleLine(world, particle, new Vec3d(ax, by, bz), new Vec3d(bx, by, bz), density);
            pc += drawParticleLine(world, particle, new Vec3d(bx, by, bz), new Vec3d(bx, ay, bz), density);
            pc += drawParticleLine(world, particle, new Vec3d(bx, ay, bz), new Vec3d(ax, ay, bz), density);

            pc += drawParticleLine(world, particle, new Vec3d(ax, ay, az), new Vec3d(ax, ay, bz), density);
            pc += drawParticleLine(world, particle, new Vec3d(ax, by, az), new Vec3d(ax, by, bz), density);
            pc += drawParticleLine(world, particle, new Vec3d(bx, by, az), new Vec3d(bx, by, bz), density);
            pc += drawParticleLine(world, particle, new Vec3d(bx, ay, az), new Vec3d(bx, ay, bz), density);
            int particleCount = pc;
            return (c_, t_) -> new NumericValue(particleCount);
        });

        this.expr.addLazyFunction("create_marker", -1, (c, t, lv) ->{
            CarpetContext cc = (CarpetContext)c;
            BlockState targetBlock = null;
            BlockValue.VectorLocator pointLocator;
            boolean interactable = true;
            String name;
            try
            {
                Value nameValue = lv.get(0).evalValue(c);
                name = nameValue instanceof NullValue ? "" : nameValue.getString();
                pointLocator = BlockValue.locateVec(cc, lv, 1, true);
                if (lv.size()>pointLocator.offset)
                {
                    BlockValue.LocatorResult blockLocator = BlockValue.fromParams(cc, lv, pointLocator.offset, true, true);
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
            Value v = lv.get(0).evalValue(c);
            if (v instanceof NBTSerializableValue)
                return (cc, tt) -> v;
            Value ret = NBTSerializableValue.parseString(v.getString());
            if (ret == null)
                return LazyValue.NULL;
            return (cc, tt) -> ret;
        });

        this.expr.addLazyFunction("escape_nbt", 1, (c, t, lv) -> {
            Value v = lv.get(0).evalValue(c);
            String string = v.getString();
            Value ret = new StringValue(StringTag.escape(string));
            return (cc, tt) -> ret;
        });

        //"overridden" native call that prints to stderr
        this.expr.addLazyFunction("print", 1, (c, t, lv) ->
        {
            ServerCommandSource s = ((CarpetContext)c).s;
            Value res = lv.get(0).evalValue(c);
            if (s.getEntity() instanceof  PlayerEntity)
            {
                Messenger.m((PlayerEntity) s.getEntity(), "w "+ res.getString());
            }
            else
            {
                Messenger.m(s, "w "+ res.getString());
            }
            return (_c, _t) -> res; // pass through for variables
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

        this.expr.addLazyFunction("logger", 1, (c, t, lv) ->
        {
            Value res = lv.get(0).evalValue(c);
            CarpetSettings.LOG.error(res.getString());
            return (_c, _t) -> res; // pass through for variables
        });

        this.expr.addLazyFunction("run", 1, (c, t, lv) -> {
            BlockPos target = ((CarpetContext)c).origin;
            Vec3d posf = new Vec3d((double)target.getX()+0.5D,(double)target.getY(),(double)target.getZ()+0.5D);
            ServerCommandSource s = ((CarpetContext)c).s;
            Value retval = new NumericValue(s.getMinecraftServer().getCommandManager().execute(
                    s.withPosition(posf).withSilent().withLevel(CarpetSettings.runPermissionLevel), lv.get(0).evalValue(c).getString()));
            return (c_, t_) -> retval;
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
            Value ret = new NumericValue(s.getWorld().getSeed());
            return (cc, tt) -> ret;
        });

        this.expr.addLazyFunction("current_dimension", 0, (c, t, lv) -> {
            ServerCommandSource s = ((CarpetContext)c).s;
            Value retval = new StringValue(NBTSerializableValue.nameFromRegistryId(Registry.DIMENSION_TYPE.getId(s.getWorld().dimension.getType())));
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
            }
            ((CarpetContext) c).s = innerSource;
            Value retval = lv.get(1).evalValue(c);
            ((CarpetContext) c).s = outerSource;
            return (cc, tt) -> retval;
        });

        this.expr.addLazyFunction("plop", 4, (c, t, lv) ->{
            BlockValue.LocatorResult locator = BlockValue.fromParams((CarpetContext)c, lv, 0);
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
                    this.forceChunkUpdate(locator.block.getPos(), ((CarpetContext) c).s.getWorld());
                result[0] = new NumericValue(res);
            });
            Value ret = result[0]; // preventing from lazy evaluating of the result in case a future completes later
            return (_c, _t) -> ret;
        });

        this.expr.addLazyFunction("schedule", -1, (c, t, lv) -> {
            if (lv.size()<2)
                throw new InternalExpressionException("'schedule' should have at least 2 arguments, delay and call name");
            long delay = NumericValue.asNumber(lv.get(0).evalValue(c)).getLong();

            Value functionValue = lv.get(1).evalValue(c);
            if (!(functionValue instanceof FunctionValue))
            {
                String name = functionValue.getString();
                functionValue = c.host.getAssertFunction(this.expr.module, name);
            }
            FunctionValue function = (FunctionValue)functionValue;

            CarpetContext cc = (CarpetContext)c;
            List<Value> args = new ArrayList<>();
            for (int i=2; i < lv.size(); i++)
            {
                Value arg = lv.get(i).evalValue(cc);
                args.add(arg);
            }
            if (function.getArguments().size() != args.size())
                throw new InternalExpressionException("Function "+function.getString()+" takes "+
                        function.getArguments().size()+" arguments, "+args.size()+" provided.");
            CarpetServer.scriptServer.events.scheduleCall(cc, function, args, delay);
            return (c_, t_) -> Value.TRUE;
        });

        this.expr.addLazyFunction("load_app_data", -1, (c, t, lv) ->
        {
            String file = null;
            if (lv.size()>0)
            {
                String origfile = lv.get(0).evalValue(c).getString();
                file = origfile.toLowerCase(Locale.ROOT).replaceAll("[^A-Za-z0-9]", "");
                if (file.isEmpty())
                {
                    throw new InternalExpressionException("Cannot use "+file+" as resource name - must have some letters and numbers");
                }
            }
            Tag state = ((CarpetScriptHost)((CarpetContext)c).host).getGlobalState(file);
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
            if (lv.size()>1)
            {
                String origfile = lv.get(1).evalValue(c).getString();
                file = origfile.toLowerCase(Locale.ROOT).replaceAll("[^A-Za-z0-9]", "");
                if (file.isEmpty())
                {
                    throw new InternalExpressionException("Cannot use "+file+" as resource name - must have some letters and numbers");
                }
            }
            NBTSerializableValue tagValue =  (val instanceof NBTSerializableValue)
                    ? (NBTSerializableValue) val
                    : new NBTSerializableValue(val.getString());
            Tag tag = tagValue.getTag();
            ((CarpetScriptHost)((CarpetContext)c).host).setGlobalState(tag, file);
            return (cc, tt) -> Value.NULL;
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
