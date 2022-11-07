package carpet.script.argument;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.BlockValue;
import carpet.script.value.ListValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import net.minecraft.core.BlockPos;

public class BlockArgument extends Argument
{
    public final BlockValue block;
    public final String replacement;
    private BlockArgument(BlockValue b, int o)
    {
        super(o);
        block = b;
        replacement = null;
    }
    private BlockArgument(BlockValue b, int o, String replacement)
    {
        super(o);
        block = b;
        this.replacement = replacement;
    }

    public static BlockArgument findIn(CarpetContext c, List<Value> params, int offset)
    {
        return findIn(c, params,offset, false, false, false);
    }

    public static BlockArgument findIn(CarpetContext c, List<Value> params, int offset, boolean acceptString)
    {
        return findIn(c, params,offset, acceptString, false, false);
    }

    public static BlockArgument findIn(CarpetContext c, List<Value> params, int offset, boolean acceptString, boolean optional, boolean anyString)
    {
        return findIn(c, params.listIterator(offset), offset, acceptString, optional, anyString);
    }

    public static BlockArgument findIn(CarpetContext c, Iterator<Value> params, int offset, boolean acceptString, boolean optional, boolean anyString)
    {
        try
        {
            Value v1 = params.next();
            //add conditional from string name
            if (optional && v1.isNull())
            {
                return new BlockArgument(null, 1 + offset);
            }
            if (anyString && v1 instanceof StringValue)
            {
                return new BlockArgument(null, 1 + offset, v1.getString());
            }
            if (acceptString && v1 instanceof StringValue)
            {
                return new BlockArgument(BlockValue.fromString(v1.getString(), c.s.registryAccess()), 1 + offset);
            }
            if (v1 instanceof BlockValue)
            {
                return new BlockArgument(((BlockValue) v1), 1 + offset);
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
                                c.s.getLevel(),
                                new BlockPos(c.origin.getX() + xpos, c.origin.getY() + ypos, c.origin.getZ() + zpos)
                        ),
                        1 + offset);
            }
            int xpos = (int) NumericValue.asNumber(v1).getLong();
            int ypos = (int) NumericValue.asNumber( params.next()).getLong();
            int zpos = (int) NumericValue.asNumber( params.next()).getLong();
            return new BlockArgument(
                    new BlockValue(
                            null,
                            c.s.getLevel(),
                            new BlockPos(c.origin.getX() + xpos, c.origin.getY() + ypos, c.origin.getZ() + zpos)
                    ),
                    3 + offset
            );
        }
        catch (IndexOutOfBoundsException | NoSuchElementException e)
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
