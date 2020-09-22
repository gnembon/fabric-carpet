package carpet.script.value;

import carpet.script.exception.InternalExpressionException;
import net.minecraft.nbt.Tag;

public class FunctionAnnotationValue extends Value
{
    // 0 global
    // 1 vararg
    public int type;
    public FunctionAnnotationValue(Value variable, int type)
    {
        if (variable.boundVariable == null)
            throw new InternalExpressionException("You can only borrow variables from the outer scope");
        this.boundVariable = variable.boundVariable;
        this.type = type;
    }

    @Override
    public String getString()
    {
        return boundVariable;
    }

    @Override
    public boolean getBoolean()
    {
        return false;
    }

    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException("Global value cannot be used as a map key");
    }

    @Override
    public Tag toTag(boolean force)
    {
        throw new UnsupportedOperationException("Global value cannot be serialized to the tag");
    }
}
