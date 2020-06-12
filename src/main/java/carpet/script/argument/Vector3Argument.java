package carpet.script.argument;

import carpet.script.CarpetContext;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.BlockValue;
import carpet.script.value.ListValue;
import carpet.script.value.NumericValue;
import carpet.script.value.Value;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class Vector3Argument extends Argument
{
    public Vec3d vec;
    public final double yaw;
    public final double pitch;
    public boolean fromBlock;
    private Vector3Argument(Vec3d v, int o)
    {
        super(o);
        this.vec = v;
        this.yaw = 0.0D;
        this.pitch = 0.0D;
    }
    private Vector3Argument(Vec3d v, int o, double y, double p)
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


    public static Vector3Argument findIn(CarpetContext c, List<LazyValue> params, int offset)
    {
        return findIn(c,params, offset, false);
    }

    public static Vector3Argument findIn(CarpetContext c, List<LazyValue> params, int offset, boolean optionalDirection)
    {
        try
        {
            Value v1 = params.get(0 + offset).evalValue(c);
            if (v1 instanceof BlockValue)
            {
                return (new Vector3Argument(new Vec3d(((BlockValue) v1).getPos()).add(0.5,0.5,0.5), 1+offset)).fromBlock();
            }
            if (v1 instanceof ListValue)
            {
                List<Value> args = ((ListValue) v1).getItems();
                Vec3d pos = new Vec3d(
                        NumericValue.asNumber(args.get(0)).getDouble(),
                        NumericValue.asNumber(args.get(1)).getDouble(),
                        NumericValue.asNumber(args.get(2)).getDouble());
                double yaw = 0.0D;
                double pitch = 0.0D;
                if (args.size()>3 && optionalDirection)
                {
                    yaw = NumericValue.asNumber(args.get(3)).getDouble();
                    pitch = NumericValue.asNumber(args.get(4)).getDouble();
                }
                return new Vector3Argument(pos,offset+1, yaw, pitch);
            }
            Vec3d pos = new Vec3d(
                    NumericValue.asNumber(v1).getDouble(),
                    NumericValue.asNumber(params.get(1 + offset).evalValue(c)).getDouble(),
                    NumericValue.asNumber(params.get(2 + offset).evalValue(c)).getDouble());
            double yaw = 0.0D;
            double pitch = 0.0D;
            int eatenLength = 3;
            if (params.size()>3+offset && optionalDirection)
            {
                yaw = NumericValue.asNumber(params.get(3 + offset).evalValue(c)).getDouble();
                pitch = NumericValue.asNumber(params.get(4 + offset).evalValue(c)).getDouble();
                eatenLength = 5;
            }

            return new Vector3Argument(pos,offset+eatenLength, yaw, pitch);
        }
        catch (IndexOutOfBoundsException e)
        {
            throw new InternalExpressionException("Position argument should be defined either by three coordinates (a triple or by three arguments), or a positioned block value");
        }
    }

    public static Vector3Argument findIn(List<Value> params, int offset, boolean optionalDirection)
    {
        try
        {
            Value v1 = params.get(0 + offset);
            if (v1 instanceof BlockValue)
            {
                return (new Vector3Argument(new Vec3d(((BlockValue) v1).getPos()).add(0.5,0.5,0.5), 1+offset)).fromBlock();
            }
            if (v1 instanceof ListValue)
            {
                List<Value> args = ((ListValue) v1).getItems();
                Vec3d pos = new Vec3d(
                        NumericValue.asNumber(args.get(0)).getDouble(),
                        NumericValue.asNumber(args.get(1)).getDouble(),
                        NumericValue.asNumber(args.get(2)).getDouble());
                double yaw = 0.0D;
                double pitch = 0.0D;
                if (args.size()>3 && optionalDirection)
                {
                    yaw = NumericValue.asNumber(args.get(3)).getDouble();
                    pitch = NumericValue.asNumber(args.get(4)).getDouble();
                }
                return new Vector3Argument(pos,offset+1, yaw, pitch);
            }
            Vec3d pos = new Vec3d(
                    NumericValue.asNumber(v1).getDouble(),
                    NumericValue.asNumber(params.get(1 + offset)).getDouble(),
                    NumericValue.asNumber(params.get(2 + offset)).getDouble());
            double yaw = 0.0D;
            double pitch = 0.0D;
            int eatenLength = 3;
            if (params.size()>3+offset && optionalDirection)
            {
                yaw = NumericValue.asNumber(params.get(3 + offset)).getDouble();
                pitch = NumericValue.asNumber(params.get(4 + offset)).getDouble();
                eatenLength = 5;
            }

            return new Vector3Argument(pos,offset+eatenLength, yaw, pitch);
        }
        catch (IndexOutOfBoundsException e)
        {
            throw new InternalExpressionException("Position argument should be defined either by three coordinates (a triple or by three arguments), or a positioned block value");
        }
    }
}
