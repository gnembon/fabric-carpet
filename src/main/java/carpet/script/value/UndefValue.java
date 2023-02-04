package carpet.script.value;

import carpet.script.exception.InternalExpressionException;
import com.google.gson.JsonElement;
import net.minecraft.nbt.Tag;

public class UndefValue extends NullValue
{
    public static final UndefValue UNDEF = new UndefValue();

    private RuntimeException getError()
    {
        return new InternalExpressionException("variable " + boundVariable + " was used before initialization under 'strict' app config");
    }

    @Override
    public String getString()
    {
        throw getError();
    }

    @Override
    public String getPrettyString()
    {
        return "undefined";
    }

    @Override
    public boolean getBoolean()
    {
        throw getError();
    }

    @Override
    public Value clone()
    {
        return new UndefValue();
    }

    @Override
    public boolean equals(final Object o)
    {
        throw getError();
    }

    @Override
    public Value slice(final long fromDesc, final Long toDesc)
    {
        throw getError();
    }

    @Override
    public NumericValue opposite()
    {
        throw getError();
    }

    @Override
    public int length()
    {
        throw getError();
    }

    @Override
    public int compareTo(final Value o)
    {
        throw getError();
    }

    @Override
    public Value in(final Value value)
    {
        throw getError();
    }

    @Override
    public String getTypeString()
    {
        return "undef";
    }

    @Override
    public int hashCode()
    {
        throw getError();
    }

    @Override
    public Tag toTag(final boolean force)
    {
        throw getError();
    }

    @Override
    public Value split(final Value delimiter)
    {
        throw getError();
    }

    @Override
    public JsonElement toJson()
    {
        throw getError();
    }

    @Override
    public boolean isNull()
    {
        throw getError();
    }

    @Override
    public Value add(final Value v)
    {
        throw getError();
    }

    @Override
    public Value subtract(final Value v)
    {
        throw getError();
    }

    @Override
    public Value multiply(final Value v)
    {
        throw getError();
    }

    @Override
    public Value divide(final Value v)
    {
        throw getError();
    }

    @Override
    public double readDoubleNumber()
    {
        throw getError();
    }

    @Override
    public long readInteger()
    {
        throw getError();
    }

}
