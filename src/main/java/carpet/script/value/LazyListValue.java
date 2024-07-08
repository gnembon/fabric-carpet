package carpet.script.value;

import carpet.script.exception.InternalExpressionException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public abstract class LazyListValue extends AbstractListValue implements Iterator<Value>
{
    public static LazyListValue rangeDouble(double from, double to, double step)
    {
        return new LazyListValue()
        {
            {
                if (step == 0)
                {
                    throw new InternalExpressionException("Range will never end with a zero step");
                }
                this.start = from;
                this.current = this.start;
                this.limit = to;
                this.stepp = step;
            }

            private final double start;
            private double current;
            private final double limit;
            private final double stepp;

            @Override
            public Value next()
            {
                Value val = new NumericValue(current);
                current += stepp;
                return val;
            }

            @Override
            public void reset()
            {
                current = start;
            }

            @Override
            public boolean hasNext()
            {
                return stepp > 0 ? (current < limit) : (current > limit);
            }

            @Override
            public String getString()
            {
                return String.format(Locale.ROOT, "[%s, %s, ..., %s)", NumericValue.of(start).getString(), NumericValue.of(start + stepp).getString(), NumericValue.of(limit).getString());
            }
        };
    }

    public static LazyListValue rangeLong(long from, long to, long step)
    {
        return new LazyListValue()
        {
            {
                if (step == 0)
                {
                    throw new InternalExpressionException("Range will never end with a zero step");
                }
                this.start = from;
                this.current = this.start;
                this.limit = to;
                this.stepp = step;
            }

            private final long start;
            private long current;
            private final long limit;
            private final long stepp;

            @Override
            public Value next()
            {
                Value val = new NumericValue(current);
                current += stepp;
                return val;
            }

            @Override
            public void reset()
            {
                current = start;
            }

            @Override
            public boolean hasNext()
            {
                return stepp > 0 ? (current < limit) : (current > limit);
            }

            @Override
            public String getString()
            {
                return String.format(Locale.ROOT, "[%s, %s, ..., %s)", NumericValue.of(start).getString(), NumericValue.of(start + stepp).getString(), NumericValue.of(limit).getString());
            }
        };
    }

    @Override
    public String getString()
    {
        return "[...]";
    }

    @Override
    public boolean getBoolean()
    {
        return hasNext();
    }

    @Override
    public void fatality()
    {
        reset();
    }

    public abstract void reset();

    @Override
    public Iterator<Value> iterator()
    {
        return this;
    }

    public List<Value> unroll()
    {
        List<Value> result = new ArrayList<>();
        this.forEachRemaining(v -> {
            if (v != Value.EOL)
            {
                result.add(v);
            }
        });
        fatality();
        return result;
    }

    @Override
    public Value slice(long from, Long to)
    {
        if (to == null || to < 0)
        {
            to = (long) Integer.MAX_VALUE;
        }
        if (from < 0)
        {
            from = 0;
        }
        if (from > to)
        {
            return ListValue.of();
        }
        List<Value> result = new ArrayList<>();
        int i;
        for (i = 0; i < from; i++)
        {
            if (hasNext())
            {
                next();
            }
            else
            {
                fatality();
                return ListValue.wrap(result);
            }
        }
        for (i = (int) from; i < to; i++)
        {
            if (hasNext())
            {
                result.add(next());
            }
            else
            {
                fatality();
                return ListValue.wrap(result);
            }
        }
        return ListValue.wrap(result);
    }

    @Override
    public Value add(Value other)
    {
        throw new InternalExpressionException("Cannot add to iterators");
    }

    @Override
    public boolean equals(Object o)
    {
        return false;
    }

    @Override
    public String getTypeString()
    {
        return "iterator";
    }

    @Override
    public Object clone()
    {
        Object copy;
        try
        {
            copy = super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            throw new InternalExpressionException("Cannot copy iterators");
        }
        ((LazyListValue) copy).reset();
        return copy;
    }

    @Override
    public Value fromConstant()
    {
        return (Value) clone();
    }

    @Override
    public int hashCode()
    {
        return ("i" + getString()).hashCode();
    }

    @Override
    public Tag toTag(boolean force, RegistryAccess regs)
    {
        if (!force)
        {
            throw new NBTSerializableValue.IncompatibleTypeException(this);
        }
        return StringTag.valueOf(getString());
    }
}
