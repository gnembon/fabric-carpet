package carpet.script.value;

import carpet.script.exception.InternalExpressionException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public MapValue(Set<Value> keySet)
    {
        this();
        for (Value v : keySet)
        {
            map.put(v, Value.NULL);
        }
    }

    @Override
    public Iterator<Value> iterator()
    {
        return new ArrayList<>(map.keySet()).iterator();
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

    @Override
    public Value deepcopy()
    {
        Map<Value, Value> copyMap = new HashMap<>();
        map.forEach((key, value) -> copyMap.put(key.deepcopy(), value.deepcopy()));
        return new MapValue(copyMap);
    }

    private MapValue(Map<Value,Value> other)
    {
        map = other;
    }

    public static MapValue wrap(Map<Value,Value> other)
    {
        return new MapValue(other);
    }

    @Override
    public Value add(Value o)
    {
        Map<Value, Value> newItems = new HashMap<>(map);
        if (o instanceof MapValue)
        {
            newItems.putAll(((MapValue) o).map);
        }
        else if (o instanceof AbstractListValue)
        {
            Iterator<Value> it = ((AbstractListValue) o).iterator();
            while (it.hasNext())
            {
                newItems.put(it.next(), Value.NULL);
            }
        }
        else
        {
            newItems.put(o, Value.NULL);
        }
        return MapValue.wrap(newItems);
    }

    @Override
    public Value subtract(Value v)
    {
        throw new InternalExpressionException("Cannot subtract from a map value");
    }

    @Override
    public Value multiply(Value v)
    {
        throw new InternalExpressionException("Cannot multiply with a map value");
    }

    @Override
    public Value divide(Value v)
    {
        throw new InternalExpressionException("Cannot divide a map value");
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
            throw new InternalExpressionException("Map constructor requires elements that have two items");
        }
        map.put(pair.getItems().get(0), pair.getItems().get(1));
    }

    @Override
    public void append(Value v)
    {
        map.put(v, Value.NULL);
    }

    @Override
    public int compareTo(Value o)
    {
        throw new InternalExpressionException("Cannot compare with a map value");
    }

    @Override
    public boolean equals(final Object o)
    {
        if (o instanceof MapValue)
        {
            return map.equals(((MapValue) o).map);
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
    public Value slice(long from, Long to)
    {
        throw new InternalExpressionException("Cannot slice a map value");
    }

    @Override
    public Value split(Value delimiter) {
    	throw new InternalExpressionException("Cannot split a map value");
    }

    @Override
    public double readDoubleNumber()
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
    public boolean delete(Value where)
    {
        Value ret = map.remove(where);
        return ret != null;
    }

    @Override
    public boolean put(Value key, Value value)
    {
        Value ret = map.put(key, value);
        return ret != null;
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

    @Override
    public Tag toTag(boolean force)
    {
        CompoundTag tag = new CompoundTag() ;
        map.forEach( (k, v) ->
        {
            if (!force && !(k instanceof StringValue)) throw new NBTSerializableValue.IncompatibleTypeException(k);
            tag.put(k.getString(), v.toTag(force));
        });
        return tag;
    }

    @Override
    public JsonElement toJson()
    {
        JsonObject jsonMap = new JsonObject();
        map.forEach((k, v) -> jsonMap.add(k.getString(), v.toJson()));
        return jsonMap;
    }
}
