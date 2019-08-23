package carpet.script.value;

import carpet.script.exception.InternalExpressionException;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

import static java.lang.Math.abs;

public class NumericValue extends Value
{
    private Double value;
    final static double epsilon = 1024*Double.MIN_VALUE;

    public static NumericValue asNumber(Value v1)
    {
        if (!(v1 instanceof NumericValue))
            throw new InternalExpressionException("Operand has to be of a numeric type");
        return ((NumericValue) v1);
    }


    @Override
    public String getString()
    {
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
        if (getDouble() == (double)getLong())
        {
            return Long.toString(getLong());
        }
        else
        {
            return String.format("%.3f..", getDouble());
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

    public static long floor(double double_1) {
        long int_1 = (long)double_1;
        return double_1 < (double)int_1 ? int_1 - 1 : int_1;
    }

    public long getLong()
    {
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
        return new NumericValue(value);
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
    public boolean equals(Value o)
    {
        if (o instanceof NullValue)
        {
            return o.equals(this);
        }
        if (o instanceof NumericValue)
        {
            return !this.subtract(o).getBoolean();
        }
        return super.equals(o);
    }

    public NumericValue(double value)
    {
        this.value = value;
    }
    public NumericValue(String value)
    {
        this(new BigDecimal(value).doubleValue());
    }
    public NumericValue(long value)
    {
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
    public double readNumber()
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

}
