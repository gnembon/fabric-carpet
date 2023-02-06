package carpet.script.value;

import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ThrowStatement;
import carpet.script.exception.Throwables;
import carpet.script.external.Carpet;
import carpet.script.external.Vanilla;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

public class ValueConversions
{
    public static Value of(final BlockPos pos)
    {
        return ListValue.of(new NumericValue(pos.getX()), new NumericValue(pos.getY()), new NumericValue(pos.getZ()));
    }

    public static Value of(final Vec3 vec)
    {
        return ListValue.of(new NumericValue(vec.x), new NumericValue(vec.y), new NumericValue(vec.z));
    }

    public static Value of(final ColumnPos cpos)
    {
        return ListValue.of(new NumericValue(cpos.x()), new NumericValue(cpos.z()));
    }

    public static Value of(final ServerLevel world)
    {
        return of(world.dimension().location());
    }

    public static Value of(final MaterialColor color)
    {
        return ListValue.of(StringValue.of(Carpet.getMapColorNames().get(color)), ofRGB(color.col));
    }

    public static <T extends Number> Value of(final MinMaxBounds<T> range)
    {
        return ListValue.of(NumericValue.of(range.getMin()), NumericValue.of(range.getMax()));
    }

    @Deprecated
    public static Value of(final ItemStack stack)
    {
        return stack == null || stack.isEmpty() ? Value.NULL : ListValue.of(
                of(BuiltInRegistries.ITEM.getKey(stack.getItem())),
                new NumericValue(stack.getCount()),
                NBTSerializableValue.fromStack(stack)
        );
    }

    public static Value of(final ItemStack stack, final RegistryAccess regs)
    {
        return stack == null || stack.isEmpty() ? Value.NULL : ListValue.of(
                of(regs.registryOrThrow(Registries.ITEM).getKey(stack.getItem())),
                new NumericValue(stack.getCount()),
                NBTSerializableValue.fromStack(stack)
        );
    }

    public static Value of(final Objective objective)
    {
        return ListValue.of(
                StringValue.of(objective.getName()),
                StringValue.of(objective.getCriteria().getName())
        );
    }


    public static Value of(final ObjectiveCriteria criteria)
    {
        return ListValue.of(
                StringValue.of(criteria.getName()),
                BooleanValue.of(criteria.isReadOnly())
        );
    }


    public static Value of(final ParticleOptions particle)
    {
        final String repr = particle.writeToString();
        return StringValue.of(repr.startsWith("minecraft:") ? repr.substring(10) : repr);
    }

    public static Value ofRGB(final int value)
    {
        return new NumericValue(value * 256 + 255);
    }

    public static Level dimFromValue(final Value dimensionValue, final MinecraftServer server)
    {
        if (dimensionValue instanceof EntityValue)
        {
            return ((EntityValue) dimensionValue).getEntity().getCommandSenderWorld();
        }
        else if (dimensionValue instanceof BlockValue bv)
        {
            if (bv.getWorld() != null)
            {
                return bv.getWorld();
            }
            else
            {
                throw new InternalExpressionException("dimension argument accepts only world-localized block arguments");
            }
        }
        else
        {
            final String dimString = dimensionValue.getString().toLowerCase(Locale.ROOT);
            return switch (dimString) {
                case "nether", "the_nether" -> server.getLevel(Level.NETHER);
                case "end", "the_end" -> server.getLevel(Level.END);
                case "overworld", "over_world" -> server.getLevel(Level.OVERWORLD);
                default -> {
                    ResourceKey<Level> dim = null;
                    final ResourceLocation id = new ResourceLocation(dimString);
                    // not using RegistryKey.of since that one creates on check
                    for (final ResourceKey<Level> world : (server.levelKeys()))
                    {
                        if (id.equals(world.location()))
                        {
                            dim = world;
                            break;
                        }
                    }
                    if (dim == null)
                    {
                        throw new ThrowStatement(dimString, Throwables.UNKNOWN_DIMENSION);
                    }
                    yield server.getLevel(dim);
                }
            };
        }
    }

