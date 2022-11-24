package carpet.script.value;

import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class StringValue extends Value
{
    public static Value EMPTY = StringValue.of("");

    private String str;

    @Override
    public String getString() {
        return str;
    }

    @Override
    public boolean getBoolean() {
        return str != null && !str.isEmpty();
    }

    public StringValue(String str)
    {
        this.str = str;
    }

    public static Value of(String value)
    {
        if (value == null) return Value.NULL;
        return new StringValue(value);
    }

    @Override
    public String getTypeString()
    {
        return "string";
    }

    @Override
    public Tag toTag(boolean force)
    {
        return StringTag.valueOf(str);
    }
}
