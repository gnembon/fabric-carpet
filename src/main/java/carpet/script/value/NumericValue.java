package carpet.script.value;

import carpet.script.exception.InternalExpressionException;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.core.RegistryAccess;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Locale;

import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;

import static java.lang.Math.abs;
import static java.lang.Math.signum;

public class NumericValue extends Value
{
    private final double value;
    private Long longValue;
    private static final double epsilon = abs(32 * ((7 * 0.1) * 10 - 7));
    private static final MathContext displayRounding = new MathContext(12, RoundingMode.HALF_EVEN);

    public static NumericValue asNumber(Value v1, String id)
    {
        if (v1 instanceof NumericValue nv)
        {
            return nv;
        }
        throw new InternalExpressionException("Argument " + id + " has to be of a numeric type");
    }

    public static NumericValue asNumber(Value v1)
    {
        if (v1 instanceof NumericValue nv)
        {
            return nv;
        }
        throw new InternalExpressionException("Operand has to be of a numeric type");
    }

    public static <T extends Number> Value of(T value)
    {
        if (value == null)
        {
            return Value.NULL;
        }
        if (value.doubleValue() == value.longValue())
        {
            return new NumericValue(value.longValue());
        }
        if (value instanceof Float)
        {
            return new NumericValue(0.000_001D * Math.round(1_000_000.0D * value.doubleValue()));
        }
        return new NumericValue(value.doubleValue());
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
            if (Double.isInfinite(value))
            {
                return (value > 0) ? "INFINITY" : "-INFINITY";
            }
            if (Double.isNaN(value))
            {
                return "NaN";
            }
            if (abs(value) < epsilon)
            {
                return (signum(value) < 0) ? "-0" : "0"; //zero rounding fails with big decimals
            }
            // dobules have 16 point precision, 12 is plenty to display
            return BigDecimal.valueOf(value).round(displayRounding).stripTrailingZeros().toPlainString();
        }
        catch (NumberFormatException exc)
        {
            throw new InternalExpressionException("Incorrect number format for " + value);
        }
    }

    @Override
    public String getPrettyString()
    {
        return longValue != null || getDouble() == getLong()
                ? Long.toString(getLong())
                : String.format(Locale.ROOT, "%.1f..", getDouble());
    }

    @Override
    public boolean getBoolean()
    {
        return abs(value) > epsilon;
    }

    public double getDouble()
    {
        return value;
    }

    public float getFloat()
    {
        return (float) value;
    }

    private static long floor(double v)
    {
        long invValue = (long) v;
        return v < invValue ? invValue - 1 : invValue;
    }

    public long getLong()
    {
        return longValue != null ? longValue : Long.valueOf(floor((value + epsilon)));
    }

    @Override
    public Value add(Value v)
    {  // TODO test if definintn add(NumericVlaue) woud solve the casting
        if (v instanceof NumericValue nv)
        {
            return longValue != null && nv.longValue != null ? new NumericValue(longValue + nv.longValue) : new NumericValue(value + nv.value);
        }
        return super.add(v);
    }

    @Override
    public Value subtract(Value v)
    {  // TODO test if definintn add(NumericVlaue) woud solve the casting
        if (v instanceof NumericValue nv)
        {
            return longValue != null && nv.longValue != null ? new NumericValue(longValue - nv.longValue) : new NumericValue(value - nv.value);
        }
        return super.subtract(v);
    }

    @Override
    public Value multiply(Value v)
    {
        if (v instanceof NumericValue nv)
        {
            return longValue != null && nv.longValue != null ? new NumericValue(longValue * nv.longValue) : new NumericValue(value * nv.value);
        }
        return v instanceof ListValue ? v.multiply(this) : new StringValue(StringUtils.repeat(v.getString(), (int) getLong()));
    }

    @Override
    public Value divide(Value v)
    {
        return v instanceof NumericValue nv ? new NumericValue(getDouble() / nv.getDouble()) : super.divide(v);
    }

    @Override
    public Value clone()
    {
        return new NumericValue(value, longValue);
    }

    @Override
    public int compareTo(Value o)
    {
        if (o.isNull())
        {
            return -o.compareTo(this);
        }
        if (o instanceof NumericValue no)
        {
            return longValue != null && no.longValue != null ? longValue.compareTo(no.longValue) : Double.compare(value, no.value);
        }
        return getString().compareTo(o.getString());
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof Value otherValue)
        {
            if (otherValue.isNull())
            {
                return o.equals(this);
            }
            if (o instanceof NumericValue no)
            {
                if (longValue != null && no.longValue != null)
                {
                    return longValue.equals(no.longValue);
                }
                if (Double.isNaN(this.value) || Double.isNaN(no.value))
                {
                    return false;
                }
                return !this.subtract(no).getBoolean();
            }
            return super.equals(o);
        }
        return false;
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
            catch (ArithmeticException ignored)
            {
            }
        }
        this.value = decimal.doubleValue();
    }

    public NumericValue(long value)
    {
        this.longValue = value;
        this.value = (double) value;
    }

    @Override
    public int length()
    {
        return Long.toString(getLong()).length();
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
        // is sufficiently close to the integer value
        return longValue != null || Math.abs(Math.floor(value + 0.5D) - value) < epsilon ? Long.hashCode(getLong()) : Double.hashCode(value);
    }


    public int getInt()
    {
        return (int) getLong();
    }

    @Override
    public Tag toTag(boolean force, RegistryAccess regs)
    {
        if (longValue != null)
        {
            if (abs(longValue) < Integer.MAX_VALUE - 2)
            {
                return IntTag.valueOf((int) (long) longValue);
            }
            return LongTag.valueOf(longValue);
        }
        long lv = getLong();
        if (value == (double) lv)
        {
            if (abs(value) < Integer.MAX_VALUE - 2)
            {
                return IntTag.valueOf((int) lv);
            }
            return LongTag.valueOf(getLong());
        }
        else
        {
            return DoubleTag.valueOf(value);
        }
    }

    @Override
    public JsonElement toJson()
    {
        if (longValue != null)
        {
            return new JsonPrimitive(longValue);
        }
        return isInteger() ? new JsonPrimitive(getLong()) : new JsonPrimitive(getDouble());
    }

    public NumericValue opposite()
    {
        return longValue != null ? new NumericValue(-longValue) : new NumericValue(-value);
    }

    public boolean isInteger()
    {
        return longValue != null || getDouble() == getLong();
    }

    public Value mod(NumericValue n2)
    {
        if (this.longValue != null && n2.longValue != null)
        {
            return new NumericValue(Math.floorMod(longValue, n2.longValue));
        }
        double x = value;
        double y = n2.value;
        if (y == 0)
        {
            throw new ArithmeticException("Division by zero");
        }
        return new NumericValue(x - Math.floor(x / y) * y);
    }
}
