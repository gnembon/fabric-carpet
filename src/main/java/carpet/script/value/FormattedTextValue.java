package carpet.script.value;

import net.minecraft.text.BaseText;

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
}
