package carpet.script.value;

import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class StringValue extends Value
{
    private String str;

    @Override
    public String getString() {
        return str;
    }

    @Override
    public boolean getBoolean() {
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

    @Override
    public String getTypeString()
    {
        return "string";
    }

    @Override
    public Tag toTag(boolean force)
    {
        return StringTag.of(str);
    }
}
