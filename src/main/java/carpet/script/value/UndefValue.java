package carpet.script.value;

import carpet.script.exception.InternalExpressionException;
import com.google.gson.JsonElement;
import net.minecraft.nbt.Tag;

public class UndefValue extends NullValue {
    public static final UndefValue UNDEF = new UndefValue();

    private RuntimeException getError() {
        return new InternalExpressionException("variable "+boundVariable+" was used before initialization under 'strict' app config");
    }


    @Override
    public String getString()
    {
        throw getError();
    }

    @Override
    public String getPrettyString()
    {
        return "undefined";
    }

    @Override
    public boolean getBoolean()
    {
        throw getError();
    }

    @Override
    public Value clone()
    {
        return new UndefValue();
    }

    @Override
    public boolean equals(final Object o)
    {
        throw getError();
    }

    @Override
    public Value slice(long fromDesc, Long toDesc) {
        throw getError();
    }

    @Override
    public NumericValue opposite() {
        throw getError();
    }

    @Override
    public int length() {
        throw getError();
    }

    @Override
    public int compareTo(Value o)
    {
        throw getError();
    }

    @Override
    public Value in(Value value) {
        throw getError();
    }

    @Override
    public String getTypeString()
    {
        return "undef";
    }

    @Override
    public int hashCode()
    {
        throw getError();
    }

    @Override
    public Tag toTag(boolean force)
    {
        throw getError();
    }

    @Override
    public Value split(Value delimiter) {
        throw getError();
    }

    @Override
    public JsonElement toJson()
    {
        throw getError();
    }

    @Override
    public boolean isNull() {
        throw getError();
    }

    @Override
    public Value add(Value v) {
        throw getError();
    }

    @Override
    public Value subtract(Value v)
    {
        throw getError();
    }

    @Override
    public Value multiply(Value v)
    {
        throw getError();
    }

    @Override
    public Value divide(Value v)
    {
        throw getError();
    }

    @Override
    public double readDoubleNumber()
    {
        throw getError();
    }

    @Override
    public long readInteger()
    {
        throw getError();
    }

    @Override
    public Value reboundedTo(String var) {
        if (getVariable() != null) {
            // Here we're making a few assumptions:
            // The bound variable of the constants in Value can be (unwillingly) set by some functions that set it on the instance directly
            // (mostly loops, I'd assume for performance). Here we're assuming UNDEF will never get to that because:
            // - Loops don't currently set variables to UNDEF, but to stuff like NULL or ZERO (therefore it won't get assigned by accident on init)
            // - We are preventing someone from rebounding UNDEF into a loop variable with this (or trying to, at least), that should prevent you
            //   from inserting UNDEF into those loop functions and therefore protect the boundVariable
            throw getError();
        }
        return super.reboundedTo(var);
    }

}
