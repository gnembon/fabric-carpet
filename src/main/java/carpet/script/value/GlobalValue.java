package carpet.script.value;

import carpet.script.exception.InternalExpressionException;

public class GlobalValue extends Value
{
    public GlobalValue(Value variable)
    {
        if (variable.boundVariable == null)
            throw new InternalExpressionException("You can only borrow variables from the outer scope");
        this.boundVariable = variable.boundVariable;
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
}
