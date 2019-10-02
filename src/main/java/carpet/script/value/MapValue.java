package carpet.script.value;

import carpet.script.exception.InternalExpressionException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MapValue extends AbstractListValue implements ContainerValueInterface
{
    private Map<Value, Value> map;

    private MapValue()
    {
        map = new HashMap<>();
    }

    public MapValue(List<Value> kvPairs)
    {
        this();
        for (Value v : kvPairs)
        {
            put(v);
        }
    }

    @Override
    public Iterator<Value> iterator()
    {
        return map.keySet().iterator();
    }

    @Override
    public String getString()
    {
        return "{"+map.entrySet().stream().map((p) -> p.getKey().getString()+": "+p.getValue().getString()).collect(Collectors.joining(", "))+"}";
    }

    @Override
    public String getPrettyString()
    {
        if (map.size()<6)
            return "{"+map.entrySet().stream().map((p) -> p.getKey().getPrettyString()+": "+p.getValue().getPrettyString()).collect(Collectors.joining(", "))+"}";
        List<Value> keys = new ArrayList<>(map.keySet());
        int max = keys.size();
        return "{"+keys.get(0).getPrettyString()+": "+map.get(keys.get(0)).getPrettyString()+", "+
                keys.get(1).getPrettyString()+": "+map.get(keys.get(1)).getPrettyString()+", ..., "+
                keys.get(max-2).getPrettyString()+": "+map.get(keys.get(max-2)).getPrettyString()+", "+
                keys.get(max-1).getPrettyString()+": "+map.get(keys.get(max-1)).getPrettyString()+"}";
    }

    @Override
    public boolean getBoolean()
    {
        return !map.isEmpty();
    }

    @Override
    public Value clone()
    {
        return new MapValue(map);
    }
    public MapValue(Map<Value,Value> other)
    {
        map = new HashMap<>(other);
    }

    @Override
    public Value add(Value o)
    {
        throw new InternalExpressionException("cannot add to a map value");
    }

    @Override
    public Value subtract(Value v)
    {
        throw new InternalExpressionException("cannot subtract to a map value");
    }

    @Override
    public Value multiply(Value v)
    {
        throw new InternalExpressionException("cannot multiply to a map value");
    }

    @Override
    public Value divide(Value v)
    {
        throw new InternalExpressionException("cannot divide to a map value");
    }

    public void put(Value v)
    {
        if (!(v instanceof ListValue))
        {
            map.put(v, Value.NULL);
            return;
        }
        ListValue pair = (ListValue)v;
        if (pair.getItems().size() != 2)
        {
            throw new InternalExpressionException("map constructor requires elements that have two elements");
        }
        map.put(pair.getItems().get(0), pair.getItems().get(1));
    }

    @Override
    public void append(Value v)
    {
        put(v);
    }

    @Override
    public int compareTo(Value o)
    {
        throw new InternalExpressionException("cannot compare to a map value");
    }

    @Override
    public boolean equals(final Value o)
    {
        if (o instanceof MapValue)
        {
            MapValue ol = (MapValue)o;
            int this_size = this.map.keySet().size();
            int o_size = ol.map.keySet().size();
            if (this_size != o_size) return false;
            if (this_size == 0) return true;
            return this.map.keySet().equals(ol.map.keySet());
        }
        return false;
    }

    public Map<Value, Value> getMap()
    {
        return map;
    }

    public void extend(List<Value> subList)
    {
        for (Value v: subList) put(v);
    }

    @Override
    public int length()
    {
        return map.size();
    }

    @Override
    public Value in(Value value1)
    {
        if (map.containsKey(value1)) return value1;
        return Value.NULL;
    }

    @Override
    public Value slice(long from, long to)
    {
        throw new InternalExpressionException("cannot slice a map value");
    }

    @Override
    public double readNumber()
    {
        return (double)map.size();
    }

    @Override
    public Value get(Value v2)
    {
        return map.getOrDefault(v2, Value.NULL);
    }

    @Override
    public boolean has(Value where)
    {
        return map.containsKey(where);
    }

    @Override
    public Value remove(Value where)
    {
        return map.remove(where);
    }

    @Override
    public Value put(Value key, Value value)
    {
        return map.put(key, value);
    }

    @Override
    public String getTypeString()
    {
        return "map";
    }

    @Override
    public int hashCode()
    {
        return map.hashCode();
    }
}
