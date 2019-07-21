package carpet.script.value;

import carpet.script.exception.InternalExpressionException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Value implements Comparable<Value>, Cloneable
{
    public static Value FALSE = new NumericValue(0);
    public static Value TRUE = new NumericValue(1);
    public static Value ZERO = FALSE;
    public static Value NULL = new NullValue();

    public String boundVariable;

    public static <T> T assertNotNull(T t)
    {
        if (t == null)
            throw new InternalExpressionException("Operand may not be null");
        return t;
    }

    public static <T> void assertNotNull(T t1, T t2)
    {
        if (t1 == null)
            throw new InternalExpressionException("First operand may not be null");
        if (t2 == null)
            throw new InternalExpressionException("Second operand may not be null");
    }

    public boolean isBound()
    {
        return boundVariable != null;
    }
    public String getVariable()
    {
        return boundVariable;
    }
    public Value reboundedTo(String var)
    {
        Value copy = null;
        try
        {
            copy = (Value)clone();
        }
        catch (CloneNotSupportedException e)
        {
            // should not happen
            e.printStackTrace();
        }
        copy.boundVariable = var;
        return copy;
    }
    public Value bindTo(String var)
    {
        this.boundVariable = var;
        return this;
    }

    public abstract String getString();

    public String getPrettyString()
    {
        return getString();
    }


    public abstract boolean getBoolean();

    public Value add(Value o) {
        String lstr = this.getString();
        if (lstr == null) // null
            return new StringValue(o.getString());
        String rstr = o.getString();
        if (rstr == null)
        {
            return new StringValue(lstr);
        }
        return new StringValue(lstr+rstr);
    }
    public Value subtract(Value v)
    {
        return new StringValue(this.getString().replace(v.getString(),""));
    }
    public Value multiply(Value v)
    {
        if (v instanceof NumericValue || v instanceof ListValue)
        {
            return v.multiply(this);
        }
        return new StringValue(this.getString()+"."+v.getString());
    }
    public Value divide(Value v)
    {
        if (v instanceof NumericValue)
        {
            String lstr = getString();
            return new StringValue(lstr.substring(0, (int)(lstr.length()/ ((NumericValue) v).getDouble())));
        }
        return new StringValue(getString()+"/"+v.getString());
    }

    public Value()
    {
        this.boundVariable = null;
    }

    @Override
    public int compareTo(final Value o)
    {
        if (o instanceof NumericValue || o instanceof ListValue)
        {
            return -o.compareTo(this);
        }
        return getString().compareTo(o.getString());
    }
    public boolean equals(final Value o)
    {
        return this.compareTo(o)==0;
    }

    public void assertAssignable()
    {
        if (boundVariable == null || boundVariable.startsWith("_"))
        {
            if (boundVariable != null)
            {
                throw new InternalExpressionException(boundVariable+ " cannot be assigned a new value");
            }
            throw new InternalExpressionException(getString()+ " is not a variable");

        }
    }

    public Value in(Value value1)
    {
        final Pattern p = Pattern.compile(value1.getString());
        final Matcher m = p.matcher(this.getString());
        return m.find()?new StringValue(m.group()):Value.NULL;
    }
    public int length()
    {
        return getString().length();
    }

    public Value slice(long from, long to)
    {
        String value = this.getString();
        int size = value.length();
        if (to > size) to = -1;
        if (from < 0) from = 0;
        if (from > size) from = size;
        if (to>=0)
            return new StringValue(value.substring((int)from, (int)to));
        return new StringValue(value.substring((int)from));
    }
    public double readNumber()
    {
        String s = getString();
        try
        {
            return Double.valueOf(s);
        }
        catch (NumberFormatException e)
        {
            return Double.NaN;
        }
    }

    public long readInteger()
    {
        return (long)readNumber();
    }

    public Value getElementAt(Value v2)
    {
        throw new InternalExpressionException("get element can only be obtained for lists and nbt values");
    }

    public String getTypeString()
    {
        throw new InternalExpressionException("How did you get here? Cannot get type of an intenal type.");
    }
}
