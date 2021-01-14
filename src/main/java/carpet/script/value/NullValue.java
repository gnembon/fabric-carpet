package carpet.script.value;

import java.util.ArrayList;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class NullValue extends NumericValue // TODO check nonsingleton code
{
    @Override
    public String getString()
    {
        return "null";
    }

    @Override
    public String getPrettyString()
    {
        return "null";
    }

    @Override
    public boolean getBoolean()
    {
        return false;
    }

    @Override
    public Value clone()
    {
        return new NullValue();
    }
    public NullValue() {super(0.0D);}

    @Override
    public boolean equals(final Object o)
    {
        return o instanceof NullValue;
    }

    @Override
    public Value multiply(Value v) {
        return Value.NULL;
    }

    @Override
    public Value add(Value v) {
        return Value.NULL;
    }

    @Override
    public Value subtract(Value v) {
        return Value.NULL;
    }

    @Override
    public Value divide(Value v) {
        return Value.NULL;
    }

    @Override
    public int compareTo(Value o)
    {
        return  o instanceof NullValue ? 0 : -1;
    }

    @Override
    public Value in(Value value) {
        return Value.NULL;
    }

    @Override
    public String getTypeString()
    {
        return "null";
    }

    @Override
    public int hashCode()
    {
        return 0;
    }

    @Override
    public Tag toTag(boolean force)
    {
        if (!force) throw new NBTSerializableValue.IncompatibleTypeException(this);
        return StringTag.of("null");
    }

    @Override
    public Value split(Value delimiter) {
    	return ListValue.wrap(new ArrayList<Value>());
    }

    @Override
    public JsonElement toJson()
    {
        return JsonNull.INSTANCE;
    }

    @Override
    public boolean isNull() {
        return true;
    }
}
