package carpet.script.value;

import java.util.ArrayList;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

public class NullValue extends NumericValue // TODO check nonsingleton code
{
    public static final NullValue NULL = new NullValue();
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

    protected NullValue() {super(0);}

    @Override
    public boolean equals(final Object o)
    {
        if (o instanceof Value value)
        {
            return value.isNull();
        }
        return false;
    }

    @Override
    public Value slice(long fromDesc, Long toDesc) {
        return Value.NULL;
    }

    @Override
    public NumericValue opposite() {
        return Value.NULL;
    }

    @Override
    public int length() {
        return 0;
    }

    @Override
    public int compareTo(Value o)
    {
        return  o.isNull() ? 0 : -1;
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
        return StringTag.valueOf("null");
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
