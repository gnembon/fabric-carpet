package carpet.script.value;

import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ThrowStatement;
import carpet.script.exception.Throwables;
import carpet.script.external.Vanilla;
import carpet.script.utils.Colors;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
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
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
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

import javax.annotation.Nullable;

public class ValueConversions
{
    public static Value of(BlockPos pos)
    {
        return ListValue.of(new NumericValue(pos.getX()), new NumericValue(pos.getY()), new NumericValue(pos.getZ()));
    }

    public static Value of(Vec3 vec)
    {
        return ListValue.of(new NumericValue(vec.x), new NumericValue(vec.y), new NumericValue(vec.z));
    }

    public static Value of(ColumnPos cpos)
    {
        return ListValue.of(new NumericValue(cpos.x()), new NumericValue(cpos.z()));
    }

    public static Value of(ServerLevel world)
    {
        return of(world.dimension().location());
    }

    public static Value of(MapColor color)
    {
        return ListValue.of(StringValue.of(Colors.mapColourName.get(color)), ofRGB(color.col));
    }

    public static <T extends Number> Value of(MinMaxBounds<T> range)
    {
        return ListValue.of(
                range.min().map(NumericValue::of).orElse(Value.NULL),
                range.max().map(NumericValue::of).orElse(Value.NULL)
        );
    }

    public static Value of(ItemStack stack, RegistryAccess regs)
    {
        return stack == null || stack.isEmpty() ? Value.NULL : ListValue.of(
                of(regs.registryOrThrow(Registries.ITEM).getKey(stack.getItem())),
                new NumericValue(stack.getCount()),
                NBTSerializableValue.fromStack(stack, regs)
        );
    }

    public static Value of(Objective objective)
    {
        return ListValue.of(
                StringValue.of(objective.getName()),
                StringValue.of(objective.getCriteria().getName())
        );
    }


    public static Value of(ObjectiveCriteria criteria)
    {
        return ListValue.of(
                StringValue.of(criteria.getName()),
                BooleanValue.of(criteria.isReadOnly())
        );
    }


    public static Value of(ParticleOptions particle, RegistryAccess regs)
    {
        String repr = ParticleTypes.CODEC.encodeStart(regs.createSerializationContext(NbtOps.INSTANCE), particle).toString();
        return StringValue.of(repr.startsWith("minecraft:") ? repr.substring(10) : repr);
    }

    public static Value ofRGB(int value)
    {
        return new NumericValue(value * 256 + 255);
    }

