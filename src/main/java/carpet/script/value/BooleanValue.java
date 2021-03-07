package carpet.script.value;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.Tag;

public class BooleanValue extends NumericValue
{
    public static final BooleanValue FALSE = new BooleanValue(false);
    public static final BooleanValue TRUE = new BooleanValue(true);

    boolean boolValue;
    private BooleanValue(boolean boolval) {
        super(boolval);
        boolValue = boolval;
    }

    @Override
    public String getString() {
        return boolValue?"true":"false";
    }

    @Override
    public String getPrettyString() {
        return getString();
    }

    @Override
    public String getTypeString() {
        return "bool";
    }

    @Override
    public Value clone() {
        return new BooleanValue(boolValue);
    }

    @Override
    public int hashCode() {
        return Boolean.hashCode(boolValue);
    }

    @Override
    public Tag toTag(boolean force) {
        return ByteTag.of(boolValue);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(boolValue);
    }

    @Override
    public boolean isInteger() {
        return true;
    }
}
