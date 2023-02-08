package carpet.script.language;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.LazyValue;
import carpet.script.api.Auxiliary;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ThrowStatement;
import carpet.script.exception.Throwables;
import carpet.script.value.BooleanValue;
import carpet.script.value.ContainerValueInterface;
import carpet.script.value.LContainerValue;
import carpet.script.value.LazyListValue;
import carpet.script.value.ListValue;
import carpet.script.value.MapValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import com.google.gson.JsonParseException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DataStructures
{
    public static void apply(Expression expression)
    {
        expression.addFunction("l", lv ->
                lv.size() == 1 && lv.get(0) instanceof final LazyListValue llv
                        ? ListValue.wrap(llv.unroll())
                        : new ListValue.ListConstructorValue(lv));

        expression.addFunction("join", lv ->
        {
            if (lv.size() < 2)
            {
                throw new InternalExpressionException("'join' takes at least 2 arguments");
            }
            String delimiter = lv.get(0).getString();
            List<Value> toJoin;
            if (lv.size() == 2 && lv.get(1) instanceof final LazyListValue llv)
            {
                toJoin = llv.unroll();
            }
            else if (lv.size() == 2 && lv.get(1) instanceof final ListValue llv)
            {
                toJoin = new ArrayList<>(llv.getItems());
            }
            else
            {
                toJoin = lv.subList(1, lv.size());
            }
            return new StringValue(toJoin.stream().map(Value::getString).collect(Collectors.joining(delimiter)));
        });

        expression.addFunction("split", lv ->
        {
            Value delimiter;
            Value hwat;
            if (lv.size() == 1)
            {
                hwat = lv.get(0);
                delimiter = null;
            }
            else if (lv.size() == 2)
            {
                delimiter = lv.get(0);
                hwat = lv.get(1);
            }
            else
            {
                throw new InternalExpressionException("'split' takes 1 or 2 arguments");
            }
            return hwat.split(delimiter);
        });

        expression.addFunction("slice", lv ->
        {
            if (lv.size() != 2 && lv.size() != 3)
            {
                throw new InternalExpressionException("'slice' takes 2 or 3 arguments");
            }
            Value hwat = lv.get(0);
            long from = NumericValue.asNumber(lv.get(1)).getLong();
            Long to = null;
            if (lv.size() == 3)
            {
                to = NumericValue.asNumber(lv.get(2)).getLong();
            }
            return hwat.slice(from, to);
        });

        expression.addFunction("sort", lv ->
        {
            List<Value> toSort = lv;
            if (lv.size() == 1 && lv.get(0) instanceof final ListValue llv)
            {
                toSort = new ArrayList<>(llv.getItems());
            }
            Collections.sort(toSort);
            return ListValue.wrap(toSort);
        });

        // needs lazy cause sort function is reused
        expression.addLazyFunction("sort_key", (c, t, lv) ->  //get working with iterators
        {
            if (lv.isEmpty())
            {
                throw new InternalExpressionException("First argument for 'sort_key' should be a List");
            }
            Value v = lv.get(0).evalValue(c);
            if (!(v instanceof final ListValue list))
            {
                throw new InternalExpressionException("First argument for 'sort_key' should be a List");
            }
            List<Value> toSort = new ArrayList<>(list.getItems());
            if (lv.size() == 1)
            {
                Collections.shuffle(toSort);
                Value ret = ListValue.wrap(toSort);
                return (ct, tt) -> ret;
            }
            LazyValue sortKey = lv.get(1);
            //scoping
            LazyValue defaultVal = c.getVariable("_");
            toSort.sort((v1, v2) -> {
                c.setVariable("_", (cc, tt) -> v1);
                Value ev1 = sortKey.evalValue(c);
                c.setVariable("_", (cc, tt) -> v2);
                Value ev2 = sortKey.evalValue(c);
                return ev1.compareTo(ev2);
            });
            //revering scope
            c.setVariable("_", defaultVal);
            Value ret = ListValue.wrap(toSort);
            return (cc, tt) -> ret;
        });

        expression.addFunction("range", lv ->
        {
            NumericValue from = Value.ZERO;
            NumericValue to;
            NumericValue step = Value.ONE;
            int argsize = lv.size();
            if (argsize == 0 || argsize > 3)
            {
                throw new InternalExpressionException("'range' accepts from 1 to 3 arguments, not " + argsize);
            }
            to = NumericValue.asNumber(lv.get(0));
            if (lv.size() > 1)
            {
                from = to;
                to = NumericValue.asNumber(lv.get(1));
                if (lv.size() > 2)
                {
                    step = NumericValue.asNumber(lv.get(2));
                }
            }
            return (from.isInteger() && to.isInteger() && step.isInteger())
                    ? LazyListValue.rangeLong(from.getLong(), to.getLong(), step.getLong())
                    : LazyListValue.rangeDouble(from.getDouble(), to.getDouble(), step.getDouble());
        });

        expression.addTypedContextFunction("m", -1, Context.MAPDEF, (c, t, lv) ->
                lv.size() == 1 && lv.get(0) instanceof final LazyListValue llv
                        ? new MapValue(llv.unroll())
                        : new MapValue(lv)
        );

        expression.addUnaryFunction("keys", v ->
                v instanceof final MapValue map
                        ? new ListValue(map.getMap().keySet())
                        : Value.NULL
        );

        expression.addUnaryFunction("values", v ->
                v instanceof final MapValue map
                        ? new ListValue(map.getMap().values())
                        : Value.NULL
        );

        expression.addUnaryFunction("pairs", v ->
                v instanceof final MapValue map
                        ? ListValue.wrap(map.getMap().entrySet().stream().map(p -> ListValue.of(p.getKey(), p.getValue())))
                        : Value.NULL);

        expression.addBinaryContextOperator(":", Operators.precedence.get("attribute~:"), true, true, false, (ctx, t, container, address) ->
        {
            if (container instanceof final LContainerValue lcv)
            {
                ContainerValueInterface outerContainer = lcv.container();
                if (outerContainer == null)
                {
                    return LContainerValue.NULL_CONTAINER;
                }
                return outerContainer.get(lcv.address()) instanceof final ContainerValueInterface cvi
                        ? new LContainerValue(cvi, address)
                        : LContainerValue.NULL_CONTAINER;
            }
            if (!(container instanceof final ContainerValueInterface cvi))
            {
                return t == Context.LVALUE ? LContainerValue.NULL_CONTAINER : Value.NULL;
            }
            return t != Context.LVALUE ? cvi.get(address) : new LContainerValue(cvi, address);
        });

        // lazy cause conditional typing - questionable
        expression.addLazyFunction("get", (c, t, lv) ->
        {
            if (lv.isEmpty())
            {
                throw new InternalExpressionException("'get' requires parameters");
            }
            if (lv.size() == 1)
            {
                Value v = lv.get(0).evalValue(c, Context.LVALUE);
                if (!(v instanceof final LContainerValue lcv))
                {
                    return LazyValue.NULL;
                }
                ContainerValueInterface container = lcv.container();
                if (container == null)
                {
                    return LazyValue.NULL;
                }
                Value ret = container.get(lcv.address());
                return (cc, tt) -> ret;
            }
            Value container = lv.get(0).evalValue(c);
            for (int i = 1; i < lv.size(); i++)
            {
                if (!(container instanceof final ContainerValueInterface cvi))
                {
                    return (cc, tt) -> Value.NULL;
                }
                container = cvi.get(lv.get(i).evalValue(c));
            }
            if (container == null)
            {
                return (cc, tt) -> Value.NULL;
            }
            Value finalContainer = container;
            return (cc, tt) -> finalContainer;
        });

        // same as `get`
        expression.addLazyFunction("has", (c, t, lv) ->
        {
            if (lv.isEmpty())
            {
                throw new InternalExpressionException("'has' requires parameters");
            }
            if (lv.size() == 1)
            {
                Value v = lv.get(0).evalValue(c, Context.LVALUE);
                if (!(v instanceof final LContainerValue lcv))
                {
                    return LazyValue.NULL;
                }
                ContainerValueInterface container = lcv.container();
                if (container == null)
                {
                    return LazyValue.NULL;
                }
                Value ret = BooleanValue.of(container.has(lcv.address()));
                return (cc, tt) -> ret;
            }
            Value container = lv.get(0).evalValue(c);
            for (int i = 1; i < lv.size() - 1; i++)
            {
                if (!(container instanceof final ContainerValueInterface cvi))
                {
                    return LazyValue.NULL;
                }
                container = cvi.get(lv.get(i).evalValue(c));
            }
            if (!(container instanceof final ContainerValueInterface cvi))
            {
                return LazyValue.NULL;
            }
            Value ret = BooleanValue.of(cvi.has(lv.get(lv.size() - 1).evalValue(c)));
            return (cc, tt) -> ret;
        });

        // same as `get`
        expression.addLazyFunction("put", (c, t, lv) ->
        {
            if (lv.size() < 2)
            {
                throw new InternalExpressionException("'put' takes at least three arguments, a container, address, and values to insert at that index");
            }
            Value container = lv.get(0).evalValue(c, Context.LVALUE);
            if (container instanceof final LContainerValue lcv)
            {
                ContainerValueInterface internalContainer = lcv.container();
                if (internalContainer == null)
                {
                    return LazyValue.NULL;
                }
                Value address = lcv.address();
                Value what = lv.get(1).evalValue(c);
                Value retVal = BooleanValue.of((lv.size() > 2)
                        ? internalContainer.put(address, what, lv.get(2).evalValue(c))
                        : internalContainer.put(address, what));
                return (cc, tt) -> retVal;

            }
            if (lv.size() < 3)
            {
                throw new InternalExpressionException("'put' takes at least three arguments, a container, address, and values to insert at that index");
            }
            if (!(container instanceof final ContainerValueInterface cvi))
            {
                return LazyValue.NULL;
            }
            Value where = lv.get(1).evalValue(c);
            Value what = lv.get(2).evalValue(c);
            Value retVal = BooleanValue.of((lv.size() > 3)
                    ? cvi.put(where, what, lv.get(3).evalValue(c))
                    : cvi.put(where, what));
            return (cc, tt) -> retVal;
        });

        // same as `get`
        expression.addLazyFunction("delete", (c, t, lv) ->
        {
            if (lv.isEmpty())
            {
                throw new InternalExpressionException("'delete' requires parameters");
            }
            if (lv.size() == 1)
            {
                Value v = lv.get(0).evalValue(c, Context.LVALUE);
                if (!(v instanceof final LContainerValue lcv))
                {
                    return LazyValue.NULL;
                }
                ContainerValueInterface container = lcv.container();
                if (container == null)
                {
                    return LazyValue.NULL;
                }
                Value ret = BooleanValue.of(container.delete(lcv.address()));
                return (cc, tt) -> ret;
            }
            Value container = lv.get(0).evalValue(c);
            for (int i = 1; i < lv.size() - 1; i++)
            {
                if (!(container instanceof final ContainerValueInterface cvi))
                {
                    return LazyValue.NULL;
                }
                container = cvi.get(lv.get(i).evalValue(c));
            }
            if (!(container instanceof final ContainerValueInterface cvi))
            {
                return LazyValue.NULL;
            }
            Value ret = BooleanValue.of(cvi.delete(lv.get(lv.size() - 1).evalValue(c)));
            return (cc, tt) -> ret;
        });

        expression.addUnaryFunction("encode_b64", v -> StringValue.of(Base64.getEncoder().encodeToString(v.getString().getBytes(StandardCharsets.UTF_8))));
        expression.addUnaryFunction("decode_b64", v -> {
            try
            {
                return StringValue.of(new String(Base64.getDecoder().decode(v.getString()), StandardCharsets.UTF_8));
            }
            catch (IllegalArgumentException iae)
            {
                throw new ThrowStatement("Invalid b64 string: " + v.getString(), Throwables.B64_ERROR);
            }
        });

        expression.addUnaryFunction("encode_json", v -> StringValue.of(v.toJson().toString()));
        expression.addUnaryFunction("decode_json", v -> {
            try
            {
                return Auxiliary.GSON.fromJson(v.getString(), Value.class);
            }
            catch (JsonParseException jpe)
            {
                throw new ThrowStatement("Invalid json string: " + v.getString(), Throwables.JSON_ERROR);
            }
        });
    }
}
