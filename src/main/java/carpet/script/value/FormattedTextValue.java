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

    public static Value combine(Value left, Value right) {
        MutableComponent text;
        if (left instanceof FormattedTextValue)
        {
            text = ((FormattedTextValue) left).getText().copy();
        }
        else
        {
            if (left.isNull())
                return right;
            text = Component.literal(left.getString());
        }
        
        if (right instanceof FormattedTextValue)
        {
            text.append(((FormattedTextValue) right).getText().copy());
            return new FormattedTextValue(text);
        }
        else
        {
            if (right.isNull())
                return left;
            text.append(right.getString());
            return new FormattedTextValue(text);
        }
    }

    public static Value of(Component text) {
        if (text == null) return Value.NULL;
        return new FormattedTextValue(text);
    }

    @Override
    public String getString() {
        return text.getString();
    }

    @Override
    public boolean getBoolean() {
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
        if (!force) throw new NBTSerializableValue.IncompatibleTypeException(this);
        return StringTag.valueOf(Component.Serializer.toJson(text));
    }

    @Override
    public JsonElement toJson() {
        return Component.Serializer.toJsonTree(text);
    }

    @Override
    public Value add(Value o) {
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

    public static Component getTextByValue(Value value) {
        return (value instanceof FormattedTextValue) ? ((FormattedTextValue) value).getText() : Component.literal(value.getString());
    }

}
