package carpet.script.argument;

import carpet.script.CarpetContext;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.BlockValue;
import carpet.script.value.ListValue;
import carpet.script.value.NullValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class BlockArgument extends Argument
{
    public final BlockValue block;
    private BlockArgument(BlockValue b, int o)
    {
        super(o);
        block = b;
    }
    public static BlockArgument findIn(CarpetContext c, List<LazyValue> params, int offset)
    {
        return findIn(c, params,offset, false, false);
    }

    public static BlockArgument findIn(CarpetContext c, List<LazyValue> params, int offset, boolean acceptString)
    {
        return findIn(c, params,offset, acceptString, false);
    }

    public static BlockArgument findIn(CarpetContext c, List<LazyValue> params, int offset, boolean acceptString, boolean optional)
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
            throw handleError(optional, acceptString);
        }
    }

    public static BlockArgument findInValues(CarpetContext c, List<Value> params, int offset, boolean acceptString, boolean optional)
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
            throw handleError(optional, acceptString);
        }
    }

    private static InternalExpressionException handleError(boolean optional, boolean acceptString)
    {
        String message = "Block-type argument should be defined either by three coordinates (a triple or by three arguments), or a block value";
        if (acceptString)
            message+=", or a string with block description";
        if (optional)
            message+=", or null";
        return new InternalExpressionException(message);
    }

}
