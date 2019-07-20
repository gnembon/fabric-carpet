package carpet.script.value;

import carpet.script.exception.InternalExpressionException;

public class GlobalValue extends Value
{
    public GlobalValue(Value variable)
    {
        variable.assertAssignable();
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
    public String getTypeString()
    {
        throw new InternalExpressionException("How did you get here? Cannot get type of an internal struct.");
    }
}
