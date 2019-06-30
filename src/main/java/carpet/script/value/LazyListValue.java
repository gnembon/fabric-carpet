package carpet.script.value;

import carpet.script.exception.InternalExpressionException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class LazyListValue extends ListValue implements Iterator<Value>
{
    public static LazyListValue range(long from, long to, long step)
    {
        return new LazyListValue()
        {
            {
                if (step == 0)
                    throw new InternalExpressionException("range will never end with zero step");
                this.current = from;
                this.limit = to;
                this.stepp = step;
            }
            private long current;
            private long limit;
            private long stepp;
            @Override
            public Value next()
            {
                Value val = new NumericValue(current);
                current += stepp;
                return val;
            }

            @Override
            public boolean hasNext()
            {
                return stepp > 0?(current < limit):(current > limit);
            }
        };
    }

    public LazyListValue()
    {
        super(Collections.emptyList());
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
    public abstract boolean hasNext();

    public abstract Value next();

    @Override
    public Iterator<Value> iterator() {return this;}

    public List<Value> unroll()
    {
        List<Value> result = new ArrayList<>();
        this.forEachRemaining(result::add);
        return result;
    }

    @Override
    public Value slice(long from, long to)
    {
        if (to < 0) to = Integer.MAX_VALUE;
        if (from < 0) from = 0;
        if (from > to)
            return ListValue.of();
        List<Value> result = new ArrayList<>();
        int i;
        for (i = 0; i < from; i++)
        {
            if (hasNext())
                next();
            else
                return ListValue.wrap(result);
        }
        for (i = (int)from; i < to; i++)
        {
            if (hasNext())
                result.add(next());
            else
                return ListValue.wrap(result);
        }
        return ListValue.wrap(result);
    }
    @Override
    public Value add(Value other)
    {
        throw new InternalExpressionException("Cannot add to iterators");
    }

    @Override
    public boolean equals(final Value o)
    {
        return false;
    }

}
