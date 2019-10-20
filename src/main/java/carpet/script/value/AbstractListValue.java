package carpet.script.value;

import carpet.script.exception.InternalExpressionException;

import java.util.Iterator;

public abstract class AbstractListValue extends Value
{
    public abstract Iterator<Value> iterator();
    public void fatality() { }
    public void append(Value v)
    {
        throw new InternalExpressionException("Cannot append a value to an abstract list");
    }
}
