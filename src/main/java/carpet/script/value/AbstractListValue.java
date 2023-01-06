package carpet.script.value;

import carpet.script.exception.InternalExpressionException;
import com.google.common.collect.Lists;

import java.util.Iterator;
import java.util.List;

public abstract class AbstractListValue extends Value implements Iterable<Value>
{
    @Override public abstract Iterator<Value> iterator();
    public List<Value> unpack()
    {
        List<Value> retVal = Lists.newArrayList(iterator());
        fatality();
        return retVal;
    }
    public void fatality() { }
    public void append(Value v)
    {
        throw new InternalExpressionException("Cannot append a value to an abstract list");
    }

    @Override
    public Value fromConstant() {
        return this.deepcopy();
    }
}
