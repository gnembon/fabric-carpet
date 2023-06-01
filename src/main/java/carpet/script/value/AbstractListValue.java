package carpet.script.value;

import carpet.script.exception.InternalExpressionException;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractListValue extends Value implements Iterable<Value>
{
    public List<Value> unpack()
    {
        ArrayList<Value> retVal = Lists.newArrayList();
        for (Value value : this)
        {
            if (value != Value.EOL)
            {
                retVal.add(value);
            }
        }
        fatality();
        return retVal;
    }

    public void fatality()
    {
    }

    public void append(Value v)
    {
        throw new InternalExpressionException("Cannot append a value to an abstract list");
    }

    @Override
    public Value fromConstant()
    {
        return this.deepcopy();
    }
}
