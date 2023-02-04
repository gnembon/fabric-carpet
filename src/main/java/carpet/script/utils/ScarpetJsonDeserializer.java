package carpet.script.utils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;

public class ScarpetJsonDeserializer implements JsonDeserializer<Value>
{

    @Override
    public Value deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException
    {
        return parseElement(json);
    }

    private Value parseElement(final JsonElement element) throws JsonParseException
    {
        if (element.isJsonObject())
        {
            return parseMap(element.getAsJsonObject());
        }
        else if (element.isJsonArray())
        {
            return parseList(element.getAsJsonArray());
        }
        else if (element.isJsonPrimitive())
        {
            return parsePrimitive(element.getAsJsonPrimitive());
        }
        return Value.NULL;
    }

    private Value parseMap(final JsonObject jsonMap) throws JsonParseException
    {
        final Map<Value, Value> map = new HashMap<>();
        jsonMap.entrySet().forEach(entry -> map.put(new StringValue(entry.getKey()), parseElement(entry.getValue())));
        return MapValue.wrap(map);
    }

    private Value parseList(final JsonArray jsonList) throws JsonParseException
    {
        final List<Value> list = new ArrayList<>();
        jsonList.forEach(elem -> list.add(parseElement(elem)));
        return new ListValue(list);
    }

    private Value parsePrimitive(final JsonPrimitive primitive) throws JsonParseException
    {
        if (primitive.isString())
        {
            return new StringValue(primitive.getAsString());
        }
        else if (primitive.isBoolean())
        {
            return primitive.getAsBoolean() ? Value.TRUE : Value.FALSE;
        }
        else if (primitive.isNumber())
        {
            return NumericValue.of(primitive.getAsNumber());
        }
        return Value.NULL;
    }
}
