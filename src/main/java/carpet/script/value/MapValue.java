package carpet.script.value;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MapValue extends AbstractListValue
{
    private Map<Value, Value> map;
    public MapValue()
    {
        map = new HashMap<>();
    }

    @Override
    public Iterator<Value> iterator()
    {
        return map.keySet().iterator();
    }

    @Override
    public String getString()
    {
        return Arrays.toString(map.entrySet().toArray());
    }

    @Override
    public boolean getBoolean()
    {
        return !map.isEmpty();
    }

}