    public static Value of(final ResourceKey<?> dim)
    {
        return of(dim.location());
    }

    public static Value of(final TagKey<?> tagKey)
    {
        return of(tagKey.location());
    }

    public static Value of(final ResourceLocation id)
    {
        if (id == null) // should be Value.NULL
        {
            return Value.NULL;
        }
        return new StringValue(simplify(id));
    }

    public static String simplify(final ResourceLocation id)
    {
        if (id == null) // should be Value.NULL
        {
            return "";
        }
        if (id.getNamespace().equals("minecraft"))
        {
            return id.getPath();
        }
        return id.toString();
    }

    public static Value of(final GlobalPos pos)
    {
        return ListValue.of(
                ValueConversions.of(pos.dimension()),
                ValueConversions.of(pos.pos())
        );
    }

    public static Value fromPath(final ServerLevel world, final Path path)
    {
        final List<Value> nodes = new ArrayList<>();
        for (int i = 0, len = path.getNodeCount(); i < len; i++)
        {
            final Node node = path.getNode(i);
            nodes.add(ListValue.of(
                    new BlockValue(null, world, node.asBlockPos()),
                    new StringValue(node.type.name().toLowerCase(Locale.ROOT)),
                    new NumericValue(node.costMalus),
                    BooleanValue.of(node.closed)
            ));
        }
        return ListValue.wrap(nodes);
    }

    public static Value fromTimedMemory(final Entity e, final long expiry, final Object v)
    {
        final Value ret = fromEntityMemory(e, v);
        return ret.isNull() || expiry == Long.MAX_VALUE ? ret : ListValue.of(ret, new NumericValue(expiry));
    }

    private static Value fromEntityMemory(final Entity e, Object v)
    {
        if (v instanceof GlobalPos pos)
        {
            return of(pos);
        }
        if (v instanceof final Entity entity)
        {
            return new EntityValue(entity);
        }
        if (v instanceof final BlockPos pos)
        {
            return new BlockValue(null, e.getCommandSenderWorld(), pos);
        }
        if (v instanceof final Number number)
        {
            return new NumericValue(number.doubleValue());
        }
        if (v instanceof final Boolean bool)
        {
            return BooleanValue.of(bool);
        }
        if (v instanceof final UUID uuid)
        {
            return ofUUID((ServerLevel) e.getCommandSenderWorld(), uuid);
        }
        if (v instanceof final DamageSource source)
        {
            return ListValue.of(
                    new StringValue(source.getMsgId()),
                    source.getEntity() == null ? Value.NULL : new EntityValue(source.getEntity())
            );
        }
        if (v instanceof final Path path)
        {
            return fromPath((ServerLevel) e.getCommandSenderWorld(), path);
        }
        if (v instanceof final PositionTracker tracker)
        {
            return new BlockValue(null, e.getCommandSenderWorld(), tracker.currentBlockPosition());
        }
        if (v instanceof final WalkTarget target)
        {
            return ListValue.of(
                    new BlockValue(null, e.getCommandSenderWorld(), target.getTarget().currentBlockPosition()),
                    new NumericValue(target.getSpeedModifier()),
                    new NumericValue(target.getCloseEnoughDist())
            );
        }
        if (v instanceof final NearestVisibleLivingEntities nvle)
        {
            v = StreamSupport.stream(nvle.findAll(entity -> true).spliterator(), false).toList();
        }
        if (v instanceof final Set<?> set)
        {
            v = new ArrayList<>(set);
        }
        if (v instanceof final List<?> l)
        {
            if (l.isEmpty())
            {
                return ListValue.of();
            }
            final Object el = l.get(0);
            if (el instanceof final Entity entity)
            {
                return ListValue.wrap(l.stream().map(o -> new EntityValue(entity)));
            }
            if (el instanceof final GlobalPos pos)
            {
                return ListValue.wrap(l.stream().map(o -> of(pos)));
            }
        }
        return Value.NULL;
    }

