package carpet.script.value;

import carpet.script.exception.ExpressionException;

public abstract class FrameworkValue extends Value
{
    @Override
    public String getString()
    {
        throw new ExpressionException("Scarpet language component cannot be used");
    }

    @Override
    public boolean getBoolean()
    {
        throw new ExpressionException("Scarpet language component cannot be used");
    }

    @Override
    public Value clone()
    {
        throw new ExpressionException("Scarpet language component cannot be used");
    }

    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException("Scarpet language component cannot be used as map key");
    }
}
