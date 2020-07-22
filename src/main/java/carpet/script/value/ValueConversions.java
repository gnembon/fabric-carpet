package carpet.script.value;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.brain.LookTarget;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.GlobalPos;
import net.minecraft.util.Identifier;
import net.minecraft.util.Timestamp;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class ValueConversions
{
    public static Value fromPos(BlockPos pos)
    {
        return ListValue.of(new NumericValue(pos.getX()), new NumericValue(pos.getY()), new NumericValue(pos.getZ()));
    }

    public static Value dimName(ServerWorld world)
    {
        return ofId(Registry.DIMENSION_TYPE.getId(world.getDimension().getType()));
    }

    public static Value dimName(DimensionType dim)
    {
        return ofId(Registry.DIMENSION_TYPE.getId(dim));
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
        for (PathNode node: path.getNodes())
        {
            nodes.add( ListValue.of(
                    new BlockValue(null, world, node.getPos()),
                    new StringValue(node.type.name().toLowerCase(Locale.ROOT)),
                    new NumericValue(node.penalty),
                    new NumericValue(node.visited)
            ));
        }
        return ListValue.wrap(nodes);
    }

    public static Value fromEntityMemory(Entity e, Object v)
    {
        if (v instanceof GlobalPos)
        {
            GlobalPos pos = (GlobalPos)v;
            return ofGlobalPos(pos);
        };
        if (v instanceof Entity)
        {
            return new EntityValue((Entity)v);
        };
        if (v instanceof BlockPos)
        {
            return new BlockValue(null, (ServerWorld) e.getEntityWorld(), (BlockPos) v);
        };
        if (v instanceof Timestamp)
        {
            return new NumericValue(((Timestamp) v).getTime());
        }
        if (v instanceof Long)
        {
            return  new NumericValue((Long)v);
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
                    new BlockValue(null, (ServerWorld) e.getEntityWorld(), ((LookTarget)v).getBlockPos()),
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
}