    private static Value ofUUID(final ServerLevel entityWorld, final UUID uuid)
    {
        final Entity current = entityWorld.getEntity(uuid);
        return ListValue.of(
                current == null ? Value.NULL : new EntityValue(current),
                new StringValue(uuid.toString())
        );
    }

    public static Value of(final AABB box)
    {
        return ListValue.of(
                ListValue.fromTriple(box.minX, box.minY, box.minZ),
                ListValue.fromTriple(box.maxX, box.maxY, box.maxZ)
        );
    }

    public static Value of(final BoundingBox box)
    {
        return ListValue.of(
                ListValue.fromTriple(box.minX(), box.minY(), box.minZ()),
                ListValue.fromTriple(box.maxX(), box.maxY(), box.maxZ())
        );
    }

    public static Value of(final StructureStart structure, final RegistryAccess regs)
    {
        if (structure == null || structure == StructureStart.INVALID_START)
        {
            return Value.NULL;
        }
        final BoundingBox boundingBox = structure.getBoundingBox();
        if (boundingBox.maxX() < boundingBox.minX() || boundingBox.maxY() < boundingBox.minY() || boundingBox.maxZ() < boundingBox.minZ())
        {
            return Value.NULL;
        }
        final Map<Value, Value> ret = new HashMap<>();
        ret.put(new StringValue("box"), of(boundingBox));
        final List<Value> pieces = new ArrayList<>();
        for (final StructurePiece piece : structure.getPieces())
        {
            final BoundingBox box = piece.getBoundingBox();
            if (box.maxX() >= box.minX() && box.maxY() >= box.minY() && box.maxZ() >= box.minZ())
            {
                pieces.add(ListValue.of(
                        new StringValue(NBTSerializableValue.nameFromRegistryId(regs.registryOrThrow(Registries.STRUCTURE_PIECE).getKey(piece.getType()))),
                        (piece.getOrientation() == null) ? Value.NULL : new StringValue(piece.getOrientation().getName()),
                        ListValue.fromTriple(box.minX(), box.minY(), box.minZ()),
                        ListValue.fromTriple(box.maxX(), box.maxY(), box.maxZ())
                ));
            }
        }
        ret.put(new StringValue("pieces"), ListValue.wrap(pieces));
        return MapValue.wrap(ret);
    }

    public static Value fromProperty(final BlockState state, final Property<?> p)
    {
        final Comparable<?> object = state.getValue(p);
        if (object instanceof Boolean || object instanceof Number)
        {
            return StringValue.of(object.toString());
        }
        if (object instanceof final StringRepresentable stringRepresentable)
        {
            return StringValue.of(stringRepresentable.getSerializedName());
        }
        throw new InternalExpressionException("Unknown property type: " + p.getName());
    }

    record SlotParam(/* Nullable */ String type, int id)
    {
        public ListValue build()
        {
            return ListValue.of(StringValue.of(type), new NumericValue(id));
        }
    }

    private static final Int2ObjectMap<SlotParam> slotIdsToSlotParams = new Int2ObjectOpenHashMap<>()
    {{
        int n;
        //covers blocks, player hotbar and inventory, and all default inventories
        for (n = 0; n < 54; ++n)
        {
            put(n, new SlotParam(null, n));
        }
        for (n = 0; n < 27; ++n)
        {
            put(200 + n, new SlotParam("enderchest", n));
        }

        // villager
        for (n = 0; n < 8; ++n)
        {
            put(300 + n, new SlotParam(null, n));
        }

        // horse, llamas, donkeys, etc.
        // two first slots are for saddle and armour
        for (n = 0; n < 15; ++n)
        {
            put(500 + n, new SlotParam(null, n + 2));
        }
        // weapon main hand
        put(98, new SlotParam("equipment", 0));
        // offhand
        put(99, new SlotParam("equipment", 5));
        // feet, legs, chest, head
        for (n = 0; n < 4; ++n)
        {
            put(100 + n, new SlotParam("equipment", n + 1));
        }
        //horse defaults saddle
        put(400, new SlotParam(null, 0));
        // armor
        put(401, new SlotParam(null, 1));
        // chest itself on the donkey is wierd - use NBT to alter that.
        //hashMap.put("horse.chest", 499);
    }};

