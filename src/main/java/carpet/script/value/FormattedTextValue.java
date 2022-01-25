package carpet.script.value;

import com.google.gson.JsonElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public class FormattedTextValue extends StringValue
{
    Text text;
    public FormattedTextValue(Text text)
    {
        super(null);
        this.text = text;
    }

    public static Value combine(Value left, Value right) {
        BaseText text;
        if (left instanceof FormattedTextValue)
        {
            text = (BaseText) ((FormattedTextValue) left).getText().shallowCopy();
        }
        else
        {
            if (left instanceof NullValue)
                return right;
            text = new LiteralText(left.getString());
        }
        
        if (right instanceof FormattedTextValue)
        {
            text.append(((FormattedTextValue) right).getText().shallowCopy());
            return new FormattedTextValue(text);
        }
        else
        {
            if (right instanceof NullValue)
                return left;
            text.append(right.getString());
            return new FormattedTextValue(text);
        }
    }

    public static Value of(Text text) {
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

    public Text getText()
    {
        return text;
    }

    @Override
    public NbtElement toTag(boolean force)
    {
        if (!force) throw new NBTSerializableValue.IncompatibleTypeException(this);
        return NbtString.of(Text.Serializer.toJson(text));
    }

    @Override
    public JsonElement toJson() {
        return Text.Serializer.toJsonTree(text);
    }

    @Override
    public Value add(Value o) {
        return combine(this, o);
    }

    public String serialize()
    {
        return Text.Serializer.toJson(text);
    }

    public static FormattedTextValue deserialize(String serialized)
    {
        return new FormattedTextValue(Text.Serializer.fromJson(serialized));
    }

    public static Text getTextByValue(Value value) {
        return (value instanceof FormattedTextValue) ? ((FormattedTextValue) value).getText() : new LiteralText(value.getString());
    }

}