    public static Level dimFromValue(Value dimensionValue, MinecraftServer server)
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
            String dimString = dimensionValue.getString().toLowerCase(Locale.ROOT);
            return switch (dimString) {
                case "nether", "the_nether" -> server.getLevel(Level.NETHER);
                case "end", "the_end" -> server.getLevel(Level.END);
                case "overworld", "over_world" -> server.getLevel(Level.OVERWORLD);
                default -> {
                    ResourceKey<Level> dim = null;
                    ResourceLocation id = ResourceLocation.parse(dimString);
                    // not using RegistryKey.of since that one creates on check
                    for (ResourceKey<Level> world : (server.levelKeys()))
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

    public static Value of(ResourceKey<?> dim)
    {
        return of(dim.location());
    }

    public static Value of(TagKey<?> tagKey)
    {
        return of(tagKey.location());
    }

    public static Value of(@Nullable ResourceLocation id)
    {
        if (id == null) // should be Value.NULL
        {
            return Value.NULL;
        }
        return new StringValue(simplify(id));
    }

    public static String simplify(ResourceLocation id)
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

    public static Value of(GlobalPos pos)
    {
        return ListValue.of(
                ValueConversions.of(pos.dimension()),
                ValueConversions.of(pos.pos())
        );
    }

    public static Value fromPath(ServerLevel world, Path path)
    {
        List<Value> nodes = new ArrayList<>();
        for (int i = 0, len = path.getNodeCount(); i < len; i++)
        {
            Node node = path.getNode(i);
            nodes.add(ListValue.of(
                    new BlockValue(null, world, node.asBlockPos()),
                    new StringValue(node.type.name().toLowerCase(Locale.ROOT)),
                    new NumericValue(node.costMalus),
                    BooleanValue.of(node.closed)
            ));
        }
        return ListValue.wrap(nodes);
    }

    public static Value fromTimedMemory(Entity e, long expiry, Object v)
    {
        Value ret = fromEntityMemory(e, v);
        return ret.isNull() || expiry == Long.MAX_VALUE ? ret : ListValue.of(ret, new NumericValue(expiry));
    }

    private static Value fromEntityMemory(Entity e, Object v)
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
            return new BlockValue(null, (ServerLevel) e.getCommandSenderWorld(), pos);
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
            return new BlockValue(null, (ServerLevel) e.getCommandSenderWorld(), tracker.currentBlockPosition());
        }
        if (v instanceof final WalkTarget target)
        {
            return ListValue.of(
                    new BlockValue(null, (ServerLevel) e.getCommandSenderWorld(), target.getTarget().currentBlockPosition()),
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
            Object el = l.get(0);
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

    private static Value ofUUID(ServerLevel entityWorld, UUID uuid)
    {
        Entity current = entityWorld.getEntity(uuid);
        return ListValue.of(
                current == null ? Value.NULL : new EntityValue(current),
                new StringValue(uuid.toString())
        );
    }

    public static Value of(AABB box)
    {
        return ListValue.of(
                ListValue.fromTriple(box.minX, box.minY, box.minZ),
                ListValue.fromTriple(box.maxX, box.maxY, box.maxZ)
        );
    }

    public static Value of(BoundingBox box)
    {
        return ListValue.of(
                ListValue.fromTriple(box.minX(), box.minY(), box.minZ()),
                ListValue.fromTriple(box.maxX(), box.maxY(), box.maxZ())
        );
    }

    public static Value of(StructureStart structure, RegistryAccess regs)
    {
        if (structure == null || structure == StructureStart.INVALID_START)
        {
            return Value.NULL;
        }
        BoundingBox boundingBox = structure.getBoundingBox();
        if (boundingBox.maxX() < boundingBox.minX() || boundingBox.maxY() < boundingBox.minY() || boundingBox.maxZ() < boundingBox.minZ())
        {
            return Value.NULL;
        }
        Map<Value, Value> ret = new HashMap<>();
        ret.put(new StringValue("box"), of(boundingBox));
        List<Value> pieces = new ArrayList<>();
        for (StructurePiece piece : structure.getPieces())
        {
            BoundingBox box = piece.getBoundingBox();
            if (box.maxX() >= box.minX() && box.maxY() >= box.minY() && box.maxZ() >= box.minZ())
            {
                pieces.add(ListValue.of(
                        NBTSerializableValue.nameFromRegistryId(regs.registryOrThrow(Registries.STRUCTURE_PIECE).getKey(piece.getType())),
                        (piece.getOrientation() == null) ? Value.NULL : new StringValue(piece.getOrientation().getName()),
                        ListValue.fromTriple(box.minX(), box.minY(), box.minZ()),
                        ListValue.fromTriple(box.maxX(), box.maxY(), box.maxZ())
                ));
            }
        }
        ret.put(new StringValue("pieces"), ListValue.wrap(pieces));
        return MapValue.wrap(ret);
    }

    public static Value of(final ScoreHolder scoreHolder)
    {
        return FormattedTextValue.of(scoreHolder.getFeedbackDisplayName());
    }

    public static Value fromProperty(BlockState state, Property<?> p)
    {
        Comparable<?> object = state.getValue(p);
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

    public static Value ofVanillaSlotResult(int itemSlot)
    {
        SlotParam ret = slotIdsToSlotParams.get(itemSlot);
        return ret == null ? ListValue.of(Value.NULL, new NumericValue(itemSlot)) : ret.build();
    }

    public static Value ofBlockPredicate(RegistryAccess registryAccess, Predicate<BlockInWorld> blockPredicate)
    {
        Vanilla.BlockPredicatePayload payload = Vanilla.BlockPredicatePayload.of(blockPredicate);
        Registry<Block> blocks = registryAccess.registryOrThrow(Registries.BLOCK);
        return ListValue.of(
                payload.state() == null ? Value.NULL : of(blocks.getKey(payload.state().getBlock())),
                payload.tagKey() == null ? Value.NULL : of(blocks.getTag(payload.tagKey()).get().key()),
                MapValue.wrap(payload.properties()),
                payload.tag() == null ? Value.NULL : new NBTSerializableValue(payload.tag())
        );
    }

    public static ItemStack getItemStackFromValue(Value value, boolean withCount, RegistryAccess regs)
    {
        if (value.isNull())
        {
            return ItemStack.EMPTY;
        }
        String name;
        int count = 1;
        CompoundTag nbtTag = null;
        if (value instanceof ListValue list)
        {
            if (list.length() != 3)
            {
                throw new ThrowStatement("item definition from list of size " + list.length(), Throwables.UNKNOWN_ITEM);
            }
            List<Value> items = list.getItems();
            name = items.get(0).getString();
            if (withCount)
            {
                count = NumericValue.asNumber(items.get(1)).getInt();
            }
            Value nbtValue = items.get(2);
            if (!nbtValue.isNull())
            {
                nbtTag = ((NBTSerializableValue) NBTSerializableValue.fromValue(nbtValue)).getCompoundTag();
            }
        }
        else
        {
            name = value.getString();
        }
        ItemStack itemInput = NBTSerializableValue.parseItem(name, nbtTag, regs);
        itemInput.setCount(count);
        return itemInput;
    }

    public static Value guess(ServerLevel serverWorld, Object o)
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
