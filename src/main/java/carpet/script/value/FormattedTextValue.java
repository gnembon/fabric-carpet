package carpet.script.value;

import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.text.BaseText;
import net.minecraft.text.Text;

public class FormattedTextValue extends StringValue
{
    BaseText text;
    public FormattedTextValue(BaseText text)
    {
        super(null);
        this.text = text;
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

    public BaseText getText()
    {
        return text;
    }

    @Override
    public Tag toTag(boolean force)
    {
        if (!force) throw new NBTSerializableValue.IncompatibleTypeException(this);
        return StringTag.of(Text.Serializer.toJson(text));
    }
}
