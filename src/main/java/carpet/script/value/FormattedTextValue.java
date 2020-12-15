package carpet.script.value;

import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
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

    @Override
    public String getString() {
        return text.getString();
    }

    @Override
    public boolean getBoolean() {
          return text.getSiblings().size() > 0;
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
    public Tag toTag(boolean force)
    {
        if (!force) throw new NBTSerializableValue.IncompatibleTypeException(this);
        return StringTag.of(Text.Serializer.toJson(text));
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

}
