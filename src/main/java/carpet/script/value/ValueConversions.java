package carpet.script.value;

import carpet.fakes.BlockPredicateInterface;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ThrowStatement;
import carpet.script.exception.Throwables;
import carpet.utils.BlockInfo;
import net.minecraft.block.BlockState;
import net.minecraft.block.MaterialColor;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.brain.LookTarget;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.predicate.NumberRange;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

import net.minecraft.state.property.Property;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.tag.TagManager;
import net.minecraft.util.Identifier;

import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.dynamic.GlobalPos;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColumnPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ValueConversions
{
    public static Value of(BlockPos pos)
    {
        return ListValue.of(new NumericValue(pos.getX()), new NumericValue(pos.getY()), new NumericValue(pos.getZ()));
    }

    public static Value ofOptional(BlockPos pos)
    {
        if (pos == null) return Value.NULL;
        return ListValue.of(new NumericValue(pos.getX()), new NumericValue(pos.getY()), new NumericValue(pos.getZ()));
    }

    public static Value of(Vec3d vec)
    {
        return ListValue.of(new NumericValue(vec.x), new NumericValue(vec.y), new NumericValue(vec.z));
    }

    public static Value of(ColumnPos cpos) { return ListValue.of(new NumericValue(cpos.x), new NumericValue(cpos.z));}

    public static Value of(ServerWorld world)
    {
        return of(world.getRegistryKey().getValue());
    }

    public static Value of(MaterialColor color) {return ListValue.of(StringValue.of(BlockInfo.mapColourName.get(color)), ofRGB(color.color));}

    public static <T extends Number> Value of(NumberRange<T> range) { return ListValue.of(NumericValue.of(range.getMin()), NumericValue.of(range.getMax()));}

    public static Value of(ItemStack stack)
    {
        if (stack == null || stack.isEmpty())
            return Value.NULL;
        return ListValue.of(
                of(Registry.ITEM.getId(stack.getItem())),
                new NumericValue(stack.getCount()),
                NBTSerializableValue.fromStack(stack)
        );
    }

    public static Value of(ScoreboardObjective objective)
    {
        return ListValue.of(
                StringValue.of(objective.getName()),
                StringValue.of(objective.getCriterion().getName())
                );
    }


    public static Value of(ScoreboardCriterion criteria)
    {
        return ListValue.of(
                StringValue.of(criteria.getName()),
                new NumericValue(criteria.isReadOnly())
        );
    }


    public static Value of(ParticleEffect particle)
    {
        String repr = particle.asString();
        if (repr.startsWith("minecraft:")) return StringValue.of(repr.substring(10));
        return StringValue.of(repr);
    }

    public static Value ofRGB(int value) {return new NumericValue(value*256+255 );}

    public static World dimFromValue(Value dimensionValue, MinecraftServer server)
    {
        if (dimensionValue instanceof EntityValue)
        {
            return ((EntityValue)dimensionValue).getEntity().getEntityWorld();
        }
        else if (dimensionValue instanceof BlockValue)
        {
            BlockValue bv = (BlockValue)dimensionValue;
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
            switch (dimString)
            {
                case "nether":
                case "the_nether":
                    return server.getWorld(World.NETHER);
                case "end":
                case "the_end":
                    return server.getWorld(World.END);
                case "overworld":
                case "over_world":
                    return server.getWorld(World.OVERWORLD);
                default:
                    RegistryKey<World> dim = null;
                    Identifier id = new Identifier(dimString);
                    // not using RegistryKey.of since that one creates on check
                    for (RegistryKey<World> world : (server.getWorldRegistryKeys()))
                    {
                        if (id.equals(world.getValue()))
                        {
                            dim = world;
                            break;
                        }
                    }
                    if (dim == null)
                        throw new ThrowStatement(dimString, Throwables.UNKNOWN_DIMENSION);
                    return server.getWorld(dim);
            }
        }
    }

    public static Value of(RegistryKey<World> dim)
    {
        return of(dim.getValue());
    }

    public static Value of(Identifier id)
    {
        if (id == null) // should be Value.NULL
            return Value.NULL;
        if (id.getNamespace().equals("minecraft"))
            return new StringValue(id.getPath());
        return new StringValue(id.toString());
    }

    public static String simplify(Identifier id)
    {
        if (id == null) // should be Value.NULL
            return "";
        if (id.getNamespace().equals("minecraft"))
            return id.getPath();
        return id.toString();
    }

    public static Value of(GlobalPos pos)
    {
        return ListValue.of(
                ValueConversions.of(pos.getDimension()),
                ValueConversions.of(pos.getPos())
        );
    }

    public static Value fromPath(ServerWorld world,  Path path)
    {
        List<Value> nodes = new ArrayList<>();
        //for (PathNode node: path.getNodes())
        for (int i = 0, len = path.getLength(); i < len; i++)
        {
            PathNode node = path.getNode(i);
            nodes.add( ListValue.of(
                    new BlockValue(null, world, node.getPos()),
                    new StringValue(node.type.name().toLowerCase(Locale.ROOT)),
                    new NumericValue(node.penalty),
                    new NumericValue(node.visited)
            ));
        }
        return ListValue.wrap(nodes);
    }

    public static Value fromTimedMemory(Entity e, long expiry, Object v)
    {
        Value ret = fromEntityMemory(e, v);
        if (ret.isNull() || expiry == Long.MAX_VALUE) return ret;
        return ListValue.of(ret, new NumericValue(expiry));
    }

    private static Value fromEntityMemory(Entity e, Object v)
    {
        if (v instanceof GlobalPos)
        {
            GlobalPos pos = (GlobalPos)v;
            return of(pos);
        }
        if (v instanceof Entity)
        {
            return new EntityValue((Entity)v);
        }
        if (v instanceof BlockPos)
        {
            return new BlockValue(null, (ServerWorld) e.getEntityWorld(), (BlockPos) v);
        }
        if (v instanceof Number)
        {
            return new NumericValue(((Number) v).doubleValue());
        }
        if (v instanceof Boolean)
        {
            return new NumericValue((Boolean) v);
        }
        if (v instanceof UUID)
        {
            return ofUUID( (ServerWorld) e.getEntityWorld(), (UUID)v);
        }
        if (v instanceof DamageSource)
        {
            DamageSource source = (DamageSource) v;
            return ListValue.of(
                    new StringValue(source.getName()),
                    source.getAttacker()==null?Value.NULL:new EntityValue(source.getAttacker())
            );
        }
        if (v instanceof Path)
        {
            return fromPath((ServerWorld)e.getEntityWorld(), (Path)v);
        }
        if (v instanceof LookTarget)
        {
            return new BlockValue(null, (ServerWorld) e.getEntityWorld(), ((LookTarget)v).getBlockPos());
        }
        if (v instanceof WalkTarget)
        {
            return ListValue.of(
                    new BlockValue(null, (ServerWorld) e.getEntityWorld(), ((WalkTarget)v).getLookTarget().getBlockPos()),
                    new NumericValue(((WalkTarget) v).getSpeed()),
                    new NumericValue(((WalkTarget) v).getCompletionRange())
            );
        }
        if (v instanceof Set)
        {
            v = new ArrayList(((Set) v));
        }
        if (v instanceof List)
        {
            List l = (List)v;
            if (l.isEmpty()) return ListValue.of();
            Object el = l.get(0);
            if (el instanceof Entity)
            {
                return ListValue.wrap((List<Value>) l.stream().map(o -> new EntityValue((Entity)o)).collect(Collectors.toList()));
            }
            if (el instanceof GlobalPos)
            {
                return ListValue.wrap((List<Value>) l.stream().map(o ->  of((GlobalPos) o)).collect(Collectors.toList()));
            }
        }
        return Value.NULL;
    }

    private static Value ofUUID(ServerWorld entityWorld, UUID uuid)
    {
        Entity current = entityWorld.getEntity(uuid);
        return ListValue.of(
                current == null?Value.NULL:new EntityValue(current),
                new StringValue(uuid.toString())
        );
    }

    public static Value of(StructureStart<?> structure)
    {
        if (structure == null || structure == StructureStart.DEFAULT) return Value.NULL;
        BlockBox boundingBox = structure.getBoundingBox();
        if (boundingBox.maxX < boundingBox.minX || boundingBox.maxY < boundingBox.minY || boundingBox.maxZ < boundingBox.minZ) return Value.NULL;
        Map<Value, Value> ret = new HashMap<>();
        ret.put(new StringValue("box"), ListValue.of(
                ListValue.fromTriple(boundingBox.minX, boundingBox.minY, boundingBox.minZ),
                ListValue.fromTriple(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ)
        ));
        List<Value> pieces = new ArrayList<>();
        for (StructurePiece piece : structure.getChildren())
        {
            BlockBox box = piece.getBoundingBox();
            if (box.maxX >= box.minX && box.maxY >= box.minY && box.maxZ >= box.minZ)
            {
                pieces.add(ListValue.of(
                        new StringValue(NBTSerializableValue.nameFromRegistryId(Registry.STRUCTURE_PIECE.getId(piece.getType()))),
                        (piece.getFacing() == null) ? Value.NULL : new StringValue(piece.getFacing().getName()),
                        ListValue.fromTriple(box.minX, box.minY, box.minZ),
                        ListValue.fromTriple(box.maxX, box.maxY, box.maxZ)
                ));
            }
        }
        ret.put(new StringValue("pieces"), ListValue.wrap(pieces));
        return MapValue.wrap(ret);
    }

    public static Value fromProperty(BlockState state, Property<?> p)
    {
        Comparable<?> object = state.get(p);
        if (object instanceof Boolean || object instanceof Number) return StringValue.of(object.toString());
        if (object instanceof StringIdentifiable)
        {
            return StringValue.of(((StringIdentifiable) object).asString());
        }
        throw new InternalExpressionException("Unknown property type: "+p.getName());
    }


    private static final Map<Integer, ListValue> slotIdsToSlotParams = new HashMap<Integer, ListValue>() {{
        int n;
        //covers blocks, player hotbar and inventory, and all default inventories
        for(n = 0; n < 54; ++n) {
            put(n, ListValue.of(Value.NULL, NumericValue.of(n)));
        }
        for(n = 0; n < 27; ++n) {
            put(200+n, ListValue.of(StringValue.of("enderchest"), NumericValue.of(n)));
        }

        // villager
        for(n = 0; n < 8; ++n) {
            put(300+n, ListValue.of(Value.NULL, NumericValue.of(n)));
        }

        // horse, llamas, donkeys, etc.
        // two first slots are for saddle and armour
        for(n = 0; n < 15; ++n) {
            put(500+n, ListValue.of(Value.NULL, NumericValue.of(n+2)));
        }
        Value equipment = StringValue.of("equipment");
        // weapon main hand
        put(98, ListValue.of(equipment, NumericValue.of(0)));
        // offhand
        put(99, ListValue.of(equipment, NumericValue.of(5)));
        // feet, legs, chest, head
        for(n = 0; n < 4; ++n) {
            put(100+n, ListValue.of(equipment, NumericValue.of(n+1)));
        }
        //horse defaults saddle
        put(400, ListValue.of(Value.NULL, NumericValue.of(0)));
        // armor
        put(401, ListValue.of(Value.NULL, NumericValue.of(1)));
        // chest itself on the donkey is wierd - use NBT to alter that.
        //hashMap.put("horse.chest", 499);
    }};

    public static Value ofVanillaSlotResult(int itemSlot)
    {
        Value ret = slotIdsToSlotParams.get(itemSlot);
        if (ret == null) return ListValue.of(Value.NULL, NumericValue.of(itemSlot));
        return ret;
    }

    public static Value ofBlockPredicate(TagManager tagManager, Predicate<CachedBlockPosition> blockPredicate)
    {
        BlockPredicateInterface predicateData = (BlockPredicateInterface) blockPredicate;
        return ListValue.of(
                predicateData.getCMBlockState()==null?Value.NULL:of(Registry.BLOCK.getId(predicateData.getCMBlockState().getBlock())),
                predicateData.getCMBlockTag()==null?Value.NULL:of(tagManager.getBlocks().getTagId(predicateData.getCMBlockTag())),
                MapValue.wrap(predicateData.getCMProperties()),
                predicateData.getCMDataTag() == null?Value.NULL:new NBTSerializableValue(predicateData.getCMDataTag())
        );
    }
}
