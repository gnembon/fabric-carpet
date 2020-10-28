package carpet.script.value;

import carpet.script.exception.InternalExpressionException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.brain.LookTarget;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;

import net.minecraft.util.dynamic.GlobalPos;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
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
import java.util.stream.Collectors;

public class ValueConversions
{
    public static Value fromPos(BlockPos pos)
    {
        return ListValue.of(new NumericValue(pos.getX()), new NumericValue(pos.getY()), new NumericValue(pos.getZ()));
    }

    public static Value fromPosOptional(BlockPos pos)
    {
        if (pos == null) return Value.NULL;
        return ListValue.of(new NumericValue(pos.getX()), new NumericValue(pos.getY()), new NumericValue(pos.getZ()));
    }

    public static Value fromVec(Vec3d vec)
    {
        return ListValue.of(new NumericValue(vec.x), new NumericValue(vec.y), new NumericValue(vec.z));
    }

    public static Value dimName(ServerWorld world)
    {
        return ofId(world.getRegistryKey().getValue());
    }

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
                        throw new InternalExpressionException("Incorrect dimension string: "+dimString);
                    return server.getWorld(dim);
            }
        }
    }

    public static Value dimName(RegistryKey<World> dim)
    {
        return ofId(dim.getValue());
    }

    public static Value ofId(Identifier id)
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

    public static Value ofGlobalPos(GlobalPos pos)
    {
        return ListValue.of(
                ValueConversions.dimName(pos.getDimension()),
                ValueConversions.fromPos(pos.getPos())
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
            return ofGlobalPos(pos);
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
                return ListValue.wrap((List<Value>) l.stream().map(o ->  ofGlobalPos((GlobalPos) o)).collect(Collectors.toList()));
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

    public static Value fromStructure(StructureStart<?> structure)
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
        }
        BlockBox boundingBox = structure.getBoundingBox();
        Map<Value, Value> ret = new HashMap<>();
        ret.put(new StringValue("box"), ListValue.of(
                ListValue.fromTriple(boundingBox.minX, boundingBox.minY, boundingBox.minZ),
                ListValue.fromTriple(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ)
        ));
        ret.put(new StringValue("pieces"), ListValue.wrap(pieces));
        return MapValue.wrap(ret);
    }
}
