package carpet.script.value;

import carpet.script.exception.InternalExpressionException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class LazyListValue extends AbstractListValue implements Iterator<Value>
{
    public static LazyListValue range(long from, long to, long step)
    {
        return new LazyListValue()
        {
            {
                if (step == 0)
                    throw new InternalExpressionException("range will never end with zero step");
                this.start = from;
                this.current = this.start;
                this.limit = to;
                this.stepp = step;
            }
            private long start;
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
            public void reset()
            {
                current = start;
            }

            @Override
            public boolean hasNext()
            {
                return stepp > 0?(current < limit):(current > limit);
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
    public abstract boolean hasNext();

    public abstract Value next();

    @Override
    public void fatality() {reset();}
    public abstract void reset();

    @Override
    public Iterator<Value> iterator() {return this;}

    public List<Value> unroll()
    {
        List<Value> result = new ArrayList<>();
        this.forEachRemaining(result::add);
        fatality();
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
            {
                fatality();
                return ListValue.wrap(result);
            }
        }
        for (i = (int)from; i < to; i++)
        {
            if (hasNext())
                result.add(next());
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
    public boolean equals(final Value o)
    {
        return false;
    }

    @Override
    public Value getElementAt(Value v2)
    {
        throw new InternalExpressionException("get element can only be obtained for regular lists");
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
            throw new InternalExpressionException("cannot copy iterator");
        }
        ((LazyListValue)copy).reset();
        return copy;
    }
}