    public static Value ofVanillaSlotResult(final int itemSlot)
    {
        final SlotParam ret = slotIdsToSlotParams.get(itemSlot);
        return ret == null ? ListValue.of(Value.NULL, new NumericValue(itemSlot)) : ret.build();
    }

    public static Value ofBlockPredicate(final RegistryAccess registryAccess, final Predicate<BlockInWorld> blockPredicate)
    {
        final Vanilla.BlockPredicatePayload payload = Vanilla.BlockPredicatePayload.of(blockPredicate);
        final Registry<Block> blocks = registryAccess.registryOrThrow(Registries.BLOCK);
        return ListValue.of(
                payload.state() == null ? Value.NULL : of(blocks.getKey(payload.state().getBlock())),
                payload.tagKey() == null ? Value.NULL : of(blocks.getTag(payload.tagKey()).get().key()),
                MapValue.wrap(payload.properties()),
                payload.tag() == null ? Value.NULL : new NBTSerializableValue(payload.tag())
        );
    }

    public static ItemStack getItemStackFromValue(final Value value, final boolean withCount, final RegistryAccess regs)
    {
        if (value.isNull())
        {
            return ItemStack.EMPTY;
        }
        final String name;
        int count = 1;
        CompoundTag nbtTag = null;
        if (value instanceof ListValue list)
        {
            if (list.length() != 3)
            {
                throw new ThrowStatement("item definition from list of size " + list.length(), Throwables.UNKNOWN_ITEM);
            }
            final List<Value> items = list.getItems();
            name = items.get(0).getString();
            if (withCount)
            {
                count = NumericValue.asNumber(items.get(1)).getInt();
            }
            final Value nbtValue = items.get(2);
            if (!nbtValue.isNull())
            {
                nbtTag = ((NBTSerializableValue) NBTSerializableValue.fromValue(nbtValue)).getCompoundTag();
            }
        }
        else
        {
            name = value.getString();
        }
        final ItemInput itemInput = NBTSerializableValue.parseItem(name, nbtTag, regs);
        try
        {
            return itemInput.createItemStack(count, false);
        }
        catch (final CommandSyntaxException cse)
        {
            if (!withCount)
            {
                throw new IllegalStateException("Unexpected exception while creating item stack of " + name + ". All items should be able to stack to one", cse);
            }
            else
            {
                throw new ThrowStatement(count + " stack size of " + name, Throwables.UNKNOWN_ITEM);
            }
        }
    }

    public static Value guess(final ServerLevel serverWorld, final Object o)
    {
        if (o == null)
        {
            return Value.NULL;
        }
        if (o instanceof final List<?> list)
        {
            return ListValue.wrap(list.stream().map(oo -> guess(serverWorld, oo)));
        }
        if (o instanceof final BlockPos pos)
        {
            return new BlockValue(null, serverWorld, pos);
        }
        if (o instanceof final Entity e)
        {
            return EntityValue.of(e);
        }
        if (o instanceof final Vec3 vec3)
        {
            return of(vec3);
        }
        if (o instanceof final Vec3i vec3i)
        {
            return of(new BlockPos(vec3i));
        }
        if (o instanceof final AABB aabb)
        {
            return of(aabb);
        }
        if (o instanceof final BoundingBox bb)
        {
            return of(bb);
        }
        if (o instanceof final ItemStack itemStack)
        {
            return of(itemStack, serverWorld.registryAccess());
        }
        if (o instanceof final Boolean bool)
        {
            return BooleanValue.of(bool);
        }
        if (o instanceof final Number number)
        {
            return NumericValue.of(number);
        }
        if (o instanceof final ResourceLocation resourceLocation)
        {
            return of(resourceLocation);
        }
        return StringValue.of(o.toString());
    }
}
