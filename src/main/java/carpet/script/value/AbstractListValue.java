package carpet.script.value;

import java.util.Iterator;

public abstract class AbstractListValue extends Value
{
    public abstract Iterator<Value> iterator();
    public void fatality() { }
}
