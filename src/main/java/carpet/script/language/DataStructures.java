package carpet.script.language;

import carpet.script.Context;
import carpet.script.Expression;
import carpet.script.Fluff;
import carpet.script.LazyValue;
import carpet.script.ReferenceArray;
import carpet.script.Tokenizer.Token;
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

public class DataStructures {
    public static void apply(Expression expression)
    {
        // custom because variable only lists have special interactions with assignment operators 
        expression.addCustomFunction("l", new Fluff.AbstractFunction(-1, "l") {
            @Override
            public Value eval(List<Value> lv) {
                if (lv.size() == 1 && lv.get(0) instanceof LazyListValue)
                    return ListValue.wrap(((LazyListValue) lv.get(0)).unroll());
                return new ListValue(lv); // copies list, given the passed one may not be mutable
            }

            @Override
            public LazyValue createExecutable(Context compilationContext, Expression expr, Token token, List<LazyValue> params) {
                boolean allVariables = params.stream().allMatch(lv -> (lv instanceof LazyValue.Variable));
                if (allVariables) {
                    return new ReferenceArray(params.stream().map(lv -> ((LazyValue.Variable)lv).name()).toArray(String[]::new), expr);
                }
                return super.createExecutable(compilationContext, expr, token, params);
            }
        });

        expression.addFunction("join", (lv) ->
        {
            if (lv.size() < 2)
                throw new InternalExpressionException("'join' takes at least 2 arguments");
            String delimiter = lv.get(0).getString();
            List<Value> toJoin;
            if (lv.size()==2 && lv.get(1) instanceof LazyListValue)
            {
                toJoin = ((LazyListValue) lv.get(1)).unroll();

            }
            else if (lv.size() == 2 && lv.get(1) instanceof ListValue)
            {
                toJoin = new ArrayList<>(((ListValue)lv.get(1)).getItems());
            }
            else
            {
                toJoin = lv.subList(1,lv.size());
            }
            return new StringValue(toJoin.stream().map(Value::getString).collect(Collectors.joining(delimiter)));
        });

        expression.addFunction("split", (lv) ->
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

        expression.addFunction("slice", (lv) ->
        {

            if (lv.size() != 2 && lv.size() != 3)
                throw new InternalExpressionException("'slice' takes 2 or 3 arguments");
            Value hwat = lv.get(0);
            long from = NumericValue.asNumber(lv.get(1)).getLong();
            Long to = null;
            if (lv.size()== 3)
                to = NumericValue.asNumber(lv.get(2)).getLong();
            return hwat.slice(from, to);
        });

        expression.addFunction("sort", (lv) ->
        {
            List<Value> toSort = lv;
            if (lv.size()==1 && lv.get(0) instanceof ListValue)
            {
                toSort = new ArrayList<>(((ListValue)lv.get(0)).getItems());
            }
            Collections.sort(toSort);
            return ListValue.wrap(toSort);
        });

        // needs lazy cause sort function is reused
        expression.addLazyFunction("sort_key", (c, t, lv) ->  //get working with iterators
        {
            if (lv.size() == 0)
                throw new InternalExpressionException("First argument for 'sort_key' should be a List");
            Value v = lv.get(0).evalValue(c);
            if (!(v instanceof ListValue))
                throw new InternalExpressionException("First argument for 'sort_key' should be a List");
            List<Value> toSort = new ArrayList<>(((ListValue) v).getItems());
            if (lv.size()==1)
            {
                Collections.shuffle(toSort);
                return ListValue.wrap(toSort);
            }
            LazyValue sortKey = lv.get(1);
            //scoping
            Value __ = c.getVariable("_");
            Collections.sort(toSort,(v1, v2) -> {
                c.setVariable("_", v1);
                Value ev1 = sortKey.evalValue(c);
                c.setVariable("_", v2);
                Value ev2 = sortKey.evalValue(c);
                return ev1.compareTo(ev2);
            });
            //revering scope
            c.setVariable("_", __);
            return ListValue.wrap(toSort);
        });

        expression.addFunction("range", (lv) ->
        {
            NumericValue from = Value.ZERO;
            NumericValue to;
            NumericValue step = Value.ONE;
            int argsize = lv.size();
            if (argsize == 0 || argsize > 3)
                throw new InternalExpressionException("'range' accepts from 1 to 3 arguments, not "+argsize);
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
        {
            Value ret;
            if (lv.size() == 1 && lv.get(0) instanceof LazyListValue)
                ret = new MapValue(((LazyListValue) lv.get(0)).unroll());
            else
                ret = new MapValue(lv);
            return ret;
        });

        expression.addUnaryFunction("keys", v ->
        {
            if (v instanceof MapValue)
                return new ListValue(((MapValue) v).getMap().keySet());
            return Value.NULL;
        });

        expression.addUnaryFunction("values", v ->
        {
            if (v instanceof MapValue)
                return new ListValue(((MapValue) v).getMap().values());
            return Value.NULL;
        });

        expression.addUnaryFunction("pairs", v ->
        {
            if (v instanceof MapValue)
                return ListValue.wrap(((MapValue) v).getMap().entrySet().stream().map(
                        (p) -> ListValue.of(p.getKey(), p.getValue())
                ).collect(Collectors.toList()));
            return Value.NULL;
        });

        expression.addBinaryContextOperator(":", Operators.precedence.get("attribute~:"),true, true, false, (ctx, t, container, address) ->
        {
            if (container instanceof LContainerValue)
            {
                ContainerValueInterface outerContainer = ((LContainerValue) container).getContainer();
                if (outerContainer == null) return LContainerValue.NULL_CONTAINER;
                Value innerContainer = outerContainer.get(((LContainerValue) container).getAddress());
                if (!(innerContainer instanceof  ContainerValueInterface)) return LContainerValue.NULL_CONTAINER;
                return new LContainerValue((ContainerValueInterface) innerContainer, address);
            }
            if (!(container instanceof ContainerValueInterface))
                return t == Context.LVALUE ? LContainerValue.NULL_CONTAINER : Value.NULL;
            if (t != Context.LVALUE) return ((ContainerValueInterface) container).get(address);
            return new LContainerValue((ContainerValueInterface) container, address);
        });

        // lazy cause conditional typing - questionable
        expression.addLazyFunction("get", (c, t, lv) ->
        {
            if (lv.size() == 0)
                throw new InternalExpressionException("'get' requires parameters");
            if (lv.size() == 1)
            {
                Value v = lv.get(0).evalValue(c, Context.LVALUE);
                if (!(v  instanceof LContainerValue))
                    return Value.NULL;
                ContainerValueInterface container = ((LContainerValue) v).getContainer();
                if (container == null)
                    return Value.NULL;
                return container.get(((LContainerValue) v).getAddress());
            }
            Value container = lv.get(0).evalValue(c);
            for (int i = 1; i < lv.size(); i++)
            {
                if (!(container instanceof ContainerValueInterface)) return Value.NULL;
                container = ((ContainerValueInterface) container).get(lv.get(i).evalValue(c));
            }
            if (container == null)
                return Value.NULL;
            return container;
        });

        // same as `get`
        expression.addLazyFunction("has", (c, t, lv) ->
        {
            if (lv.size() == 0)
                throw new InternalExpressionException("'has' requires parameters");
            if (lv.size() == 1)
            {
                Value v = lv.get(0).evalValue(c, Context.LVALUE);
                if (!(v  instanceof LContainerValue))
                    return Value.NULL;
                ContainerValueInterface container = ((LContainerValue) v).getContainer();
                if (container == null)
                    return Value.NULL;
                return BooleanValue.of(container.has(((LContainerValue) v).getAddress()));
            }
            Value container = lv.get(0).evalValue(c);
            for (int i = 1; i < lv.size()-1; i++)
            {
                if (!(container instanceof ContainerValueInterface)) return Value.NULL;
                container = ((ContainerValueInterface) container).get(lv.get(i).evalValue(c));
            }
            if (!(container instanceof ContainerValueInterface))
                return Value.NULL;
            return BooleanValue.of(((ContainerValueInterface) container).has(lv.get(lv.size()-1).evalValue(c)));
        });

        // same as `get`
        expression.addLazyFunction("put", (c, t, lv) ->
        {
            if(lv.size()<2)
            {
                throw new InternalExpressionException("'put' takes at least three arguments, a container, address, and values to insert at that index");
            }
            Value container = lv.get(0).evalValue(c, Context.LVALUE);
            if (container instanceof LContainerValue)
            {
                ContainerValueInterface internalContainer = ((LContainerValue) container).getContainer();
                if (internalContainer == null)
                {
                    return Value.NULL;
                }
                Value address = ((LContainerValue) container).getAddress();
                Value what = lv.get(1).evalValue(c);
                Value retVal = BooleanValue.of((lv.size() > 2)
                        ? internalContainer.put(address, what, lv.get(2).evalValue(c))
                        : internalContainer.put(address, what));
                return retVal;

            }
            if(lv.size()<3)
            {
                throw new InternalExpressionException("'put' takes at least three arguments, a container, address, and values to insert at that index");
            }
            if (!(container instanceof ContainerValueInterface))
            {
                return Value.NULL;
            }
            Value where = lv.get(1).evalValue(c);
            Value what = lv.get(2).evalValue(c);
            Value retVal = BooleanValue.of((lv.size()>3)
                    ? ((ContainerValueInterface) container).put(where, what, lv.get(3).evalValue(c))
                    : ((ContainerValueInterface) container).put(where, what));
            return retVal;
        });

        // same as `get`
        expression.addLazyFunction("delete", (c, t, lv) ->
        {
            if (lv.size() == 0)
                throw new InternalExpressionException("'delete' requires parameters");
            if (lv.size() == 1)
            {
                Value v = lv.get(0).evalValue(c, Context.LVALUE);
                if (!(v  instanceof LContainerValue))
                    return Value.NULL;
                ContainerValueInterface container = ((LContainerValue) v).getContainer();
                if (container == null)
                    return Value.NULL;
                return BooleanValue.of(container.delete(((LContainerValue) v).getAddress()));
            }
            Value container = lv.get(0).evalValue(c);
            for (int i = 1; i < lv.size()-1; i++)
            {
                if (!(container instanceof ContainerValueInterface)) return Value.NULL;
                container = ((ContainerValueInterface) container).get(lv.get(i).evalValue(c));
            }
            if (!(container instanceof ContainerValueInterface))
                return Value.NULL;
            return BooleanValue.of(((ContainerValueInterface) container).delete(lv.get(lv.size()-1).evalValue(c)));
        });

        expression.addUnaryFunction("encode_b64", v -> StringValue.of(Base64.getEncoder().encodeToString(v.getString().getBytes(StandardCharsets.UTF_8))));
        expression.addUnaryFunction("decode_b64", v -> {
            try {
                return StringValue.of(new String(Base64.getDecoder().decode(v.getString()), StandardCharsets.UTF_8));
            } catch (IllegalArgumentException iae){
                throw new ThrowStatement("Invalid b64 string: " + v.getString(), Throwables.B64_ERROR);
            }
        });

        expression.addUnaryFunction("encode_json", v -> StringValue.of(v.toJson().toString()));
        expression.addUnaryFunction("decode_json", v -> {
            try {
                return Auxiliary.GSON.fromJson(v.getString(), Value.class);
            } catch (JsonParseException jpe){
                throw new ThrowStatement("Invalid json string: " + v.getString(), Throwables.JSON_ERROR);
            }
        });
    }
}
