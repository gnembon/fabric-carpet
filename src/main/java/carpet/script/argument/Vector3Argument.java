package carpet.script.argument;

import carpet.script.exception.InternalExpressionException;
import carpet.script.value.BlockValue;
import carpet.script.value.EntityValue;
import carpet.script.value.ListValue;
import carpet.script.value.NumericValue;
import carpet.script.value.Value;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class Vector3Argument extends Argument
{
    public Vec3 vec;
    public final double yaw;
    public final double pitch;
    public boolean fromBlock = false;
    @Nullable
    public Entity entity = null;

    private Vector3Argument(Vec3 v, int o)
    {
        super(o);
        this.vec = v;
        this.yaw = 0.0D;
        this.pitch = 0.0D;
    }

    private Vector3Argument(Vec3 v, int o, double y, double p)
    {
        super(o);
        this.vec = v;
        this.yaw = y;
        this.pitch = p;
    }

    private Vector3Argument fromBlock()
    {
        fromBlock = true;
        return this;
    }

    private Vector3Argument withEntity(Entity e)
    {
        entity = e;
        return this;
    }

    public static Vector3Argument findIn(List<Value> params, int offset)
    {
        return findIn(params, offset, false, false);
    }

    public static Vector3Argument findIn(List<Value> params, int offset, boolean optionalDirection, boolean optionalEntity)
    {
        return findIn(params.listIterator(offset), offset, optionalDirection, optionalEntity);
    }

    public static Vector3Argument findIn(Iterator<Value> params, int offset, boolean optionalDirection, boolean optionalEntity)
    {
        try
        {
            Value v1 = params.next();
            if (v1 instanceof BlockValue)
            {
                return (new Vector3Argument(Vec3.atCenterOf(((BlockValue) v1).getPos()), 1 + offset)).fromBlock();
            }
            if (optionalEntity && v1 instanceof EntityValue)
            {
                Entity e = ((EntityValue) v1).getEntity();
                return new Vector3Argument(e.position(), 1 + offset).withEntity(e);
            }
            if (v1 instanceof ListValue)
            {
                List<Value> args = ((ListValue) v1).getItems();
                Vec3 pos = new Vec3(
                        NumericValue.asNumber(args.get(0)).getDouble(),
                        NumericValue.asNumber(args.get(1)).getDouble(),
                        NumericValue.asNumber(args.get(2)).getDouble());
                double yaw = 0.0D;
                double pitch = 0.0D;
                if (args.size() > 3 && optionalDirection)
                {
                    yaw = NumericValue.asNumber(args.get(3)).getDouble();
                    pitch = NumericValue.asNumber(args.get(4)).getDouble();
                }
                return new Vector3Argument(pos, offset + 1, yaw, pitch);
            }
            Vec3 pos = new Vec3(
                    NumericValue.asNumber(v1).getDouble(),
                    NumericValue.asNumber(params.next()).getDouble(),
                    NumericValue.asNumber(params.next()).getDouble());
            double yaw = 0.0D;
            double pitch = 0.0D;
            int eatenLength = 3;
            if (params.hasNext() && optionalDirection)
            {
                yaw = NumericValue.asNumber(params.next()).getDouble();
                pitch = NumericValue.asNumber(params.next()).getDouble();
                eatenLength = 5;
            }

            return new Vector3Argument(pos, offset + eatenLength, yaw, pitch);
        }
        catch (IndexOutOfBoundsException | NoSuchElementException e)
        {
            throw new InternalExpressionException("Position argument should be defined either by three coordinates (a triple or by three arguments), or a positioned block value");
        }
    }
}
