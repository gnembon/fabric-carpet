package carpet.script.value;

import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class StringValue extends Value
{
    public static Value EMPTY = StringValue.of("");

    private final String str;

    @Override
    public String getString()
    {
        return str;
    }

    @Override
    public boolean getBoolean()
    {
        return str != null && !str.isEmpty();
    }

    @Override
    public Value clone()
    {
        return new StringValue(str);
    }

    public StringValue(final String str)
    {
        this.str = str;
    }

    public static Value of(final String value)
    {
        return value == null ? Value.NULL : new StringValue(value);
    }

    @Override
    public String getTypeString()
    {
        return "string";
    }

    @Override
    public Tag toTag(final boolean force)
    {
        return StringTag.valueOf(str);
    }
}
