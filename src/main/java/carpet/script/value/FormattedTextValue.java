package carpet.script.value;

import com.google.gson.JsonElement;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class FormattedTextValue extends StringValue
{
    Component text;

    public FormattedTextValue(Component text)
    {
        super(null);
        this.text = text;
    }

    public static Value combine(Value left, Value right)
    {
        MutableComponent text;
        if (left instanceof final FormattedTextValue ftv)
        {
            text = ftv.getText().copy();
        }
        else
        {
            if (left.isNull())
            {
                return right;
            }
            text = Component.literal(left.getString());
        }

        if (right instanceof final FormattedTextValue ftv)
        {
            text.append(ftv.getText().copy());
            return new FormattedTextValue(text);
        }
        if (right.isNull())
        {
            return left;
        }
        text.append(right.getString());
        return new FormattedTextValue(text);
    }

    public static Value of(Component text)
    {
        return text == null ? Value.NULL : new FormattedTextValue(text);
    }

    @Override
    public String getString()
    {
        return text.getString();
    }

    @Override
    public boolean getBoolean()
    {
        return !text.getString().isEmpty();
    }

    @Override
    public Value clone()
    {
        return new FormattedTextValue(text);
    }

    @Override
    public String getTypeString()
    {
        return "text";
    }

    public Component getText()
    {
        return text;
    }

    @Override
    public Tag toTag(boolean force)
    {
        if (!force)
        {
            throw new NBTSerializableValue.IncompatibleTypeException(this);
        }
        return StringTag.valueOf(Component.Serializer.toJson(text));
    }

    @Override
    public JsonElement toJson()
    {
        return Component.Serializer.toJsonTree(text);
    }

    @Override
    public Value add(Value o)
    {
        return combine(this, o);
    }

    public String serialize()
    {
        return Component.Serializer.toJson(text);
    }

    public static FormattedTextValue deserialize(String serialized)
    {
        return new FormattedTextValue(Component.Serializer.fromJson(serialized));
    }

    public static Component getTextByValue(Value value)
    {
        return (value instanceof final FormattedTextValue ftv) ? ftv.getText() : Component.literal(value.getString());
    }

}
