package carpet.script.value;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;

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

    public StringValue(String str)
    {
        this.str = str;
    }

    public static Value of(@Nullable String value)
    {
        return value == null ? Value.NULL : new StringValue(value);
    }

    @Override
    public String getTypeString()
    {
        return "string";
    }

    @Override
    public Tag toTag(boolean force, RegistryAccess regs)
    {
        return StringTag.valueOf(str);
    }
}
