package carpet.script.utils;

import carpet.script.CarpetContext;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class CarpetArgParser
{
    public static Vector3Argument locateVector(CarpetContext c, List<LazyValue> params, int offset)
    {
        return locateVector(c,params, offset, false);
    }

    public static Vector3Argument locateVector(CarpetContext c, List<LazyValue> params, int offset, boolean optionalDirection)
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
            throw new InternalExpressionException("Position should be defined either by three coordinates, or a block value");
        }
    }

    public static BlockArgument locateBlock(CarpetContext c, List<LazyValue> params, int offset)
    {
        return locateBlock(c, params,offset, false, false);
    }

    public static BlockArgument locateBlock(CarpetContext c, List<LazyValue> params, int offset, boolean acceptString)
    {
        return locateBlock(c, params,offset, acceptString, false);
    }

    public static BlockArgument locateBlock(CarpetContext c, List<LazyValue> params, int offset, boolean acceptString, boolean optional)
    {
        try
        {
            Value v1 = params.get(0 + offset).evalValue(c);
            //add conditional from string name
            if (optional && v1 instanceof NullValue)
            {
                return new BlockArgument(null, 1+offset);
            }
            if (acceptString && v1 instanceof StringValue)
            {
                return new BlockArgument(BlockValue.fromString(v1.getString()), 1+offset);
            }
            if (v1 instanceof BlockValue)
            {
                return new BlockArgument(((BlockValue) v1), 1+offset);
            }
            if (v1 instanceof ListValue)
            {
                List<Value> args = ((ListValue) v1).getItems();
                int xpos = (int) NumericValue.asNumber(args.get(0)).getLong();
                int ypos = (int) NumericValue.asNumber(args.get(1)).getLong();
                int zpos = (int) NumericValue.asNumber(args.get(2)).getLong();
                return new BlockArgument(
                        new BlockValue(
                                null,
                                c.s.getWorld(),
                                new BlockPos(c.origin.getX() + xpos, c.origin.getY() + ypos, c.origin.getZ() + zpos)
                        ),
                        1+offset);
            }
            int xpos = (int) NumericValue.asNumber(v1).getLong();
            int ypos = (int) NumericValue.asNumber( params.get(1 + offset).evalValue(c)).getLong();
            int zpos = (int) NumericValue.asNumber( params.get(2 + offset).evalValue(c)).getLong();
            return new BlockArgument(
                    new BlockValue(
                            null,
                            c.s.getWorld(),
                            new BlockPos(c.origin.getX() + xpos, c.origin.getY() + ypos, c.origin.getZ() + zpos)
                    ),
                    3+offset
            );
        }
        catch (IndexOutOfBoundsException e)
        {
            throw new InternalExpressionException("Block should be defined either by three coordinates, a block value, or a proper string");
        }
    }

    public static BlockArgument fromParamValues(CarpetContext c, List<Value> params, int offset, boolean acceptString, boolean optional)
    {
        try
        {
            Value v1 = params.get(0 + offset);
            //add conditional from string name
            if (optional && v1 instanceof NullValue)
            {
                return new BlockArgument(null, 1+offset);
            }
            if (acceptString && v1 instanceof StringValue)
            {
                return new BlockArgument(BlockValue.fromString(v1.getString()), 1+offset);
            }
            if (v1 instanceof BlockValue)
            {
                return new BlockArgument(((BlockValue) v1), 1+offset);
            }
            if (v1 instanceof ListValue)
            {
                List<Value> args = ((ListValue) v1).getItems();
                int xpos = (int) NumericValue.asNumber(args.get(0)).getLong();
                int ypos = (int) NumericValue.asNumber(args.get(1)).getLong();
                int zpos = (int) NumericValue.asNumber(args.get(2)).getLong();
                return new BlockArgument(
                        new BlockValue(
                                null,
                                c.s.getWorld(),
                                new BlockPos(c.origin.getX() + xpos, c.origin.getY() + ypos, c.origin.getZ() + zpos)
                        ),
                        1+offset);
            }
            int xpos = (int) NumericValue.asNumber(v1).getLong();
            int ypos = (int) NumericValue.asNumber( params.get(1 + offset)).getLong();
            int zpos = (int) NumericValue.asNumber( params.get(2 + offset)).getLong();
            return new BlockArgument(
                    new BlockValue(
                            null,
                            c.s.getWorld(),
                            new BlockPos(c.origin.getX() + xpos, c.origin.getY() + ypos, c.origin.getZ() + zpos)
                    ),
                    3+offset
            );
        }
        catch (IndexOutOfBoundsException e)
        {
            throw new InternalExpressionException("Block should be defined either by three coordinates, a block value, or a proper string");
        }
    }

    public static class BlockArgument
    {
        public final BlockValue block;
        public final int offset;
        BlockArgument(BlockValue b, int o)
        {
            block = b;
            offset = o;
        }
    }

    public static class Vector3Argument
    {
        public Vec3d vec;
        public final int offset;
        public final double yaw;
        public final double pitch;
        public boolean fromBlock;
        Vector3Argument(Vec3d v, int o)
        {
            vec = v;
            offset = o;
            yaw = 0.0D;
            pitch = 0.0D;
        }
        Vector3Argument(Vec3d v, int o, double y, double p)
        {
            vec = v;
            offset = o;
            yaw = y;
            pitch = p;
        }

        public Vector3Argument fromBlock()
        {
            fromBlock = true;
            return this;
        }
    }
}
