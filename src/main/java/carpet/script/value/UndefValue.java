package carpet.script.value;

import carpet.script.exception.InternalExpressionException;
import com.google.gson.JsonElement;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.Tag;

public class UndefValue extends NullValue
{
    public static final UndefValue UNDEF = new UndefValue();
    public static final UndefValue EOL = new UndefValue();

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
    public boolean equals(Object o)
    {
        throw getError();
    }

    @Override
    public Value slice(long fromDesc, Long toDesc)
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
    public int compareTo(Value o)
    {
        throw getError();
    }

    @Override
    public Value in(Value value)
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
    public Tag toTag(boolean force, RegistryAccess regs)
    {
        throw getError();
    }

    @Override
    public Value split(Value delimiter)
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
    public Value add(Value v)
    {
        throw getError();
    }

    @Override
    public Value subtract(Value v)
    {
        throw getError();
    }

    @Override
    public Value multiply(Value v)
    {
        throw getError();
    }

    @Override
    public Value divide(Value v)
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
