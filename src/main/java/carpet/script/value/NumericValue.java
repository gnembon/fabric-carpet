package carpet.script.value;

import carpet.script.exception.InternalExpressionException;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Locale;

import static java.lang.Math.abs;

public class NumericValue extends Value
{
    private Double value;
    private Long longValue;
    final static double epsilon = 1024*Double.MIN_VALUE;

    public static NumericValue asNumber(Value v1, String id)
    {
        if (!(v1 instanceof NumericValue))
            throw new InternalExpressionException("Argument "+id+" has to be of a numeric type");
        return ((NumericValue) v1);
    }

    public static NumericValue asNumber(Value v1)
    {
        if (!(v1 instanceof NumericValue))
            throw new InternalExpressionException("Operand has to be of a numeric type");
        return ((NumericValue) v1);
    }


    @Override
    public String getString()
    {
        if (longValue != null)
        {
            return Long.toString(getLong());
        }
        try
        {
            return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
        }
        catch (NumberFormatException exc)
        {
            throw new ArithmeticException("Incorrect number format for "+value);
        }
    }

    @Override
    public String getPrettyString()
    {

        if (longValue!= null ||  getDouble() == (double)getLong())
        {
            return Long.toString(getLong());
        }
        else
        {
            return String.format(Locale.ROOT, "%.1f..", getDouble());
        }
    }

    @Override
    public boolean getBoolean()
    {
        return value != null && abs(value) > epsilon;
    }
    public double getDouble()
    {
        return value;
    }
    public float getFloat()
    {
        return value.floatValue();
    }

    private static long floor(double double_1) {
        long int_1 = (long)double_1;
        return double_1 < (double)int_1 ? int_1 - 1 : int_1;
    }

    public long getLong()
    {
        if (longValue != null) return longValue;
        return floor((value+epsilon));
    }

    @Override
    public Value add(Value v)
    {  // TODO test if definintn add(NumericVlaue) woud solve the casting
        if (v instanceof NumericValue)
        {
            return new NumericValue(getDouble() + ((NumericValue) v).getDouble() );
        }
        return super.add(v);
    }
    public Value subtract(Value v) {  // TODO test if definintn add(NumericVlaue) woud solve the casting
        if (v instanceof NumericValue)
        {
            return new NumericValue(getDouble() - (((NumericValue) v).getDouble()));
        }
        return super.subtract(v);
    }
    public Value multiply(Value v)
    {
        if (v instanceof NumericValue)
        {
            return new NumericValue(getDouble() * ((NumericValue) v).getDouble() );
        }
        if (v instanceof ListValue)
        {
            return v.multiply(this);
        }
        return new StringValue(StringUtils.repeat(v.getString(), (int) getLong()));
    }
    public Value divide(Value v)
    {
        if (v instanceof NumericValue)
        {
            return new NumericValue(getDouble() / ((NumericValue) v).getDouble() );
        }
        return super.divide(v);
    }

    @Override
    public Value clone()
    {
        return new NumericValue(value, longValue);
    }

    @Override
    public int compareTo(Value o)
    {
        if (o instanceof NullValue)
        {
            return -o.compareTo(this);
        }
        if (o instanceof NumericValue)
        {
            return value.compareTo(((NumericValue) o).getDouble());
        }
        return getString().compareTo(o.getString());
    }
    @Override
    public boolean equals(Object o)
    {
        if (o instanceof NullValue)
        {
            return o.equals(this);
        }
        if (o instanceof NumericValue)
        {
            return !this.subtract((Value) o).getBoolean();
        }
        return super.equals(o);
    }

    public NumericValue(double value)
    {
        this.value = value;
    }
    private NumericValue(double value, Long longValue)
    {
        this.value = value;
        this.longValue = longValue;
    }

    public NumericValue(String value)
    {
        BigDecimal decimal = new BigDecimal(value);
        if (decimal.stripTrailingZeros().scale() <= 0)
        {
            try
            {
                longValue = decimal.longValueExact();
            }
            catch (ArithmeticException ignored) {}
        }
        this.value = decimal.doubleValue();
    }
    public NumericValue(long value)
    {
        this.longValue = value;
        this.value = (double)value;
    }
    public NumericValue(boolean boolval)
    {
        this(boolval?1.0D:0.0D);
    }

    @Override
    public int length()
    {
        return Integer.toString(value.intValue()).length();
    }

    @Override
    public double readDoubleNumber()
    {
        return value;
    }

    @Override
    public long readInteger()
    {
        return getLong();
    }

    @Override
    public String getTypeString()
    {
        return "number";
    }

    @Override
    public int hashCode()
    {
        if (Math.abs(Math.floor(value + 0.5D)-value) < epsilon) // is sufficiently close to the integer value
            return Long.hashCode(getLong());
        return Double.hashCode(value);
    }


    public int getInt()
    {
        return (int)getLong();
    }

    @Override
    public Tag toTag(boolean force)
    {
        if (longValue != null)
            return LongTag.of(longValue);
        long lv = getLong();
        if (value == (double)lv)
        {
            if (abs(value) < Integer.MAX_VALUE-2)
                return IntTag.of((int)lv);
            return LongTag.of(getLong());
        }
        else
        {
            return DoubleTag.of(value);
        }
    }

    @Override
    public JsonElement toJson()
    {
        if (longValue != null)
            return new JsonPrimitive(longValue);
        long lv = getLong();
        if (value == (double)lv)
        {
            return new JsonPrimitive(getLong());
        }
        else
        {
            return new JsonPrimitive(value);
        }
    }
}
