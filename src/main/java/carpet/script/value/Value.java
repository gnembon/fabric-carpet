package carpet.script.value;

import carpet.CarpetSettings;
import carpet.script.exception.InternalExpressionException;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import net.minecraft.nbt.Tag;

public abstract class Value implements Comparable<Value>, Cloneable
{
    public static NumericValue FALSE = BooleanValue.FALSE;
    public static NumericValue TRUE = BooleanValue.TRUE;
    public static NumericValue ZERO = new NumericValue(0);
    public static NumericValue ONE = new NumericValue(1);

    public static NullValue NULL = NullValue.NULL;
    public static UndefValue UNDEF = UndefValue.UNDEF;

    public String boundVariable;

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
        Value copy;
        try
        {
            copy = (Value)clone();
        }
        catch (CloneNotSupportedException e)
        {
            // should not happen
            CarpetSettings.LOG.error("Failed to clone variable", e);
            throw new InternalExpressionException("Variable of type "+getTypeString()+" is not cloneable. Tell gnembon about it, this shoudn't happen");
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
        if (o instanceof FormattedTextValue)
        {
            return FormattedTextValue.combine(this, o);
        }
        String leftStr = this.getString();
        String rightStr = o.getString();
        return new StringValue(leftStr + rightStr);
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
        if (o instanceof NumericValue || o instanceof ListValue || o instanceof ThreadValue)
        {
            return -o.compareTo(this);
        }
        return getString().compareTo(o.getString());
    }

    @Override // for hashmap key access, and == operator
    public boolean equals(final Object o)
    {
        if (o instanceof Value)
            return this.compareTo((Value) o)==0;
        return false;
    }

    public void assertAssignable()
    {
        if (boundVariable == null)// || boundVariable.startsWith("_"))
        {
            /*if (boundVariable != null)
            {
                throw new InternalExpressionException(boundVariable+ " cannot be assigned a new value");
            }*/
            throw new InternalExpressionException(getString()+ " is not a variable");

        }
    }

    public Value in(Value value1)
    {
        final Pattern p;
        try
        {
            p = Pattern.compile(value1.getString());
        }
        catch (PatternSyntaxException pse)
        {
            throw new InternalExpressionException("Incorrect matching pattern: "+pse.getMessage());
        }
        final Matcher m = p.matcher(this.getString());
        if (!m.find()) return Value.NULL;
        int gc = m.groupCount();
        if (gc == 0) return new StringValue(m.group());
        if (gc == 1) return StringValue.of(m.group(1));
        List<Value> groups = new ArrayList<>(gc);
        for (int i = 1; i <= gc; i++)
        {
            groups.add(StringValue.of(m.group(i)));
        }
        return ListValue.wrap(groups);
    }
    public int length()
    {
        return getString().length();
    }

    public Value slice(long fromDesc, Long toDesc)
    {
        String value = this.getString();
        int size = value.length();
        int from = ListValue.normalizeIndex(fromDesc, size);
        if (toDesc == null) return new StringValue(value.substring(from));
        int to = ListValue.normalizeIndex(toDesc, size+1);
        if (from > to) return StringValue.EMPTY;
        return new StringValue(value.substring(from, to));
    }
    
    public Value split(Value delimiter)
    {
        if (delimiter == null)
        {
            delimiter = StringValue.EMPTY;
        }
        try
        {
            return ListValue.wrap(Arrays.stream(getString().split(delimiter.getString())).map(StringValue::new).collect(Collectors.toList()));
        }
        catch (PatternSyntaxException pse)
        {
            throw new InternalExpressionException("Incorrect pattern for 'split': "+pse.getMessage());
        }
    }
    
    public double readDoubleNumber()
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
        return (long) readDoubleNumber();
    }

    public String getTypeString()
    {
        throw new InternalExpressionException("How did you get here? Cannot get type of an intenal type.");
    }

    @Override
    public int hashCode()
    {
        String stringVal = getString();
        if (stringVal.isEmpty()) return 0;
        return ("s"+stringVal).hashCode();
    }

    public Value deepcopy()
    {
        try
        {
            return (Value)this.clone();
        }
        catch (CloneNotSupportedException e)
        {
            // should never happen
            throw new InternalExpressionException("Cannot make a copy of value: "+this);
        }
    }

    public abstract Tag toTag(boolean force);

    public JsonElement toJson()
    {
        return new JsonPrimitive(getString());
    }

    public boolean isNull() { return false; }

    /**
     * @return retrieves useful in-run value of an optimized code-base value.
     * For immutable values (most of them) it can return itself,
     * but for mutables, it needs to be its copy or deep copy.
     */
    public Value fromConstant() { return this; }
}
